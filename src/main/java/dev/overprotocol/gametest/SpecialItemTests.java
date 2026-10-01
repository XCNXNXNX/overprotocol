package dev.overprotocol.gametest;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.registry.ModContent;
import dev.overprotocol.item.RedCarpetRollItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overprotocol.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialItemTests {
    @GameTest(template = "test_empty")
    public static void droppedB2SurvivesCactusLavaAndFireButNotOtherDamage(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var stack = new ItemStack(ModContent.B2_ITEM.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Protected aircraft"));
        var drop = new ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), stack);
        var damage = h.getLevel().damageSources();
        for (var source : new net.minecraft.world.damagesource.DamageSource[]{
                damage.cactus(), damage.lava(), damage.inFire(), damage.onFire()}) {
            h.assertTrue(!drop.hurt(source, 1000), "Protected damage must be rejected by the real dropped item");
            h.assertTrue(!drop.isRemoved() && ItemStack.matches(stack, drop.getItem()), "Drop must retain its item and custom name");
        }
        var ordinary = new ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModContent.RED_CARPET_ROLL_ITEM.get()));
        h.assertTrue(ordinary.hurt(damage.cactus(), 1000) && ordinary.isRemoved(), "Protection must not leak to the carpet roll");
        h.assertTrue(drop.hurt(damage.generic(), 1000) && drop.isRemoved(), "B2 must retain normal damage behaviour outside the requested protection");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void styledNamesAndPartialRollTooltipPreserveRealItemData(GameTestHelper h) {
        var b2 = new ItemStack(ModContent.B2_ITEM.get());
        var guard = new ItemStack(ModContent.HONOR_GUARD_ITEM.get());
        var roll = RedCarpetRollItem.withRemaining(17);
        h.assertTrue(b2.getRarity() == Items.NETHER_STAR.getDefaultInstance().getRarity() && b2.has(DataComponents.FIRE_RESISTANT),
            "B2 must match nether-star rarity and use vanilla fire resistance independently of its styled title");
        for (var pair : new Object[][]{{b2, ChatFormatting.LIGHT_PURPLE}, {guard, ChatFormatting.GOLD}, {roll, ChatFormatting.RED}}) {
            var stack = (ItemStack) pair[0];
            var colour = (ChatFormatting) pair[1];
            h.assertTrue(stack.getHoverName().getStyle().getColor().getValue() == colour.getColor()
                && stack.getHoverName().getStyle().isBold(), "Special item's default title must keep its colour and emphasis");
        }
        var lines = roll.getTooltipLines(Item.TooltipContext.of(h.getLevel()), null, TooltipFlag.NORMAL);
        h.assertTrue(lines.stream().anyMatch(line -> colouredNumber(line, "17")), "Partial roll tooltip must show the coloured actual remaining length");
        h.assertTrue(lines.stream().anyMatch(line -> colouredNumber(line, "30")), "Tooltip must distinguish capacity from remaining length");
        roll.set(DataComponents.CUSTOM_NAME, Component.literal("Reception runner"));
        var encoded = roll.save(h.getLevel().registryAccess());
        var restored = ItemStack.parse(h.getLevel().registryAccess(), encoded).orElseThrow();
        h.assertTrue(restored.getHoverName().getString().equals("Reception runner") && RedCarpetRollItem.remaining(restored) == 17,
            "Styled presentation must not replace custom names or lose remaining length");
        h.succeed();
    }

    private static boolean colouredNumber(Component line, String value) {
        // Dedicated servers do not load the client's language pack. Check the formatted arguments
        // that the client actually substitutes rather than a server's untranslated key string.
        if (!(line.getContents() instanceof TranslatableContents text)) return false;
        for (var argument : text.getArgs()) {
            if (argument instanceof Component number && number.getString().equals(value)) {
                return number.getStyle().isBold() && number.getStyle().getColor() != null
                    && number.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor();
            }
        }
        return false;
    }

    private SpecialItemTests() {}
}
