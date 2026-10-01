package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.CandleHolderBlock;
import dev.overprotocol.registry.ModContent;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CandleHolderTests {
    private static final BlockPos POS = new BlockPos(2,2,2);
    private static BlockHitResult hit(GameTestHelper h) {
        var pos=h.absolutePos(POS);
        return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
    }
    private static void stand(GameTestHelper h) {
        h.setBlock(POS.below(),ModContent.CEREMONIAL_TABLE.get());
        h.setBlock(POS,ModContent.CANDLE_HOLDER.get());
    }
    @GameTest(template="test_empty")
    public static void allSeventeenCandlesInsertAndReturnExactColor(GameTestHelper h) {
        for (int index=1;index<=17;index++) {
            stand(h);
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            var item=CandleHolderBlock.candleItem(index);
            var stack=new ItemStack(item,2);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            h.assertTrue(h.getBlockState(POS).useItemOn(stack,h.getLevel(),player,InteractionHand.MAIN_HAND,hit(h)).consumesAction(),
                "Insertion must consume the interaction");
            h.assertBlockProperty(POS,CandleHolderBlock.CANDLE,index);
            h.assertTrue(stack.getCount()==1,"Insertion must consume one candle");
            var second=new ItemStack(CandleHolderBlock.candleItem(index==17 ? 1 : index+1),2);
            player.setItemInHand(InteractionHand.MAIN_HAND,second);
            h.getBlockState(POS).useItemOn(second,h.getLevel(),player,InteractionHand.MAIN_HAND,hit(h));
            h.assertTrue(second.getCount()==2,"Occupied holder must not consume another candle");
            h.assertBlockProperty(POS,CandleHolderBlock.CANDLE,index);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            h.getBlockState(POS).useWithoutItem(h.getLevel(),player,hit(h));
            h.assertBlockProperty(POS,CandleHolderBlock.CANDLE,0);
            h.assertBlockProperty(POS,CandleHolderBlock.LIT,false);
            h.assertTrue(player.getInventory().countItem(item)==1,"Removal must return exactly the original candle");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void flintLightsAndEmptyHandExtinguishes(GameTestHelper h) {
        stand(h);
        h.setBlock(POS,ModContent.CANDLE_HOLDER.get().defaultBlockState().setValue(CandleHolderBlock.CANDLE,16));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var flint=new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND,flint);
        h.assertTrue(flint.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h))).consumesAction(),"Flint must light holder candle");
        h.assertBlockProperty(POS,CandleHolderBlock.LIT,true);
        h.assertTrue(h.getBlockState(POS).getLightEmission(h.getLevel(),h.absolutePos(POS))==3,"Lit single candle must emit light 3");
        h.assertTrue(flint.getDamageValue()==1,"Ignition must use one flint durability");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.getBlockState(POS).useWithoutItem(h.getLevel(),player,hit(h));
        h.assertBlockProperty(POS,CandleHolderBlock.LIT,false);
        h.assertTrue(h.getBlockState(POS).getLightEmission(h.getLevel(),h.absolutePos(POS))==0,"Extinguished candle must emit no light");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void fireChargeLightsAndConsumesOne(GameTestHelper h) {
        stand(h);
        h.setBlock(POS,ModContent.CANDLE_HOLDER.get().defaultBlockState().setValue(CandleHolderBlock.CANDLE,1));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var charge=new ItemStack(Items.FIRE_CHARGE,2);
        player.setItemInHand(InteractionHand.MAIN_HAND,charge);
        h.assertTrue(charge.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h))).consumesAction(),"Fire charge must light holder");
        h.assertBlockProperty(POS,CandleHolderBlock.LIT,true);
        h.assertTrue(charge.getCount()==1,"Fire charge must consume one");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void emptyHolderRejectsIgnitionAndAdventureInsertion(GameTestHelper h) {
        stand(h);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var flint=new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND,flint);
        var state=h.getBlockState(POS);
        h.assertTrue(state.getToolModifiedState(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h)),
            ItemAbilities.FIRESTARTER_LIGHT,false)==null,"Empty holder cannot be lit");
        h.assertTrue(state.useItemOn(flint,h.getLevel(),player,InteractionHand.MAIN_HAND,hit(h)).consumesAction(),
            "Empty holder must swallow firestarter use so it cannot ignite the block above");
        h.assertTrue(flint.getDamageValue()==0,"Rejected ignition must not damage flint");
        var adventure=h.makeMockPlayer(GameType.ADVENTURE);
        GameType.ADVENTURE.updatePlayerAbilities(adventure.getAbilities());
        var candle=new ItemStack(Items.BLUE_CANDLE,2);
        adventure.setItemInHand(InteractionHand.MAIN_HAND,candle);
        state.useItemOn(candle,h.getLevel(),adventure,InteractionHand.MAIN_HAND,hit(h));
        h.assertBlockProperty(POS,CandleHolderBlock.CANDLE,0);
        h.assertTrue(candle.getCount()==2,"Adventure player cannot modify holder");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void supportLossDropsHolderAndOriginalCandle(GameTestHelper h) {
        stand(h);
        h.setBlock(POS,ModContent.CANDLE_HOLDER.get().defaultBlockState().setValue(CandleHolderBlock.CANDLE,13)
            .setValue(CandleHolderBlock.LIT,true));
        h.setBlock(POS.below(),Blocks.AIR);
        h.runAfterDelay(2,()->{
            h.assertBlockPresent(Blocks.AIR,POS);
            h.assertItemEntityCountIs(ModContent.CANDLE_HOLDER_ITEM.get(),POS,3,1);
            h.assertItemEntityCountIs(Items.BLUE_CANDLE,POS,3,1);
            h.succeed();
        });
    }
    @GameTest(template="test_empty")
    public static void directBreakDropsBothOnce(GameTestHelper h) {
        stand(h);
        h.setBlock(POS,ModContent.CANDLE_HOLDER.get().defaultBlockState().setValue(CandleHolderBlock.CANDLE,1));
        h.getLevel().destroyBlock(h.absolutePos(POS),true);
        h.runAfterDelay(2,()->{
            h.assertItemEntityCountIs(ModContent.CANDLE_HOLDER_ITEM.get(),POS,3,1);
            h.assertItemEntityCountIs(Items.CANDLE,POS,3,1);
            h.succeed();
        });
    }
    @GameTest(template="test_empty")
    public static void holderItemPlacesOnTableAndRecipeMatches(GameTestHelper h) {
        h.setBlock(POS.below(),ModContent.CEREMONIAL_TABLE.get());
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var holder=new ItemStack(ModContent.CANDLE_HOLDER_ITEM.get(),2);
        player.setItemInHand(InteractionHand.MAIN_HAND,holder);
        var table=h.absolutePos(POS.below());
        var tableHit=new BlockHitResult(Vec3.atCenterOf(table).add(0,.5,0),Direction.UP,table,false);
        h.assertTrue(holder.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,tableHit)).consumesAction(),"Holder must place on tabletop");
        h.assertBlockPresent(ModContent.CANDLE_HOLDER.get(),POS);
        h.assertTrue(holder.getCount()==1,"Placement must consume one holder");
        var input=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,new ItemStack(Items.GOLD_NUGGET),ItemStack.EMPTY,
            ItemStack.EMPTY,new ItemStack(Items.GOLD_NUGGET),ItemStack.EMPTY,new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),new ItemStack(Items.GOLD_NUGGET)));
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
            Overprotocol.MOD_ID,"candle_holder")).orElseThrow().value();
        h.assertTrue(recipe.matches(input,h.getLevel()),"Holder recipe must match five gold nuggets");
        var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(ModContent.CANDLE_HOLDER_ITEM.get()) && result.getCount()==1,"Holder recipe must produce one");
        h.succeed();
    }
}
