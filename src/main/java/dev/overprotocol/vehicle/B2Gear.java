package dev.overprotocol.vehicle;

import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Visual landing-gear kinematics and openings, in the same metre/block coordinates as the mesh. */
public final class B2Gear {
    public record Bay(String name,double minX,double maxX,double minZ,double maxZ,double floor) {
        public boolean contains(double x,double z,double tolerance) {
            return x>=minX-tolerance && x<=maxX+tolerance && z>=minZ-tolerance && z<=maxZ+tolerance;
        }
        public double roof(double x,double z) { return Math.min(floor+1.70,B2Geometry.topAt(x,z)-.045); }
    }
    public static final Bay LEFT=new Bay("left",-6.75,-2.70,.04,2.80,1.64);
    public static final Bay RIGHT=new Bay("right",2.70,6.75,.04,2.80,1.64);
    public static final Bay NOSE=new Bay("nose",-.72,.72,-7.75,-4.03,1.72);
    public static final List<Bay> BAYS=List.of(LEFT,RIGHT,NOSE);
    public static final double MAIN_X=5.65,MAIN_Y=2.48,MAIN_Z=1.42,MAIN_RADIUS=.63;
    public static final Vec3 NOSE_HINGE=new Vec3(0,2.30,-7.03);
    public static final Vec3 NOSE_AXLE=new Vec3(0,.46,-7.03);

    public record Transform(Vec3 pivot,double angle,boolean xAxis,Vec3 offset) {
        public Vec3 rotate(Vec3 v) {
            double c=Math.cos(angle),s=Math.sin(angle);
            return xAxis?new Vec3(v.x,v.y*c-v.z*s,v.y*s+v.z*c)
                :new Vec3(v.x*c-v.y*s,v.x*s+v.y*c,v.z);
        }
        public Vec3 apply(Vec3 v) { return rotate(v.subtract(pivot)).add(pivot).add(offset); }
    }
    public static double extension(float amount) { return smooth((amount-.22)/.78); }
    public static double doorOpening(float amount) { return smooth(amount/.22); }
    public static Vec3 mainHinge(int side) { return new Vec3(side*MAIN_X,MAIN_Y,MAIN_Z); }
    public static Vec3 mainAxle(int side) { return new Vec3(side*MAIN_X,MAIN_RADIUS,MAIN_Z); }
    public static Transform mainLeg(int side,float amount) {
        return new Transform(mainHinge(side),-side*(1-extension(amount))*Math.PI/2,false,Vec3.ZERO);
    }
    public static Transform mainBogie(int side,float amount) {
        // The bogie counter-rotates at its hinge, keeping both axles level throughout the fold.
        Vec3 axle=mainAxle(side),shift=mainLeg(side,amount).apply(axle).subtract(axle);
        return new Transform(Vec3.ZERO,0,false,shift);
    }
    public static Transform nose(float amount) {
        double fold=Math.PI/2+Math.atan2(NOSE_HINGE.z-NOSE_AXLE.z,NOSE_HINGE.y-NOSE_AXLE.y);
        return new Transform(NOSE_HINGE,-(1-extension(amount))*fold,true,Vec3.ZERO);
    }
    public static Transform door(Bay bay,boolean second,float amount) {
        double angle=Math.toRadians(102)*doorOpening(amount);
        if(bay==NOSE) {
            double x=second?bay.maxX:bay.minX;
            return new Transform(new Vec3(x,bay.floor,0),second?angle:-angle,false,Vec3.ZERO);
        }
        double z=second?bay.maxZ:bay.minZ;
        return new Transform(new Vec3(0,bay.floor,z),second?-angle:angle,true,Vec3.ZERO);
    }
    public static double fairing(double x,double z,double original) {
        double result=original;
        for(var bay:BAYS) {
            double blend=bay==NOSE?.35:.50;
            double weight=smooth((x-bay.minX+blend)/blend)*smooth((bay.maxX+blend-x)/blend)
                *smooth((z-bay.minZ+blend)/blend)*smooth((bay.maxZ+blend-z)/blend);
            result+=(bay.floor-result)*weight;
        }
        return result;
    }
    private static double smooth(double x) { x=Math.max(0,Math.min(1,x));return x*x*(3-2*x); }
    private B2Gear() {}
}
