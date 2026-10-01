package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A golden stanchion with a red velvet rope. Ropes are drawn towards every neighbouring barrier,
 * so a line of them reads as one continuous cordon regardless of the order they were placed in.
 */
public final class CeremonialRopeBlock extends Block {
    public static final MapCodec<CeremonialRopeBlock> CODEC = simpleCodec(CeremonialRopeBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
        Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    private static final VoxelShape POST = Shapes.or(
        box(5.6, 0.0, 5.6, 10.4, 1.2, 10.4), box(7.0, 1.2, 7.0, 9.0, 13.6, 9.0),
        box(6.4, 13.6, 6.4, 9.6, 15.6, 9.6));
    /** The red collar the rope is knotted through. */
    private static final VoxelShape COLLAR = box(6.6, 11.2, 6.6, 9.4, 12.8, 9.4);
    private static final Map<Direction, VoxelShape> ROPES = Map.of(
        Direction.NORTH, box(7.4, 10.5, 0.0, 8.6, 12.8, 6.6),
        Direction.EAST, box(9.4, 10.5, 7.4, 16.0, 12.8, 8.6),
        Direction.SOUTH, box(7.4, 10.5, 9.4, 8.6, 12.8, 16.0),
        Direction.WEST, box(0.0, 10.5, 7.4, 6.6, 12.8, 8.6));
    private static final VoxelShape[] SHAPES = createShapes();

    public CeremonialRopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
            .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    public MapCodec<CeremonialRopeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState();
        for (var direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(CONNECTIONS.get(direction),
                context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock() instanceof CeremonialRopeBlock);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var property = CONNECTIONS.get(direction);
        return property == null ? super.updateShape(state, direction, neighbor, level, pos, neighborPos)
            : state.setValue(property, neighbor.getBlock() instanceof CeremonialRopeBlock);
    }

    public static int connectionMask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
            | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[connectionMask(state)];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        var result = state;
        for (var direction : Direction.Plane.HORIZONTAL) {
            result = result.setValue(CONNECTIONS.get(rotation.rotate(direction)), state.getValue(CONNECTIONS.get(direction)));
        }
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        var result = state;
        for (var direction : Direction.Plane.HORIZONTAL) {
            result = result.setValue(CONNECTIONS.get(mirror.mirror(direction)), state.getValue(CONNECTIONS.get(direction)));
        }
        return result;
    }

    /** Bit used by {@link #connectionMask} for each horizontal direction. */
    private static int bit(Direction direction) {
        return switch (direction) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 8;
            default -> 0;
        };
    }

    private static VoxelShape[] createShapes() {
        var shapes = new VoxelShape[16];
        for (int mask = 0; mask < 16; mask++) {
            var shape = mask == 0 ? POST : Shapes.or(POST, COLLAR);
            for (var direction : Direction.Plane.HORIZONTAL) {
                if ((mask & bit(direction)) != 0) shape = Shapes.or(shape, ROPES.get(direction));
            }
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
}
