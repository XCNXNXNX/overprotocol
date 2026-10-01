package dev.overprotocol.vehicle;

import dev.overprotocol.block.CeremonialChairBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible one-person anchor. The chair remains a normal block and owns its own drop. */
public final class CeremonialSeatEntity extends Entity {
    public CeremonialSeatEntity(EntityType<? extends CeremonialSeatEntity> type,Level level) {
        super(type,level);noPhysics=true;setNoGravity(true);setInvulnerable(true);
    }
    public BlockPos anchor() { return BlockPos.containing(getX(),getY()+.001,getZ()); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof Player && getPassengers().isEmpty(); }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position().add(0,.625,0); }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && (!(level().getBlockState(anchor()).getBlock() instanceof CeremonialChairBlock)
            || getPassengers().isEmpty())) { ejectPassengers();discard(); }
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        var facing=Direction.fromYRot(getYRot());
        for(var direction:new Direction[]{facing,facing.getClockWise(),facing.getCounterClockWise(),facing.getOpposite()}) {
            var pos=anchor().relative(direction);
            var place=Vec3.atBottomCenterOf(pos);
            var box=new AABB(place.x-.3,place.y,place.z-.3,place.x+.3,place.y+1.8,place.z+.3);
            if(Block.canSupportCenter(level(),pos.below(),Direction.UP) && level().noCollision(passenger,box)) return place;
        }
        return Vec3.atBottomCenterOf(anchor().above());
    }
}
