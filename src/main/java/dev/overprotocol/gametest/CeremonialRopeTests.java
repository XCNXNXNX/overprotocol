package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.CeremonialRopeBlock;
import dev.overprotocol.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CeremonialRopeTests {
    private static final BlockPos FLOOR = new BlockPos(2, 1, 2);
    private static final BlockPos POST = FLOOR.above();
    private static final BlockPos FLOOR_SOUTH = new BlockPos(2, 1, 3);
    private static final BlockPos POST_SOUTH = FLOOR_SOUTH.above();
    private static final BlockPos FLOOR_DIAGONAL = new BlockPos(3, 1, 3);
    private static final BlockPos POST_DIAGONAL = FLOOR_DIAGONAL.above();

    @GameTest(template = "test_empty")
    public static void placingAlongsideLinksBothBarriers(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(FLOOR_SOUTH, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.CEREMONIAL_ROPE_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(placeOn(helper, player, FLOOR));
        stack.useOn(placeOn(helper, player, FLOOR_SOUTH));
        helper.assertTrue(stack.getCount() == 0, "Both barriers must be placed from the stack");
        helper.assertBlockProperty(POST, CeremonialRopeBlock.SOUTH, true);
        helper.assertBlockProperty(POST_SOUTH, CeremonialRopeBlock.NORTH, true);
        helper.assertBlockProperty(POST_SOUTH, CeremonialRopeBlock.SOUTH, false);
        helper.assertBlockProperty(POST, CeremonialRopeBlock.NORTH, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void diagonalNeighboursDoNotConnect(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(FLOOR_DIAGONAL, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.CEREMONIAL_ROPE_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(placeOn(helper, player, FLOOR));
        stack.useOn(placeOn(helper, player, FLOOR_DIAGONAL));
        helper.assertBlockProperty(POST, CeremonialRopeBlock.SOUTH, false);
        helper.assertBlockProperty(POST, CeremonialRopeBlock.EAST, false);
        helper.assertBlockProperty(POST_DIAGONAL, CeremonialRopeBlock.NORTH, false);
        helper.assertBlockProperty(POST_DIAGONAL, CeremonialRopeBlock.WEST, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void breakingOneBarrierDropsOneAndClearsTheLink(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(FLOOR_SOUTH, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.CEREMONIAL_ROPE_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(placeOn(helper, player, FLOOR));
        stack.useOn(placeOn(helper, player, FLOOR_SOUTH));
        helper.getLevel().destroyBlock(helper.absolutePos(POST_SOUTH), true);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModContent.CEREMONIAL_ROPE.get(), POST_SOUTH);
            helper.assertBlockProperty(POST, CeremonialRopeBlock.SOUTH, false);
            helper.assertItemEntityCountIs(ModContent.CEREMONIAL_ROPE_ITEM.get(), POST_SOUTH, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void recipeProducesTwoBarriers(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "ceremonial_rope");
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
        var ingredients = new ArrayList<ItemStack>(List.of(
            ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY,
            new ItemStack(Items.GOLD_INGOT), new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get()), new ItemStack(Items.GOLD_INGOT),
            ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY));
        var input = CraftingInput.of(3, 3, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Four gold ingots around a carpet must match");
        var result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModContent.CEREMONIAL_ROPE_ITEM.get()) && result.getCount() == 2,
            "The recipe must produce two rope barriers");
        ingredients.set(4, new ItemStack(Items.RED_CARPET));
        helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()),
            "A plain red carpet must not substitute for a ceremonial carpet");
        helper.succeed();
    }

    private static UseOnContext placeOn(GameTestHelper helper, Player player, BlockPos relativePos) {
        var absolute = helper.absolutePos(relativePos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
    }

    private CeremonialRopeTests() {}
}
