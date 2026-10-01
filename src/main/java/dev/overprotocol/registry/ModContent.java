package dev.overprotocol.registry;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.CeremonialTableBlock;
import dev.overprotocol.block.CandleHolderBlock;
import dev.overprotocol.block.CeremonialRopeBlock;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.block.HonorGuardBlockEntity;
import dev.overprotocol.block.CeremonialChairBlock;
import dev.overprotocol.block.CeremonialPodiumBlock;
import dev.overprotocol.block.WelcomeLampBlock;
import dev.overprotocol.item.HonorGuardItem;
import dev.overprotocol.item.B2Item;
import dev.overprotocol.item.RedCarpetRollItem;
import dev.overprotocol.vehicle.RedCarpetRollEntity;
import dev.overprotocol.vehicle.B2Entity;
import dev.overprotocol.vehicle.B2Geometry;
import dev.overprotocol.vehicle.CeremonialSeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.EnumMap;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Overprotocol.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Overprotocol.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Overprotocol.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Overprotocol.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Overprotocol.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<B2Entity>> B2 = ENTITIES.register("b2_spirit",
        () -> EntityType.Builder.<B2Entity>of(B2Entity::new, MobCategory.MISC).sized(8F, B2Geometry.HEIGHT)
            .clientTrackingRange(16).updateInterval(2).build("overprotocol:b2_spirit"));
    public static final DeferredItem<B2Item> B2_ITEM = ITEMS.register("b2_spirit",
        () -> new B2Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final DeferredHolder<EntityType<?>, EntityType<RedCarpetRollEntity>> RED_CARPET_ROLL = ENTITIES.register("red_carpet_roll",
        () -> EntityType.Builder.<RedCarpetRollEntity>of(RedCarpetRollEntity::new,MobCategory.MISC).sized(3F,.7F)
            .clientTrackingRange(10).updateInterval(1).build("overprotocol:red_carpet_roll"));
    public static final DeferredItem<RedCarpetRollItem> RED_CARPET_ROLL_ITEM = ITEMS.register("red_carpet_roll",
        () -> new RedCarpetRollItem(new Item.Properties().stacksTo(1)));
    public static final DeferredBlock<CarpetBlock> RED_RUNNER = BLOCKS.register("red_runner",
        () -> new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_CARPET)));
    public static final DeferredItem<BlockItem> RED_RUNNER_ITEM = ITEMS.registerSimpleBlockItem("red_runner",RED_RUNNER);
    public static final EnumMap<DyeColor, DeferredBlock<CarpetBlock>> CARPETS = new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor, DeferredBlock<CeremonialTableBlock>> TABLES = new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor, DeferredItem<BlockItem>> CARPET_ITEMS = new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor, DeferredItem<BlockItem>> TABLE_ITEMS = new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor,DeferredBlock<CeremonialChairBlock>> CHAIRS=new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor,DeferredItem<BlockItem>> CHAIR_ITEMS=new EnumMap<>(DyeColor.class);
    static {
        for (var color : DyeColor.values()) {
            var carpet = BLOCKS.register(carpetName(color), () -> new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(
                BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_carpet")))));
            CARPETS.put(color, carpet);
            CARPET_ITEMS.put(color, ITEMS.registerSimpleBlockItem(carpetName(color), carpet));
            var table = BLOCKS.register(tableName(color), () -> new CeremonialTableBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(color).noOcclusion()));
            TABLES.put(color, table);
            TABLE_ITEMS.put(color, ITEMS.registerSimpleBlockItem(tableName(color), table));
            var chair=BLOCKS.register(chairName(color),()->new CeremonialChairBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(color).noOcclusion()));
            CHAIRS.put(color,chair);CHAIR_ITEMS.put(color,ITEMS.registerSimpleBlockItem(chairName(color),chair));
        }
    }
    // Keep the original red registry IDs so existing carpets and tables remain in saves.
    public static final DeferredBlock<CarpetBlock> CEREMONIAL_CARPET = CARPETS.get(DyeColor.RED);
    public static final DeferredItem<BlockItem> CEREMONIAL_CARPET_ITEM = CARPET_ITEMS.get(DyeColor.RED);
    public static final DeferredBlock<CeremonialTableBlock> CEREMONIAL_TABLE = TABLES.get(DyeColor.RED);
    public static final DeferredItem<BlockItem> CEREMONIAL_TABLE_ITEM = TABLE_ITEMS.get(DyeColor.RED);
    public static final DeferredBlock<CandleHolderBlock> CANDLE_HOLDER = BLOCKS.register("candle_holder",
        () -> new CandleHolderBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL)
            .mapColor(net.minecraft.world.level.material.MapColor.GOLD).noOcclusion()
            .lightLevel(state -> state.getValue(CandleHolderBlock.LIT) ? 3 : 0)));
    public static final DeferredItem<BlockItem> CANDLE_HOLDER_ITEM = ITEMS.registerSimpleBlockItem("candle_holder", CANDLE_HOLDER);
    public static final DeferredBlock<HonorGuardBlock> HONOR_GUARD = BLOCKS.register("honor_guard",
        () -> new HonorGuardBlock(BlockBehaviour.Properties.of().strength(1.8F).sound(SoundType.WOOD)
            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED).noOcclusion()));
    public static final DeferredItem<BlockItem> HONOR_GUARD_ITEM = ITEMS.register("honor_guard",
        () -> new HonorGuardItem(HONOR_GUARD.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HonorGuardBlockEntity>> HONOR_GUARD_BE =
        BLOCK_ENTITIES.register("honor_guard",
            () -> BlockEntityType.Builder.of(HonorGuardBlockEntity::new, HONOR_GUARD.get()).build(null));
    public static final DeferredBlock<CeremonialRopeBlock> CEREMONIAL_ROPE = BLOCKS.register("ceremonial_rope",
        () -> new CeremonialRopeBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL)
            .mapColor(net.minecraft.world.level.material.MapColor.GOLD).noOcclusion()));
    public static final DeferredItem<BlockItem> CEREMONIAL_ROPE_ITEM = ITEMS.registerSimpleBlockItem("ceremonial_rope", CEREMONIAL_ROPE);
    public static final DeferredHolder<EntityType<?>,EntityType<CeremonialSeatEntity>> CEREMONIAL_SEAT=ENTITIES.register("ceremonial_seat",
        ()->EntityType.Builder.<CeremonialSeatEntity>of(CeremonialSeatEntity::new,MobCategory.MISC).sized(.1F,.1F)
            .clientTrackingRange(6).updateInterval(10).build("overprotocol:ceremonial_seat"));
    public static final DeferredBlock<CeremonialPodiumBlock> CEREMONIAL_PODIUM=BLOCKS.register("ceremonial_podium",
        ()->new CeremonialPodiumBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN).noOcclusion()));
    public static final DeferredItem<BlockItem> CEREMONIAL_PODIUM_ITEM=ITEMS.registerSimpleBlockItem("ceremonial_podium",CEREMONIAL_PODIUM);
    public static final DeferredBlock<WelcomeLampBlock> WELCOME_LAMP=BLOCKS.register("welcome_lamp",()->new WelcomeLampBlock(
        BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL).mapColor(net.minecraft.world.level.material.MapColor.GOLD)
            .noOcclusion().lightLevel(state->state.getValue(WelcomeLampBlock.LIT)?15:0)));
    public static final DeferredItem<BlockItem> WELCOME_LAMP_ITEM=ITEMS.registerSimpleBlockItem("welcome_lamp",WELCOME_LAMP);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CEREMONIAL_TAB = TABS.register("ceremonial",
        () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.overprotocol"))
            .icon(() -> CEREMONIAL_CARPET_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (var color : DyeColor.values()) output.accept(CARPET_ITEMS.get(color).get());
                for (var color : DyeColor.values()) output.accept(TABLE_ITEMS.get(color).get());
                for (var color : DyeColor.values()) output.accept(CHAIR_ITEMS.get(color).get());
                output.accept(CEREMONIAL_PODIUM_ITEM.get());
                output.accept(WELCOME_LAMP_ITEM.get());
                output.accept(CANDLE_HOLDER_ITEM.get());
                output.accept(HONOR_GUARD_ITEM.get());
                output.accept(CEREMONIAL_ROPE_ITEM.get());
                output.accept(B2_ITEM.get());
                output.accept(RED_CARPET_ROLL_ITEM.get());
            }).build());
    public static String carpetName(DyeColor color) {
        return color == DyeColor.RED ? "ceremonial_carpet" : color.getName() + "_ceremonial_carpet";
    }
    public static String tableName(DyeColor color) {
        return color == DyeColor.RED ? "ceremonial_table" : color.getName() + "_ceremonial_table";
    }
    public static String chairName(DyeColor color) {return color.getName()+"_ceremonial_chair";}
    public static Item vanillaCarpet(DyeColor color) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_carpet"));
    }
    private ModContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event)->
            event.modify(BlockEntityType.LECTERN,CEREMONIAL_PODIUM.get()));
    }
}
