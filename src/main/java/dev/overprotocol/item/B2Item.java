package dev.overprotocol.item;

import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.B2Geometry;
import dev.overprotocol.vehicle.B2Flight;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Locale;

public final class B2Item extends Item {
    public B2Item(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
    }
    @Override public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return !source.is(DamageTypes.CACTUS);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        var level = context.getLevel();
        var pos = context.getClickedPos().relative(context.getClickedFace());
        var plane = ModContent.B2.get().create(level);
        if (plane == null) return InteractionResult.FAIL;
        plane.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, player.getYRot(), 0);
        if (!level.noCollision(plane, plane.getBoundingBox()) || !plane.airframeClear(Vec3.ZERO)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.overprotocol.b2.space"), true);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            var name = context.getItemInHand().get(DataComponents.CUSTOM_NAME);
            if (name != null) plane.setCustomName(name);
            if (!level.addFreshEntity(plane)) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.overprotocol.b2.category").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(SpecialItemText.line("tooltip.overprotocol.b2.dimensions",
            SpecialItemText.number(B2Geometry.SPAN), SpecialItemText.number(B2Geometry.LENGTH)));
        lines.add(SpecialItemText.line("tooltip.overprotocol.b2.speed",
            SpecialItemText.number((int) B2Flight.MAX_KMH), SpecialItemText.number(String.format(Locale.ROOT, "%.2f", B2Flight.MAX_SPEED * 20))));
        lines.add(SpecialItemText.line("tooltip.overprotocol.b2.protection").withStyle(ChatFormatting.GREEN));
        lines.add(SpecialItemText.line("tooltip.overprotocol.b2.deploy", SpecialItemText.key("RMB")));
        lines.add(SpecialItemText.line("tooltip.overprotocol.b2.recover", SpecialItemText.key("Shift + RMB")));
        if (SpecialItemText.expanded()) {
            lines.add(SpecialItemText.line("tooltip.overprotocol.b2.steer"));
            lines.add(SpecialItemText.line("tooltip.overprotocol.b2.throttle", SpecialItemText.key("W / S")));
            lines.add(SpecialItemText.line("tooltip.overprotocol.b2.assist", SpecialItemText.key("A / D"), SpecialItemText.key("Space / Ctrl")));
            lines.add(SpecialItemText.line("tooltip.overprotocol.b2.actions", SpecialItemText.key("G"), SpecialItemText.key("Shift")));
        } else lines.add(SpecialItemText.line("tooltip.overprotocol.special.more", SpecialItemText.key("Shift")));
    }
}
