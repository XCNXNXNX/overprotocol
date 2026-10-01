package dev.overprotocol.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.LecternBlock;

/** Uses the vanilla book, menu and redstone behaviour with an original walnut/gold model. */
public final class CeremonialPodiumBlock extends LecternBlock {
    public static final MapCodec<LecternBlock> CODEC=simpleCodec(CeremonialPodiumBlock::new);
    public CeremonialPodiumBlock(Properties properties) { super(properties); }
    @Override public MapCodec<LecternBlock> codec() { return CODEC; }
}
