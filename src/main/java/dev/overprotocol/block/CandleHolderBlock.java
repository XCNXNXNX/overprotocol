package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.*;

public final class CandleHolderBlock extends AbstractCandleBlock {
    public static final MapCodec<CandleHolderBlock> CODEC = simpleCodec(CandleHolderBlock::new);
    // 0 = empty; 1 = undyed; 2..17 = vanilla DyeColor IDs + 2.
    public static final IntegerProperty CANDLE = IntegerProperty.create("candle", 0, 17);
    private static final VoxelShape EMPTY = Shapes.or(box(4, 0, 4, 12, 2, 12),
        box(7, 2, 7, 9, 8, 9), box(5, 8, 5, 11, 10, 11));
    private static final VoxelShape FILLED = Shapes.or(EMPTY, box(7, 10, 7, 9, 16, 9));
    public CandleHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CANDLE, 0).setValue(LIT, false));
    }
    @Override public MapCodec<CandleHolderBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CANDLE, LIT);
    }
    public static Item candleItem(int index) {
        if (index == 0) return Items.AIR;
        if (index == 1) return Items.CANDLE;
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(DyeColor.byId(index - 2).getName() + "_candle"));
    }
    public static int candleIndex(ItemStack stack) {
        for (int index = 1; index <= 17; index++) if (stack.is(candleItem(index))) return index;
        return 0;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return ItemInteractionResult.FAIL;
        int index = candleIndex(stack);
        if (index != 0) {
            if (state.getValue(CANDLE) == 0 && !level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(CANDLE, index).setValue(LIT, false));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.CANDLE_PLACE, SoundSource.BLOCKS, 1, 1);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT) && !canBeLit(state))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild || state.getValue(CANDLE) == 0) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                var candle = new ItemStack(candleItem(state.getValue(CANDLE)));
                level.setBlockAndUpdate(pos, state.setValue(CANDLE, 0).setValue(LIT, false));
                if (!player.getInventory().add(candle)) player.drop(candle, false);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(LIT)) {
            if (!level.isClientSide) extinguish(player, state, level, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public BlockState getToolModifiedState(BlockState state, UseOnContext context,
            ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.FIRESTARTER_LIGHT && context.getItemInHand().canPerformAction(ability))
            return canBeLit(state) ? state.setValue(LIT, true) : null;
        return super.getToolModifiedState(state, context, ability, simulate);
    }
    @Override protected boolean canBeLit(BlockState state) { return state.getValue(CANDLE) > 0 && !state.getValue(LIT); }
    @Override protected Iterable<Vec3> getParticleOffsets(BlockState state) {
        return state.getValue(CANDLE) == 0 ? List.of() : List.of(new Vec3(.5, 1.0625, .5));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CANDLE) == 0 ? EMPTY : FILLED;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
