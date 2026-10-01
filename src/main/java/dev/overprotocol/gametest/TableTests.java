package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.CeremonialTableBlock;
import dev.overprotocol.registry.ModContent;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TableTests {
    private static final BlockPos CENTER = new BlockPos(2,1,2);
    private static BlockHitResult hit(GameTestHelper h) {
        var pos=h.absolutePos(CENTER);return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
    }
    @GameTest(template="test_empty")
    public static void emptyHandSneakingTogglesClothPreservesJoinsAndSupportsCandle(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setShiftKeyDown(true);
        for(var color:DyeColor.values()) {
            h.setBlock(CENTER,ModContent.TABLES.get(color).get());
            h.setBlock(CENTER.east(),ModContent.TABLES.get(DyeColor.BLUE).get());
            h.setBlock(CENTER.above(),ModContent.CANDLE_HOLDER.get());
            var dressed=h.getBlockState(CENTER);var before=dressed.getCollisionShape(h.getLevel(),h.absolutePos(CENTER));
            h.assertTrue(dressed.getValue(CeremonialTableBlock.CLOTH)
                && dressed.useWithoutItem(h.getLevel(),player,hit(h)).consumesAction(),"Every table starts dressed and can be uncovered");
            var bare=h.getBlockState(CENTER);
            h.assertTrue(!bare.getValue(CeremonialTableBlock.CLOTH) && bare.getValue(CeremonialTableBlock.EAST),
                "Uncovering preserves colour and four-way connection state");
            h.assertTrue(Shapes.joinIsNotEmpty(before,bare.getCollisionShape(h.getLevel(),h.absolutePos(CENTER)),BooleanOp.ONLY_FIRST),
                "Hidden cloth no longer leaves its skirt collision");
            h.assertBlockPresent(ModContent.CANDLE_HOLDER.get(),CENTER.above());
            var saved=NbtUtils.writeBlockState(bare);
            var restored=NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),saved);
            h.assertTrue(restored.equals(bare),"Bare state and original colour persist through block-state storage");
            saved.getCompound("Properties").remove("cloth");
            h.assertTrue(NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),saved)
                .getValue(CeremonialTableBlock.CLOTH),"Older worlds without the property retain their cloth");
            h.setBlock(CENTER.east(),Blocks.AIR);
            h.assertTrue(!h.getBlockState(CENTER).getValue(CeremonialTableBlock.CLOTH)
                && !h.getBlockState(CENTER).getValue(CeremonialTableBlock.EAST),"Splitting while uncovered keeps cloth hidden");
            h.getBlockState(CENTER).useWithoutItem(h.getLevel(),player,hit(h));
            h.assertTrue(h.getBlockState(CENTER).getValue(CeremonialTableBlock.CLOTH)
                && !h.getBlockState(CENTER).getValue(CeremonialTableBlock.EAST),"Next click restores cloth with current outer skirts");
            h.assertBlockPresent(ModContent.CANDLE_HOLDER.get(),CENTER.above());h.setBlock(CENTER.above(),Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void tableClothToggleRequiresSneakingEmptyHandsAndBuildPermission(GameTestHelper h) {
        h.setBlock(CENTER,ModContent.CEREMONIAL_TABLE.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(!h.getBlockState(CENTER).useWithoutItem(h.getLevel(),player,hit(h)).consumesAction(),"Standing clicks do not toggle cloth");
        player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(!h.getBlockState(CENTER).useWithoutItem(h.getLevel(),player,hit(h)).consumesAction(),"Held items do not toggle cloth");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
        h.assertTrue(!h.getBlockState(CENTER).useWithoutItem(h.getLevel(),player,hit(h)).consumesAction(),"Off-hand items do not toggle cloth");
        var adventure=h.makeMockPlayer(GameType.ADVENTURE);GameType.ADVENTURE.updatePlayerAbilities(adventure.getAbilities());adventure.setShiftKeyDown(true);
        h.assertTrue(!h.getBlockState(CENTER).useWithoutItem(h.getLevel(),adventure,hit(h)).consumesAction(),"Adventure cannot toggle cloth");
        h.assertBlockProperty(CENTER,CeremonialTableBlock.CLOTH,true);h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void mixedColorSquareAndFourWayJoinSplit(GameTestHelper h) {
        for (int x=1;x<=3;x++) for(int z=1;z<=3;z++)
            h.setBlock(new BlockPos(x,1,z), ModContent.TABLES.get((x+z)%2 == 0 ? DyeColor.RED : DyeColor.BLUE).get());
        h.assertTrue(CeremonialTableBlock.connectionMask(h.getBlockState(CENTER)) == 15, "Interior must join all four sides");
        h.assertBlockProperty(new BlockPos(1,1,1), CeremonialTableBlock.NORTH, false);
        h.assertBlockProperty(new BlockPos(1,1,1), CeremonialTableBlock.WEST, false);
        h.assertBlockProperty(new BlockPos(1,1,1), CeremonialTableBlock.EAST, true);
        h.setBlock(CENTER, Blocks.AIR);
        for (var direction : Direction.Plane.HORIZONTAL)
            h.assertBlockProperty(CENTER.relative(direction), CeremonialTableBlock.CONNECTIONS.get(direction.getOpposite()), false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void innerSkirtsDisappearAndRestore(GameTestHelper h) {
        for (var direction : Direction.Plane.HORIZONTAL) {
            h.setBlock(CENTER, ModContent.CEREMONIAL_TABLE.get());
            var isolated=h.getBlockState(CENTER).getCollisionShape(h.getLevel(), h.absolutePos(CENTER));
            h.setBlock(CENTER.relative(direction), ModContent.CEREMONIAL_TABLE.get());
            var joined=h.getBlockState(CENTER).getCollisionShape(h.getLevel(), h.absolutePos(CENTER));
            h.assertTrue(Shapes.joinIsNotEmpty(isolated,joined,BooleanOp.ONLY_FIRST), "Internal cloth skirt must disappear");
            h.setBlock(CENTER.relative(direction),Blocks.AIR);
            var restored=h.getBlockState(CENTER).getCollisionShape(h.getLevel(), h.absolutePos(CENTER));
            h.assertTrue(!Shapes.joinIsNotEmpty(isolated,restored,BooleanOp.NOT_SAME),"Outer skirt must return after splitting");
        }
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void placementJoinsRegardlessOfPlayerFacing(GameTestHelper h) {
        for (var looking : Direction.Plane.HORIZONTAL) {
            h.setBlock(CENTER.below(),Blocks.STONE);
            h.setBlock(CENTER.north(),ModContent.TABLES.get(DyeColor.WHITE).get());
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            player.setYRot(looking.toYRot());
            var stack=new ItemStack(ModContent.CEREMONIAL_TABLE_ITEM.get(),2);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var floor=h.absolutePos(CENTER.below());
            var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
            h.assertTrue(stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Table must place");
            h.assertBlockProperty(CENTER,CeremonialTableBlock.NORTH,true);
            h.assertTrue(stack.getCount()==1,"Placement must consume exactly one table");
            h.setBlock(CENTER,Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void allColorsHaveMatchingCarpetAndTableRecipes(GameTestHelper h) {
        for (var color : DyeColor.values()) {
            var carpet=ModContent.CARPET_ITEMS.get(color).get();
            var vanilla=ModContent.vanillaCarpet(color);
            var carpetInput=CraftingInput.of(3,3,List.of(new ItemStack(vanilla),new ItemStack(vanilla),new ItemStack(vanilla),
                new ItemStack(vanilla),new ItemStack(Items.GOLD_NUGGET),new ItemStack(vanilla),
                new ItemStack(vanilla),new ItemStack(vanilla),new ItemStack(vanilla)));
            var carpetRecipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                Overprotocol.MOD_ID,ModContent.carpetName(color))).orElseThrow().value();
            h.assertTrue(carpetRecipe.matches(carpetInput,h.getLevel()),"Matching color carpet recipe missing: "+color);
            var carpetResult=carpetRecipe.assemble(carpetInput,h.getLevel().registryAccess());
            h.assertTrue(carpetResult.is(carpet) && carpetResult.getCount()==8,"Carpet recipe output wrong");
            var input=CraftingInput.of(3,3,List.of(new ItemStack(carpet),new ItemStack(carpet),new ItemStack(carpet),
                new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.DARK_OAK_PLANKS),
                new ItemStack(Items.STICK),ItemStack.EMPTY,new ItemStack(Items.STICK)));
            var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                Overprotocol.MOD_ID,ModContent.tableName(color))).orElseThrow().value();
            h.assertTrue(recipe.matches(input,h.getLevel()),"Matching color table recipe missing: "+color);
            var result=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(ModContent.TABLE_ITEMS.get(color).get()) && result.getCount()==2,"Table color/output wrong");
            var wrong=new ArrayList<>(input.items());
            wrong.set(0,new ItemStack(ModContent.CARPET_ITEMS.get(DyeColor.byId((color.getId()+1)%16)).get()));
            h.assertTrue(!recipe.matches(CraftingInput.of(3,3,wrong),h.getLevel()),"Mixed carpet colors must not match");
        }
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void breakingCenterDropsOnceAndUpdatesAllNeighbors(GameTestHelper h) {
        h.setBlock(CENTER,ModContent.CEREMONIAL_TABLE.get());
        for(var direction:Direction.Plane.HORIZONTAL) h.setBlock(CENTER.relative(direction),ModContent.TABLES.get(DyeColor.GREEN).get());
        h.getLevel().destroyBlock(h.absolutePos(CENTER),true);
        h.runAfterDelay(2,()->{
            for(var direction:Direction.Plane.HORIZONTAL)
                h.assertBlockProperty(CENTER.relative(direction),CeremonialTableBlock.CONNECTIONS.get(direction.getOpposite()),false);
            h.assertItemEntityCountIs(ModContent.CEREMONIAL_TABLE_ITEM.get(),CENTER,3,1);
            h.succeed();
        });
    }
}
