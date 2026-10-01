package dev.overprotocol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overprotocol.Overprotocol;
import dev.overprotocol.vehicle.B2Entity;
import dev.overprotocol.vehicle.B2Geometry;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class B2Renderer extends EntityRenderer<B2Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "textures/entity/b2_palette.png");
    public B2Renderer(EntityRendererProvider.Context context) {
        super(context); shadowRadius = 10F;
        B2Mesh.initialize();
        Overprotocol.LOGGER.info("B-2 vehicle renderer and original mesh ready");
    }
    @Override public void render(B2Entity plane, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0,B2Geometry.PIVOT_Y,0);
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, plane.yRotO, plane.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, plane.xRotO, plane.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, plane.previousBank, plane.bank())));
        pose.translate(0,-B2Geometry.PIVOT_Y,0);
        B2Mesh.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, plane.gearAmount(partial));
        pose.popPose();
        super.render(plane,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(B2Entity entity) { return TEXTURE; }
}
