package dev.overprotocol.vehicle;

import dev.overprotocol.item.RedCarpetRollItem;
import dev.overprotocol.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import java.util.List;

/** A three-metre-wide carpet roll. Each interaction spends exactly one metre, on the server. */
public final class RedCarpetRollEntity extends Entity {
    public static final int LENGTH = 30;
    public static final float HALF_WIDTH = 1.5F;
    public static final float BACK_EDGE = -.5F;
    private static final EntityDataAccessor<Integer> REMAINING = SynchedEntityData.defineId(RedCarpetRollEntity.class, EntityDataSerializers.INT);

    public RedCarpetRollEntity(EntityType<? extends RedCarpetRollEntity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
        noPhysics = true;
        setNoGravity(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(REMAINING, LENGTH); }
    public int remaining() { return entityData.get(REMAINING); }
    public void setRemaining(int value) {
        entityData.set(REMAINING, Mth.clamp(value, 1, LENGTH));
        setBoundingBox(makeBoundingBox());
    }
    public static float radius(int remaining) { return (float)Math.sqrt(.065*.065 + remaining * .012 / Math.PI); }
    public Direction facing() { return Direction.fromYRot(getYRot()); }
    public BlockPos anchor() { return BlockPos.containing(getX(), getY()+.001, getZ()); }
    public static List<BlockPos> row(BlockPos centre, Direction facing) {
        var across = facing.getClockWise();
        return List.of(centre.relative(across,-1), centre, centre.relative(across));
    }
    public static boolean rowSupported(Level level, BlockPos centre, Direction facing) {
        for (var p:row(centre,facing)) if (!level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p)
            || !Block.canSupportCenter(level,p.below(),Direction.UP)) return false;
        return true;
    }
    public static boolean rowAvailable(Level level, BlockPos centre, Direction facing, Player player, ItemStack stack) {
        if (level.isOutsideBuildHeight(centre) || !rowSupported(level,centre,facing)) return false;
        for (var p:row(centre,facing)) {
            var state=level.getBlockState(p);
            if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || !level.mayInteract(player,p)
                || !player.mayUseItemAt(p,Direction.UP,stack)) return false;
        }
        return true;
    }
    public static AABB bounds(BlockPos centre, Direction facing, float radius) {
        double x=centre.getX()+.5, z=centre.getZ()+.5;
        boolean eastWest=facing.getAxis()==Direction.Axis.X;
        return new AABB(x-(eastWest?.5:HALF_WIDTH),centre.getY(),z-(eastWest?HALF_WIDTH:.5),
            x+(eastWest?.5:HALF_WIDTH),centre.getY()+radius*2,z+(eastWest?HALF_WIDTH:.5));
    }
    @Override protected AABB makeBoundingBox() { return bounds(anchor(),facing(),radius(remaining())); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(REMAINING)) setBoundingBox(makeBoundingBox());
    }
    @Override public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
        if (!level().isClientSide && tickCount%20==0 && !rowSupported(level(),anchor(),facing())) {
            spawnAtLocation(getPickResult()); discard();
        }
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (isRemoved() || !player.getAbilities().mayBuild || player.isSpectator()) return InteractionResult.FAIL;
        if (player.isSecondaryUseActive()) {
            if (hand!=InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
                return InteractionResult.FAIL;
            return rewindOrPack(player);
        }
        var pos=anchor(); var next=pos.relative(facing()); var item=getPickResult();
        boolean clear=rowAvailable(level(),pos,facing(),player,item);
        if (remaining()>1) clear=clear && rowAvailable(level(),next,facing(),player,item)
            && level().getEntities(this,bounds(next,facing(),radius(remaining()-1)),Entity::canBeCollidedWith).isEmpty();
        if (!clear) {
            if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.overprotocol.red_carpet_roll.blocked"),true);
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide) {
            // Preflight the complete row before any write; notify neighbours only after all three cells exist.
            var carpet=ModContent.RED_RUNNER.get().defaultBlockState();
            for (var p:row(pos,facing())) level().setBlock(p,carpet,Block.UPDATE_CLIENTS);
            for (var p:row(pos,facing())) level().updateNeighborsAt(p,carpet.getBlock());
            level().playSound(null,pos,SoundEvents.WOOL_PLACE,SoundSource.BLOCKS,.6F,1F);
            if (remaining()==1) discard();
            else {
                setRemaining(remaining()-1);
                setPos(next.getX()+.5,next.getY(),next.getZ()+.5);
                hasImpulse=true;
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private InteractionResult rewindOrPack(Player player) {
        if (remaining()==LENGTH) {
            if (!level().isClientSide) {
                var item=getPickResult();
                if (!player.getInventory().add(item)) player.drop(item,false);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        var previous=anchor().relative(facing().getOpposite());
        var item=getPickResult();
        boolean clear=!level().isOutsideBuildHeight(previous) && rowSupported(level(),previous,facing());
        for (var p:row(previous,facing())) clear=clear && level().getBlockState(p).is(ModContent.RED_RUNNER.get())
            && level().mayInteract(player,p) && player.mayUseItemAt(p,Direction.UP,item);
        clear=clear && level().getEntities(this,bounds(previous,facing(),radius(remaining()+1)),Entity::canBeCollidedWith).isEmpty();
        if (!clear) {
            if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.overprotocol.red_carpet_roll.rewind_blocked"),true);
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide) {
            // Consume all three carpet cells without drops before crediting one metre to the roll.
            for (var p:row(previous,facing())) level().setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
            for (var p:row(previous,facing())) level().updateNeighborsAt(p,ModContent.RED_RUNNER.get());
            level().playSound(null,previous,SoundEvents.WOOL_PLACE,SoundSource.BLOCKS,.6F,.85F);
            setRemaining(remaining()+1);
            setPos(previous.getX()+.5,previous.getY(),previous.getZ()+.5);
            hasImpulse=true;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if (isRemoved()) return false;
        if (source.getEntity() instanceof Player player && !player.getAbilities().mayBuild) return false;
        if (!level().isClientSide) {
            if (!(source.getEntity() instanceof Player player && player.getAbilities().instabuild)) spawnAtLocation(getPickResult());
            discard();
        }
        return true;
    }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public ItemStack getPickResult() {
        var item=RedCarpetRollItem.withRemaining(remaining());
        if (hasCustomName()) item.set(DataComponents.CUSTOM_NAME,getCustomName());
        return item;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("Remaining",remaining()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { setRemaining(tag.contains("Remaining")?tag.getInt("Remaining"):LENGTH); }
}
