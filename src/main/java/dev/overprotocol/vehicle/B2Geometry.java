package dev.overprotocol.vehicle;

/** Photo-proportioned, independently constructed flying-wing surface; units are blocks. */
public final class B2Geometry {
    public static final float SPAN = 52.12F, LENGTH = 20.9F, HEIGHT = 5.1F;
    public static final double HALF_SPAN = SPAN / 2.0, PIVOT_Y = 2.5;
    public static final double[][] STATIONS = {
        // Centre tail, exhaust notch, inboard shoulder, outer-wing elbow, clipped tip.
        // Landmarks measured on the user's overhead photograph, then symmetrised.
        {0,9.20}, {3.90,6.58}, {6.40,8.38}, {13.07,3.46},
        {22.41,10.45}, {HALF_SPAN,7.85}
    };
    private static final double[][] SPINE = {
        {-10.45,2.48,.10},{-9.65,2.80,.55},{-8.65,3.62,1.20},
        {-7.55,4.68,1.55},{-6.45,5.06,1.74},{-4.90,4.91,1.94},
        {-2.50,4.45,2.14},{.50,4.03,2.25},{3.30,3.55,2.18},
        {6.30,2.96,1.66},{9.20,2.48,.25}
    };
    private static final double[][] NACELLE = {
        {-5.50,0,1.12},{-4.15,.65,1.22},{-2.65,.83,1.45},
        {.40,.65,1.62},{3.00,.37,1.53},{5.70,0,1.32}
    };
    public static double leading(double x) { return -10.45 + Math.abs(x) * (18.30 / HALF_SPAN); }
    public static double trailing(double x) {
        x = Math.min(Math.abs(x), HALF_SPAN);
        for (int i=1;i<STATIONS.length;i++) if (x <= STATIONS[i][0]) {
            var a=STATIONS[i-1];var b=STATIONS[i];
            return a[1]+(b[1]-a[1])*(x-a[0])/(b[0]-a[0]);
        }
        return STATIONS[STATIONS.length-1][1];
    }
    public static double top(double x, double chord) {
        return topAt(x,z(x,chord));
    }
    public static double bottom(double x, double chord) {
        double base=2.40-(.08+.66*Math.exp(-Math.pow(x/6.5,2)))
            *Math.pow(Math.max(0,Math.sin(Math.PI*clamp(chord))),.65);
        return B2Gear.fairing(x,z(x,chord),base);
    }
    public static double z(double x,double chord) { return leading(x)+(trailing(x)-leading(x))*chord; }
    public static double chord(double x,double z) {
        return clamp((z-leading(x))/Math.max(.001,trailing(x)-leading(x)));
    }
    public static double bottomAt(double x,double z) { return bottom(x,chord(x,z)); }
    public static double topAt(double x,double z) {
        double u=clamp(Math.abs(x)/HALF_SPAN),t=chord(x,z);
        double wing=2.48+.32*Math.pow(1-u,.7)*Math.pow(Math.max(0,Math.sin(Math.PI*t)),.65);
        double crown=profile(SPINE,z,1),width=profile(SPINE,z,2);
        double y=wing+Math.max(0,crown-wing)*Math.exp(-Math.pow(Math.abs(x)/Math.max(.1,width),2.8));
        double dx=Math.abs(x)-4.12;
        y+=profile(NACELLE,z,1)*Math.exp(-Math.pow(dx/profile(NACELLE,z,2),4));
        // Visible intake recesses cut into the blended nacelles, rather than black plates on top.
        double inletFront=intakeFront(Math.abs(x)),inletBack=intakeBack(Math.abs(x));
        double intake=band(Math.abs(x),2.72,5.52,.13)*band(z,inletFront,inletBack,.12);
        y-=intake*.53;
        // A pair of shallow exhaust troughs runs right out through each aft notch.
        double exhaust=band(Math.abs(x),2.72,5.52,.17)*smooth((z-4.35)/.75);
        return y+(2.49-y)*exhaust;
    }
    public static double intakeFront(double x) {
        double d=Math.abs(x-4.12);
        return -4.78+.42*Math.abs(d-.70)/.70;
    }
    public static double intakeBack(double x) { return intakeFront(x)+.86; }
    private static double band(double x,double a,double b,double blend) {
        return smooth((x-a)/blend)*smooth((b-x)/blend);
    }
    private static double smooth(double x) { x=clamp(x);return x*x*(3-2*x); }
    private static double clamp(double x) { return Math.max(0,Math.min(1,x)); }
    /** Cubic Hermite profiles give continuous slopes between measured visual landmarks. */
    private static double profile(double[][] rows,double z,int column) {
        if(z<=rows[0][0])return rows[0][column];
        for(int i=1;i<rows.length;i++)if(z<=rows[i][0]) {
            var a=rows[i-1];var b=rows[i];
            var before=rows[Math.max(0,i-2)];var after=rows[Math.min(rows.length-1,i+1)];
            double length=b[0]-a[0],t=(z-a[0])/length,t2=t*t,t3=t2*t;
            double ma=(b[column]-before[column])/(b[0]-before[0]);
            double mb=(after[column]-a[column])/(after[0]-a[0]);
            double result=(2*t3-3*t2+1)*a[column]+(t3-2*t2+t)*length*ma
                +(-2*t3+3*t2)*b[column]+(t3-t2)*length*mb;
            return Math.max(Math.min(a[column],b[column]),Math.min(Math.max(a[column],b[column]),result));
        }
        return rows[rows.length-1][column];
    }
    private B2Geometry() {}
}
