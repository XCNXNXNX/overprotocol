package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.*;
import dev.overprotocol.network.B2ControlPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import java.util.ArrayList;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class B2Tests {
    private static final BlockPos POS = new BlockPos(2,40,2);
    private static B2Entity plane(GameTestHelper h) {
        var plane = ModContent.B2.get().create(h.getLevel());
        var pos = h.absolutePos(POS);
        plane.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        // Unspawned entities permit deterministic simulation without nearby test vehicles ticking.
        return plane;
    }
    private static Player board(GameTestHelper h, B2Entity plane) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(player.startRiding(plane), "Pilot must be able to board");
        return player;
    }

    @GameTest(template="test_empty")
    public static void pilotOnlyInputsAndGearCannotRetractOnRunway(GameTestHelper h) {
        var plane=plane(h); var pilot=board(h,plane);
        var guest=h.makeMockPlayer(GameType.SURVIVAL); guest.startRiding(plane);
        var stranger=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(!plane.acceptControls(guest,B2Flight.THROTTLE_UP),"Second seat must not control aircraft");
        h.assertTrue(!plane.acceptControls(stranger,B2Flight.GEAR),"Non-passenger must not control aircraft");
        h.assertTrue(!plane.acceptControls(pilot,128),"Unknown input bits must be rejected");
        plane.setOnGround(true);
        plane.acceptControls(pilot,B2Flight.GEAR);
        h.assertTrue(plane.gearDown(),"Cannot retract gear on the ground");
        plane.acceptControls(pilot,0); plane.setOnGround(false);
        plane.acceptControls(pilot,B2Flight.GEAR);
        h.assertTrue(!plane.gearDown(),"Airborne pilot can retract gear");
        plane.acceptControls(pilot,B2Flight.GEAR);
        h.assertTrue(!plane.gearDown(),"Holding gear key must not toggle repeatedly");
        pilot.stopRiding();
        h.assertTrue(plane.throttle()==0,"Dismount must clear throttle immediately");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void lowSpeedCannotTakeOffAndFlightHasFiniteLimits(GameTestHelper h) {
        var step=B2Flight.step(0,0,0,0,0,0,B2Flight.CLIMB,true,true,true);
        h.assertTrue(step.vertical()<0,"Stationary aircraft must not rise like a helicopter");
        for(int i=0;i<1000;i++) {
            step=B2Flight.step(step.throttle(),step.speed(),step.pitch(),step.yaw(),step.bank(),step.vertical(),
                B2Flight.THROTTLE_UP|B2Flight.CLIMB|B2Flight.RIGHT,false,true,false);
            h.assertTrue(Float.isFinite(step.speed()) && Double.isFinite(step.vertical()),"Flight state must stay finite");
            h.assertTrue(step.speed()<=B2Flight.MAX_SPEED && step.throttle()<=1 && step.pitch()<=B2Flight.MAX_PITCH+.01,"Flight state must stay bounded");
        }
        h.assertTrue(step.vertical()>0 && step.bank()<0,"Powered climb must climb and bank while turning");
        for(int i=0;i<600;i++) step=B2Flight.step(step.throttle(),step.speed(),step.pitch(),step.yaw(),step.bank(),step.vertical(),0,false,false,true);
        h.assertTrue(step.throttle()==0 && step.vertical()<0,"Unpiloted aircraft must lose power and descend");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void runwayTakeoffThenDescentAndBraking(GameTestHelper h) {
        var plane=plane(h); var pilot=board(h,plane);
        var origin=h.absolutePos(POS);
        // Flat, isolated elevated runway covers wing clearance and the full simulated roll-out.
        for(int x=-28;x<=28;x++) for(int z=-15;z<=700;z++)
            h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        plane.setOnGround(true);
        double startY=plane.getY();
        for(int i=0;i<90;i++) { plane.acceptControls(pilot,B2Flight.THROTTLE_UP); plane.tick(); }
        h.assertTrue(plane.speed()>.38 && plane.onGround(),"Throttle should produce ground roll before rotation");
        for(int i=0;i<65;i++) { plane.acceptControls(pilot,0,0,1); plane.tick(); }
        h.assertTrue(plane.getY()>startY+1 && !plane.onGround(),"Runway speed and pitch must produce takeoff");
        // Allow a nine-second approach: the new flight path eases out as airspeed falls.
        int landingTicks=0;
        for(;landingTicks<180 && !plane.onGround();landingTicks++) { plane.acceptControls(pilot,B2Flight.THROTTLE_DOWN,0,-1); plane.tick(); }
        h.assertTrue(plane.onGround() && plane.gearDown(),"Descent should land with gear down; relative position="
            + plane.position().subtract(Vec3.atLowerCornerOf(origin)) + ", speed=" + plane.speed());
        for(int i=0;i<60;i++) { plane.acceptControls(pilot,B2Flight.THROTTLE_DOWN); plane.tick(); }
        h.assertTrue(plane.speed()<.06,"Ground braking must park the aircraft");
        Overprotocol.LOGGER.info("B-2 mouse runway takeoff, {} tick approach, landing and braking passed",landingTicks);
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void wingObstructionStopsAircraftOutsideFuselage(GameTestHelper h) {
        var plane=plane(h); var pilot=board(h,plane);
        // Obstacle near a wing shoulder, outside the narrow central entity collider.
        var wing=BlockPos.containing(plane.localToWorld(16,2.55,4));
        h.getLevel().setBlock(wing,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!plane.airframeClear(Vec3.ZERO),"Wing obstruction must be detected beyond fuselage");
        var start=plane.position();
        plane.acceptControls(pilot,B2Flight.THROTTLE_UP); plane.tick();
        h.assertTrue(plane.speed()==0 && plane.throttle()==0,"Wing collision must stop propulsion");
        h.assertTrue(plane.position().subtract(start).horizontalDistanceSqr()==0,"Wing collision must stop horizontal motion");
        pilot.stopRiding();
        h.getLevel().setBlock(wing,Blocks.AIR.defaultBlockState(),2);
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void saveReloadKeepsNameAndGearWithoutStaleControls(GameTestHelper h) {
        var plane=plane(h); var pilot=board(h,plane);
        plane.setCustomName(Component.literal("Spirit of Ceremony"));
        plane.acceptControls(pilot,B2Flight.GEAR|B2Flight.THROTTLE_UP);
        var tag=new CompoundTag(); plane.saveWithoutId(tag);
        var loaded=plane(h); loaded.load(tag);
        h.assertTrue(loaded.hasCustomName() && loaded.getCustomName().getString().equals("Spirit of Ceremony"),"Name must survive entity save/load");
        h.assertTrue(!loaded.gearDown(),"Retracted gear must survive save/load");
        h.assertTrue(loaded.throttle()==0 && loaded.speed()==0 && loaded.getDeltaMovement().equals(Vec3.ZERO),"Reload must reset stale motion and input");
        h.assertTrue(ItemStack.isSameItemSameComponents(plane.getPickResult(),loaded.getPickResult()),"Packed item must retain custom name");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void deploymentConsumesOnceAndPackingReturnsOnce(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var stack=ModContent.B2_ITEM.get().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Spirit of Ceremony"));
        var expected=stack.copy(); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var ground=h.absolutePos(POS).below();
        h.getLevel().setBlock(ground,Blocks.STONE.defaultBlockState(),2);
        var hit=new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false);
        var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(result.consumesAction() && stack.isEmpty(),"Successful survival deployment must consume one aircraft item");
        var nearby=h.getLevel().getEntitiesOfClass(B2Entity.class,new AABB(h.absolutePos(POS)).inflate(4));
        h.assertTrue(nearby.size()==1,"Deployment must spawn exactly one aircraft");
        var plane=nearby.getFirst(); plane.setOnGround(true);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(plane.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Sneak-right-click may pack a parked aircraft even with a held item");
        h.assertTrue(player.getMainHandItem().is(Items.STICK),"Packing must preserve the held item");
        h.assertTrue(plane.isRemoved(),"Packing must remove aircraft");
        h.assertTrue(!plane.interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Repeated packet must not pack a removed aircraft again");
        int count=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var item=player.getInventory().getItem(i);
            if(ItemStack.isSameItemSameComponents(item,expected))count+=item.getCount();
        }
        h.assertTrue(count==1,"Packing must return exactly one aircraft with its custom name");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void blockedDeploymentDoesNotConsumeOrSpawn(GameTestHelper h) {
        var preview=plane(h); var player=h.makeMockPlayer(GameType.SURVIVAL);
        var stack=ModContent.B2_ITEM.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var ground=h.absolutePos(POS).below();
        h.getLevel().setBlock(ground,Blocks.STONE.defaultBlockState(),2);
        var wing=BlockPos.containing(preview.localToWorld(16,2.55,4));
        h.getLevel().setBlock(wing,Blocks.STONE.defaultBlockState(),2);
        player.setYRot(0);
        var hit=new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false);
        var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(!result.consumesAction() && stack.getCount()==1,"Obstructed placement must keep aircraft item");
        h.assertTrue(h.getLevel().getEntitiesOfClass(B2Entity.class,new AABB(h.absolutePos(POS)).inflate(4)).isEmpty(),"Obstructed placement must not create aircraft");
        h.getLevel().setBlock(wing,Blocks.AIR.defaultBlockState(),2);
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fullSizeAndPublicSpeedIncludingClimb(GameTestHelper h) {
        h.assertTrue(Math.abs(B2Geometry.SPAN-52.12)<.001 && Math.abs(B2Geometry.LENGTH-20.9)<.001
            && Math.abs(B2Geometry.HEIGHT-5.1)<.001,"Full-size aircraft public dimensions must remain correct");
        float publicLimit=1010F/72F;
        h.assertTrue(Math.abs(B2Flight.MAX_SPEED-publicLimit)<.0001,"Top speed must convert 1010 km/h to blocks/tick");
        var step=new B2Flight.Step(1,0,0,0,0,0);
        for(int i=0;i<1000;i++)step=B2Flight.step(step.throttle(),step.speed(),step.pitch(),step.yaw(),step.bank(),step.vertical(),0,false,true,false);
        h.assertTrue(Math.abs(step.speed()-publicLimit)<.001,"Level cruise must actually reach the public speed approximation");
        for(int i=0;i<200;i++) {
            step=B2Flight.step(step.throttle(),step.speed(),step.pitch(),step.yaw(),step.bank(),step.vertical(),B2Flight.CLIMB,false,true,false);
            h.assertTrue(Math.hypot(step.speed(),step.vertical())<=publicLimit+.0001,"Climb must not exceed total speed cap");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void netherStarSurroundedByEightNetheriteBlocksCraftsAircraft(GameTestHelper h) {
        var items=new ArrayList<ItemStack>();
        for(int i=0;i<9;i++)items.add(new ItemStack(i==4?Items.NETHER_STAR:Items.NETHERITE_BLOCK));
        var input=CraftingInput.of(3,3,items);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
        h.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input,h.getLevel().registryAccess()).is(ModContent.B2_ITEM.get()),
            "Eight netherite blocks around a nether star must craft exactly the aircraft");
        items.set(0,ItemStack.EMPTY);
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,items),h.getLevel()).isEmpty(),
            "Missing one netherite block must not craft aircraft");
        h.succeed();
    }

    private static B2Flight.Step fly(B2Flight.Step s, float turn, float pitch) {
        return B2Flight.step(s.throttle(),s.speed(),s.pitch(),s.yaw(),s.bank(),s.vertical(),0,turn,pitch,false,true,false);
    }

    @GameTest(template="test_empty")
    public static void mouseFramePartitionAndResetDoNotChangeControls(GameTestHelper h) {
        var slow=new B2MouseInput(); var fast=new B2MouseInput();
        for(int tick=0;tick<30;tick++) {
            slow.add(.7,-.4);
            for(int frame=0;frame<12;frame++)fast.add(.7/12,-.4/12);
            slow.tick(); fast.tick();
            h.assertTrue(Math.abs(slow.turn()-fast.turn())<.00001 && Math.abs(slow.pitch()-fast.pitch())<.00001,
                "The same movement split across render frames must produce the same controls");
        }
        h.assertTrue(slow.turn()>0 && slow.pitch()<0,"Mouse axes must retain their direction");
        for(int i=0;i<30;i++)slow.tick();
        h.assertTrue(slow.turn()==0 && slow.pitch()==0,"Released mouse must return fully to centre");
        fast.add(100,-100); fast.reset(); fast.tick();
        h.assertTrue(fast.turn()==0 && fast.pitch()==0,"Menu/seat reset must discard pending frame deltas as well as held stick");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void bankingBuildsReversesAndReturnsToStableHeading(GameTestHelper h) {
        var s=new B2Flight.Step(1,B2Flight.MAX_SPEED,0,178,0,0);
        s=fly(s,1,0);
        h.assertTrue(s.bank()<0 && s.bank()>-10 && Mth.wrapDegrees(s.yaw()-178)>0,
            "Right turn must start gradually, including through the yaw wrap boundary");
        for(int i=0;i<40;i++)s=fly(s,1,0);
        h.assertTrue(s.bank()<-25,"Sustained input must build a useful turning bank");
        float before=s.bank(), heading=s.yaw();
        s=fly(s,-1,0);
        h.assertTrue(s.bank()>before && s.bank()<0 && Mth.wrapDegrees(s.yaw()-heading)>0,
            "Reversing input must first undo the existing bank, not snap the turn in the other direction");
        for(int i=0;i<40;i++)s=fly(s,-1,0);
        h.assertTrue(s.bank()>25,"Reversal must eventually establish the opposite turn");
        for(int i=0;i<60;i++)s=fly(s,0,0);
        heading=s.yaw();
        for(int i=0;i<20;i++)s=fly(s,0,0);
        h.assertTrue(Math.abs(s.bank())<.02 && Math.abs(Mth.wrapDegrees(s.yaw()-heading))<.02,
            "Released controls must settle to level flight and a stable new heading");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void mousePitchSettlesAndGentleInputsStayPrecise(GameTestHelper h) {
        var level=new B2Flight.Step(1,B2Flight.MAX_SPEED,0,0,0,0);
        var up=level; var down=level; var gentle=level;
        for(int i=0;i<60;i++) {
            up=fly(up,0,1); down=fly(down,0,-1); gentle=fly(gentle,.15F,.15F);
            h.assertTrue(Math.hypot(up.speed(),up.vertical())<=B2Flight.MAX_SPEED+.0001,
                "Mouse climb must obey the total-speed cap");
        }
        h.assertTrue(up.vertical()>.5 && down.vertical()<-.5 && Math.abs(up.pitch()+down.pitch())<.01,
            "Mouse up/down must produce symmetric, bounded climb and descent");
        h.assertTrue(gentle.pitch()>0 && gentle.pitch()<3 && Math.abs(gentle.bank())<4,
            "Small mouse corrections should make small attitude changes");
        for(int i=0;i<80;i++)up=fly(up,0,0);
        h.assertTrue(Math.abs(up.pitch())<.01 && Math.abs(up.vertical())<.001 && up.throttle()==1,
            "Neutral must settle climb without requiring the throttle key to remain held");
        var parked=B2Flight.step(0,0,0,0,0,0,0,1,1,true,true,true);
        h.assertTrue(parked.yaw()==0 && parked.vertical()<0,"Mouse cannot rotate or lift an unpowered parked plane");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void analogPacketsRoundTripAndRejectInvalidOrGuestInput(GameTestHelper h) {
        var payload=new B2ControlPayload(57,B2Flight.THROTTLE_UP|B2Flight.GEAR,.37F,-.62F,2.5F);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            B2ControlPayload.STREAM_CODEC.encode(buffer,payload);
            h.assertTrue(payload.equals(B2ControlPayload.STREAM_CODEC.decode(buffer)),"Both analog axes and keys must survive network encoding");
        } finally { buffer.release(); }
        var plane=plane(h); var pilot=board(h,plane);
        var guest=h.makeMockPlayer(GameType.SURVIVAL); guest.startRiding(plane);
        h.assertTrue(!plane.acceptControls(guest,B2Flight.GEAR,1,1),"Passenger may not send analog flight controls");
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,1.001F,-1.001F}) {
            h.assertTrue(!plane.acceptControls(pilot,B2Flight.GEAR,invalid,0)
                && !plane.acceptControls(pilot,B2Flight.GEAR,0,invalid),"Invalid axis must reject the entire packet");
        }
        h.assertTrue(plane.gearDown(),"Rejected analog packet must not toggle gear");
        h.assertTrue(plane.acceptControls(pilot,0,-1,1),"Pilot may use full valid analog range");
        pilot.stopRiding();
        h.assertTrue(!plane.acceptControls(pilot,0,1,1),"Old pilot loses input permission immediately on exit");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void wheelAdjustsAndHoldsThrottleOnlyForPilot(GameTestHelper h) {
        var plane=plane(h);var pilot=board(h,plane);
        var guest=h.makeMockPlayer(GameType.SURVIVAL);guest.startRiding(plane);
        h.assertTrue(!plane.acceptControls(guest,0,0,0,4),"Passenger cannot adjust throttle with wheel");
        plane.acceptControls(pilot,0,0,0,2);
        h.assertTrue(Math.abs(plane.throttle()-.1F)<.0001,"Two notches add ten percent throttle");
        plane.tick();plane.acceptControls(pilot,0,0,0,0);plane.tick();
        h.assertTrue(Math.abs(plane.throttle()-.1F)<.0001,"Throttle is retained after wheel release");
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,4.01F,-4.01F})
            h.assertTrue(!plane.acceptControls(pilot,B2Flight.GEAR,0,0,invalid),"Invalid wheel input rejects entire packet");
        h.assertTrue(plane.gearDown() && Math.abs(plane.throttle()-.1F)<.0001,"Rejected input cannot mutate gear or throttle");
        for(int i=0;i<10;i++)plane.acceptControls(pilot,0,0,0,4);
        h.assertTrue(plane.throttle()==1,"Wheel up saturates at full throttle");
        for(int i=0;i<10;i++)plane.acceptControls(pilot,0,0,0,-4);
        h.assertTrue(plane.throttle()==0,"Wheel down saturates at idle");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fullSpeedSweepCannotTunnelPastWingObstacle(GameTestHelper h) {
        var plane=plane(h);var motion=new Vec3(0,0,B2Flight.MAX_SPEED);
        var obstacle=BlockPos.containing(plane.localToWorld(16,2.55,4).add(motion.scale(.9)));
        h.getLevel().setBlock(obstacle,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(plane.airframeClear(Vec3.ZERO),"Obstacle must be beyond the currently stationary wing");
        h.assertTrue(!plane.airframeClear(motion),"Full-speed movement must sweep the intermediate wing path");
        h.getLevel().setBlock(obstacle,Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
}
