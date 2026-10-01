package dev.overprotocol.vehicle;

import dev.overprotocol.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** A persistent two-seat display aircraft. Movement is simulated by the server. */
public final class B2Entity extends Entity {
    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(B2Entity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(B2Entity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BANK = SynchedEntityData.defineId(B2Entity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> GEAR = SynchedEntityData.defineId(B2Entity.class, EntityDataSerializers.BOOLEAN);
    private int controls;
    private float steering, elevator;
    private long lastInput = Long.MIN_VALUE, lastGear = Long.MIN_VALUE;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw, lerpPitch;
    public float previousBank;
    private float gearAmount=1,previousGearAmount=1;
    public float gearAmount(float partial) { return Mth.lerp(partial,previousGearAmount,gearAmount); }

    public B2Entity(EntityType<? extends B2Entity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(THROTTLE, 0F); builder.define(SPEED, 0F);
        builder.define(BANK, 0F); builder.define(GEAR, true);
    }
    public float throttle() { return entityData.get(THROTTLE); }
    public float speed() { return entityData.get(SPEED); }
    public float bank() { return entityData.get(BANK); }
    public boolean gearDown() { return entityData.get(GEAR); }

    /** No position/velocity is accepted from the client, and only the first seat can send controls. */
    public boolean acceptControls(Player sender, int input) {
        return acceptControls(sender, input, 0, 0);
    }
    public boolean acceptControls(Player sender, int input, float steering, float elevator) {
        return acceptControls(sender, input, steering, elevator, 0);
    }
    public boolean acceptControls(Player sender, int input, float steering, float elevator, float scroll) {
        if (level().isClientSide || getControllingPassenger() != sender || (input & ~B2Flight.VALID_INPUT) != 0
            || !B2Flight.validAxis(steering) || !B2Flight.validAxis(elevator) || !B2Flight.validScroll(scroll)) return false;
        long now = level().getGameTime();
        if ((input & B2Flight.GEAR) != 0 && (controls & B2Flight.GEAR) == 0 && !onGround()
            && (lastGear == Long.MIN_VALUE || now - lastGear >= 10)) {
            entityData.set(GEAR, !gearDown());
            lastGear = now;
        }
        controls = input;
        this.steering = steering;
        this.elevator = elevator;
        entityData.set(THROTTLE, B2Flight.scrollThrottle(throttle(), scroll));
        lastInput = now;
        return true;
    }

    @Override public void tick() {
        super.tick();
        previousBank = bank();
        previousGearAmount=gearAmount;
        gearAmount=Mth.approach(gearAmount,gearDown()?1:0,.035F);
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(getX() + (lerpX - getX()) / lerpSteps, getY() + (lerpY - getY()) / lerpSteps,
                    getZ() + (lerpZ - getZ()) / lerpSteps);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps);
                setXRot(getXRot() + (lerpPitch - getXRot()) / lerpSteps);
                lerpSteps--;
            }
            return;
        }
        boolean pilot = getControllingPassenger() instanceof Player;
        if (!pilot || lastInput == Long.MIN_VALUE || level().getGameTime() - lastInput > 20) {
            controls = 0; steering = elevator = 0;
        }
        var step = B2Flight.step(throttle(), speed(), -getXRot(), getYRot(), bank(), getDeltaMovement().y,
            controls, steering, elevator, onGround(), pilot && lastInput != Long.MIN_VALUE && level().getGameTime() - lastInput <= 20, gearDown());
        float oldYaw = getYRot();
        setYRot(step.yaw()); setXRot(-step.pitch());
        entityData.set(THROTTLE, step.throttle()); entityData.set(SPEED, step.speed()); entityData.set(BANK, step.bank());
        // Check the swept wing/nose envelope as well as the ordinary fuselage collider.
        Vec3 motion = new Vec3(-Math.sin(Math.toRadians(getYRot())) * step.speed(), step.vertical(),
            Math.cos(Math.toRadians(getYRot())) * step.speed());
        if (!airframeClear(motion)) {
            setYRot(oldYaw);
            entityData.set(SPEED, 0F); entityData.set(THROTTLE, 0F); entityData.set(BANK, 0F);
            motion = new Vec3(0, Math.min(0, motion.y), 0);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision) { entityData.set(SPEED, 0F); entityData.set(THROTTLE, 0F); }
        if (onGround()) {
            entityData.set(GEAR, true);
            setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        }
        hasImpulse = true;
    }

    /** Overlapping cells cover the full-size swept wing and nose; movement extends their bounds. */
    public boolean airframeClear(Vec3 motion) {
        for (int x = -24; x <= 24; x += 4) {
            double front = B2Geometry.leading(x),rear=B2Geometry.trailing(x);
            int rows=Math.max(1,(int)Math.ceil((rear-front)/3));
            for (int row=0;row<=rows;row++) {
                double z=front+(rear-front)*row/rows;
                var center = localToWorld(x, 2.55, z);
                var box = new AABB(center.x - 2.1, center.y - .45, center.z - 2.1,
                    center.x + 2.1, center.y + .65, center.z + 2.1).expandTowards(motion);
                if (!level().getWorldBorder().isWithinBounds(box) || !level().hasChunkAt(BlockPos.containing(center))
                    || !level().hasChunkAt(BlockPos.containing(center.add(motion)))
                    || level().getBlockCollisions(this, box).iterator().hasNext()) return false;
            }
        }
        return true;
    }

    public Vec3 localToWorld(double x, double y, double z) {
        double yaw = Math.toRadians(getYRot());
        // The mesh's negative Z is the nose; vanilla yaw 0 faces positive world Z.
        return position().add(-x * Math.cos(yaw) + z * Math.sin(yaw), y,
            -x * Math.sin(yaw) - z * Math.cos(yaw));
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (isRemoved()) return InteractionResult.FAIL;
        if (player.isSecondaryUseActive()) {
            if (!getPassengers().isEmpty() || !onGround() || speed() > .06F
                || !player.getAbilities().mayBuild) return InteractionResult.PASS;
            if (!level().isClientSide) {
                var item = getPickResult();
                if (!player.getInventory().add(item)) player.drop(item, false);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSpectator() || getPassengers().size() >= 2) return InteractionResult.FAIL;
        if (!level().isClientSide) {
            if (!player.startRiding(this)) return InteractionResult.FAIL;
            player.displayClientMessage(Component.translatable("message.overprotocol.b2.controls"), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }
    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof Player && getPassengers().size() < 2; }
    @Override protected void removePassenger(Entity passenger) {
        boolean pilot = passenger == getControllingPassenger();
        super.removePassenger(passenger);
        if (pilot) { controls = 0; steering = elevator = 0; lastInput = Long.MIN_VALUE; entityData.set(THROTTLE, 0F); }
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) {
        int index = getPassengers().indexOf(passenger);
        var offset = new Vec3(index == 0 ? -.65 : .65, .38, -6.7)
            .zRot((float)Math.toRadians(-bank())).xRot((float)Math.toRadians(getXRot()))
            .yRot((float)Math.toRadians(180 - getYRot()));
        return position().add(offset).add(0,B2Geometry.PIVOT_Y,0);
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Exit behind the cockpit on top of the wing, away from wheels and the fuselage collider.
        return localToWorld(4.5, 3.0, -2.0);
    }
    @Override public boolean isControlledByLocalInstance() { return !level().isClientSide; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canCollideWith(Entity other) { return !isPassengerOfSameVehicle(other) && other.canBeCollidedWith(); }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(27, 8, 27); }
    @Override public ItemStack getPickResult() {
        var item = ModContent.B2_ITEM.get().getDefaultInstance();
        if (hasCustomName()) item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        return item;
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYaw = yaw; lerpPitch = pitch; lerpSteps = Math.max(1, steps);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("GearDown", gearDown());
        // A parked/reloaded display must never resume a stale pilot's throttle.
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(GEAR, !tag.contains("GearDown") || tag.getBoolean("GearDown"));
        entityData.set(THROTTLE, 0F); entityData.set(SPEED, 0F); entityData.set(BANK, 0F);
        controls = 0; steering = elevator = 0; lastInput = Long.MIN_VALUE;
        setDeltaMovement(Vec3.ZERO);
    }
}
