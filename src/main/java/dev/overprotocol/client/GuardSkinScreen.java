package dev.overprotocol.client;

import dev.overprotocol.network.GuardSkinPayload;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.block.HonorGuardBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/** The statue's panel: three ways to pick a skin, the held item and a quarter turn. */
@OnlyIn(Dist.CLIENT)
public final class GuardSkinScreen extends Screen {
    private final BlockPos pos;
    private Button mainHandButton;
    private EditBox nameField;

    public GuardSkinScreen(BlockPos pos) {
        super(Component.translatable("screen.overprotocol.honor_guard"));
        this.pos = pos;
    }

    private static Component text(String key) {
        return Component.translatable(key);
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 2 - 62;
        this.nameField = new EditBox(this.font, left, top + 14, 200, 18, text("screen.overprotocol.honor_guard.field"));
        this.nameField.setMaxLength(32);
        this.nameField.setHint(Component.literal("Notch"));
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);
        this.addRenderableWidget(Button.builder(text("screen.overprotocol.honor_guard.by_name"),
                button -> this.send(GuardSkinPayload.NAME, this.nameField.getValue()))
            .bounds(left, top + 36, 200, 20).build());
        this.addRenderableWidget(Button.builder(text("screen.overprotocol.honor_guard.self"),
                button -> this.send(GuardSkinPayload.SELF, ""))
            .bounds(left, top + 60, 96, 20).build());
        this.addRenderableWidget(Button.builder(text("screen.overprotocol.honor_guard.file"),
                button -> LocalSkins.chooseFile(id -> {
                    if (id != null) this.send(GuardSkinPayload.FILE, id);
                }))
            .bounds(left + 104, top + 60, 96, 20).build());
        this.mainHandButton = this.addRenderableWidget(Button.builder(Component.empty(),
                button -> this.send(GuardSkinPayload.TAKE, ""))
            .bounds(left, top + 88, 200, 20).build());
        this.updateHeldItem();
        this.addRenderableWidget(Button.builder(text("screen.overprotocol.honor_guard.turn"),
                button -> this.send(GuardSkinPayload.TURN, ""))
            .bounds(left, top + 118, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
            .bounds(left + 104, top + 118, 96, 20).build());
    }

    @Override public void tick() {
        super.tick();
        this.updateHeldItem();
    }

    private void updateHeldItem() {
        var level = this.minecraft == null ? null : this.minecraft.level;
        var owner = level == null ? this.pos : HonorGuardBlock.ownerPos(level, this.pos);
        var guard = level != null && level.getBlockEntity(owner) instanceof HonorGuardBlockEntity found ? found : null;
        boolean holding = guard != null && !guard.getHeldItem().isEmpty();
        var name = holding ? guard.getHeldItem().getHoverName() : text("screen.overprotocol.honor_guard.nothing");
        this.mainHandButton.setMessage(text("screen.overprotocol.honor_guard.main_hand").copy().append(": ").append(name));
        this.mainHandButton.active = holding;
    }

    private void send(int action, String value) {
        PacketDistributor.sendToServer(new GuardSkinPayload(this.pos, action, value == null ? "" : value.trim()));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            this.send(GuardSkinPayload.NAME, this.nameField.getValue());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centre = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centre, this.height / 2 - 82, 0xFFFFFF);
        graphics.drawCenteredString(this.font, text("screen.overprotocol.honor_guard.hint"), centre, this.height / 2 - 70, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
