package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
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
public final class CarpetTests {
    private static final BlockPos FLOOR = new BlockPos(2, 1, 2);
    private static final BlockPos CARPET = FLOOR.above();

    @GameTest(template = "test_empty")
    public static void survivalPlacementConsumesOneAndStaysThin(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.CEREMONIAL_CARPET_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var floor = helper.absolutePos(FLOOR);
        var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result.consumesAction(), "The carpet must be placeable with its block item");
        helper.assertBlockPresent(ModContent.CEREMONIAL_CARPET.get(), CARPET);
        helper.assertTrue(stack.getCount() == 1, "Survival placement must consume exactly one carpet");
        var shape = helper.getBlockState(CARPET).getCollisionShape(helper.getLevel(), helper.absolutePos(CARPET));
        helper.assertTrue(shape.max(Direction.Axis.Y) == 1.0 / 16.0, "Carpet collision must be one pixel high");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void removingSupportDropsExactlyOneCarpet(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(CARPET, ModContent.CEREMONIAL_CARPET.get());
        helper.setBlock(FLOOR, Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, CARPET);
            helper.assertItemEntityCountIs(ModContent.CEREMONIAL_CARPET_ITEM.get(), CARPET, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void breakingCarpetDropsExactlyOne(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.setBlock(CARPET, ModContent.CEREMONIAL_CARPET.get());
        helper.getLevel().destroyBlock(helper.absolutePos(CARPET), true);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.STONE, FLOOR);
            helper.assertBlockPresent(Blocks.AIR, CARPET);
            helper.assertItemEntityCountIs(ModContent.CEREMONIAL_CARPET_ITEM.get(), CARPET, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void recipeRequiresGoldAndReturnsEight(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "ceremonial_carpet");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var recipe = (CraftingRecipe) holder.value();
        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ingredients.add(new ItemStack(i == 4 ? Items.GOLD_NUGGET : Items.RED_CARPET));
        }
        var input = CraftingInput.of(3, 3, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Eight red carpets and gold must match");
        var result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModContent.CEREMONIAL_CARPET_ITEM.get()) && result.getCount() == 8,
            "The recipe must produce eight ceremonial carpets");
        ingredients.set(4, new ItemStack(Items.IRON_NUGGET));
        helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, ingredients), helper.getLevel()),
            "Iron must not substitute for the gold ornament");
        helper.succeed();
    }
}
