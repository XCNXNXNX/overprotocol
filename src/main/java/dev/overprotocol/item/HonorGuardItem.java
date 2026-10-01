package dev.overprotocol.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Adds the in-game usage hints for the honour guard statue. */
public final class HonorGuardItem extends BlockItem {
    public HonorGuardItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.overprotocol.honor_guard.category").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.poses", SpecialItemText.number(4)));
        tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.panel", SpecialItemText.key("Shift + RMB")));
        tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.equip", SpecialItemText.key("RMB")));
        if (SpecialItemText.expanded()) {
            tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.cycle"));
            tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.skin"));
            tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.picker"));
            tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.hold"));
            tooltip.add(SpecialItemText.line("tooltip.overprotocol.honor_guard.take"));
        } else tooltip.add(SpecialItemText.line("tooltip.overprotocol.special.more", SpecialItemText.key("Shift")));
    }
}
