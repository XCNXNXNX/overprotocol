package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.CeremonialSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CeremonialChairBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CeremonialChairBlock> CODEC=simpleCodec(CeremonialChairBlock::new);
    private static final VoxelShape[] SHAPES=shapes();
    public CeremonialChairBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)); }
    @Override public MapCodec<CeremonialChairBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.isSpectator() || player.isSecondaryUseActive() || player.isPassenger()
            || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if(!level.getEntitiesOfClass(CeremonialSeatEntity.class,new AABB(pos).inflate(.05),seat->seat.anchor().equals(pos)).isEmpty()) {
            if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.overprotocol.chair.occupied"),true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Check the occupant's head room without treating the chair itself as an obstruction.
        if(level.getBlockCollisions(player,new AABB(pos.getX()+.2,pos.getY()+1,pos.getZ()+.2,
            pos.getX()+.8,pos.getY()+1.85,pos.getZ()+.8)).iterator().hasNext()) {
            if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.overprotocol.chair.space"),true);
            return InteractionResult.FAIL;
        }
        if(!level.isClientSide) {
            var seat=ModContent.CEREMONIAL_SEAT.get().create(level);
            if(seat==null)return InteractionResult.FAIL;
            seat.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);seat.setYRot(state.getValue(FACING).toYRot());
            if(!level.addFreshEntity(seat))return InteractionResult.FAIL;
            if(!player.startRiding(seat)) {seat.discard();return InteractionResult.FAIL;}
            seat.positionRider(player);player.setYRot(seat.getYRot());player.setYHeadRot(seat.getYRot());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static VoxelShape[] shapes() {
        var result=new VoxelShape[4];
        var original=Shapes.or(box(2,8,2,14,10,14),box(2,0,2,4,8,4),box(12,0,2,14,8,4),
            box(2,0,12,4,16,14),box(12,0,12,14,16,14),box(4,10,12,12,16,14),
            box(2,10,3,4,13,12),box(12,10,3,14,13,12));
        for(var direction:Direction.Plane.HORIZONTAL) {
            int turns=(direction.get2DDataValue()+2)%4;
            var shape=original;
            for(int i=0;i<turns;i++) {
                var rotated=new VoxelShape[]{Shapes.empty()};
                shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->rotated[0]=Shapes.or(rotated[0],Shapes.box(1-z2,y1,x1,1-z1,y2,x2)));
                shape=rotated[0];
            }
            result[direction.get2DDataValue()]=shape.optimize();
        }
        return result;
    }
}
