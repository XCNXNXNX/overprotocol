package dev.overprotocol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overprotocol.Overprotocol;
import dev.overprotocol.vehicle.B2Geometry;
import dev.overprotocol.vehicle.B2Gear;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;

/** Original lofted flying-wing mesh. Negative Z is the nose; units are full-size Minecraft blocks. */
final class B2Mesh {
    private record Face(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,Vec3 na,Vec3 nb,Vec3 nc,Vec3 nd) {}
    private record MovingPart(String name,List<Face> faces,B2Gear.Bay bay,boolean door,
                              java.util.function.Function<Float,B2Gear.Transform> pose) {}
    private static final List<Face> BODY=new ArrayList<>(), LIGHTS=new ArrayList<>();
    private static final List<MovingPart> MOVING=new ArrayList<>();
    private static final int SKIN=0xFF515961, EDGE=0xFF343C45, BELLY=0xFF3E464F,
        METAL=0xFF929CA8, TIRE=0xFF151A20, GLASS=0xFF1D303A, MARK=0xFF89949B;

    static {
        wing(); cockpit(); engines(); undercarriage(); panels();
        box(LIGHTS,-25.92,2.49,7.88,-25.80,2.54,7.91,0xFFD5463E);
        box(LIGHTS,25.80,2.49,7.88,25.92,2.54,7.91,0xFF5BA77C);
        box(LIGHTS,-.06,2.50,9.05,.06,2.55,9.13,0xFFECE7D6);
    }

    private static void wing() {
        var xs=new TreeSet<Double>();
        for(var station:B2Geometry.STATIONS) { xs.add(station[0]);if(station[0]!=0)xs.add(-station[0]); }
        for(double x:new double[]{.45,1.0,1.7,2.3,2.72,2.85,3.4,4.12,4.82,5.39,5.52,5.7,7.2}) {
            xs.add(x);xs.add(-x);
        }
        var corners=new ArrayList<>(xs);
        for(int i=1;i<corners.size();i++) {
            double a=corners.get(i-1),b=corners.get(i);
            int count=(int)Math.ceil((b-a)/(Math.max(Math.abs(a),Math.abs(b))<7.3?.17:.54));
            for(int j=1;j<count;j++)xs.add(a+(b-a)*j/count);
        }
        var spans=new ArrayList<>(xs);
        for(int i=1;i<spans.size();i++) {
            double x1=spans.get(i-1),x2=spans.get(i);
            // Concentrate samples around the cockpit/inlets; smooth normals remove polygon bands.
            boolean centre=Math.max(Math.abs(x1),Math.abs(x2))<7.3;
            int strips=centre?106:36;
            for(int j=0;j<strips;j++) {
                double t1=j/(double)strips,t2=(j+1)/(double)strips;
                if(centre)curvedQuad(BODY,surface(x1,sectionZ(x1,j),0),surface(x1,sectionZ(x1,j+1),0),
                    surface(x2,sectionZ(x2,j+1),0),surface(x2,sectionZ(x2,j),0),SKIN,true);
                else curvedQuad(BODY,skin(x1,t1,true),skin(x1,t2,true),skin(x2,t2,true),skin(x2,t1,true),SKIN,true);
                bellyQuad(skin(x1,t1,false),skin(x2,t1,false),skin(x2,t2,false),skin(x1,t2,false));
            }
            quad(BODY,skin(x1,0,false),skin(x2,0,false),skin(x2,0,true),skin(x1,0,true),EDGE);
            quad(BODY,skin(x1,1,true),skin(x2,1,true),skin(x2,1,false),skin(x1,1,false),EDGE);
        }
        for(int side:new int[]{-1,1}) {
            double x=side*B2Geometry.HALF_SPAN;
            quad(BODY,skin(x,0,true),skin(x,1,true),skin(x,1,false),skin(x,0,false),EDGE);
        }
    }
    private static Vec3 skin(double x,double chord,boolean top) {
        return v(x,top?B2Geometry.top(x,chord):B2Geometry.bottom(x,chord),B2Geometry.z(x,chord));
    }
    /** Align the body tessellation with both saw-toothed intake lips and exhaust ramp ends. */
    private static double sectionZ(double x,int row) {
        double a=B2Geometry.intakeFront(Math.abs(x)),b=B2Geometry.intakeBack(Math.abs(x));
        double[] anchors={B2Geometry.leading(x),a-.08,a+.16,b-.16,b+.08,4.35,5.10,B2Geometry.trailing(x)};
        int[] counts={34,4,6,4,40,6,12};
        for(int i=0;i<counts.length;i++) {
            if(row<=counts[i])return anchors[i]+(anchors[i+1]-anchors[i])*row/counts[i];
            row-=counts[i];
        }
        return anchors[anchors.length-1];
    }

    private static void cockpit() {
        // Six flush panes follow the same rounded surface as the central fuselage.
        for(int sign:new int[]{-1,1}) {
            surfacePatch(new double[][]{{sign*.052,-8.90},{sign*.83,-8.45},{sign*.89,-7.49},{sign*.052,-7.62}},GLASS,.028);
            surfacePatch(new double[][]{{sign*.90,-8.39},{sign*1.31,-7.95},{sign*1.42,-7.04},{sign*.98,-7.44}},0xFF263B47,.028);
            surfacePatch(new double[][]{{sign*1.37,-7.87},{sign*1.59,-7.13},{sign*1.62,-6.28},{sign*1.47,-6.98}},0xFF293E48,.028);
            line(new double[][]{{sign*.31,-8.49},{sign*.66,-7.86}},.017,0xFF818C91);
            line(new double[][]{{sign*.31,-8.49},{sign*.54,-8.29}},.025,EDGE);
            // Ejection hatches sit on the curved canopy roof, not raised cubes.
            line(new double[][]{{sign*.22,-6.42},{sign*.95,-6.26},{sign*1.01,-5.11},{sign*.23,-5.27},{sign*.22,-6.42}},.018,0xFF77838C);
        }
        line(new double[][]{{-.43,-9.55},{0,-9.72},{.43,-9.55}},.025,MARK);
    }

    private static void engines() {
        for(int sign:new int[]{-1,1}) {
            for(int i=0;i<28;i++) {
                double a=2.75+i*.098,b=a+.098;
                // Recessed, saw-toothed mouths. The central thin divider splits each pair.
                for(int j=0;j<8;j++) {
                    double ta=.02+j*.12,tb=ta+.12;
                    curvedQuad(BODY,surface(sign*a,B2Geometry.intakeFront(a)+ta*.86,.035),
                        surface(sign*a,B2Geometry.intakeFront(a)+tb*.86,.035),
                        surface(sign*b,B2Geometry.intakeFront(b)+tb*.86,.035),
                        surface(sign*b,B2Geometry.intakeFront(b)+ta*.86,.035),0xFF111C25,true);
                }
            }
            var lip=new double[5][2];
            for(int j=0;j<5;j++) {double x=2.74+j*.69;lip[j]=new double[]{sign*x,B2Geometry.intakeBack(x)};}
            line(lip,.035,0xFF77818A);
            double split=4.12;
            rod(BODY,surface(sign*split,B2Geometry.intakeFront(split),.025),
                surface(sign*split,B2Geometry.intakeBack(split),.025),.022,6,EDGE);
            // Four sunken exhaust channels, two on each side, following the aft V notches.
            for(int channel=0;channel<2;channel++) {
                double inner=2.87+channel*1.31,outer=inner+1.20;
                for(int i=0;i<10;i++) {
                    double x1=inner+(outer-inner)*i/10,x2=inner+(outer-inner)*(i+1)/10;
                    double rear1=B2Geometry.trailing(x1)-.045,rear2=B2Geometry.trailing(x2)-.045;
                    for(int j=0;j<16;j++) {
                        double z1=4.45+(rear1-4.45)*j/16,z2=4.45+(rear1-4.45)*(j+1)/16;
                        double z3=4.45+(rear2-4.45)*(j+1)/16,z4=4.45+(rear2-4.45)*j/16;
                        int color=j<4?0xFF19232B:(i==0||i==9?0xFF8B8980:(i%3==0?0xFF747978:0xFF646D70));
                        curvedQuad(BODY,surface(sign*x1,z1,.018),surface(sign*x1,z2,.018),
                            surface(sign*x2,z3,.018),surface(sign*x2,z4,.018),color,true);
                    }
                }
                line(new double[][]{{sign*inner,4.46},{sign*inner,B2Geometry.trailing(inner)-.04}},.025,0xFF9A9B90);
                line(new double[][]{{sign*outer,4.46},{sign*outer,B2Geometry.trailing(outer)-.04}},.025,0xFF9A9B90);
            }
        }
    }

    private static void undercarriage() {
        for(var bay:B2Gear.BAYS)wheelWell(bay);
        for(int sign:new int[]{-1,1}) {
            var bay=sign<0?B2Gear.LEFT:B2Gear.RIGHT;
            var leg=moving(bay.name()+"_strut",bay,false,amount->B2Gear.mainLeg(sign,amount));
            var bogie=moving(bay.name()+"_bogie",bay,false,amount->B2Gear.mainBogie(sign,amount));
            double x=sign*B2Gear.MAIN_X,y=B2Gear.MAIN_Y,z=B2Gear.MAIN_Z;
            var hinge=B2Gear.mainHinge(sign);
            // Fixed trunnion and roof attachment remain connected while the lower leg folds.
            rod(BODY,hinge,v(x,bay.roof(x,z)-.02,z),.145,12,0xFF566570);
            rod(BODY,v(x,y,z-1.04),v(x,y,z+.20),.15,12,0xFF677580);
            rod(leg,hinge,v(x,1.06,z),.115,12,METAL);
            rod(leg,v(x,1.17,z),B2Gear.mainAxle(sign),.078,12,0xFFC2CAD0);
            rod(leg,v(x,y,z-.98),v(x,.98,z),.074,10,METAL);
            rod(leg,v(x,1.31,z),v(x,1.02,z+.20),.042,8,0xFF798B95);
            rod(leg,v(x,1.02,z+.20),v(x,.80,z),.042,8,0xFF798B95);
            for(double axleZ:new double[]{z-.60,z+.60}) {
                rod(bogie,v(x-.69,.63,axleZ),v(x+.69,.63,axleZ),.10,10,METAL);
                for(double dx:new double[]{-.66,.66})wheel(bogie,x+dx,.63,axleZ,.63,.34);
            }
            rod(bogie,v(x,.63,z-.60),v(x,.63,z+.60),.14,10,0xFF697583);
        }
        var nose=moving("nose_gear",B2Gear.NOSE,false,B2Gear::nose);
        var root=B2Gear.NOSE_HINGE;
        rod(BODY,root,v(0,B2Gear.NOSE.roof(0,root.z)-.02,root.z),.13,12,0xFF566570);
        rod(BODY,root.add(-.29,0,0),root.add(.29,0,0),.125,12,0xFF677580);
        rod(nose,root,v(0,1.01,-6.90),.105,12,METAL);
        rod(nose,v(0,1.13,-6.87),B2Gear.NOSE_AXLE,.074,12,0xFFC2CAD0);
        rod(nose,v(0,1.27,-6.84),v(0,.91,-7.15),.04,8,METAL);
        rod(nose,v(0,.91,-7.15),v(0,.60,-7.00),.04,8,METAL);
        rod(nose,v(-.4,.46,-7.03),v(.4,.46,-7.03),.08,10,METAL);
        wheel(nose,-.32,.46,-7.03,.46,.24);wheel(nose,.32,.46,-7.03,.46,.24);
    }
    private static List<Face> moving(String name,B2Gear.Bay bay,boolean door,java.util.function.Function<Float,B2Gear.Transform> pose) {
        var faces=new ArrayList<Face>();MOVING.add(new MovingPart(name,faces,bay,door,pose));return faces;
    }
    private static void wheel(List<Face> target,double x,double y,double z,double radius,double width) {
        Vec3 a=v(x-width/2,y,z),b=v(x+width/2,y,z);
        rod(target,a,b,radius,20,TIRE);
        for(int sign:new int[]{-1,1}) {
            double xx=x+sign*(width/2+.008);
            rod(target,v(xx,y,z),v(xx+sign*.012,y,z),radius*.46,20,0xFF78838E);
            for(int i=0;i<8;i++) {
                double angle=i*Math.PI/4;
                rod(target,v(xx+sign*.022,y,z),v(xx+sign*.022,y+Math.cos(angle)*radius*.4,z+Math.sin(angle)*radius*.4),.017,6,0xFF303C48);
            }
        }
    }

    private static void wheelWell(B2Gear.Bay bay) {
        double x0=bay.minX(),x1=bay.maxX(),z0=bay.minZ(),z1=bay.maxZ(),y=bay.floor();
        // These are real openings in the lower skin, with walls and a roof below the upper skin.
        for(int i=0;i<16;i++)for(int j=0;j<16;j++) {
            double a=x0+(x1-x0)*i/16,b=x0+(x1-x0)*(i+1)/16;
            double c=z0+(z1-z0)*j/16,d=z0+(z1-z0)*(j+1)/16;
            quad(BODY,v(a,bay.roof(a,c),c),v(b,bay.roof(b,c),c),v(b,bay.roof(b,d),d),v(a,bay.roof(a,d),d),0xFF26313A);
        }
        for(int i=0;i<16;i++) {
            double a=x0+(x1-x0)*i/16,b=x0+(x1-x0)*(i+1)/16;
            quad(BODY,v(a,y,z0),v(b,y,z0),v(b,bay.roof(b,z0),z0),v(a,bay.roof(a,z0),z0),0xFF46545E);
            quad(BODY,v(a,y,z1),v(a,bay.roof(a,z1),z1),v(b,bay.roof(b,z1),z1),v(b,y,z1),0xFF46545E);
            double c=z0+(z1-z0)*i/16,d=z0+(z1-z0)*(i+1)/16;
            quad(BODY,v(x0,y,c),v(x0,bay.roof(x0,c),c),v(x0,bay.roof(x0,d),d),v(x0,y,d),0xFF46545E);
            quad(BODY,v(x1,y,c),v(x1,y,d),v(x1,bay.roof(x1,d),d),v(x1,bay.roof(x1,c),c),0xFF46545E);
        }
        box(BODY,x0-.055,y-.018,z0-.055,x0+.015,y+.025,z1+.055,EDGE);
        box(BODY,x1-.015,y-.018,z0-.055,x1+.055,y+.025,z1+.055,EDGE);
        box(BODY,x0,y-.018,z0-.055,x1,y+.025,z0+.015,EDGE);
        box(BODY,x0,y-.018,z1-.015,x1,y+.025,z1+.055,EDGE);
        for(boolean second:new boolean[]{false,true}) {
            var door=moving(bay.name()+"_door_"+(second?"b":"a"),bay,true,amount->B2Gear.door(bay,second,amount));
            double a=x0+.018,b=x1-.018,c=z0+.018,d=z1-.018;
            if(bay==B2Gear.NOSE) {if(second)a=.009;else b=-.009;}
            else {if(second)c=(z0+z1)/2+.009;else d=(z0+z1)/2-.009;}
            box(door,a,y-.018,c,b,y+.018,d,BELLY);
            // Inner stiffener, visible only with the bay open.
            box(door,a+.08,y+.018,c+.10,b-.08,y+.045,d-.10,0xFF62717B);
        }
    }

    /** Subtract each rectangular wheel opening from lower-skin polygons before triangulation. */
    private static void bellyQuad(Vec3 a,Vec3 b,Vec3 c,Vec3 d) {
        List<List<Vec3>> pieces=new ArrayList<>();pieces.add(List.of(a,b,c,d));
        for(var bay:B2Gear.BAYS) {
            var next=new ArrayList<List<Vec3>>();
            for(var polygon:pieces)subtractBay(polygon,bay,next);
            pieces=next;
        }
        for(var polygon:pieces) {
            if(polygon.size()==4)curvedQuad(BODY,polygon.get(0),polygon.get(1),polygon.get(2),polygon.get(3),BELLY,false);
            else for(int i=1;i<polygon.size()-1;i++)
                curvedQuad(BODY,polygon.get(0),polygon.get(i),polygon.get(i+1),polygon.get(i+1),BELLY,false);
        }
    }
    private static void subtractBay(List<Vec3> poly,B2Gear.Bay bay,List<List<Vec3>> output) {
        if(poly.stream().allMatch(p->p.x<=bay.minX()) || poly.stream().allMatch(p->p.x>=bay.maxX())
            || poly.stream().allMatch(p->p.z<=bay.minZ()) || poly.stream().allMatch(p->p.z>=bay.maxZ())) {
            output.add(poly);return;
        }
        var remaining=poly;
        double[] bounds={bay.minX(),bay.maxX(),bay.minZ(),bay.maxZ()};
        for(int side=0;side<4 && remaining.size()>=3;side++) {
            boolean xAxis=side<2,keepGreater=side%2==1;
            var outside=clip(remaining,xAxis,bounds[side],keepGreater);
            if(outside.size()>=3)output.add(outside);
            remaining=clip(remaining,xAxis,bounds[side],!keepGreater);
        }
    }
    private static List<Vec3> clip(List<Vec3> poly,boolean xAxis,double edge,boolean greater) {
        var result=new ArrayList<Vec3>();
        if(poly.isEmpty())return result;
        var previous=poly.getLast();double before=(xAxis?previous.x:previous.z)-edge;
        for(var current:poly) {
            double after=(xAxis?current.x:current.z)-edge;
            boolean a=greater?before>=0:before<=0,b=greater?after>=0:after<=0;
            if(a!=b)result.add(previous.lerp(current,before/(before-after)));
            if(b)result.add(current);
            previous=current;before=after;
        }
        return result;
    }

    private static void panels() {
        for(int sign:new int[]{-1,1}) {
            // Paired walkways trace the photo's large cranked outline and engine shoulders.
            double[][] walkway={{23.10,7.62},{22.35,8.05},{13.08,1.08},{6.12,6.20},
                {6.18,.38},{7.65,-2.72},{7.52,-3.18},{4.18,-6.39}};
            for(int parallel=0;parallel<2;parallel++) {
                var path=new double[walkway.length][2];
                for(int i=0;i<path.length;i++)path[i]=new double[]{sign*(walkway[i][0]+parallel*.15),walkway[i][1]+parallel*.18};
                line(path,.023,MARK);
            }
            // Hinges and independent elevon ends belong near the trailing edge, not in a grid.
            line(mirror(new double[][]{{6.58,7.18},{13.07,2.34},{22.41,9.31},{25.09,7.37}},sign),.045,0xFF2D3741);
            for(double x:new double[]{8.60,10.78,16.95,19.95,23.50})
                line(mirror(new double[][]{{x,B2Geometry.trailing(x)-1.17},{x,B2Geometry.trailing(x)-.08}},sign),.028,EDGE);
            for(double z:new double[]{-1.50,-.96})
                line(mirror(new double[][]{{2.85,z},{3.60,z},{3.60,z+.29},{2.85,z+.29},{2.85,z}},sign),.014,MARK);
            line(mirror(new double[][]{{6.44,-1.58},{6.83,-1.58},{6.83,-1.06},{6.44,-1.06},{6.44,-1.58}},sign),.016,MARK);
            double x=sign*1.72;
            box(BODY,x-.02,1.88,-1.52,x+.02,1.91,5.41,0xFF202B36);
            for(double z:new double[]{-1.52,5.41})box(BODY,x-1.13,1.89,z-.02,x+1.13,1.92,z+.02,0xFF202B36);
            insignia(sign*18.55,5.78);
        }
        line(new double[][]{{-.24,-.22},{.24,-.22},{.32,.54},{-.32,.54},{-.24,-.22}},.020,MARK);
        line(new double[][]{{0,-.18},{0,.43}},.027,EDGE);
        for(int sign:new int[]{-1,1}) {
            line(new double[][]{{sign*.12,-3.78},{sign*.48,-3.78},{sign*.51,-3.03},{sign*.12,-3.03}},.015,MARK);
            for(int i=0;i<4;i++)line(new double[][]{{sign*.48,-2.5+i*.17},{sign*.70,-2.5+i*.17}},.017,MARK);
        }
    }

    private static void insignia(double x,double z) {
        var centre=surface(x,z,.020);
        for(int i=0;i<10;i++) {
            double a=i*Math.PI/5-Math.PI/2,b=(i+1)*Math.PI/5-Math.PI/2;
            double ra=i%2==0?.26:.115,rb=i%2==0?.115:.26;
            var p=surface(x+Math.cos(a)*ra,z+Math.sin(a)*ra,.020);
            var q=surface(x+Math.cos(b)*rb,z+Math.sin(b)*rb,.020);
            curvedQuad(BODY,centre,p,q,q,0xFFA2A9AD,true);
        }
        for(int sign:new int[]{-1,1})surfacePatch(new double[][]{{x+sign*.20,z-.065},{x+sign*.49,z-.065},
            {x+sign*.49,z+.065},{x+sign*.20,z+.065}},0xFF9AA3A8,.020);
    }
    private static double[][] mirror(double[][] points,int sign) {
        var result=new double[points.length][2];
        for(int i=0;i<points.length;i++)result[i]=new double[]{points[i][0]*sign,points[i][1]};
        return result;
    }
    private static Vec3 surface(double x,double z,double offset) {return v(x,B2Geometry.topAt(x,z)+offset,z);}
    /** Thin surface ribbons follow the curved skin without cylindrical seams or floating ends. */
    private static void line(double[][] path,double width,int color) {
        for(int i=1;i<path.length;i++) {
            var a=path[i-1];var b=path[i];double dx=b[0]-a[0],dz=b[1]-a[1],length=Math.hypot(dx,dz);
            if(length<.0001)continue;
            double nx=-dz/length*width/2,nz=dx/length*width/2;int pieces=(int)Math.ceil(length/.18);
            for(int j=0;j<pieces;j++) {
                double x1=a[0]+dx*j/pieces,z1=a[1]+dz*j/pieces,x2=a[0]+dx*(j+1)/pieces,z2=a[1]+dz*(j+1)/pieces;
                curvedQuad(BODY,surface(x1+nx,z1+nz,.042),surface(x2+nx,z2+nz,.042),
                    surface(x2-nx,z2-nz,.042),surface(x1-nx,z1-nz,.042),color,true);
            }
        }
    }
    private static void surfacePatch(double[][] corners,int color,double offset) {
        for(int i=0;i<8;i++)for(int j=0;j<8;j++) {
            curvedQuad(BODY,patchPoint(corners,i/8.0,j/8.0,offset),patchPoint(corners,(i+1)/8.0,j/8.0,offset),
                patchPoint(corners,(i+1)/8.0,(j+1)/8.0,offset),patchPoint(corners,i/8.0,(j+1)/8.0,offset),color,true);
        }
    }
    private static Vec3 patchPoint(double[][] c,double u,double t,double offset) {
        double x=(1-t)*((1-u)*c[0][0]+u*c[1][0])+t*((1-u)*c[3][0]+u*c[2][0]);
        double z=(1-t)*((1-u)*c[0][1]+u*c[1][1])+t*((1-u)*c[3][1]+u*c[2][1]);
        return surface(x,z,offset);
    }

    static void render(PoseStack stack,VertexConsumer vertices,int light,float gear) {
        draw(BODY,stack.last(),vertices,light);
        draw(LIGHTS,stack.last(),vertices,LightTexture.FULL_BRIGHT);
        for(var part:MOVING) {
            var transform=part.pose.apply(gear);var p=transform.pivot();var offset=transform.offset();
            stack.pushPose();
            stack.translate(p.x+offset.x,p.y+offset.y,p.z+offset.z);
            stack.mulPose((transform.xAxis()?Axis.XP:Axis.ZP).rotation((float)transform.angle()));
            stack.translate(-p.x,-p.y,-p.z);
            draw(part.faces,stack.last(),vertices,light);
            stack.popPose();
        }
    }
    static void initialize() {
        var path=System.getProperty("overprotocol.b2.modelExport");
        if(path!=null)try { exportObj(Path.of(path),1); } catch(IOException error) {
            Overprotocol.LOGGER.warn("Cannot export development B-2 mesh",error);
        }
        Overprotocol.LOGGER.info("B-2 lofted mesh: {} body faces, {} articulated gear/door faces",BODY.size(),movingFaceCount());
    }
    public static void main(String[] args)throws IOException {
        var path=Path.of(args[0]);
        verifyGearMotion(path.resolveSibling("b2-gear-check.json"));
        exportObj(path,1);
        exportObj(path.resolveSibling("b2-gear-folding.obj"),.55F);
        exportObj(path.resolveSibling("b2-gear-doors.obj"),.11F);
        exportObj(path.resolveSibling("b2-gear-stowed.obj"),0);
        System.out.println("B-2 mesh exported: "+BODY.size()+" body, "+movingFaceCount()+" articulated gear/doors, "+LIGHTS.size()+" lights");
    }
    private static int movingFaceCount() { return MOVING.stream().mapToInt(p->p.faces.size()).sum(); }
    private static LinkedHashMap<String,List<Face>> posedParts(float gear) {
        var result=new LinkedHashMap<String,List<Face>>();result.put("airframe",BODY);result.put("lights",LIGHTS);
        for(var part:MOVING) {
            var transform=part.pose.apply(gear);var faces=new ArrayList<Face>();
            for(var f:part.faces)faces.add(new Face(transform.apply(f.a),transform.apply(f.b),transform.apply(f.c),transform.apply(f.d),
                f.color,transform.rotate(f.na),transform.rotate(f.nb),transform.rotate(f.nc),transform.rotate(f.nd)));
            result.put(part.name,faces);
        }
        return result;
    }
    private static void verifyGearMotion(Path report)throws IOException {
        long checked=0;
        for(int frame=0;frame<=40;frame++) {
            float amount=frame/40F;
            for(var part:MOVING) {
                var transform=part.pose.apply(amount);var bay=part.bay;
                for(var face:part.faces)for(var point:List.of(face.a,face.b,face.c,face.d)) {
                    var p=transform.apply(point);checked++;
                    if(!Double.isFinite(p.x+p.y+p.z))throw new IllegalStateException("Non-finite gear vertex");
                    if(p.y < -.003)throw new IllegalStateException("Gear/door below ground: "+part.name+" at "+amount+" "+p);
                    if(part.door) {
                        if(p.y>B2Geometry.bottomAt(p.x,p.z)+.065 && !bay.contains(p.x,p.z,.035))
                            throw new IllegalStateException("Door penetrates belly: "+part.name+" at "+amount+" "+p);
                    } else {
                        if(p.y>bay.floor()+.03 && !bay.contains(p.x,p.z,.025))
                            throw new IllegalStateException("Gear misses bay: "+part.name+" at "+amount+" "+p);
                        if(p.y>bay.roof(p.x,p.z)+.005)
                            throw new IllegalStateException("Gear pierces bay roof: "+part.name+" at "+amount+" "+p);
                        if(frame==0 && (p.y<bay.floor()+.04 || !bay.contains(p.x,p.z,0)))
                            throw new IllegalStateException("Stowed gear protrudes: "+part.name+" "+p);
                    }
                }
            }
            for(int side:new int[]{-1,1}) {
                var hinge=B2Gear.mainHinge(side);var axle=B2Gear.mainAxle(side);
                if(B2Gear.mainLeg(side,amount).apply(hinge).distanceTo(hinge)>1e-6
                    || B2Gear.mainLeg(side,amount).apply(axle).distanceTo(B2Gear.mainBogie(side,amount).apply(axle))>1e-6)
                    throw new IllegalStateException("Disconnected main gear joint");
            }
            if(B2Gear.nose(amount).apply(B2Gear.NOSE_HINGE).distanceTo(B2Gear.NOSE_HINGE)>1e-6)
                throw new IllegalStateException("Disconnected nose hinge");
        }
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var faces:posedParts(1).values())for(var face:faces)for(var p:List.of(face.a,face.b,face.c,face.d)) {
            double[] xyz={p.x,p.y,p.z};for(int i=0;i<3;i++){min[i]=Math.min(min[i],xyz[i]);max[i]=Math.max(max[i],xyz[i]);}
        }
        if(Math.abs(max[0]-min[0]-52.12)>.001 || Math.abs(max[2]-min[2]-20.9)>.001
            || Math.abs(max[1]-min[1]-5.1)>.006 || Math.abs(min[1])>.003)
            throw new IllegalStateException("Full-size dimensions or tyre contact changed");
        Files.createDirectories(report.getParent());
        Files.writeString(report,String.format(Locale.ROOT,
            "{\n  \"motion_samples\":41,\n  \"checked_vertices\":%d,\n  \"clearance_passed\":true,\n  \"joints_connected\":true,\n  \"dimensions_xyz\":[%.6f,%.6f,%.6f],\n  \"ground_contact_y\":%.6f\n}\n",
            checked,max[0]-min[0],max[1]-min[1],max[2]-min[2],min[1]));
        System.out.println("B-2 gear clearance and hinge checks passed across 41 poses; "+checked+" vertices checked");
    }
    private static void exportObj(Path path,float gear)throws IOException {
        Files.createDirectories(path.getParent());
        String materialName=path.getFileName().toString().replaceFirst("\\.obj$","")+".mtl";
        var text=new StringBuilder("# Overprotocol original full-size B-2; units=blocks/metres; negative Z=nose\n");
        text.append("mtllib ").append(materialName).append('\n');
        var materials=new StringBuilder("# Original mesh vertex colours exported as OBJ materials\n");
        var colors=new TreeSet<Integer>();
        var parts=posedParts(gear);
        for(var faces:parts.values())for(var face:faces)colors.add(face.color);
        for(int color:colors) {
            materials.append(String.format(Locale.ROOT,"newmtl color_%08x\nKd %.6f %.6f %.6f\nKa 0.15 0.15 0.15\nillum 1\n\n",
                color,((color>>16)&255)/255.0,((color>>8)&255)/255.0,(color&255)/255.0));
        }
        int index=1,normal=1;
        for(var part:parts.entrySet()) {
            var faces=part.getValue();text.append("g ").append(part.getKey()).append('\n');
            for(var face:faces) {
                text.append(String.format(Locale.ROOT,"usemtl color_%08x\n",face.color));
                for(var p:List.of(face.a,face.b,face.c,face.d))text.append(String.format(Locale.ROOT,"v %.6f %.6f %.6f\n",p.x,p.y,p.z));
                for(var n:List.of(face.na,face.nb,face.nc,face.nd))text.append(String.format(Locale.ROOT,"vn %.6f %.6f %.6f\n",n.x,n.y,n.z));
                text.append("f");
                for(int j=0;j<4;j++)text.append(' ').append(index+j).append("//").append(normal+j);
                text.append('\n');index+=4;normal+=4;
            }
        }
        Files.writeString(path.resolveSibling(materialName),materials);
        Files.writeString(path,text);
    }
    private static void draw(List<Face> faces,PoseStack.Pose pose,VertexConsumer consumer,int light) {
        for(var face:faces) {
            vertex(consumer,pose,face.a,face.na,face.color,light);vertex(consumer,pose,face.b,face.nb,face.color,light);
            vertex(consumer,pose,face.c,face.nc,face.color,light);vertex(consumer,pose,face.d,face.nd,face.color,light);
        }
    }
    private static void vertex(VertexConsumer consumer,PoseStack.Pose pose,Vec3 point,Vec3 normal,int color,int light) {
        consumer.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(color)
            .setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
    }
    private static Vec3 v(double x,double y,double z) { return new Vec3(x,y,z); }
    private static void quad(List<Face> target,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color) {
        var normal=b.subtract(a).cross(c.subtract(a)).normalize();
        target.add(new Face(a,b,c,d,color,normal,normal,normal,normal));
    }
    private static void curvedQuad(List<Face> target,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,boolean top) {
        target.add(new Face(a,b,c,d,color,surfaceNormal(a,top),surfaceNormal(b,top),surfaceNormal(c,top),surfaceNormal(d,top)));
    }
    private static Vec3 surfaceNormal(Vec3 point,boolean top) {
        double e=.025,x=point.x,z=point.z;
        double dx=top?B2Geometry.topAt(x+e,z)-B2Geometry.topAt(x-e,z):B2Geometry.bottomAt(x+e,z)-B2Geometry.bottomAt(x-e,z);
        double dz=top?B2Geometry.topAt(x,z+e)-B2Geometry.topAt(x,z-e):B2Geometry.bottomAt(x,z+e)-B2Geometry.bottomAt(x,z-e);
        return (top?v(-dx,2*e,-dz):v(dx,-2*e,dz)).normalize();
    }
    private static void rod(List<Face> target,Vec3 start,Vec3 end,double radius,int segments,int color) {
        var axis=end.subtract(start).normalize();
        var basis=axis.cross(Math.abs(axis.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
        var second=axis.cross(basis).normalize();
        for(int i=0;i<segments;i++) {
            double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            var u=basis.scale(Math.cos(a)*radius).add(second.scale(Math.sin(a)*radius));
            var vv=basis.scale(Math.cos(b)*radius).add(second.scale(Math.sin(b)*radius));
            quad(target,start.add(u),start.add(vv),end.add(vv),end.add(u),color);
            quad(target,start,start.add(vv),start.add(u),start.add(u),color);
            quad(target,end,end.add(u),end.add(vv),end.add(vv),color);
        }
    }
    private static void box(List<Face> target,double x1,double y1,double z1,double x2,double y2,double z2,int color) {
        var a=v(x1,y1,z1);var b=v(x2,y1,z1);var c=v(x2,y1,z2);var d=v(x1,y1,z2);
        var e=v(x1,y2,z1);var f=v(x2,y2,z1);var g=v(x2,y2,z2);var h=v(x1,y2,z2);
        quad(target,e,h,g,f,color);quad(target,a,b,c,d,color);quad(target,a,e,f,b,color);
        quad(target,b,f,g,c,color);quad(target,c,g,h,d,color);quad(target,d,h,e,a,color);
    }
    private B2Mesh() {}
}
