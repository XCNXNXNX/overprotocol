package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.*;
import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.CeremonialSeatEntity;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReceptionTests {
    private static final BlockPos POS=new BlockPos(3,1,3);
    private static BlockHitResult hit(GameTestHelper h) {var p=h.absolutePos(POS);return new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);}
    private static void floor(GameTestHelper h) {
        for(int x=1;x<=5;x++)for(int z=1;z<=5;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
    }
    private static List<CeremonialSeatEntity> seats(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(CeremonialSeatEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
    }
    private static ItemStack programme() {
        var book=new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.CUSTOM_NAME,Component.literal("Reception programme"));
        book.set(DataComponents.WRITABLE_BOOK_CONTENT,new WritableBookContent(List.of(
            Filterable.passThrough("Welcome"),Filterable.passThrough("Ceremony"),Filterable.passThrough("Dinner"))));
        return book;
    }
    private static void assertRecipe(GameTestHelper h,String id,List<ItemStack> cells,Item output,int count) {
        var input=CraftingInput.of(3,3,cells);
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID,id)).orElseThrow().value();
        h.assertTrue(recipe.matches(input,h.getLevel()),"Reception recipe must match: "+id);
        var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(output) && result.getCount()==count,"Reception recipe result/count: "+id);
    }
    @GameTest(template="test_empty")
    public static void receptionRecipesCraftEveryColourAndBothFixtures(GameTestHelper h) {
        for(var color:DyeColor.values())assertRecipe(h,ModContent.chairName(color),List.of(
            new ItemStack(Items.GOLD_NUGGET),new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(ModContent.CARPET_ITEMS.get(color).get()),new ItemStack(Items.DARK_OAK_PLANKS),
            new ItemStack(Items.STICK),ItemStack.EMPTY,new ItemStack(Items.STICK)),ModContent.CHAIR_ITEMS.get(color).get(),2);
        assertRecipe(h,"ceremonial_podium",List.of(new ItemStack(Items.GOLD_NUGGET),new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.LECTERN),new ItemStack(Items.DARK_OAK_PLANKS),
            new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.DARK_OAK_PLANKS),new ItemStack(Items.DARK_OAK_PLANKS)),ModContent.CEREMONIAL_PODIUM_ITEM.get(),1);
        assertRecipe(h,"welcome_lamp",List.of(ItemStack.EMPTY,new ItemStack(Items.GOLD_INGOT),ItemStack.EMPTY,
            new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.SEA_LANTERN),new ItemStack(Items.GOLD_INGOT),
            ItemStack.EMPTY,new ItemStack(Items.GOLD_INGOT),ItemStack.EMPTY),ModContent.WELCOME_LAMP_ITEM.get(),1);h.succeed();
    }
    @GameTest(template="test_empty")
    public static void chairsPlaceFaceSitAndDismountInAllFourDirections(GameTestHelper h) {
        floor(h);
        for(var facing:Direction.Plane.HORIZONTAL) {
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.setYRot(facing.getOpposite().toYRot());
            var stack=new ItemStack(ModContent.CHAIR_ITEMS.get(DyeColor.RED).get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var support=h.absolutePos(POS.below());var placeHit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
            h.assertTrue(stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,placeHit)).consumesAction() && stack.getCount()==1,
                "Chair placement consumes one item");
            h.assertBlockProperty(POS,CeremonialChairBlock.FACING,facing);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.useBlock(POS,player);h.assertTrue(player.getVehicle() instanceof CeremonialSeatEntity && seats(h).size()==1,"Chair creates exactly one occupied seat");
            var seat=(CeremonialSeatEntity)player.getVehicle();
            h.assertTrue(seat.getPassengers().size()==1 && Math.abs(player.getYRot()-facing.toYRot())<.01
                && Math.abs(seat.getPassengerRidingPosition(player).y-(h.absolutePos(POS).getY()+.625))<.001,
                "Rider uses chair facing and cushion attachment height");
            var exit=seat.getDismountLocationForPassenger(player);
            h.assertTrue(BlockPos.containing(exit).equals(h.absolutePos(POS).relative(facing)),"Clear front tile is chosen for dismounting");
            player.stopRiding();seat.tick();h.assertTrue(seat.isRemoved() && seats(h).isEmpty(),"Empty seat cleans up after dismount");
            h.assertBlockPresent(ModContent.CHAIRS.get(DyeColor.RED).get(),POS);h.setBlock(POS,Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void occupiedChairCannotDoubleSeatAndBreakingReleasesRider(GameTestHelper h) {
        floor(h);h.setBlock(POS,ModContent.CHAIRS.get(DyeColor.BLUE).get());
        var first=h.makeMockPlayer(GameType.SURVIVAL);var second=h.makeMockPlayer(GameType.SURVIVAL);
        h.useBlock(POS,first);h.useBlock(POS,second);
        h.assertTrue(first.isPassenger() && !second.isPassenger() && seats(h).size()==1,"Occupied chair cannot create a second overlapping seat");
        var seat=(CeremonialSeatEntity)first.getVehicle();h.getLevel().destroyBlock(h.absolutePos(POS),true);seat.tick();
        h.assertTrue(!first.isPassenger() && seat.isRemoved() && seats(h).isEmpty(),"Breaking a chair releases its rider and removes the anchor");
        h.assertItemEntityCountIs(ModContent.CHAIR_ITEMS.get(DyeColor.BLUE).get(),POS,2,1);h.succeed();
    }
    @GameTest(template="test_empty")
    public static void chairNeedsEmptyHandsAndHeadRoom(GameTestHelper h) {
        floor(h);h.setBlock(POS,ModContent.CHAIRS.get(DyeColor.WHITE).get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.getBlockState(POS).useWithoutItem(h.getLevel(),player,hit(h));h.assertTrue(!player.isPassenger(),"Held main-hand items cannot mount a chair");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
        h.getBlockState(POS).useWithoutItem(h.getLevel(),player,hit(h));h.assertTrue(!player.isPassenger(),"Held off-hand items cannot mount a chair");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.setBlock(POS.above(),Blocks.STONE);h.useBlock(POS,player);
        h.assertTrue(!player.isPassenger() && seats(h).isEmpty(),"Low ceiling cannot create an occupied seat");
        h.setBlock(POS.above(),Blocks.AIR);h.useBlock(POS,player);h.assertTrue(player.isPassenger(),"Clearing head room permits normal sitting");
        var seat=(CeremonialSeatEntity)player.getVehicle();player.stopRiding();seat.tick();h.succeed();
    }
    @GameTest(template="test_empty")
    public static void podiumBookStorageReloadAndSingleDrop(GameTestHelper h) {
        h.setBlock(POS,ModContent.CEREMONIAL_PODIUM.get());var player=h.makeMockPlayer(GameType.SURVIVAL);var book=programme();book.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND,book);h.useBlock(POS,player);
        h.assertTrue(book.getCount()==1 && h.getBlockState(POS).getValue(LecternBlock.HAS_BOOK),"Podium accepts exactly one book");
        var entity=(LecternBlockEntity)h.getBlockEntity(POS);
        h.assertTrue(BlockEntityType.LECTERN.isValid(h.getBlockState(POS)) && entity.getBook().getCount()==1,"Vanilla lectern type accepts the custom podium");
        var saved=entity.saveWithFullMetadata(h.getLevel().registryAccess());
        var loaded=BlockEntity.loadStatic(h.absolutePos(POS),h.getBlockState(POS),saved,h.getLevel().registryAccess());
        h.assertTrue(loaded instanceof LecternBlockEntity
            && ItemStack.isSameItemSameComponents(entity.getBook(),((LecternBlockEntity)loaded).getBook()),"Book content and name survive native block-entity reload");
        h.useBlock(POS,player);h.assertTrue(book.getCount()==1,"A second book cannot replace or consume the first");
        h.getLevel().destroyBlock(h.absolutePos(POS),true);
        h.assertItemEntityCountIs(ModContent.CEREMONIAL_PODIUM_ITEM.get(),POS,2,1);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2),e->e.getItem().is(Items.WRITABLE_BOOK));
        h.assertTrue(drops.size()==1 && drops.getFirst().getItem().getCount()==1
            && drops.getFirst().getItem().get(DataComponents.WRITABLE_BOOK_CONTENT).pages().size()==3,"Breaking drops the stored book exactly once with its pages");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void podiumMenuPagesSignalsAndBookRetrieval(GameTestHelper h) {
        h.setBlock(POS,ModContent.CEREMONIAL_PODIUM.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,programme());h.useBlock(POS,player);
        var entity=(LecternBlockEntity)h.getBlockEntity(POS);var menu=(LecternMenu)entity.createMenu(1,player.getInventory(),player);
        h.assertTrue(entity.getRedstoneSignal()==1 && menu.clickMenuButton(player,2) && entity.getPage()==1
            && entity.getRedstoneSignal()==8 && h.getBlockState(POS).getValue(LecternBlock.POWERED),"Page turns retain vanilla comparator and pulse behaviour");
        menu.clickMenuButton(player,102);h.assertTrue(entity.getPage()==2 && entity.getRedstoneSignal()==15,"Direct page selection retains full comparator range");
        var visitor=h.makeMockPlayer(GameType.ADVENTURE);GameType.ADVENTURE.updatePlayerAbilities(visitor.getAbilities());
        h.assertTrue(!menu.clickMenuButton(visitor,3) && entity.hasBook(),"Read-only visitor cannot take the podium book");
        h.assertTrue(menu.clickMenuButton(player,3) && !entity.hasBook() && !h.getBlockState(POS).getValue(LecternBlock.HAS_BOOK),"Taking the book clears native podium state");
        int books=0;for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var item=player.getInventory().getItem(i);if(item.is(Items.WRITABLE_BOOK))books+=item.getCount();
        }
        h.assertTrue(books==1,"Book retrieval returns one copy to inventory");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void welcomeLampSwitchesLightPersistsAndRequiresBuildPermission(GameTestHelper h) {
        h.setBlock(POS,ModContent.WELCOME_LAMP.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        var state=h.getBlockState(POS);h.assertTrue(state.getValue(WelcomeLampBlock.LIT) && state.getLightEmission()==15,"Welcome lamp starts lit at full brightness");
        h.useBlock(POS,player);state=h.getBlockState(POS);h.assertTrue(!state.getValue(WelcomeLampBlock.LIT) && state.getLightEmission()==0,"Empty-hand click switches off emitted light");
        var restored=NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),NbtUtils.writeBlockState(state));
        h.assertTrue(restored.equals(state),"Lamp switch state survives storage");
        var visitor=h.makeMockPlayer(GameType.ADVENTURE);GameType.ADVENTURE.updatePlayerAbilities(visitor.getAbilities());
        h.getBlockState(POS).useWithoutItem(h.getLevel(),visitor,hit(h));h.assertBlockProperty(POS,WelcomeLampBlock.LIT,false);
        h.useBlock(POS,player);h.assertBlockProperty(POS,WelcomeLampBlock.LIT,true);
        h.getLevel().destroyBlock(h.absolutePos(POS),true);h.assertItemEntityCountIs(ModContent.WELCOME_LAMP_ITEM.get(),POS,2,1);h.succeed();
    }
}
