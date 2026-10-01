package dev.overprotocol.data;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.*;
import dev.overprotocol.registry.ModContent;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.*;
import net.minecraft.data.loot.*;
import net.minecraft.data.recipes.*;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.*;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModData {
    private ModData() {}
    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookup = event.getLookupProvider();
        var existing = event.getExistingFileHelper();
        generator.addProvider(event.includeClient(), new Models(output, existing));
        generator.addProvider(event.includeClient(), new Languages(output, "en_us"));
        generator.addProvider(event.includeClient(), new Languages(output, "zh_cn"));
        generator.addProvider(event.includeServer(), new Recipes(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        var blockTags = new BlockTagsProvider(output, lookup, Overprotocol.MOD_ID, existing) {
            @Override protected void addTags(HolderLookup.Provider registries) {
                for (var color : DyeColor.values()) {
                tag(BlockTags.WOOL_CARPETS).add(ModContent.CARPETS.get(color).get());
                    tag(BlockTags.MINEABLE_WITH_AXE).add(ModContent.TABLES.get(color).get());
                    tag(BlockTags.MINEABLE_WITH_AXE).add(ModContent.CHAIRS.get(color).get());
                }
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModContent.CANDLE_HOLDER.get());
                tag(BlockTags.MINEABLE_WITH_AXE).add(ModContent.HONOR_GUARD.get());
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModContent.CEREMONIAL_ROPE.get());
                tag(BlockTags.WOOL_CARPETS).add(ModContent.RED_RUNNER.get());
                tag(BlockTags.MINEABLE_WITH_AXE).add(ModContent.CEREMONIAL_PODIUM.get());
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModContent.WELCOME_LAMP.get());
            }
        };
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTagsProvider(output, lookup,
            blockTags.contentsGetter(), Overprotocol.MOD_ID, existing) {
            @Override protected void addTags(HolderLookup.Provider registries) {
                for (var color : DyeColor.values()) tag(ItemTags.WOOL_CARPETS).add(ModContent.CARPET_ITEMS.get(color).get());
                tag(ItemTags.WOOL_CARPETS).add(ModContent.RED_RUNNER_ITEM.get());
            }
        });
    }
    private static final class Models extends BlockStateProvider {
        Models(PackOutput output, ExistingFileHelper existing) { super(output, Overprotocol.MOD_ID, existing); }
        @Override protected void registerStatesAndModels() {
            simpleBlockWithItem(ModContent.RED_RUNNER.get(),models().carpet("red_runner",modLoc("block/red_velvet")));
            rollItemModel();
            var bareTable=bareTableModel();
            for (var color : DyeColor.values()) {
                var carpetName = ModContent.carpetName(color);
                simpleBlockWithItem(ModContent.CARPETS.get(color).get(), models().carpet(carpetName, modLoc("block/" + carpetName)));
                var tableModels = new BlockModelBuilder[16];
                for (int mask = 0; mask < 16; mask++) tableModels[mask] = tableModel(color, mask);
                getVariantBuilder(ModContent.TABLES.get(color).get()).forAllStates(state ->
                    ConfiguredModel.builder().modelFile(state.getValue(CeremonialTableBlock.CLOTH)
                        ? tableModels[CeremonialTableBlock.connectionMask(state)] : bareTable).build());
                simpleBlockItem(ModContent.TABLES.get(color).get(), tableModels[0]);
                var chair=chairModel(color);
                horizontalBlock(ModContent.CHAIRS.get(color).get(),chair);
                simpleBlockItem(ModContent.CHAIRS.get(color).get(),chair);
            }
            var podium=podiumModel();
            getVariantBuilder(ModContent.CEREMONIAL_PODIUM.get()).forAllStatesExcept(state->ConfiguredModel.builder().modelFile(podium)
                .rotationY((int)(state.getValue(LecternBlock.FACING).toYRot()+180)%360).build(),LecternBlock.HAS_BOOK,LecternBlock.POWERED);
            simpleBlockItem(ModContent.CEREMONIAL_PODIUM.get(),podium);
            var lampOn=lampModel(true);var lampOff=lampModel(false);
            getVariantBuilder(ModContent.WELCOME_LAMP.get()).forAllStates(state->ConfiguredModel.builder()
                .modelFile(state.getValue(WelcomeLampBlock.LIT)?lampOn:lampOff).build());
            simpleBlockItem(ModContent.WELCOME_LAMP.get(),lampOn);
            var ropeModels = new BlockModelBuilder[16];
            for (int mask = 0; mask < 16; mask++) ropeModels[mask] = ropeModel(mask);
            getVariantBuilder(ModContent.CEREMONIAL_ROPE.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(ropeModels[CeremonialRopeBlock.connectionMask(state)]).build());
            simpleBlockItem(ModContent.CEREMONIAL_ROPE.get(), ropeModels[0]);
            // The statue is drawn by its block entity renderer, so the block state only needs an
            // empty placeholder model; the inventory icon is a flat sprite.
            // The statue has no baked geometry, but it still needs a particle texture: breaking it
            // scatters terrain particles taken from the model's #particle sprite.
            var guardPlaceholder = models().withExistingParent("block/honor_guard", mcLoc("block/block"))
                .ao(false).texture("particle", modLoc("block/honor_guard_particle"));
            getVariantBuilder(ModContent.HONOR_GUARD.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(guardPlaceholder).build());
            models().withExistingParent("item/honor_guard", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/honor_guard"));
            models().withExistingParent("item/b2_spirit", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/b2_spirit"));
            var holderModels = new BlockModelBuilder[18];
            for (int index = 0; index <= 17; index++) holderModels[index] = holderModel(index);
            getVariantBuilder(ModContent.CANDLE_HOLDER.get()).forAllStatesExcept(state ->
                ConfiguredModel.builder().modelFile(holderModels[state.getValue(CandleHolderBlock.CANDLE)]).build(), CandleHolderBlock.LIT);
            simpleBlockItem(ModContent.CANDLE_HOLDER.get(), holderModels[0]);
        }
        private BlockModelBuilder ropeModel(int mask) {
            var model = models().withExistingParent("ceremonial_rope_" + mask, mcLoc("block/block"))
                .texture("particle", modLoc("block/table_gold"))
                .texture("gold", modLoc("block/table_gold"))
                .texture("rope", modLoc("block/rope_red"));
            cuboid(model, 5.6F, 0.0F, 5.6F, 10.4F, 1.2F, 10.4F, "#gold");
            cuboid(model, 7.0F, 1.2F, 7.0F, 9.0F, 13.6F, 9.0F, "#gold");
            cuboid(model, 6.4F, 13.6F, 6.4F, 9.6F, 15.6F, 9.6F, "#gold");
            if (mask != 0) cuboid(model, 6.6F, 11.2F, 6.6F, 9.4F, 12.8F, 9.4F, "#rope");
            if ((mask & 1) != 0) rope(model, Direction.NORTH);
            if ((mask & 2) != 0) rope(model, Direction.EAST);
            if ((mask & 4) != 0) rope(model, Direction.SOUTH);
            if ((mask & 8) != 0) rope(model, Direction.WEST);
            return model;
        }

        /**
         * Three rope segments per direction. The cord leaves the collar high, dips through the
         * middle of the span and arrives at the block edge at the same height on both sides, so two
         * neighbouring barriers always meet flush.
         */
        private void rope(BlockModelBuilder model, Direction direction) {
            switch (direction) {
                case NORTH -> {
                    cuboid(model, 7.4F, 11.6F, 4.4F, 8.6F, 12.8F, 6.6F, "#rope");
                    cuboid(model, 7.4F, 10.9F, 2.2F, 8.6F, 12.1F, 4.4F, "#rope");
                    cuboid(model, 7.4F, 10.5F, 0.0F, 8.6F, 11.7F, 2.2F, "#rope");
                }
                case EAST -> {
                    cuboid(model, 9.4F, 11.6F, 7.4F, 11.6F, 12.8F, 8.6F, "#rope");
                    cuboid(model, 11.6F, 10.9F, 7.4F, 13.8F, 12.1F, 8.6F, "#rope");
                    cuboid(model, 13.8F, 10.5F, 7.4F, 16.0F, 11.7F, 8.6F, "#rope");
                }
                case SOUTH -> {
                    cuboid(model, 7.4F, 11.6F, 9.4F, 8.6F, 12.8F, 11.6F, "#rope");
                    cuboid(model, 7.4F, 10.9F, 11.6F, 8.6F, 12.1F, 13.8F, "#rope");
                    cuboid(model, 7.4F, 10.5F, 13.8F, 8.6F, 11.7F, 16.0F, "#rope");
                }
                case WEST -> {
                    cuboid(model, 4.4F, 11.6F, 7.4F, 6.6F, 12.8F, 8.6F, "#rope");
                    cuboid(model, 2.2F, 10.9F, 7.4F, 4.4F, 12.1F, 8.6F, "#rope");
                    cuboid(model, 0.0F, 10.5F, 7.4F, 2.2F, 11.7F, 8.6F, "#rope");
                }
                default -> { }
            }
        }

        private BlockModelBuilder tableModel(DyeColor color, int mask) {
            var model = models().withExistingParent(ModContent.tableName(color) + "_" + mask, mcLoc("block/block"))
                .texture("particle", modLoc("block/table_linen"))
                .texture("cloth", modLoc("block/table_linen"))
                .texture("skirt", modLoc("block/table_pleats"))
                .texture("wood", modLoc("block/table_wood"));
            model.element().from(0, 13, 0).to(16, 16, 16).allFaces((direction, face) ->
                { face.texture(direction == Direction.DOWN ? "#wood" : "#cloth");
                    if(direction!=Direction.DOWN)face.tintindex(0); }).end();
            cuboid(model, 6, 0, 6, 10, 13, 10, "#wood");
            if ((mask & 1) == 0) cuboid(model, 0, 3, 0, 16, 13, 1, "#skirt");
            if ((mask & 2) == 0) cuboid(model, 15, 3, 0, 16, 13, 16, "#skirt");
            if ((mask & 4) == 0) cuboid(model, 0, 3, 15, 16, 13, 16, "#skirt");
            if ((mask & 8) == 0) cuboid(model, 0, 3, 0, 1, 13, 16, "#skirt");
            return model;
        }
        private BlockModelBuilder holderModel(int index) {
            var model = models().withExistingParent("candle_holder_" + index, mcLoc("block/block"))
                .texture("particle", modLoc("block/table_gold")).texture("gold", modLoc("block/table_gold"));
            cuboid(model, 4, 0, 4, 12, 2, 12, "#gold");
            cuboid(model, 7, 2, 7, 9, 8, 9, "#gold");
            cuboid(model, 5, 8, 5, 11, 10, 11, "#gold");
            if (index > 0) {
                var candleName = BuiltInRegistries.ITEM.getKey(CandleHolderBlock.candleItem(index)).getPath();
                model.texture("candle", mcLoc("block/" + candleName));
                model.element().from(7, 10, 7).to(9, 16, 9).allFaces((direction, face) -> {
                    face.texture("#candle");
                    switch (direction) {
                        case UP -> face.uvs(0, 6, 2, 8);
                        case DOWN -> face.uvs(0, 14, 2, 16);
                        default -> face.uvs(0, 8, 2, 14);
                    }
                }).end();
                for (int angle : new int[]{-45, 45}) {
                    model.element().from(7.5F, 16, 8).to(8.5F, 17, 8)
                        .rotation().origin(8, 16, 8).axis(Direction.Axis.Y).angle(angle).end()
                        .face(Direction.NORTH).texture("#candle").uvs(0, 5, 1, 6).end()
                        .face(Direction.SOUTH).texture("#candle").uvs(0, 5, 1, 6).end().end();
                }
            }
            return model;
        }
        private BlockModelBuilder bareTableModel() {
            var model=models().withExistingParent("ceremonial_table_bare",mcLoc("block/block"))
                .texture("particle",modLoc("block/table_wood")).texture("wood",modLoc("block/table_wood"));
            cuboid(model,0,13,0,16,16,16,"#wood");
            cuboid(model,6,0,6,10,13,10,"#wood");
            return model;
        }
        private BlockModelBuilder chairModel(DyeColor color) {
            var model=models().withExistingParent(ModContent.chairName(color),mcLoc("block/block"))
                .texture("particle",modLoc("block/table_wood")).texture("wood",modLoc("block/table_wood"))
                .texture("gold",modLoc("block/table_gold")).texture("cloth",modLoc("block/table_linen"));
            var parts=new ArrayList<ChairPart>();
            for(int x:new int[]{2,12}) for(int z:new int[]{2,12}) {
                chairPart(parts,x,1,z,x+2,7,z+2,"#wood");chairPart(parts,x,0,z,x+2,1,z+2,"#gold");
            }
            chairPart(parts,2,7,2,14,8,14,"#gold");chairPart(parts,2,8,2,14,10,14,"#cloth");
            chairPart(parts,2,10,12,4,15,14,"#gold");chairPart(parts,12,10,12,14,15,14,"#gold");
            chairPart(parts,4,10,12,12,15,14,"#wood");chairPart(parts,4,11,11.8F,12,15,12,"#cloth");
            chairPart(parts,2,15,12,14,16,14,"#gold");
            for(int x:new int[]{2,12}) {
                chairPart(parts,x,10,3,x+2,12,4,"#gold");chairPart(parts,x,12,3,x+2,13,12,"#gold");
            }
            chairSurface(model,parts);
            return model;
        }
        private record ChairPart(float[] from,float[] to,String texture) {}
        private void chairPart(List<ChairPart> parts,float x1,float y1,float z1,float x2,float y2,float z2,String texture) {
            parts.add(new ChairPart(new float[]{x1,y1,z1},new float[]{x2,y2,z2},texture));
        }
        /** Emit only the union's exterior: adjoining materials share an edge, never a rendered face. */
        private void chairSurface(BlockModelBuilder model,List<ChairPart> parts) {
            for(var part:parts) for(var direction:Direction.values()) {
                int axis=switch(direction.getAxis()) {case X->0;case Y->1;case Z->2;};
                int u=(axis+1)%3,v=(axis+2)%3;
                boolean positive=direction.getAxisDirection()==Direction.AxisDirection.POSITIVE;
                float plane=positive?part.to()[axis]:part.from()[axis];
                var us=chairCuts(parts,part,u);var vs=chairCuts(parts,part,v);
                for(int i=0;i<us.size()-1;i++) for(int j=0;j<vs.size()-1;j++) {
                    var probe=new float[3];probe[axis]=plane+(positive?.0001F:-.0001F);
                    probe[u]=(us.get(i)+us.get(i+1))*.5F;probe[v]=(vs.get(j)+vs.get(j+1))*.5F;
                    boolean covered=parts.stream().anyMatch(other->probe[0]>other.from()[0] && probe[0]<other.to()[0]
                        && probe[1]>other.from()[1] && probe[1]<other.to()[1] && probe[2]>other.from()[2] && probe[2]<other.to()[2]);
                    if(covered)continue;
                    var from=part.from().clone();var to=part.to().clone();from[axis]=to[axis]=plane;
                    from[u]=us.get(i);to[u]=us.get(i+1);from[v]=vs.get(j);to[v]=vs.get(j+1);
                    var element=model.element().from(from[0],from[1],from[2]).to(to[0],to[1],to[2]);
                    var face=element.face(direction).texture(part.texture());
                    if(part.texture().equals("#cloth"))face.tintindex(0);
                    face.end().end();
                }
            }
        }
        private List<Float> chairCuts(List<ChairPart> parts,ChairPart part,int axis) {
            var cuts=new TreeSet<Float>();cuts.add(part.from()[axis]);cuts.add(part.to()[axis]);
            for(var other:parts)for(float edge:new float[]{other.from()[axis],other.to()[axis]})
                if(edge>part.from()[axis] && edge<part.to()[axis])cuts.add(edge);
            return new ArrayList<>(cuts);
        }
        private BlockModelBuilder podiumModel() {
            var model=models().withExistingParent("ceremonial_podium",mcLoc("block/block"))
                .texture("particle",modLoc("block/table_wood")).texture("wood",modLoc("block/table_wood")).texture("gold",modLoc("block/table_gold"));
            cuboid(model,0,0,0,16,1,16,"#wood");cuboid(model,0,1,0,16,2,16,"#gold");
            cuboid(model,4,2,4,12,14,12,"#wood");
            cuboid(model,4,2,3.8F,5,12,4,"#gold");cuboid(model,11,2,3.8F,12,12,4,"#gold");
            cuboid(model,6,7,3.7F,10,10,3.85F,"#gold");
            model.element().from(0,12,3).to(16,16,16).rotation().origin(8,8,8).axis(Direction.Axis.X).angle(-22.5F).end()
                .allFaces((direction,face)->face.texture("#gold").uvs(0,0,16,16)).end();
            model.element().from(1,15.94F,4).to(15,16.02F,15).rotation().origin(8,8,8).axis(Direction.Axis.X).angle(-22.5F).end()
                .allFaces((direction,face)->face.texture("#wood").uvs(0,0,16,16)).end();
            return model;
        }
        private BlockModelBuilder lampModel(boolean lit) {
            var model=models().withExistingParent("welcome_lamp_"+(lit?"on":"off"),mcLoc("block/block"))
                .texture("particle",modLoc("block/table_gold")).texture("gold",modLoc("block/table_gold"))
                .texture("light",mcLoc(lit?"block/sea_lantern":"block/quartz_block_side"));
            cuboid(model,4,0,4,12,1,12,"#gold");cuboid(model,5,1,5,11,2,11,"#gold");
            cuboid(model,7,2,7,9,10,9,"#gold");cuboid(model,4,10,4,12,11,12,"#gold");
            cuboid(model,5,11,5,11,15,11,"#light");
            for(int x:new int[]{4,11})for(int z:new int[]{4,11})cuboid(model,x,11,z,x+1,15,z+1,"#gold");
            cuboid(model,4,15,4,12,16,12,"#gold");
            return model;
        }
        private void cuboid(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
            model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> {
                face.texture(texture); if(texture.equals("#cloth") || texture.equals("#skirt"))face.tintindex(0);
            }).end();
        }
        private void rollItemModel() {
            var model=models().withExistingParent("item/red_carpet_roll",mcLoc("block/block"))
                .texture("particle",modLoc("block/red_velvet")).texture("roll",modLoc("block/red_velvet"));
            // The icon is wider than one block; inferred UVs would leave its atlas sprite.
            model.element().from(-16,2,5).to(32,12,11)
                .allFaces((direction,face)->face.texture("#roll").uvs(0,0,16,16)).end();
            model.element().from(-16,4,3).to(32,10,13)
                .allFaces((direction,face)->face.texture("#roll").uvs(0,0,16,16)).end();
            model.transforms().transform(ItemDisplayContext.GUI).rotation(25,40,0).scale(.38F).end()
                .transform(ItemDisplayContext.GROUND).scale(.20F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0,45,0).scale(.30F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0,45,0).scale(.30F).end().end();
        }
    }
    private static final class Languages extends LanguageProvider {
        private final boolean chinese;
        private static final String[] COLORS = {"白色", "橙色", "品红色", "淡蓝色", "黄色", "黄绿色", "粉红色", "灰色",
            "淡灰色", "青色", "紫色", "蓝色", "棕色", "绿色", "红色", "黑色"};
        Languages(PackOutput output, String locale) { super(output, Overprotocol.MOD_ID, locale); chinese = locale.equals("zh_cn"); }
        @Override protected void addTranslations() {
            for (var color : DyeColor.values()) {
                var name = chinese ? COLORS[color.getId()] : color.getName().replace('_', ' ');
                add(ModContent.CARPETS.get(color).get(), chinese ? name + "礼宾地毯" : name + " Ceremonial Carpet");
                add(ModContent.TABLES.get(color).get(), chinese ? name + "桌布礼宾桌" : name + " Ceremonial Table");
                add(ModContent.CHAIRS.get(color).get(),chinese?name+"礼宾椅":name+" Ceremonial Chair");
            }
            add(ModContent.CEREMONIAL_PODIUM.get(),chinese?"礼宾讲台":"Ceremonial Podium");
            add(ModContent.WELCOME_LAMP.get(),chinese?"金色迎宾灯":"Golden Welcome Lamp");
            add("entity.overprotocol.ceremonial_seat",chinese?"礼宾椅座位":"Ceremonial Chair Seat");
            add("message.overprotocol.chair.occupied",chinese?"这张椅子已经有人就座":"This chair is occupied");
            add("message.overprotocol.chair.space",chinese?"座位上方需要足够的空间":"The chair needs clear head room");
            add(ModContent.CANDLE_HOLDER.get(), chinese ? "金色烛台" : "Golden Candle Holder");
            add(ModContent.HONOR_GUARD.get(), chinese ? "仪仗队雕像" : "Honor Guard Statue");
            add(ModContent.CEREMONIAL_ROPE.get(), chinese ? "礼宾护栏" : "Ceremonial Rope Barrier");
            add(ModContent.RED_CARPET_ROLL_ITEM.get(), chinese ? "30 米红地毯卷" : "30 m Red Carpet Roll");
            add(ModContent.RED_RUNNER.get(), chinese ? "红绒地毯" : "Red Velvet Carpet");
            add("entity.overprotocol.red_carpet_roll", chinese ? "红地毯卷" : "Red Carpet Roll");
            add("tooltip.overprotocol.red_carpet_roll.length", chinese ? "宽 3 格，剩余 %s 米" : "3 blocks wide, %s metres remaining");
            add("tooltip.overprotocol.red_carpet_roll.use", chinese ? "地面右键放下；右键卷筒铺 1 米；空手潜行右键卷回 1 米，完整时收起" : "Place on ground; right-click roll to lay 1 metre; sneak-right-click with empty hands to rewind 1 metre, or pack a full roll");
            add("tooltip.overprotocol.red_carpet_roll.category", chinese ? "礼宾陈设 · 纯红绒面" : "Ceremonial decor · red velvet");
            add("tooltip.overprotocol.red_carpet_roll.size", chinese ? "宽 %s 格 · 总长 %s 米" : "%s blocks wide · %s m full length");
            add("tooltip.overprotocol.red_carpet_roll.remaining", chinese ? "余量  %s 米" : "Remaining  %s m");
            add("tooltip.overprotocol.red_carpet_roll.lay", chinese ? "%s 放置 / 每次铺 %s 米" : "%s place / lay %s m per click");
            add("tooltip.overprotocol.red_carpet_roll.rewind", chinese ? "空手 %s · 卷回 %s 米" : "Empty hands: %s · rewind %s m");
            add("tooltip.overprotocol.red_carpet_roll.pack", chinese ? "完全卷回后，再次操作收进背包" : "When fully rewound, repeat to pack");
            add("message.overprotocol.red_carpet_roll.blocked", chinese ? "前方需要 3 格宽的平整空地，不能跨过障碍或悬空" : "Needs a clear, supported row 3 blocks wide; cannot cross obstacles or gaps");
            add("message.overprotocol.red_carpet_roll.rewind_blocked", chinese ? "后方需要完整的 3 格红毯及地面支撑，卷回位置不能有障碍" : "Rewinding needs a complete supported row of 3 red carpet blocks and a clear roll position");
            add(ModContent.B2_ITEM.get(), chinese ? "B-2 幽灵轰炸机" : "B-2 Spirit");
            add("entity.overprotocol.b2_spirit", chinese ? "B-2 幽灵轰炸机" : "B-2 Spirit");
            add("tooltip.overprotocol.b2.place", chinese ? "实机尺寸：翼展 52.12 格、机长 20.9 格；右键空地部署，右键机身登机" : "Full size: 52.12-block span, 20.9-block length; deploy on open ground, right-click fuselage to board");
            add("tooltip.overprotocol.b2.controls", chinese ? "鼠标转向/俯仰；滚轮/W/S 油门；A/D、空格/Ctrl 辅助；G 起落架；Shift 下机" : "Mouse steer/pitch; Scroll/W/S throttle; A/D, Space/Ctrl assist; G gear; Shift dismount");
            add("tooltip.overprotocol.b2.pack", chinese ? "停稳并下机后，潜行右键收回；驾驶自动跟随镜头，最高约 1010 km/h（280.56 格/秒）" : "Sneak-right-click to pack when parked and empty; chase camera, top speed approx. 1010 km/h (280.56 b/s)");
            add("tooltip.overprotocol.special.more", chinese ? "按住 %s 查看操作详情" : "Hold %s for controls");
            add("tooltip.overprotocol.special.right_click", chinese ? "右键" : "RMB");
            add("tooltip.overprotocol.special.sneak_click", chinese ? "潜行右键" : "Sneak + RMB");
            add("tooltip.overprotocol.b2.category", chinese ? "特殊载具 · 幽灵飞翼" : "Special vehicle · Spirit flying wing");
            add("tooltip.overprotocol.b2.dimensions", chinese ? "翼展 %s 米 · 机长 %s 米" : "%s m span · %s m length");
            add("tooltip.overprotocol.b2.speed", chinese ? "极速约 %s km/h · %s 格/秒" : "Approx. %s km/h · %s blocks/s");
            add("tooltip.overprotocol.b2.protection", chinese ? "掉落物保护 · 耐火 / 抗仙人掌" : "Dropped item: fire / cactus resistant");
            add("tooltip.overprotocol.b2.deploy", chinese ? "%s 空地部署 / 机身登机" : "%s deploy / board fuselage");
            add("tooltip.overprotocol.b2.recover", chinese ? "停稳、无人乘坐：%s 收回" : "Parked and empty: %s to pack");
            add("tooltip.overprotocol.b2.steer", chinese ? "鼠标转向 / 俯仰 · 松手逐渐回稳" : "Mouse steer / pitch · release to level");
            add("tooltip.overprotocol.b2.throttle", chinese ? "滚轮 / %s 调节油门" : "Scroll / %s for throttle");
            add("tooltip.overprotocol.b2.assist", chinese ? "%s 转向 · %s 升降辅助" : "%s turn · %s climb / descend assist");
            add("tooltip.overprotocol.b2.actions", chinese ? "%s 起落架 · %s 下机" : "%s landing gear · %s dismount");
            add("message.overprotocol.b2.space", chinese ? "空间不足：请准备宽至少 56 格的空旷场地，并避开树木和建筑" : "Not enough clearance: prepare an open area at least 56 blocks wide, away from trees and buildings");
            add("message.overprotocol.b2.controls", chinese ? "B-2：鼠标转向/俯仰，松手回稳；滚轮/W/S 油门；已进入第三人称跟随视角" : "B-2: mouse steer/pitch, release to level; scroll/W/S throttle; chase view active");
            add("key.overprotocol.b2.gear", chinese ? "收放起落架" : "Toggle landing gear");
            add("key.categories.overprotocol.b2", chinese ? "顶格礼遇：B-2" : "Overprotocol: B-2");
            add("hud.overprotocol.b2", chinese ? "B-2  油门 %s%%  速度 %s 格/秒  高度 %s  起落架 %s" : "B-2  Throttle %s%%  Speed %s b/s  Alt %s  Gear %s");
            add("hud.overprotocol.b2.passenger", chinese ? "乘客席 · Shift 下机" : "Passenger seat · Shift exit");
            add("hud.overprotocol.b2.gear_down", chinese ? "放下" : "DOWN");
            add("hud.overprotocol.b2.gear_up", chinese ? "收起" : "UP");
            add("hud.overprotocol.b2.compact", chinese ? "B-2 · %s km/h · 油门 %s%% · 起落架%s" : "B-2 · %s km/h · Throttle %s%% · Gear %s");
            add("hud.overprotocol.b2.mouse", chinese ? "鼠标转向/俯仰 · 滚轮 / %s / %s 油门" : "Mouse steer/pitch · Scroll / %s / %s throttle");
            add("hud.overprotocol.b2.actions", chinese ? "%s 起落架 · %s 下机" : "%s gear · %s dismount");
            add("hud.overprotocol.b2.rider", chinese ? "乘客席 · %s 下机" : "Passenger seat · %s dismount");
            add("hud.overprotocol.b2.keys", chinese ? "鼠标操纵 · W/S 油门 · %s 起落架 · Shift 下机" : "Mouse steer · W/S power · %s gear · Shift exit");
            add("tooltip.overprotocol.honor_guard.cycle", chinese
                ? "空手右键：立正 / 敬礼 / 持械 / 高举"
                : "Empty-hand click: attention / salute / arms / raise");
            add("tooltip.overprotocol.honor_guard.hold", chinese
                ? "可持原版旗帜、工具和 TaCZ 枪械"
                : "Holds banners, tools and TaCZ guns");
            add("tooltip.overprotocol.honor_guard.rotate", chinese
                ? "潜行右键：打开控制面板（换皮肤 / 旋转 / 换姿态）"
                : "Sneak-right-click: open the control panel (skin, turn, pose)");
            add("tooltip.overprotocol.honor_guard.skin", chinese
                ? "命名牌写上玩家 ID：右键换肤"
                : "Named tag: right-click to use that player's skin");
            add("tooltip.overprotocol.honor_guard.picker", chinese
                ? "空白命名牌右键：打开面板"
                : "Blank tag: right-click to open panel");
            add("tooltip.overprotocol.honor_guard.category", chinese ? "礼宾陈设 · 可换肤 / 可持物" : "Ceremonial decor · custom skins / equipment");
            add("tooltip.overprotocol.honor_guard.poses", chinese ? "%s 种礼仪姿态 · 原版玩家比例" : "%s ceremonial poses · player proportions");
            add("tooltip.overprotocol.honor_guard.panel", chinese ? "%s 打开控制面板" : "%s open control panel");
            add("tooltip.overprotocol.honor_guard.equip", chinese ? "手持物品 %s 交给雕像" : "Hold an item and %s to equip");
            add("tooltip.overprotocol.honor_guard.take", chinese ? "在控制面板中点击手持物取回" : "Click the held item in the panel to take it");
            add("screen.overprotocol.honor_guard", chinese ? "仪仗队雕像" : "Honor Guard Statue");
            add("screen.overprotocol.honor_guard.hint", chinese
                ? "潜行右键雕像即可随时打开这个面板"
                : "Sneak-right-click the statue to reopen this panel");
            add("screen.overprotocol.honor_guard.by_name", chinese ? "按名字获取皮肤" : "Fetch skin by name");
            add("screen.overprotocol.honor_guard.self", chinese ? "用我自己的皮肤" : "Use my own skin");
            add("screen.overprotocol.honor_guard.file", chinese ? "本地图片…" : "Local image...");
            add("screen.overprotocol.honor_guard.turn", chinese ? "旋转 90°" : "Turn 90°");
            add("screen.overprotocol.honor_guard.main_hand", chinese ? "手持物（点击取回）" : "Held item (click to take)");
            add("screen.overprotocol.honor_guard.nothing", chinese ? "空手" : "nothing");
            add("overprotocol.pose.attention", chinese ? "立正" : "Attention");
            add("overprotocol.pose.salute", chinese ? "敬礼" : "Salute");
            add("overprotocol.pose.present", chinese ? "持械" : "Present arms");
            add("overprotocol.pose.raise", chinese ? "高举" : "Raise");
            add("screen.overprotocol.honor_guard.pose", chinese ? "切换姿态" : "Next pose");
            add("screen.overprotocol.honor_guard.file_failed", chinese
                ? "读取本地皮肤图片失败，请确认是 64x64 的 PNG"
                : "Could not read that skin image; make sure it is a 64x64 PNG");
            add("screen.overprotocol.honor_guard.bad_size", chinese
                ? "皮肤图片必须是 64x64 的 PNG（64x32 的旧版皮肤请先用画图软件补成 64x64）"
                : "The skin image must be a 64x64 PNG (convert legacy 64x32 skins first)");
            add("screen.overprotocol.honor_guard.field", chinese ? "玩家 ID" : "Player ID");
            add("screen.overprotocol.honor_guard.clear", chinese ? "恢复默认" : "Reset");
            add("itemGroup.overprotocol", chinese ? "顶格礼遇" : "Overprotocol");
        }
    }
    private static final class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) { super(output, lookup); }
        @Override protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,ModContent.CEREMONIAL_PODIUM.get())
                .pattern("GPG").pattern("PLP").pattern("PPP").define('G',Items.GOLD_NUGGET).define('P',Items.DARK_OAK_PLANKS).define('L',Items.LECTERN)
                .unlockedBy("has_lectern",has(Items.LECTERN)).save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,ModContent.WELCOME_LAMP.get())
                .pattern(" G ").pattern("GSG").pattern(" G ").define('G',Items.GOLD_INGOT).define('S',Items.SEA_LANTERN)
                .unlockedBy("has_sea_lantern",has(Items.SEA_LANTERN)).save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,ModContent.RED_CARPET_ROLL_ITEM.get())
                .pattern("WWW").pattern("WWW").pattern("WWW").define('W',Items.RED_WOOL)
                .unlockedBy("has_red_wool",has(Items.RED_WOOL)).save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModContent.B2_ITEM.get())
                .pattern("NNN").pattern("NSN").pattern("NNN")
                .define('N', Items.NETHERITE_BLOCK).define('S', Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR)).save(output);
            for (var color : DyeColor.values()) {
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,ModContent.CHAIRS.get(color).get(),2)
                    .pattern("GPG").pattern("PCP").pattern("S S").define('G',Items.GOLD_NUGGET).define('P',Items.DARK_OAK_PLANKS)
                    .define('C',ModContent.CARPETS.get(color).get()).define('S',Items.STICK)
                    .unlockedBy("has_ceremonial_carpet",has(ModContent.CARPETS.get(color).get())).save(output);
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModContent.CARPETS.get(color).get(), 8)
                    .pattern("RRR").pattern("RGR").pattern("RRR")
                    .define('R', ModContent.vanillaCarpet(color)).define('G', Items.GOLD_NUGGET)
                    .unlockedBy("has_carpet", has(ModContent.vanillaCarpet(color)))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, ModContent.carpetName(color)));
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModContent.TABLES.get(color).get(), 2)
                    .pattern("CCC").pattern("PPP").pattern("S S")
                    .define('C', ModContent.CARPETS.get(color).get()).define('P', Items.DARK_OAK_PLANKS).define('S', Items.STICK)
                    .unlockedBy("has_ceremonial_carpet", has(ModContent.CARPETS.get(color).get()))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, ModContent.tableName(color)));
            }
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModContent.CANDLE_HOLDER.get())
                .pattern(" G ").pattern(" G ").pattern("GGG").define('G', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET))
                .save(output, ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "candle_holder"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModContent.HONOR_GUARD_ITEM.get())
                .pattern(" G ").pattern("WCW").pattern("CSC")
                .define('G', Items.GOLD_INGOT).define('W', Items.WHITE_WOOL)
                .define('C', ModContent.CEREMONIAL_CARPET_ITEM.get()).define('S', Items.STICK)
                .unlockedBy("has_ceremonial_carpet", has(ModContent.CEREMONIAL_CARPET_ITEM.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "honor_guard"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModContent.CEREMONIAL_ROPE_ITEM.get(), 2)
                .pattern(" G ").pattern("GCG").pattern(" G ")
                .define('G', Items.GOLD_INGOT).define('C', ModContent.CEREMONIAL_CARPET_ITEM.get())
                .unlockedBy("has_ceremonial_carpet", has(ModContent.CEREMONIAL_CARPET_ITEM.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "ceremonial_rope"));
        }
    }
    private static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) { super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries); }
        @Override protected void generate() {
            dropSelf(ModContent.RED_RUNNER.get());
            for (var color : DyeColor.values()) {
                dropSelf(ModContent.CARPETS.get(color).get());
                dropSelf(ModContent.TABLES.get(color).get());
                dropSelf(ModContent.CHAIRS.get(color).get());
            }
            dropSelf(ModContent.CEREMONIAL_PODIUM.get());dropSelf(ModContent.WELCOME_LAMP.get());
            dropSelf(ModContent.CEREMONIAL_ROPE.get());
            // Only the lower half owns the drop: when either half is mined the partner is removed
            // through Block#updateOrDestroy, which would otherwise hand out a second statue.
            add(ModContent.HONOR_GUARD.get(), createSinglePropConditionTable(
                ModContent.HONOR_GUARD.get(), HonorGuardBlock.HALF, DoubleBlockHalf.LOWER));
            var holder = ModContent.CANDLE_HOLDER.get();
            var loot = createSingleItemTable(holder);
            for (int index = 1; index <= 17; index++)
                loot.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(CandleHolderBlock.candleItem(index)))
                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(holder)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CandleHolderBlock.CANDLE, index)))
                    .when(ExplosionCondition.survivesExplosion()));
            add(holder, loot);
        }
        @Override protected Iterable<Block> getKnownBlocks() {
            return ModContent.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get()).toList();
        }
    }
}
