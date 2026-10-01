package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.*;
import dev.overprotocol.registry.ModContent;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaczGuardTests {
    private static final BlockPos POS = new BlockPos(2,1,2);

    private static boolean available(GameTestHelper helper) {
        if (ModList.get().isLoaded("tacz")) return true;
        Overprotocol.LOGGER.info("TaCZ absent: skipping optional real-gun statue test");
        helper.succeed();
        return false;
    }
    private static HonorGuardBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.STONE);
        helper.setBlock(POS, ModContent.HONOR_GUARD.get().defaultBlockState()
            .setValue(HonorGuardBlock.HALF, DoubleBlockHalf.LOWER).setValue(HonorGuardBlock.POSE, GuardPose.PRESENT));
        helper.setBlock(POS.above(), ModContent.HONOR_GUARD.get().defaultBlockState()
            .setValue(HonorGuardBlock.HALF, DoubleBlockHalf.UPPER).setValue(HonorGuardBlock.POSE, GuardPose.PRESENT));
        return (HonorGuardBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    }
    private static ItemStack gun(GameTestHelper helper) {
        try {
            var builderClass = Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder");
            var builder = builderClass.getMethod("create").invoke(null);
            builderClass.getMethod("setId", ResourceLocation.class).invoke(builder,
                ResourceLocation.fromNamespaceAndPath("tacz", "m4a1"));
            builderClass.getMethod("setAmmoCount", int.class).invoke(builder, 17);
            builderClass.getMethod("setAmmoInBarrel", boolean.class).invoke(builder, true);
            var attachmentType = Class.forName("com.tacz.guns.api.item.attachment.AttachmentType");
            var scope = attachmentType.getField("SCOPE").get(null);
            builderClass.getMethod("putAttachment", attachmentType, ResourceLocation.class).invoke(builder, scope,
                ResourceLocation.fromNamespaceAndPath("tacz", "sight_552"));
            var stack = (ItemStack)builderClass.getMethod("build", HolderLookup.Provider.class)
                .invoke(builder, helper.getLevel().registryAccess());
            helper.assertTrue(!stack.isEmpty(), "TaCZ default M4A1 must load");
            var iGun = Class.forName("com.tacz.guns.api.item.IGun");
            var installed = (ItemStack)iGun.getMethod("getAttachment", HolderLookup.Provider.class, ItemStack.class, attachmentType)
                .invoke(stack.getItem(), helper.getLevel().registryAccess(), stack, scope);
            helper.assertTrue(!installed.isEmpty(), "Real scope must be installed before testing data retention");
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Statue gun data regression"));
            return stack;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot create real TaCZ regression gun", error);
        }
    }
    @GameTest(template="test_empty")
    public static void actualGunOfferAndReturnPreserveAmmoAndScope(GameTestHelper helper) {
        if (!available(helper)) return;
        var guard = place(helper);
        var expected = gun(helper);
        var offered = expected.copyWithCount(2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, offered);
        var pos = helper.absolutePos(POS);
        helper.useBlock(POS, player, new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        helper.assertTrue(offered.getCount()==1, "Exactly one gun must enter statue hand");
        helper.assertTrue(ItemStack.matches(expected,guard.getHeldItem()), "Held gun must preserve all components");
        helper.assertTrue(ItemStack.matches(expected,guard.takeItem()), "Returned gun must preserve ID, ammo, scope and name");
        helper.assertTrue(guard.getHeldItem().isEmpty(), "Taking gun must leave no stale stack");
        Overprotocol.LOGGER.info("Real TaCZ gun offer/return with scope and ammo passed");
        helper.succeed();
    }
    @GameTest(template="test_empty")
    public static void actualGunSaveAndSyncPreserveSkinAndAllComponents(GameTestHelper helper) {
        if (!available(helper)) return;
        var guard = place(helper);
        var expected = gun(helper);
        guard.setLocalSkin("retained-slim-skin");
        guard.offerItem(expected);
        var saved = guard.saveWithFullMetadata(helper.getLevel().registryAccess());
        var reloaded = new HonorGuardBlockEntity(guard.getBlockPos(),guard.getBlockState());
        reloaded.loadWithComponents(saved,helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(expected,reloaded.getHeldItem()), "Saved gun must preserve all components");
        helper.assertTrue(reloaded.getLocalSkin().equals("retained-slim-skin"), "Custom skin must remain independent of gun");
        var synced = new HonorGuardBlockEntity(guard.getBlockPos(),guard.getBlockState());
        synced.loadWithComponents(guard.getUpdateTag(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(expected,synced.getHeldItem()), "Client update tag must contain entire gun stack");
        guard.takeItem();
        synced.onDataPacket(null, ClientboundBlockEntityDataPacket.create(guard), helper.getLevel().registryAccess());
        helper.assertTrue(synced.getHeldItem().isEmpty() && synced.getLocalSkin().equals("retained-slim-skin"),
            "Real-gun removal must reach the renderer while retaining the custom skin");
        guard.clearSkin();
        synced.onDataPacket(null, ClientboundBlockEntityDataPacket.create(guard), helper.getLevel().registryAccess());
        helper.assertTrue(synced.getHeldItem().isEmpty() && synced.getLocalSkin().isEmpty(),
            "Real-gun client state must clear when the statue returns to its default uniform");
        Overprotocol.LOGGER.info("Real TaCZ gun save/sync with skin, scope and ammo passed");
        helper.succeed();
    }
    @GameTest(template="test_empty")
    public static void actualGunDropsOnceAndReplacementHasEmptyHand(GameTestHelper helper) {
        if (!available(helper)) return;
        var guard = place(helper);
        var expected = gun(helper);
        guard.offerItem(expected);
        helper.getLevel().destroyBlock(helper.absolutePos(POS),true);
        helper.runAfterDelay(2,()->{
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(POS)).inflate(3), entity -> entity.getItem().is(expected.getItem()));
            helper.assertTrue(drops.size()==1 && ItemStack.matches(expected,drops.getFirst().getItem()),
                "Destruction must drop one complete gun with scope and ammunition");
            var replacement = place(helper);
            helper.assertTrue(replacement.getHeldItem().isEmpty(), "Replacement statue must not inherit removed gun");
            Overprotocol.LOGGER.info("Real TaCZ gun drop/replacement with scope and ammo passed");
            helper.succeed();
        });
    }
    private TaczGuardTests() {}
}
