package dev.overprotocol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.authlib.GameProfile;
import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.GuardPose;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.block.HonorGuardBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;

/**
 * Draws the honour guard with the game's own baked player model, so a real skin lands on it exactly
 * the way it does on a player. The pose stack mirrors LivingEntityRenderer + PlayerRenderer,
 * including the 0.9375 player scale that makes a player 1.875 blocks tall rather than 2.
 */
@OnlyIn(Dist.CLIENT)
public final class HonorGuardRenderer implements BlockEntityRenderer<HonorGuardBlockEntity> {
    private static final ResourceLocation GUARD_SKIN =
        ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "textures/entity/honor_guard.png");
    private static final PlayerSkin FALLBACK =
        new PlayerSkin(GUARD_SKIN, null, null, null, PlayerSkin.Model.WIDE, true);
    /** PlayerRenderer.scale: players render at 30/32 of the raw model. */
    private static final float PLAYER_SCALE = 0.9375F;

    private final PlayerModel<LivingEntity> wide;
    private final PlayerModel<LivingEntity> slim;
    // Render-only context. Never add this player to a world, tick it, or run gun gameplay on it.
    private RemotePlayer renderPlayer;
    private static final GameProfile RENDER_PROFILE = new GameProfile(
        UUID.fromString("ef422a61-a160-4a25-82d3-34fa68132091"), "OverprotocolGuard");

    public HonorGuardRenderer(BlockEntityRendererProvider.Context context) {
        this.wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        TaczGuardCompat.initialize();
    }

    @Override
    public AABB getRenderBoundingBox(HonorGuardBlockEntity guard) {
        return new AABB(guard.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 2, 0);
    }

    @Override
    public void render(HonorGuardBlockEntity guard, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        var state = guard.getBlockState();
        if (!(state.getBlock() instanceof HonorGuardBlock) || state.getValue(HonorGuardBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        var skin = resolveSkin(guard);
        var model = skin.model() == PlayerSkin.Model.SLIM ? this.slim : this.wide;
        var actor = prepareRenderPlayer(guard);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.getValue(HonorGuardBlock.FACING).toYRot()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        if (actor != null && state.getValue(HonorGuardBlock.POSE) == GuardPose.PRESENT) {
            applyPose(model, GuardPose.ATTENTION);
            if (!TaczGuardCompat.applyHold(actor, guard.getHeldItem(), model)) {
                // A custom animation may have modified a part before throwing. Do not leak it.
                applyPose(model, state.getValue(HonorGuardBlock.POSE));
            }
        } else {
            applyPose(model, state.getValue(HonorGuardBlock.POSE));
        }
        copySkinLayers(model);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(skin.texture())),
            packedLight, packedOverlay, -1);
        renderItemInHand(actor, guard.getHeldItem(), model, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static void applyPose(PlayerModel<LivingEntity> model, GuardPose pose) {
        // Models are shared by all statues. Reset positions as well as rotations: gun pack poses
        // (e.g. miniguns) can move the arms and twist the torso, including on slim skins.
        for (var part : java.util.List.of(model.head, model.hat, model.body, model.jacket,
                model.rightArm, model.rightSleeve, model.leftArm, model.leftSleeve,
                model.rightLeg, model.rightPants, model.leftLeg, model.leftPants)) part.resetPose();
        // EntityModel.young defaults to true and the baby branch halves the model
        model.young = false;
        model.riding = false;
        model.crouching = false;
        model.attackTime = 0;
        model.swimAmount = 0;
        model.head.xRot = 0.0F;
        model.head.yRot = 0.0F;
        model.head.zRot = 0.0F;
        model.rightArm.xRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.xRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = 0.0F;
        switch (pose) {
            case SALUTE -> {
                model.rightArm.zRot = 0.35F;
                model.rightArm.xRot = -2.5F;
            }
            case PRESENT -> {
                model.rightArm.zRot = 0.1F;
                model.rightArm.xRot = -1.25F;
            }
            case RAISE -> {
                model.rightArm.zRot = 0.15F;
                model.rightArm.xRot = -2.95F;
            }
            default -> { }
        }
    }

    private static void copySkinLayers(PlayerModel<LivingEntity> model) {
        // The second skin layer lives on separate parts, so it has to be dragged along with the
        // joints every frame: without this the sleeves and jacket stay in the rest pose and the
        // overlay looks like it never updated.
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    /** Draws the carried item in the guard's right hand, following that arm's current pose. */
    private static void renderItemInHand(RemotePlayer actor, ItemStack stack, PlayerModel<LivingEntity> model,
                                         PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (stack.isEmpty() || actor == null) return;
        poseStack.pushPose();
        model.translateToHand(HumanoidArm.RIGHT, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(1.0F / 16.0F, 0.125F, -0.625F);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(actor,
            actor.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private RemotePlayer prepareRenderPlayer(HonorGuardBlockEntity guard) {
        var level = Minecraft.getInstance().level;
        if (level == null || guard.getHeldItem().isEmpty()) return null;
        if (renderPlayer == null || renderPlayer.level() != level) renderPlayer = new RemotePlayer(level, RENDER_PROFILE);
        // Give item model overrides the same entity context that a player's ItemInHandLayer uses.
        // A copy keeps rendering callbacks from mutating the statue's stored gun/attachments/ammo.
        renderPlayer.setItemSlot(EquipmentSlot.MAINHAND, guard.getHeldItem().copy());
        renderPlayer.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        renderPlayer.setPos(guard.getBlockPos().getX() + .5, guard.getBlockPos().getY(), guard.getBlockPos().getZ() + .5);
        float yaw = guard.getBlockState().getValue(HonorGuardBlock.FACING).toYRot();
        renderPlayer.setYRot(yaw);
        renderPlayer.yBodyRot = renderPlayer.yBodyRotO = yaw;
        renderPlayer.yHeadRot = renderPlayer.yHeadRotO = yaw;
        renderPlayer.setXRot(0);
        renderPlayer.setOnGround(true);
        return renderPlayer;
    }

    private static PlayerSkin resolveSkin(HonorGuardBlockEntity guard) {
        var local = guard.getLocalSkin();
        if (!local.isEmpty()) {
            var imported = LocalSkins.skin(local);
            if (imported != null) return imported;
        }
        var profile = guard.getProfile();
        if (profile == null || !profile.isResolved()) return FALLBACK;
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile.gameProfile());
    }
}
