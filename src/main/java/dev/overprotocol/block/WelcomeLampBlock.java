package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WelcomeLampBlock extends Block {
    public static final MapCodec<WelcomeLampBlock> CODEC=simpleCodec(WelcomeLampBlock::new);
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    private static final VoxelShape SHAPE=Shapes.or(box(4,0,4,12,2,12),box(7,2,7,9,10,9),box(4,10,4,12,16,12));
    public WelcomeLampBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(LIT,true));}
    @Override public MapCodec<WelcomeLampBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(LIT);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPE;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty() || !player.getAbilities().mayBuild
            || player.isSpectator() || !level.mayInteract(player,pos))return InteractionResult.PASS;
        if(!level.isClientSide)level.setBlockAndUpdate(pos,state.cycle(LIT));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
