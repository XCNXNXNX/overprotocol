package dev.overprotocol.block;

import com.mojang.authlib.GameProfile;
import dev.overprotocol.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Holds the player profile whose skin the statue wears. Only the lower half of the statue owns one;
 * the renderer draws both blocks from it.
 *
 * <p>The name is stored as an unresolved {@link ResolvableProfile} and looked up against the session
 * service on the server, exactly like a player head, so the resolved profile (with its texture
 * property) reaches the client and can be turned into a skin.
 */
public final class HonorGuardBlockEntity extends BlockEntity {
    private static final String PROFILE_KEY = "profile";
    private static final String LOCAL_SKIN_KEY = "local_skin";
    private static final String HELD_KEY = "held";

    private ResolvableProfile profile;
    private String localSkin = "";
    private ItemStack held = ItemStack.EMPTY;

    public HonorGuardBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.HONOR_GUARD_BE.get(), pos, state);
    }

    public ResolvableProfile getProfile() {
        return profile;
    }

    /** Whatever the guard is carrying in its right hand. */
    public ItemStack getHeldItem() {
        return held;
    }


    /** Puts one item into the guard's hand and hands back whatever was there before. */
    public ItemStack offerItem(ItemStack offered) {
        var previous = this.held;
        this.held = offered.copyWithCount(1);
        sync();
        return previous;
    }

    public ItemStack takeItem() {
        var previous = this.held;
        this.held = ItemStack.EMPTY;
        sync();
        return previous;
    }

    /**
     * Pushes the current state again. The control panel opens from a client-side interaction, so
     * the server re-sends on that click: whatever happened earlier, the panel cannot come up with
     * stale data and a dead take button.
     */
    public void resync() {
        sync();
    }

    /** Called when the statue is destroyed so nothing vanishes with it. */
    public void dropContents() {
        this.held = ItemStack.EMPTY;
    }

    /** Id of a PNG the player imported from their own disk, or "" when the statue uses a profile. */
    public String getLocalSkin() {
        return localSkin;
    }

    /** Points the statue at a player name; an empty name restores the built-in guard uniform. */
    public void setProfileName(String name) {
        if (name == null || name.isBlank()) {
            clearSkin();
            return;
        }
        applyProfile(new ResolvableProfile(new GameProfile(Util.NIL_UUID, name.trim())));
    }

    /** Uses an already known profile, e.g. the skin of the player who clicked. */
    public void setProfile(GameProfile gameProfile) {
        applyProfile(new ResolvableProfile(gameProfile));
    }

    /** Uses a skin image the client imported from disk; other players fall back to the uniform. */
    public void setLocalSkin(String id) {
        this.localSkin = id == null ? "" : id.trim();
        this.profile = null;
        sync();
    }

    public void clearSkin() {
        this.profile = null;
        this.localSkin = "";
        sync();
    }

    private void applyProfile(ResolvableProfile requested) {
        this.localSkin = "";
        this.profile = requested;
        sync();
        if (this.level instanceof ServerLevel server && !requested.isResolved()) {
            requested.resolve().thenAcceptAsync(resolved -> {
                this.profile = resolved;
                this.setChanged();
                server.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
            }, server.getServer());
        }
    }

    /**
     * Pushes the whole block entity to every nearby client. {@code sendBlockUpdated} alone is not
     * enough for a block that has no baked model, so the data packet is sent directly as well.
     */
    private void sync() {
        this.setChanged();
        if (!(this.level instanceof ServerLevel server)) return;
        push(server);
        // a second push on the next tick: a packet sent in the same tick as a GUI action can be
        // overtaken by the chunk/tracking update for the very block that changed
        server.getServer().execute(() -> {
            if (this.level == server && !this.isRemoved()) push(server);
        });
    }

    private void push(ServerLevel server) {
        server.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        var packet = ClientboundBlockEntityDataPacket.create(this);
        var centre = Vec3.atCenterOf(this.getBlockPos());
        for (var player : server.getPlayers(player -> player.distanceToSqr(centre) < 4096.0)) {
            player.connection.send(packet);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.profile != null) {
            tag.put(PROFILE_KEY, ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.profile).getOrThrow());
        }
        if (!this.localSkin.isEmpty()) {
            tag.putString(LOCAL_SKIN_KEY, this.localSkin);
        }
        if (!this.held.isEmpty()) {
            tag.put(HELD_KEY, this.held.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.profile = tag.contains(PROFILE_KEY)
            ? ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag.get(PROFILE_KEY)).result().orElse(null)
            : null;
        this.localSkin = tag.getString(LOCAL_SKIN_KEY);
        this.held = tag.contains(HELD_KEY) ? ItemStack.parse(registries, tag.get(HELD_KEY)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;

    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        // This is a complete snapshot, including an empty default-uniform/empty-hand snapshot.
        // NeoForge's default handler skips empty tags and would leave the previous item visible.
        this.loadWithComponents(packet.getTag(), registries);
    }
}
