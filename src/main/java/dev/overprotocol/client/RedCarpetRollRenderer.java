package dev.overprotocol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overprotocol.Overprotocol;
import dev.overprotocol.vehicle.RedCarpetRollEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Round cloth roll, exposed layered ends and a small loose lip; radius follows remaining length. */
public final class RedCarpetRollRenderer extends EntityRenderer<RedCarpetRollEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID,"textures/block/red_velvet.png");
    public RedCarpetRollRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius=1F; }
    @Override public void render(RedCarpetRollEntity roll,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light) {
        stack.pushPose();stack.mulPose(Axis.YP.rotationDegrees(-roll.getYRot()));
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var pose=stack.last();float r=RedCarpetRollEntity.radius(roll.remaining());
        for(int i=0;i<48;i++) {
            double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;
            var p=circle(-RedCarpetRollEntity.HALF_WIDTH,r,a,r);var q=circle(-RedCarpetRollEntity.HALF_WIDTH,r,b,r);
            var normal=new Vector3f(0,(float)Math.sin((a+b)/2),(float)Math.cos((a+b)/2));
            quad(consumer,pose,p,q,new Vector3f(RedCarpetRollEntity.HALF_WIDTH,q.y,q.z),new Vector3f(RedCarpetRollEntity.HALF_WIDTH,p.y,p.z),normal,0xFFFFFFFF,light);
            for(int side:new int[]{-1,1}) {
                float x=side*RedCarpetRollEntity.HALF_WIDTH;
                var centre=new Vector3f(x,r,0);
                quad(consumer,pose,centre,circle(x,r,a,r),circle(x,r,b,r),circle(x,r,b,r),
                    new Vector3f(side,0,0),0xFFD7B9B9,light);
                quad(consumer,pose,centre,circle(x,r,a,.065F),circle(x,r,b,.065F),circle(x,r,b,.065F),
                    new Vector3f(side,0,0),0xFF7E635A,light);
            }
        }
        for(int side:new int[]{-1,1}) for(int i=0;i<180;i++) {
            double a=i*Math.PI*10/180,b=(i+1)*Math.PI*10/180;
            float ra=.069F+(r-.073F)*i/180,rb=.069F+(r-.073F)*(i+1)/180;
            float x=side*(RedCarpetRollEntity.HALF_WIDTH+.0005F);
            quad(consumer,pose,circle(x,r,a,ra-.003F),circle(x,r,a,ra+.003F),
                circle(x,r,b,rb+.003F),circle(x,r,b,rb-.003F),new Vector3f(side,0,0),0xFF713033,light);
        }
        quad(consumer,pose,new Vector3f(-RedCarpetRollEntity.HALF_WIDTH,.0625F,RedCarpetRollEntity.BACK_EDGE),
            new Vector3f(RedCarpetRollEntity.HALF_WIDTH,.0625F,RedCarpetRollEntity.BACK_EDGE),
            new Vector3f(RedCarpetRollEntity.HALF_WIDTH,.0625F,0),new Vector3f(-RedCarpetRollEntity.HALF_WIDTH,.0625F,0),
            new Vector3f(0,1,0),0xFFFFFFFF,light);
        stack.popPose();super.render(roll,yaw,partial,stack,buffers,light);
    }
    private static Vector3f circle(float x,float centreY,double angle,float radius) {
        return new Vector3f(x,centreY+(float)Math.sin(angle)*radius,(float)Math.cos(angle)*radius);
    }
    private static void quad(VertexConsumer c,PoseStack.Pose pose,Vector3f a,Vector3f b,Vector3f d,Vector3f e,Vector3f normal,int color,int light) {
        vertex(c,pose,a,0,0,normal,color,light);vertex(c,pose,b,1,0,normal,color,light);
        vertex(c,pose,d,1,1,normal,color,light);vertex(c,pose,e,0,1,normal,color,light);
    }
    private static void vertex(VertexConsumer c,PoseStack.Pose pose,Vector3f p,float u,float v,Vector3f normal,int color,int light) {
        c.addVertex(pose,p.x,p.y,p.z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light).setNormal(pose,normal.x,normal.y,normal.z);
    }
    @Override public ResourceLocation getTextureLocation(RedCarpetRollEntity roll) { return TEXTURE; }
}
