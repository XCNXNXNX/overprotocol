package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.GuardPose;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.block.HonorGuardBlockEntity;
import dev.overprotocol.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HonorGuardTests {
    private static final BlockPos FLOOR = new BlockPos(3, 1, 3);
    private static final BlockPos LOWER = FLOOR.above();
    private static final BlockPos UPPER = LOWER.above();

    @GameTest(template = "test_empty")
    public static void placementBuildsBothHalvesAndConsumesOne(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.HONOR_GUARD_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = stack.useOn(placeOn(helper, player, FLOOR));
        helper.assertTrue(result.consumesAction(), "The statue must be placeable with its block item");
        helper.assertBlockPresent(ModContent.HONOR_GUARD.get(), LOWER);
        helper.assertBlockPresent(ModContent.HONOR_GUARD.get(), UPPER);
        helper.assertBlockProperty(LOWER, HonorGuardBlock.HALF, DoubleBlockHalf.LOWER);
        helper.assertBlockProperty(UPPER, HonorGuardBlock.HALF, DoubleBlockHalf.UPPER);
        helper.assertBlockProperty(LOWER, HonorGuardBlock.FACING, player.getDirection().getOpposite());
        helper.assertBlockProperty(UPPER, HonorGuardBlock.FACING, player.getDirection().getOpposite());
        helper.assertBlockProperty(LOWER, HonorGuardBlock.POSE, GuardPose.ATTENTION);
        helper.assertTrue(stack.getCount() == 1, "Survival placement must consume exactly one statue");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void placementIsRejectedWhenTheSpaceAboveIsBlocked(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(UPPER, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.HONOR_GUARD_ITEM.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = stack.useOn(placeOn(helper, player, FLOOR));
        helper.assertTrue(!result.consumesAction(), "A statue with no head room must not be placed");
        helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), LOWER);
        helper.assertTrue(stack.getCount() == 1, "A rejected placement must not consume the statue");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void emptyHandCyclesThePoseOfBothHalves(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var expected : new GuardPose[] {GuardPose.SALUTE, GuardPose.PRESENT, GuardPose.RAISE, GuardPose.ATTENTION}) {
            helper.useBlock(LOWER, player);
            helper.assertBlockProperty(LOWER, HonorGuardBlock.POSE, expected);
            helper.assertBlockProperty(UPPER, HonorGuardBlock.POSE, expected);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void sneakingOpensThePanelWithoutChangingAnything(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.SALUTE, Direction.NORTH);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        helper.useBlock(UPPER, player);
        helper.assertBlockProperty(LOWER, HonorGuardBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(UPPER, HonorGuardBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(LOWER, HonorGuardBlock.POSE, GuardPose.SALUTE);
        helper.assertBlockProperty(UPPER, HonorGuardBlock.POSE, GuardPose.SALUTE);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void thePanelTurnsAndPosesBothHalves(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var level = helper.getLevel();
        var absolute = helper.absolutePos(LOWER);
        HonorGuardBlock.turn(level, absolute, level.getBlockState(absolute));
        helper.assertBlockProperty(LOWER, HonorGuardBlock.FACING, Direction.NORTH.getClockWise());
        helper.assertBlockProperty(UPPER, HonorGuardBlock.FACING, Direction.NORTH.getClockWise());
        HonorGuardBlock.cyclePose(level, absolute, level.getBlockState(absolute));
        helper.assertBlockProperty(LOWER, HonorGuardBlock.POSE, GuardPose.SALUTE);
        helper.assertBlockProperty(UPPER, HonorGuardBlock.POSE, GuardPose.SALUTE);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void miningTheLowerHalfDropsExactlyOne(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(LOWER), true);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), LOWER);
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), UPPER);
            helper.assertItemEntityCountIs(ModContent.HONOR_GUARD_ITEM.get(), LOWER, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void miningTheUpperHalfDropsExactlyOne(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        mineLikeAPlayer(helper, UPPER, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), LOWER);
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), UPPER);
            helper.assertItemEntityCountIs(ModContent.HONOR_GUARD_ITEM.get(), UPPER, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void creativeMiningClearsBothHalvesWithoutDrops(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        mineLikeAPlayer(helper, UPPER, helper.makeMockPlayer(GameType.CREATIVE));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), LOWER);
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), UPPER);
            helper.assertItemEntityNotPresent(ModContent.HONOR_GUARD_ITEM.get(), LOWER, 3.0);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void removingTheFloorBreaksTheWholeStatue(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        helper.setBlock(FLOOR, Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), LOWER);
            helper.assertBlockNotPresent(ModContent.HONOR_GUARD.get(), UPPER);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void collisionFollowsTheFigureNotTheWholeBlock(GameTestHelper helper) {
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var level = helper.getLevel();
        var shape = helper.getBlockState(LOWER).getCollisionShape(level, helper.absolutePos(LOWER));
        helper.assertTrue(shape.max(Direction.Axis.Y) == 1.0, "Each half must be a full block tall");
        helper.assertTrue(shape.max(Direction.Axis.X) < 1.0, "The statue must be narrower than a full block");
        helper.assertTrue(shape.min(Direction.Axis.X) > 0.0, "The statue must be inset from the block edge");
        helper.assertTrue(shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z) < 1.0,
            "The statue must be shallower than a full block");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void recipeNeedsGoldWoolAndCeremonialCarpet(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "honor_guard");
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
        var ingredients = new ArrayList<ItemStack>(List.of(
            ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY,
            new ItemStack(Items.WHITE_WOOL), new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get()), new ItemStack(Items.WHITE_WOOL),
            new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get()), new ItemStack(Items.STICK), new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get())));
        var input = CraftingInput.of(3, 3, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "The ceremonial recipe must match");
        var result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModContent.HONOR_GUARD_ITEM.get()) && result.getCount() == 1,
            "The recipe must produce exactly one honour guard");
        ingredients.set(4, new ItemStack(Items.RED_CARPET));
        helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()),
            "A plain red carpet must not substitute for a ceremonial carpet");
        ingredients.set(4, new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get()));
        ingredients.set(1, new ItemStack(Items.COPPER_INGOT));
        helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()),
            "Copper must not substitute for the gold cap badge");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void onlyTheLowerHalfOwnsTheDrop(GameTestHelper helper) {
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var level = helper.getLevel();
        var lower = Block.getDrops(helper.getBlockState(LOWER), level, helper.absolutePos(LOWER), null, null, ItemStack.EMPTY);
        var upper = Block.getDrops(helper.getBlockState(UPPER), level, helper.absolutePos(UPPER), null, null, ItemStack.EMPTY);
        helper.assertTrue(lower.size() == 1 && lower.get(0).is(ModContent.HONOR_GUARD_ITEM.get()),
            "The lower half must drop one statue but dropped " + lower.size());
        helper.assertTrue(upper.isEmpty(), "The upper half must drop nothing but dropped " + upper.size());
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void onlyTheLowerHalfOwnsABlockEntity(GameTestHelper helper) {
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(LOWER)) instanceof HonorGuardBlockEntity,
            "The lower half must own the statue block entity");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(UPPER)) == null,
            "The upper half must not own a second block entity");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void aNamedNameTagGivesTheStatueAPlayerProfile(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Notch"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tag);
        var absolute = helper.absolutePos(LOWER);
        helper.useBlock(LOWER, player, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertTrue(tag.isEmpty(), "A survival name tag must be consumed");
        var guard = (HonorGuardBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(guard.getProfile() != null, "The statue must remember the requested player");
        helper.assertTrue(guard.getProfile().name().orElse("").equals("Notch"),
            "The stored profile must carry the requested name");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void anUnnamedNameTagIsLeftAlone(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tag = new ItemStack(Items.NAME_TAG);
        player.setItemInHand(InteractionHand.MAIN_HAND, tag);
        var absolute = helper.absolutePos(LOWER);
        helper.useBlock(LOWER, player, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertTrue(tag.getCount() == 1, "A blank name tag must be kept");
        var guard = (HonorGuardBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(guard.getProfile() == null, "A blank name tag must not change the skin");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void anOfferedItemGoesIntoTheGuardHand(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var banner = new ItemStack(Items.RED_BANNER, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, banner);
        var absolute = helper.absolutePos(LOWER);
        helper.useBlock(LOWER, player, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        var guard = (HonorGuardBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(guard.getHeldItem().is(Items.RED_BANNER), "The guard must hold the offered banner");
        helper.assertTrue(guard.getHeldItem().getCount() == 1, "Only a single item goes into the hand");
        helper.assertTrue(banner.getCount() == 1, "Exactly one item must leave the stack");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void breakingTheStatueDropsBothHands(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        var guard = (HonorGuardBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(LOWER));
        guard.offerItem(new ItemStack(Items.RED_BANNER));
        helper.getLevel().destroyBlock(helper.absolutePos(LOWER), true);
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityCountIs(Items.RED_BANNER, LOWER, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void takingTheItemEmptiesTheHand(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        var guard = (HonorGuardBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(LOWER));
        guard.offerItem(new ItemStack(Items.RED_BANNER));
        helper.assertTrue(guard.getHeldItem().is(Items.RED_BANNER), "The banner goes into the hand");
        var second = guard.offerItem(new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(second.is(Items.RED_BANNER), "Offering a second item hands the first one back");
        guard.takeItem();
        helper.assertTrue(guard.getHeldItem().isEmpty(), "Taking the item must leave the hand empty");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bothHandsSurviveASaveLoadRoundTrip(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        var level = helper.getLevel();
        var guard = (HonorGuardBlockEntity) level.getBlockEntity(helper.absolutePos(LOWER));
        guard.offerItem(new ItemStack(Items.RED_BANNER));
        var saved = guard.getUpdateTag(level.registryAccess());
        var reloaded = new HonorGuardBlockEntity(helper.absolutePos(LOWER),
            level.getBlockState(helper.absolutePos(LOWER)));
        reloaded.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(reloaded.getHeldItem().is(Items.RED_BANNER), "the held item must survive the round trip");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void defaultSkinTakeAndResetApplyEmptySnapshotsToExistingClient(GameTestHelper helper) {
        placeGuard(helper, GuardPose.PRESENT, Direction.NORTH);
        var level = helper.getLevel();
        var server = (HonorGuardBlockEntity) level.getBlockEntity(helper.absolutePos(LOWER));
        var client = new HonorGuardBlockEntity(server.getBlockPos(), server.getBlockState());
        for (boolean custom : new boolean[]{false, true}) {
            if (custom) server.setLocalSkin("regression-skin");
            server.offerItem(new ItemStack(Items.IRON_SWORD));
            applyPacket(helper, server, client);
            helper.assertTrue(client.getHeldItem().is(Items.IRON_SWORD), "Client must receive the equipped item");
            var returned = server.takeItem();
            helper.assertTrue(returned.is(Items.IRON_SWORD), "Taking must return the actual stored sword");
            applyPacket(helper, server, client);
            helper.assertTrue(client.getHeldItem().isEmpty(), "Default and custom statues must clear stale client hands after take");
            helper.assertTrue(server.takeItem().isEmpty(), "A repeated take must not duplicate the item");
            server.clearSkin();
            applyPacket(helper, server, client);
            helper.assertTrue(client.getHeldItem().isEmpty() && client.getLocalSkin().isEmpty() && client.getProfile() == null,
                "Reset to the entirely empty default snapshot must clear old client state");
        }
        helper.succeed();
    }

    private static void applyPacket(GameTestHelper helper, HonorGuardBlockEntity server, HonorGuardBlockEntity client) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ClientboundBlockEntityDataPacket.STREAM_CODEC.encode(buffer, ClientboundBlockEntityDataPacket.create(server));
            var decoded = ClientboundBlockEntityDataPacket.STREAM_CODEC.decode(buffer);
            client.onDataPacket(null, decoded, helper.getLevel().registryAccess());
        } finally { buffer.release(); }
    }

    @GameTest(template = "test_empty")
    public static void eitherHalfResolvesToTheOwningHalf(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        placeGuard(helper, GuardPose.ATTENTION, Direction.NORTH);
        var level = helper.getLevel();
        var lower = helper.absolutePos(LOWER);
        var upper = helper.absolutePos(UPPER);
        helper.assertTrue(HonorGuardBlock.ownerPos(level, lower).equals(lower),
            "clicking the lower half must resolve to the lower half");
        helper.assertTrue(HonorGuardBlock.ownerPos(level, upper).equals(lower),
            "clicking the upper half must resolve to the half that owns the block entity");
        helper.succeed();
    }

    private static void placeGuard(GameTestHelper helper, GuardPose pose, Direction facing) {
        helper.setBlock(LOWER, ModContent.HONOR_GUARD.get().defaultBlockState()
            .setValue(HonorGuardBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(HonorGuardBlock.FACING, facing)
            .setValue(HonorGuardBlock.POSE, pose));
        helper.setBlock(UPPER, ModContent.HONOR_GUARD.get().defaultBlockState()
            .setValue(HonorGuardBlock.HALF, DoubleBlockHalf.UPPER)
            .setValue(HonorGuardBlock.FACING, facing)
            .setValue(HonorGuardBlock.POSE, pose));
    }

    private static UseOnContext placeOn(GameTestHelper helper, Player player, BlockPos relativePos) {
        var absolute = helper.absolutePos(relativePos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
    }

    /** Mirrors what the server game mode does when a survival player breaks a block. */
    private static void mineLikeAPlayer(GameTestHelper helper, BlockPos relativePos, Player player) {
        var level = helper.getLevel();
        var absolute = helper.absolutePos(relativePos);
        var state = level.getBlockState(absolute);
        state.getBlock().playerWillDestroy(level, absolute, state, player);
        level.removeBlock(absolute, false);
        state.getBlock().playerDestroy(level, player, absolute, state, null, ItemStack.EMPTY);
    }

    private HonorGuardTests() {}
}
