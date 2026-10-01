package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CeremonialTableBlock extends Block {
    public static final MapCodec<CeremonialTableBlock> CODEC = simpleCodec(CeremonialTableBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty CLOTH = BooleanProperty.create("cloth");
    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
        Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    private static final VoxelShape[] SHAPES = createShapes();
    private static final VoxelShape BARE_SHAPE = Shapes.or(box(0,13,0,16,16,16),box(6,0,6,10,13,10)).optimize();

    public CeremonialTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
            .setValue(SOUTH, false).setValue(WEST, false).setValue(CLOTH,true));
    }
    @Override public MapCodec<CeremonialTableBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, CLOTH);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState();
        for (var direction : Direction.Plane.HORIZONTAL)
            state = state.setValue(CONNECTIONS.get(direction),
                context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock() instanceof CeremonialTableBlock);
        return state;
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var property = CONNECTIONS.get(direction);
        return property == null ? super.updateShape(state, direction, neighbor, level, pos, neighborPos)
            : state.setValue(property, neighbor.getBlock() instanceof CeremonialTableBlock);
    }
    public static int connectionMask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
            | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CLOTH) ? SHAPES[connectionMask(state)] : BARE_SHAPE;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
            || !player.getAbilities().mayBuild || player.isSpectator() || !level.mayInteract(player,pos)) return InteractionResult.PASS;
        if (!level.isClientSide) level.setBlockAndUpdate(pos,state.cycle(CLOTH));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        var result = state;
        for (var direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(CONNECTIONS.get(rotation.rotate(direction)), state.getValue(CONNECTIONS.get(direction)));
        return result;
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        var result = state;
        for (var direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(CONNECTIONS.get(mirror.mirror(direction)), state.getValue(CONNECTIONS.get(direction)));
        return result;
    }
    private static VoxelShape[] createShapes() {
        var shapes = new VoxelShape[16];
        for (int mask = 0; mask < 16; mask++) {
            var shape = Shapes.or(box(0, 13, 0, 16, 16, 16), box(6, 0, 6, 10, 13, 10));
            if ((mask & 1) == 0) shape = Shapes.or(shape, box(0, 3, 0, 16, 13, 1));
            if ((mask & 2) == 0) shape = Shapes.or(shape, box(15, 3, 0, 16, 13, 16));
            if ((mask & 4) == 0) shape = Shapes.or(shape, box(0, 3, 15, 16, 13, 16));
            if ((mask & 8) == 0) shape = Shapes.or(shape, box(0, 3, 0, 1, 13, 16));
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
}
