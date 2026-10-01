package dev.overprotocol.item;

import dev.overprotocol.registry.ModContent;
import dev.overprotocol.vehicle.RedCarpetRollEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import java.util.List;

public final class RedCarpetRollItem extends Item {
    public RedCarpetRollItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }
    public static int remaining(ItemStack stack) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return data.contains("Remaining")?Mth.clamp(data.getInt("Remaining"),1,RedCarpetRollEntity.LENGTH):RedCarpetRollEntity.LENGTH;
    }
    public static ItemStack withRemaining(int remaining) {
        var stack=ModContent.RED_CARPET_ROLL_ITEM.get().getDefaultInstance();
        var tag=new CompoundTag();tag.putInt("Remaining",Mth.clamp(remaining,1,RedCarpetRollEntity.LENGTH));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return stack;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        if (player==null || !player.getAbilities().mayBuild) return InteractionResult.FAIL;
        var level=context.getLevel();var stack=context.getItemInHand();
        var pos=level.getBlockState(context.getClickedPos()).canBeReplaced()?context.getClickedPos()
            :context.getClickedPos().relative(context.getClickedFace());
        var facing=player.getDirection();
        if (!RedCarpetRollEntity.rowAvailable(level,pos,facing,player,stack)) return InteractionResult.FAIL;
        var roll=ModContent.RED_CARPET_ROLL.get().create(level);
        if (roll==null) return InteractionResult.FAIL;
        roll.setYRot(facing.toYRot());roll.setRemaining(remaining(stack));
        roll.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        if (!level.noCollision(roll,roll.getBoundingBox())) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            var name=stack.get(DataComponents.CUSTOM_NAME);if(name!=null)roll.setCustomName(name);
            if (!level.addFreshEntity(roll)) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.overprotocol.red_carpet_roll.category").withStyle(ChatFormatting.DARK_RED));
        lines.add(SpecialItemText.line("tooltip.overprotocol.red_carpet_roll.size",
            SpecialItemText.number(3), SpecialItemText.number(RedCarpetRollEntity.LENGTH)));
        lines.add(SpecialItemText.line("tooltip.overprotocol.red_carpet_roll.remaining",
            SpecialItemText.number(remaining(stack))));
        lines.add(SpecialItemText.line("tooltip.overprotocol.red_carpet_roll.lay", SpecialItemText.key("RMB"), SpecialItemText.number(1)));
        lines.add(SpecialItemText.line("tooltip.overprotocol.red_carpet_roll.rewind", SpecialItemText.key("Shift + RMB"), SpecialItemText.number(1)));
        lines.add(SpecialItemText.line("tooltip.overprotocol.red_carpet_roll.pack"));
    }
}
