package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.item.RedCarpetRollItem;
import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.RedCarpetRollEntity;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedCarpetRollTests {
    private static final BlockPos POS=new BlockPos(3,20,3);
    private static void floor(GameTestHelper h,BlockPos centre,Direction facing,int length) {
        for(int i=0;i<length;i++)for(var p:RedCarpetRollEntity.row(centre.relative(facing,i),facing)) {
            h.getLevel().setBlock(p.below(),Blocks.STONE.defaultBlockState(),2);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static RedCarpetRollEntity roll(GameTestHelper h,Direction facing) {
        var r=ModContent.RED_CARPET_ROLL.get().create(h.getLevel());
        var pos=h.absolutePos(POS);r.setYRot(facing.toYRot());r.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        return r;
    }
    @GameTest(template="test_empty")
    public static void nineRedWoolCraftThirtyMetreRoll(GameTestHelper h) {
        var items=new ArrayList<ItemStack>();for(int i=0;i<9;i++)items.add(new ItemStack(Items.RED_WOOL));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,items),h.getLevel());
        h.assertTrue(recipe.isPresent(),"Nine red wool recipe exists");
        var result=recipe.get().value().assemble(CraftingInput.of(3,3,items),h.getLevel().registryAccess());
        h.assertTrue(result.is(ModContent.RED_CARPET_ROLL_ITEM.get()) && result.getCount()==1 && RedCarpetRollItem.remaining(result)==30,
            "Nine red wool must craft one full thirty-metre roll");
        items.set(0,new ItemStack(Items.BLUE_WOOL));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,items),h.getLevel()).isEmpty(),
            "Other wool colours must not match");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void thirtyClicksLayExactlyNinetyCellsAndConsumeRoll(GameTestHelper h) {
        var r=roll(h,Direction.SOUTH);var start=r.anchor();floor(h,start,r.facing(),31);
        var player=h.makeMockPlayer(GameType.SURVIVAL);float radius=RedCarpetRollEntity.radius(30);
        for(int i=0;i<30;i++) {
            h.assertTrue(r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Each click must lay one metre");
            for(var p:RedCarpetRollEntity.row(start.relative(Direction.SOUTH,i),Direction.SOUTH))
                h.assertTrue(h.getLevel().getBlockState(p).is(ModContent.RED_RUNNER.get()),"Each row is three pure red carpet cells");
            if(i<29) {
                h.assertTrue(r.remaining()==29-i && r.anchor().equals(start.relative(Direction.SOUTH,i+1)),"Remaining and advancing end must agree");
                float next=RedCarpetRollEntity.radius(r.remaining());h.assertTrue(next<radius,"Roll must visibly get thinner");radius=next;
            }
        }
        h.assertTrue(r.isRemoved(),"No empty roll remains after thirty rows");
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Repeat interaction cannot lay an extra row");
        for(var p:RedCarpetRollEntity.row(start.relative(Direction.SOUTH,30),Direction.SOUTH))
            h.assertTrue(h.getLevel().isEmptyBlock(p),"No thirty-first row may be placed");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void obstaclesAndUnsupportedGroundNeverSpendOrPartiallyLay(GameTestHelper h) {
        var r=roll(h,Direction.EAST);var start=r.anchor();floor(h,start,r.facing(),2);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var next=start.east();var edge=RedCarpetRollEntity.row(next,r.facing()).getFirst();
        h.getLevel().setBlock(edge,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Side obstacle must block full-width advance");
        h.getLevel().setBlock(edge,Blocks.AIR.defaultBlockState(),2);h.getLevel().setBlock(edge.below(),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"A side gap must block advance");
        h.assertTrue(r.remaining()==30 && r.anchor().equals(start),"Failure cannot move roll or spend length");
        for(var p:RedCarpetRollEntity.row(start,r.facing()))h.assertTrue(h.getLevel().isEmptyBlock(p),"Failed advance must not leave a partial row");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void orientationAndThreeBlockFootprintMatchEveryDirection(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var facing:Direction.Plane.HORIZONTAL) {
            var r=roll(h,facing);var start=r.anchor();floor(h,start,facing,2);
            var box=r.getBoundingBox();
            h.assertTrue(Math.abs((facing.getAxis()==Direction.Axis.Z?box.getXsize():box.getZsize())-3)<.001,
                "Roll occupies exactly three blocks across travel");
            h.assertTrue(Math.abs((facing.getAxis()==Direction.Axis.Z?box.getZsize():box.getXsize())-1)<.001
                && Math.abs(RedCarpetRollEntity.BACK_EDGE+.5F)<.001,"Exposed carpet lip reaches the block boundary");
            h.assertTrue(r.interact(player,InteractionHand.MAIN_HAND).consumesAction() && r.anchor().equals(start.relative(facing)),
                "All compass directions advance in facing direction");
            player.setShiftKeyDown(true);
            h.assertTrue(r.interact(player,InteractionHand.MAIN_HAND).consumesAction() && r.anchor().equals(start) && r.remaining()==30,
                "All compass directions rewind exactly one metre to the original grid position");
            for(var p:RedCarpetRollEntity.row(start,facing))h.assertTrue(h.getLevel().isEmptyBlock(p),"Rewind removes all three cells without leaving an edge");
            player.setShiftKeyDown(false);r.discard();
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void partialRollPersistsRewindsAndPacksOnceWithName(GameTestHelper h) {
        var r=roll(h,Direction.NORTH);floor(h,r.anchor(),r.facing(),5);
        var player=h.makeMockPlayer(GameType.SURVIVAL);r.setCustomName(Component.literal("Welcome"));
        for(int i=0;i<3;i++)r.interact(player,InteractionHand.MAIN_HAND);
        var tag=new CompoundTag();r.saveWithoutId(tag);var loaded=roll(h,Direction.SOUTH);loaded.load(tag);
        h.assertTrue(loaded.remaining()==27 && loaded.facing()==Direction.NORTH && loaded.anchor().equals(r.anchor()),
            "Reload preserves remaining length, direction and deployed end");
        var item=loaded.getPickResult();
        h.assertTrue(RedCarpetRollItem.remaining(item)==27 && item.get(DataComponents.CUSTOM_NAME).getString().equals("Welcome"),
            "Packed item keeps actual remainder and name");
        var end=loaded.anchor();r.discard();player.setShiftKeyDown(true);
        for(int i=0;i<3;i++) {
            h.assertTrue(loaded.interact(player,InteractionHand.MAIN_HAND).consumesAction() && !loaded.isRemoved()
                && loaded.remaining()==28+i && loaded.anchor().equals(end.relative(Direction.SOUTH,i+1)),
                "Each empty-hand sneak click rewinds one row, including after reload");
            for(var p:RedCarpetRollEntity.row(loaded.anchor(),loaded.facing()))
                h.assertTrue(h.getLevel().isEmptyBlock(p),"Rewound carpet is consumed without a remaining row");
        }
        h.assertTrue(loaded.interact(player,InteractionHand.MAIN_HAND).consumesAction() && loaded.isRemoved()
            && !loaded.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Only the next click packs the complete roll, exactly once");
        int count=0;for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var stack=player.getInventory().getItem(i);if(stack.is(ModContent.RED_CARPET_ROLL_ITEM.get())) {
                count+=stack.getCount();h.assertTrue(RedCarpetRollItem.remaining(stack)==30
                    && stack.get(DataComponents.CUSTOM_NAME).getString().equals("Welcome"),"Rewound roll retains full length and its name");
            }
        }
        h.assertTrue(count==1,"Exactly one complete roll is returned");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void rewindRequiresCompleteSupportedRowEmptyHandsAndClearPosition(GameTestHelper h) {
        var r=roll(h,Direction.WEST);var start=r.anchor();floor(h,start,r.facing(),2);
        var player=h.makeMockPlayer(GameType.SURVIVAL);r.interact(player,InteractionHand.MAIN_HAND);
        var end=r.anchor();player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Held main-hand items cannot rewind or pack");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Off-hand items also prevent empty-hand rewinding");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        var edge=RedCarpetRollEntity.row(start,r.facing()).getFirst();h.getLevel().setBlock(edge,Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"A missing side carpet cannot credit a complete metre");
        h.assertTrue(h.getLevel().getBlockState(start).is(ModContent.RED_RUNNER.get()),"Failed rewind leaves other carpet cells intact");
        h.getLevel().setBlock(edge,ModContent.RED_RUNNER.get().defaultBlockState(),2);
        h.getLevel().setBlock(edge.below(),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Missing support prevents rewinding into a gap");
        h.getLevel().setBlock(edge.below(),Blocks.STONE.defaultBlockState(),2);
        // Removing support triggers vanilla carpet survival updates, even with client-only flags.
        h.getLevel().setBlock(edge,ModContent.RED_RUNNER.get().defaultBlockState(),2);
        var blocker=roll(h,r.facing());h.getLevel().addFreshEntity(blocker);
        h.assertTrue(!r.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Another roll obstructs the rewind position");blocker.discard();
        var adventure=h.makeMockPlayer(GameType.ADVENTURE);GameType.ADVENTURE.updatePlayerAbilities(adventure.getAbilities());adventure.setShiftKeyDown(true);
        h.assertTrue(!r.interact(adventure,InteractionHand.MAIN_HAND).consumesAction(),"Adventure players cannot remove carpet");
        h.assertTrue(r.remaining()==29 && r.anchor().equals(end),"All rejected interactions preserve length and position");
        for(var p:RedCarpetRollEntity.row(start,r.facing()))
            h.assertTrue(h.getLevel().getBlockState(p).is(ModContent.RED_RUNNER.get()),"Rejected interactions never consume a partial row");
        // The earlier deliberate support removal may legitimately drop a carpet. Compare against
        // that baseline so this assertion detects any additional items created by rewinding itself.
        var existingDrops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(start).inflate(2))
            .stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        h.assertTrue(r.interact(player,InteractionHand.MAIN_HAND).consumesAction() && r.remaining()==30 && r.anchor().equals(start),
            "A complete clear row can be rewound after obstacles are resolved");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(start).inflate(2))
            .stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet()).equals(existingDrops),
            "Rewinding cannot also drop the consumed carpet as items");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void placementRequiresEntireWidthAndConsumesOnlyOnce(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setYRot(0);
        var pos=h.absolutePos(POS);floor(h,pos,Direction.SOUTH,1);
        var stack=RedCarpetRollItem.withRemaining(18);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
        var edge=pos.east();h.getLevel().setBlock(edge,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction() && stack.getCount()==1,
            "Obstructed width must retain item");
        h.getLevel().setBlock(edge,Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction() && stack.isEmpty(),
            "Successful deployment consumes one roll");
        var rolls=h.getLevel().getEntitiesOfClass(RedCarpetRollEntity.class,new AABB(pos).inflate(3));
        h.assertTrue(rolls.size()==1 && rolls.getFirst().remaining()==18,"Deployment creates exactly one roll with the saved remainder");
        rolls.getFirst().discard();h.succeed();
    }
}
