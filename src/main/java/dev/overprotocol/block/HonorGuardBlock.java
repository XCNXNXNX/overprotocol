package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import dev.overprotocol.client.ClientScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A two-block-tall ceremonial guard figure. Both halves carry the same facing and pose, so the
 * statue can be read from either half. Right clicking with an empty hand cycles the pose and
 * sneaking while right clicking turns the guard on the spot.
 */
public final class HonorGuardBlock extends Block implements EntityBlock {
    public static final MapCodec<HonorGuardBlock> CODEC = simpleCodec(HonorGuardBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<GuardPose> POSE = EnumProperty.create("pose", GuardPose.class);

    private static final VoxelShape LOWER_SHAPE = box(0.9, 0.0, 4.2, 15.1, 16.0, 11.0);
    private static final VoxelShape UPPER_SHAPE = box(0.8, 0.0, 3.0, 15.2, 16.0, 12.2);
    /** Vanilla level event used when a creative player silently removes the other half. */
    private static final int DESTROY_PARTICLES = 2001;

    public HonorGuardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(FACING, Direction.SOUTH)
            .setValue(POSE, GuardPose.ATTENTION));
    }

    @Override
    public MapCodec<HonorGuardBlock> codec() {
        return CODEC;
    }

    /** The figure is drawn by {@link dev.overprotocol.client.HonorGuardRenderer}, never by a baked model. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new HonorGuardBlockEntity(pos, state) : null;
    }

    /**
     * The half that owns the block entity. Only the lower half has one, so anything that needs the
     * statue's data - the renderer, the control panel, the network handlers - has to look here and
     * not at whichever half the player happened to click.
     */
    public static BlockPos ownerPos(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HonorGuardBlock)) return pos;
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    /** Returns the block entity that owns the statue, whichever half was clicked. */
    private static HonorGuardBlockEntity guardAt(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(ownerPos(level, pos)) instanceof HonorGuardBlockEntity guard ? guard : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof NameTagItem)) {
            // anything else offered to the guard is put into its right hand
            if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!player.getAbilities().mayBuild) return ItemInteractionResult.FAIL;
            if (!level.isClientSide) {
                var guard = guardAt(level, pos, state);
                if (guard == null) return ItemInteractionResult.FAIL;
                var previous = guard.offerItem(stack);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.getAbilities().mayBuild) return ItemInteractionResult.FAIL;
        var name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) {
            // A blank tag is the statue's remote control: it opens the skin picker and is kept.
            if (level.isClientSide) ClientScreens.openGuardSkin(ownerPos(level, pos));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            var guard = guardAt(level, pos, state);
            if (guard == null) return ItemInteractionResult.FAIL;
            guard.setProfileName(name.getString());
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.1F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, POSE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (pos.getY() >= level.getMaxBuildHeight() - 1) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) return null;
        return defaultBlockState()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            var below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighbor.is(this) && neighbor.getValue(HALF) != half
                ? state.setValue(FACING, neighbor.getValue(FACING)).setValue(POSE, neighbor.getValue(POSE))
                : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            // Sneaking opens the control panel on the client; the server answers by re-sending the
            // statue's current contents so the panel can never come up stale.
            if (level.isClientSide) {
                ClientScreens.openGuardSkin(ownerPos(level, pos));
            } else {
                var guard = guardAt(level, pos, state);
                if (guard != null) guard.resync();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        applyToBothHalves(level, pos, state, state.getValue(POSE).next(), state.getValue(FACING));
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, 0.6F, 1.4F);
        return InteractionResult.sidedSuccess(false);
    }

    /** Nothing may vanish with the statue: both hands are emptied onto the ground. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            var guard = guardAt(level, pos, state);
            if (guard != null) {
                var main = guard.getHeldItem();
                guard.dropContents();
                if (!main.isEmpty()) Block.popResource(level, pos, main);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Turns the whole statue a quarter turn clockwise; shared with the control panel. */
    public static void turn(Level level, BlockPos pos, BlockState state) {
        applyToBothHalves(level, pos, state, state.getValue(POSE), state.getValue(FACING).getClockWise());
        level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.7F, 1.2F);
    }

    /** Advances the pose from outside the block's own interaction path. */
    public static void cyclePose(Level level, BlockPos pos, BlockState state) {
        applyToBothHalves(level, pos, state, state.getValue(POSE).next(), state.getValue(FACING));
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    private static void applyToBothHalves(Level level, BlockPos pos, BlockState state, GuardPose pose, Direction facing) {
        var lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        for (var target : new BlockPos[] {lower, lower.above()}) {
            var current = level.getBlockState(target);
            if (current.is(state.getBlock())) {
                level.setBlock(target, current.setValue(POSE, pose).setValue(FACING, facing), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (player.isCreative()) {
                // Both halves share one loot table, so a creative break must clear the partner
                // silently instead of letting the neighbour update drop anything.
                removePartnerForCreative(level, pos, state, player);
            } else {
                // Dropping here (instead of in playerDestroy) makes either half yield exactly one
                // statue no matter which one the player mined.
                dropResources(state, level, pos, null, player, player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        // The loot was already produced in playerWillDestroy; the real state would drop a second copy.
        super.playerDestroy(level, player, pos, Blocks.AIR.defaultBlockState(), blockEntity, tool);
    }

    private static void removePartnerForCreative(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) return;
        var below = pos.below();
        var belowState = level.getBlockState(below);
        if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, DESTROY_PARTICLES, below, Block.getId(belowState));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
