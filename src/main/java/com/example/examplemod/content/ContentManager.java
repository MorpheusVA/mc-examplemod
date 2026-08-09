package com.example.examplemod.content;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.data.BlockDefinition;
import com.example.examplemod.content.data.FluidDefinition;
import com.example.examplemod.content.data.ItemDefinition;
import com.example.examplemod.content.data.LocalizedText;
import com.example.examplemod.content.fluid.DynamicFluidHolder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

public class ContentManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ContentManager INSTANCE = new ContentManager();

    private Path rootDir;
    private Path itemsDir;
    private Path blocksDir;
    private Path fluidsDir;
    private Path texturesItemDir;
    private Path texturesBlockDir;
    private Path texturesFluidDir;
    private Path generatedPackDir;

    private final Map<String, ItemDefinition> itemDefinitions = new LinkedHashMap<>();
    private final Map<String, BlockDefinition> blockDefinitions = new LinkedHashMap<>();
    private final Map<String, FluidDefinition> fluidDefinitions = new LinkedHashMap<>();

    private final Map<String, DynamicItem> dynamicItems = new LinkedHashMap<>();
    private final Map<String, DynamicBlock> dynamicBlocks = new LinkedHashMap<>();
    private final Map<String, BlockItem> dynamicBlockItems = new LinkedHashMap<>();
    private final Map<String, DynamicFluidHolder> dynamicFluids = new LinkedHashMap<>();

    public static ContentManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        try {
            this.rootDir = FMLPaths.CONFIGDIR.get().resolve("examplemod");
            this.itemsDir = rootDir.resolve("items");
            this.blocksDir = rootDir.resolve("blocks");
            this.fluidsDir = rootDir.resolve("fluids");
            this.texturesItemDir = rootDir.resolve("textures").resolve("item");
            this.texturesBlockDir = rootDir.resolve("textures").resolve("block");
            this.texturesFluidDir = rootDir.resolve("textures").resolve("fluid");
            this.generatedPackDir = rootDir.resolve("generated_pack");

            // Ensure directories exist
            Files.createDirectories(itemsDir);
            Files.createDirectories(blocksDir);
            Files.createDirectories(fluidsDir);
            Files.createDirectories(texturesItemDir);
            Files.createDirectories(texturesBlockDir);
            Files.createDirectories(texturesFluidDir);
            Files.createDirectories(generatedPackDir);

            // Export default examples if missing
            createDefaultExamplesIfMissing();

            // Load definitions from JSON files
            loadDefinitions();

            // Generate runtime resource pack
            generateResourcePack();

            LOGGER.info("[ExampleMod] Loaded {} items, {} blocks, and {} fluids from external configuration at: {}",
                    itemDefinitions.size(), blockDefinitions.size(), fluidDefinitions.size(), rootDir.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Failed to initialize external content manager", e);
        }
    }

    private void createDefaultExamplesIfMissing() {
        try {
            Path defaultItemJson = itemsDir.resolve("example_item.json");
            if (!Files.exists(defaultItemJson)) {
                ItemDefinition itemDef = new ItemDefinition();
                itemDef.id = "example_item";
                itemDef.name = new LocalizedText();
                itemDef.name.put("en_us", "Example Item");
                itemDef.name.put("pt_br", "Item de Exemplo");

                itemDef.description = new LocalizedText();
                itemDef.description.put("en_us", "An enchanted crystal item loaded dynamically from external config. Restores hunger quickly.");
                itemDef.description.put("pt_br", "Um item de cristal encantado carregado dinamicamente via configuração externa. Restaura a fome rapidamente.");

                itemDef.type = "food";
                itemDef.max_stack_size = 64;
                itemDef.rarity = "rare";
                itemDef.food = new ItemDefinition.FoodProperties();
                itemDef.food.nutrition = 4;
                itemDef.food.saturation = 2.0f;
                itemDef.food.always_edible = true;

                try (Writer writer = Files.newBufferedWriter(defaultItemJson, StandardCharsets.UTF_8)) {
                    GSON.toJson(itemDef, writer);
                }
            }

            Path defaultBlockJson = blocksDir.resolve("example_block.json");
            if (!Files.exists(defaultBlockJson)) {
                BlockDefinition blockDef = new BlockDefinition();
                blockDef.id = "example_block";
                blockDef.name = new LocalizedText();
                blockDef.name.put("en_us", "Example Block");
                blockDef.name.put("pt_br", "Bloco de Exemplo");

                blockDef.description = new LocalizedText();
                blockDef.description.put("en_us", "A sturdy runic stone block loaded dynamically from external config.");
                blockDef.description.put("pt_br", "Um bloco de pedra rúnica resistente carregado dinamicamente via configuração externa.");

                blockDef.destroy_time = 1.5f;
                blockDef.explosion_resistance = 6.0f;
                blockDef.light_emission = 5;
                blockDef.sound_type = "stone";
                blockDef.map_color = "color_cyan";

                blockDef.waila_info = new LocalizedText();
                blockDef.waila_info.put("en_us", "⚡ Infused with Cyan Energy (External Config)");
                blockDef.waila_info.put("pt_br", "⚡ Infundido com Energia Ciano (Config Externa)");

                blockDef.has_item = true;

                try (Writer writer = Files.newBufferedWriter(defaultBlockJson, StandardCharsets.UTF_8)) {
                    GSON.toJson(blockDef, writer);
                }
            }

            Path defaultFluidJson = fluidsDir.resolve("acid.json");
            if (!Files.exists(defaultFluidJson)) {
                FluidDefinition fluidDef = new FluidDefinition();
                fluidDef.id = "acid";
                fluidDef.name = new LocalizedText();
                fluidDef.name.put("en_us", "Corrosive Acid");
                fluidDef.name.put("pt_br", "Ácido Corrosivo");

                fluidDef.description = new LocalizedText();
                fluidDef.description.put("en_us", "A highly corrosive bubbling green liquid. Causes poison and blindness on contact.");
                fluidDef.description.put("pt_br", "Um líquido verde e borbulhante altamente corrosivo. Provoca envenenamento e cegueira em contato.");

                fluidDef.rendering.tint_color = "#E040FF40"; // Translucent green
                fluidDef.rendering.is_translucent = true;
                fluidDef.rendering.luminosity = 6;
                fluidDef.physics.flow_speed_ticks = 10;
                fluidDef.physics.flow_distance = 6;
                fluidDef.physics.infinite_source = false;
                fluidDef.physics.density = 1200;
                fluidDef.physics.viscosity = 1500;
                fluidDef.behaviors.drowns_player = true;
                fluidDef.behaviors.catch_fire = false;

                FluidDefinition.StatusEffectConfig poison = new FluidDefinition.StatusEffectConfig();
                poison.effect = "minecraft:poison";
                poison.duration_seconds = 1;
                poison.amplifier = 1;
                poison.linger_seconds = 3.0f;

                FluidDefinition.StatusEffectConfig blindness = new FluidDefinition.StatusEffectConfig();
                blindness.effect = "minecraft:blindness";
                blindness.duration_seconds = 1;
                blindness.amplifier = 0;
                blindness.linger_seconds = 1.0f;

                fluidDef.behaviors.status_effects = java.util.List.of(poison, blindness);
                fluidDef.bucket.has_bucket = true;

                try (Writer writer = Files.newBufferedWriter(defaultFluidJson, StandardCharsets.UTF_8)) {
                    GSON.toJson(fluidDef, writer);
                }
            }

            // Copy default textures if not present
            Path defaultItemTexture = texturesItemDir.resolve("example_item.png");
            if (!Files.exists(defaultItemTexture)) {
                try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/item/example_item.png")) {
                    if (in != null) Files.copy(in, defaultItemTexture, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path defaultBlockTexture = texturesBlockDir.resolve("example_block.png");
            if (!Files.exists(defaultBlockTexture)) {
                try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/block/example_block.png")) {
                    if (in != null) Files.copy(in, defaultBlockTexture, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path defaultAcidStillTexture = texturesFluidDir.resolve("acid_still.png");
            if (!Files.exists(defaultAcidStillTexture)) {
                try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/fluid/acid_still.png")) {
                    if (in != null) Files.copy(in, defaultAcidStillTexture, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path defaultAcidFlowTexture = texturesFluidDir.resolve("acid_flow.png");
            if (!Files.exists(defaultAcidFlowTexture)) {
                try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/fluid/acid_flow.png")) {
                    if (in != null) Files.copy(in, defaultAcidFlowTexture, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[ExampleMod] Could not create default examples", e);
        }
    }

    private void loadDefinitions() {
        itemDefinitions.clear();
        blockDefinitions.clear();
        fluidDefinitions.clear();
        dynamicFluids.clear();

        // Load items
        try (Stream<Path> stream = Files.walk(itemsDir, 1)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    ItemDefinition def = GSON.fromJson(reader, ItemDefinition.class);
                    if (def != null && def.id != null && !def.id.isBlank()) {
                        itemDefinitions.put(def.id, def);
                    }
                } catch (Exception e) {
                    LOGGER.error("[ExampleMod] Error reading item JSON: {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error scanning items directory", e);
        }

        // Load blocks
        try (Stream<Path> stream = Files.walk(blocksDir, 1)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    BlockDefinition def = GSON.fromJson(reader, BlockDefinition.class);
                    if (def != null && def.id != null && !def.id.isBlank()) {
                        blockDefinitions.put(def.id, def);
                    }
                } catch (Exception e) {
                    LOGGER.error("[ExampleMod] Error reading block JSON: {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error scanning blocks directory", e);
        }

        // Load fluids
        try (Stream<Path> stream = Files.walk(fluidsDir, 1)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    FluidDefinition def = GSON.fromJson(reader, FluidDefinition.class);
                    if (def != null && def.id != null && !def.id.isBlank()) {
                        fluidDefinitions.put(def.id, def);
                        DynamicFluidHolder holder = new DynamicFluidHolder(def);
                        dynamicFluids.put(def.id, holder);
                    }
                } catch (Exception e) {
                    LOGGER.error("[ExampleMod] Error reading fluid JSON: {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error scanning fluids directory", e);
        }
    }

    private void generateResourcePack() {
        try {
            // 1. Generate pack.mcmeta
            JsonObject packMeta = new JsonObject();
            JsonObject packObj = new JsonObject();
            packObj.addProperty("pack_format", 34);
            packObj.addProperty("description", "ExampleMod Dynamic Resources");
            packMeta.add("pack", packObj);

            Files.writeString(generatedPackDir.resolve("pack.mcmeta"), GSON.toJson(packMeta), StandardCharsets.UTF_8);

            Path assetsModDir = generatedPackDir.resolve("assets").resolve(ExampleMod.MODID);
            Path modelsItemDir = assetsModDir.resolve("models").resolve("item");
            Path modelsBlockDir = assetsModDir.resolve("models").resolve("block");
            Path blockstatesDir = assetsModDir.resolve("blockstates");
            Path texturesPackItemDir = assetsModDir.resolve("textures").resolve("item");
            Path texturesPackBlockDir = assetsModDir.resolve("textures").resolve("block");
            Path langDir = assetsModDir.resolve("lang");

            Files.createDirectories(modelsItemDir);
            Files.createDirectories(modelsBlockDir);
            Files.createDirectories(blockstatesDir);
            Files.createDirectories(texturesPackItemDir);
            Files.createDirectories(texturesPackBlockDir);
            Files.createDirectories(langDir);

            // Process and copy textures (including automatic GIF to sprite sheet conversion & .mcmeta animations)
            processTextureDirectory(texturesItemDir, texturesPackItemDir);
            processTextureDirectory(texturesBlockDir, texturesPackBlockDir);
            processTextureDirectory(texturesFluidDir, texturesPackBlockDir);

            List<String> langs = List.of("en_us", "pt_br");
            Map<String, JsonObject> langJsonMap = new HashMap<>();
            for (String lang : langs) {
                JsonObject lJson = new JsonObject();
                lJson.addProperty("itemGroup.examplemod", "en_us".equals(lang) ? "Example Mod Tab" : "Aba do Example Mod");
                lJson.addProperty("config.jade.plugin_examplemod.dynamic_block_provider", "en_us".equals(lang) ? "Dynamic Block Info" : "Info do Bloco Dinâmico");
                langJsonMap.put(lang, lJson);
            }

            // 2. Generate item models and lang
            for (ItemDefinition itemDef : itemDefinitions.values()) {
                JsonObject itemModel = new JsonObject();
                itemModel.addProperty("parent", "item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", ExampleMod.MODID + ":item/" + itemDef.id);
                itemModel.add("textures", textures);

                Files.writeString(modelsItemDir.resolve(itemDef.id + ".json"), GSON.toJson(itemModel), StandardCharsets.UTF_8);

                for (String lang : langs) {
                    JsonObject lJson = langJsonMap.get(lang);
                    String name = itemDef.name != null ? itemDef.name.get(lang) : itemDef.id;
                    lJson.addProperty("item." + ExampleMod.MODID + "." + itemDef.id, name);
                    if (itemDef.description != null) {
                        lJson.addProperty("jei." + ExampleMod.MODID + "." + itemDef.id + ".description", itemDef.description.get(lang));
                    }
                }
            }

            // 3. Generate block models, blockstates, block item models and lang
            for (BlockDefinition blockDef : blockDefinitions.values()) {
                JsonObject blockModel = new JsonObject();
                JsonObject texturesObj = new JsonObject();

                if (blockDef.textures != null) {
                    BlockDefinition.BlockTextures t = blockDef.textures;
                    if (t.all != null) {
                        blockModel.addProperty("parent", "minecraft:block/cube_all");
                        texturesObj.addProperty("all", ExampleMod.MODID + ":block/" + t.all);
                    } else if (t.up != null || t.down != null || t.north != null || t.south != null || t.east != null || t.west != null || t.front != null || t.back != null || t.left != null || t.right != null) {
                        blockModel.addProperty("parent", "minecraft:block/cube");
                        String topTex = t.up != null ? t.up : (t.top != null ? t.top : blockDef.id);
                        String bottomTex = t.down != null ? t.down : (t.bottom != null ? t.bottom : topTex);
                        String northTex = t.north != null ? t.north : (t.front != null ? t.front : (t.side != null ? t.side : blockDef.id));
                        String southTex = t.south != null ? t.south : (t.back != null ? t.back : (t.side != null ? t.side : blockDef.id));
                        String eastTex = t.east != null ? t.east : (t.right != null ? t.right : (t.side != null ? t.side : blockDef.id));
                        String westTex = t.west != null ? t.west : (t.left != null ? t.left : (t.side != null ? t.side : blockDef.id));
                        String particleTex = t.particle != null ? t.particle : (t.front != null ? t.front : (t.north != null ? t.north : topTex));

                        texturesObj.addProperty("up", ExampleMod.MODID + ":block/" + topTex);
                        texturesObj.addProperty("down", ExampleMod.MODID + ":block/" + bottomTex);
                        texturesObj.addProperty("north", ExampleMod.MODID + ":block/" + northTex);
                        texturesObj.addProperty("south", ExampleMod.MODID + ":block/" + southTex);
                        texturesObj.addProperty("east", ExampleMod.MODID + ":block/" + eastTex);
                        texturesObj.addProperty("west", ExampleMod.MODID + ":block/" + westTex);
                        texturesObj.addProperty("particle", ExampleMod.MODID + ":block/" + particleTex);
                    } else if (t.top != null || t.bottom != null || t.side != null) {
                        blockModel.addProperty("parent", "minecraft:block/cube_bottom_top");
                        String topTex = t.top != null ? t.top : blockDef.id;
                        String bottomTex = t.bottom != null ? t.bottom : topTex;
                        String sideTex = t.side != null ? t.side : blockDef.id;

                        texturesObj.addProperty("top", ExampleMod.MODID + ":block/" + topTex);
                        texturesObj.addProperty("bottom", ExampleMod.MODID + ":block/" + bottomTex);
                        texturesObj.addProperty("side", ExampleMod.MODID + ":block/" + sideTex);
                    } else {
                        blockModel.addProperty("parent", "minecraft:block/cube_all");
                        String texName = blockDef.texture != null ? blockDef.texture : blockDef.id;
                        texturesObj.addProperty("all", ExampleMod.MODID + ":block/" + texName);
                    }
                } else {
                    blockModel.addProperty("parent", "minecraft:block/cube_all");
                    String texName = blockDef.texture != null ? blockDef.texture : blockDef.id;
                    texturesObj.addProperty("all", ExampleMod.MODID + ":block/" + texName);
                }
                blockModel.add("textures", texturesObj);

                Files.writeString(modelsBlockDir.resolve(blockDef.id + ".json"), GSON.toJson(blockModel), StandardCharsets.UTF_8);

                // Blockstate generation based on rotation mode
                JsonObject blockState = new JsonObject();
                JsonObject variants = new JsonObject();
                String rot = blockDef.rotation != null ? blockDef.rotation.toLowerCase() : "none";

                if ("horizontal".equals(rot)) {
                    JsonObject vNorth = new JsonObject();
                    vNorth.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    variants.add("facing=north", vNorth);

                    JsonObject vEast = new JsonObject();
                    vEast.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vEast.addProperty("y", 90);
                    variants.add("facing=east", vEast);

                    JsonObject vSouth = new JsonObject();
                    vSouth.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vSouth.addProperty("y", 180);
                    variants.add("facing=south", vSouth);

                    JsonObject vWest = new JsonObject();
                    vWest.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vWest.addProperty("y", 270);
                    variants.add("facing=west", vWest);
                } else if ("directional".equals(rot) || "all".equals(rot) || "6ways".equals(rot)) {
                    JsonObject vNorth = new JsonObject();
                    vNorth.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    variants.add("facing=north", vNorth);

                    JsonObject vEast = new JsonObject();
                    vEast.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vEast.addProperty("y", 90);
                    variants.add("facing=east", vEast);

                    JsonObject vSouth = new JsonObject();
                    vSouth.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vSouth.addProperty("y", 180);
                    variants.add("facing=south", vSouth);

                    JsonObject vWest = new JsonObject();
                    vWest.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vWest.addProperty("y", 270);
                    variants.add("facing=west", vWest);

                    JsonObject vUp = new JsonObject();
                    vUp.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vUp.addProperty("x", 270);
                    variants.add("facing=up", vUp);

                    JsonObject vDown = new JsonObject();
                    vDown.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vDown.addProperty("x", 90);
                    variants.add("facing=down", vDown);
                } else if ("axis".equals(rot) || "pillar".equals(rot)) {
                    JsonObject vY = new JsonObject();
                    vY.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    variants.add("axis=y", vY);

                    JsonObject vZ = new JsonObject();
                    vZ.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vZ.addProperty("x", 90);
                    variants.add("axis=z", vZ);

                    JsonObject vX = new JsonObject();
                    vX.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vX.addProperty("x", 90);
                    vX.addProperty("y", 90);
                    variants.add("axis=x", vX);
                } else if ("invertible".equals(rot) || "half".equals(rot) || "upside_down".equals(rot)) {
                    JsonObject vNorthB = new JsonObject();
                    vNorthB.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    variants.add("facing=north,half=bottom", vNorthB);

                    JsonObject vEastB = new JsonObject();
                    vEastB.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vEastB.addProperty("y", 90);
                    variants.add("facing=east,half=bottom", vEastB);

                    JsonObject vSouthB = new JsonObject();
                    vSouthB.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vSouthB.addProperty("y", 180);
                    variants.add("facing=south,half=bottom", vSouthB);

                    JsonObject vWestB = new JsonObject();
                    vWestB.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vWestB.addProperty("y", 270);
                    variants.add("facing=west,half=bottom", vWestB);

                    JsonObject vNorthT = new JsonObject();
                    vNorthT.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vNorthT.addProperty("x", 180);
                    vNorthT.addProperty("y", 0);
                    variants.add("facing=north,half=top", vNorthT);

                    JsonObject vEastT = new JsonObject();
                    vEastT.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vEastT.addProperty("x", 180);
                    vEastT.addProperty("y", 90);
                    variants.add("facing=east,half=top", vEastT);

                    JsonObject vSouthT = new JsonObject();
                    vSouthT.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vSouthT.addProperty("x", 180);
                    vSouthT.addProperty("y", 180);
                    variants.add("facing=south,half=top", vSouthT);

                    JsonObject vWestT = new JsonObject();
                    vWestT.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    vWestT.addProperty("x", 180);
                    vWestT.addProperty("y", 270);
                    variants.add("facing=west,half=top", vWestT);
                } else {
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", ExampleMod.MODID + ":block/" + blockDef.id);
                    variants.add("", variant);
                }

                blockState.add("variants", variants);
                Files.writeString(blockstatesDir.resolve(blockDef.id + ".json"), GSON.toJson(blockState), StandardCharsets.UTF_8);

                // Block Item Model
                if (blockDef.has_item) {
                    JsonObject blockItemModel = new JsonObject();
                    blockItemModel.addProperty("parent", ExampleMod.MODID + ":block/" + blockDef.id);
                    Files.writeString(modelsItemDir.resolve(blockDef.id + ".json"), GSON.toJson(blockItemModel), StandardCharsets.UTF_8);
                }

                for (String lang : langs) {
                    JsonObject lJson = langJsonMap.get(lang);
                    String name = blockDef.name != null ? blockDef.name.get(lang) : blockDef.id;
                    lJson.addProperty("block." + ExampleMod.MODID + "." + blockDef.id, name);
                    if (blockDef.description != null) {
                        lJson.addProperty("jei." + ExampleMod.MODID + "." + blockDef.id + ".description", blockDef.description.get(lang));
                    }
                    if (blockDef.waila_info != null) {
                        lJson.addProperty("jade." + ExampleMod.MODID + "." + blockDef.id + ".info", blockDef.waila_info.get(lang));
                    }
                }
            }

            // 4. Generate fluid bucket models, textures (auto-masking + animation), and lang
            for (FluidDefinition fluidDef : fluidDefinitions.values()) {
                if (fluidDef.bucket.has_bucket) {
                    String bucketTex = fluidDef.bucket.texture != null ? fluidDef.bucket.texture : fluidDef.id + "_bucket";

                    Path userBucketPath = texturesItemDir.resolve(bucketTex + ".png");
                    if (!Files.exists(userBucketPath)) {
                        generateFluidBucketTexture(fluidDef, texturesPackItemDir.resolve(bucketTex + ".png"), texturesPackItemDir.resolve(bucketTex + ".png.mcmeta"));
                    }

                    JsonObject bucketModel = new JsonObject();
                    bucketModel.addProperty("parent", "item/generated");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", ExampleMod.MODID + ":item/" + bucketTex);
                    bucketModel.add("textures", textures);

                    Files.writeString(modelsItemDir.resolve(fluidDef.id + "_bucket.json"), GSON.toJson(bucketModel), StandardCharsets.UTF_8);

                    for (String lang : langs) {
                        JsonObject lJson = langJsonMap.get(lang);
                        String fluidName = fluidDef.name != null ? fluidDef.name.get(lang) : fluidDef.id;
                        String bucketPrefix = "pt_br".equalsIgnoreCase(lang) ? "Balde de " : "Bucket of ";
                        lJson.addProperty("item." + ExampleMod.MODID + "." + fluidDef.id + "_bucket", bucketPrefix + fluidName);
                        if (fluidDef.description != null) {
                            lJson.addProperty("jei." + ExampleMod.MODID + "." + fluidDef.id + "_bucket.description", fluidDef.description.get(lang));
                        }
                    }
                }

                for (String lang : langs) {
                    JsonObject lJson = langJsonMap.get(lang);
                    String fluidName = fluidDef.name != null ? fluidDef.name.get(lang) : fluidDef.id;
                    lJson.addProperty("fluid_type." + ExampleMod.MODID + "." + fluidDef.id, fluidName);
                    lJson.addProperty("block." + ExampleMod.MODID + "." + fluidDef.id, fluidName);
                }

                // Fluid Blockstate
                JsonObject fluidBlockState = new JsonObject();
                JsonObject variants = new JsonObject();
                JsonObject variant = new JsonObject();
                variant.addProperty("model", ExampleMod.MODID + ":block/" + fluidDef.id);
                variants.add("", variant);
                fluidBlockState.add("variants", variants);
                Files.writeString(blockstatesDir.resolve(fluidDef.id + ".json"), GSON.toJson(fluidBlockState), StandardCharsets.UTF_8);

                // Fluid Block Model (particle, still, and flow textures)
                JsonObject fluidBlockModel = new JsonObject();
                JsonObject textures = new JsonObject();
                String stillTex = fluidDef.rendering.still_texture != null ? fluidDef.rendering.still_texture : fluidDef.id + "_still";
                String flowTex = fluidDef.rendering.flow_texture != null ? fluidDef.rendering.flow_texture : fluidDef.id + "_flow";
                textures.addProperty("particle", ExampleMod.MODID + ":block/" + stillTex);
                textures.addProperty("still", ExampleMod.MODID + ":block/" + stillTex);
                textures.addProperty("flow", ExampleMod.MODID + ":block/" + flowTex);
                fluidBlockModel.add("textures", textures);
                Files.writeString(modelsBlockDir.resolve(fluidDef.id + ".json"), GSON.toJson(fluidBlockModel), StandardCharsets.UTF_8);

                // Handle JSON-configured frametime / interpolate for animated fluid textures
                Integer stillFt = fluidDef.rendering.still_frametime != null ? fluidDef.rendering.still_frametime : fluidDef.rendering.frametime;
                Integer flowFt = fluidDef.rendering.flow_frametime != null ? fluidDef.rendering.flow_frametime : fluidDef.rendering.frametime;
                Boolean interp = fluidDef.rendering.interpolate != null ? fluidDef.rendering.interpolate : true;

                if (stillFt != null) {
                    generateOrUpdateMcmeta(texturesPackBlockDir.resolve(stillTex + ".png.mcmeta"), stillFt, interp);
                }
                if (flowFt != null) {
                    generateOrUpdateMcmeta(texturesPackBlockDir.resolve(flowTex + ".png.mcmeta"), flowFt, interp);
                }
            }

            // 6. Generate Atlas sources so all fluid and block textures are guaranteed in blocks.png atlas
            Path atlasesDir = generatedPackDir.resolve("assets").resolve("minecraft").resolve("atlases");
            Files.createDirectories(atlasesDir);

            JsonObject atlasJson = new JsonObject();
            com.google.gson.JsonArray sources = new com.google.gson.JsonArray();
            JsonObject dirSource = new JsonObject();
            dirSource.addProperty("type", "directory");
            dirSource.addProperty("source", ExampleMod.MODID + ":block");
            dirSource.addProperty("prefix", ExampleMod.MODID + ":block/");
            sources.add(dirSource);
            atlasJson.add("sources", sources);

            Files.writeString(atlasesDir.resolve("blocks.json"), GSON.toJson(atlasJson), StandardCharsets.UTF_8);

            // 7. Generate Data Tags for Items, Blocks, and Fluids (compatible with KubeJS, CraftTweaker, Almost Unified)
            Map<String, Map<String, List<String>>> tagData = new HashMap<>();

            for (ItemDefinition itemDef : itemDefinitions.values()) {
                if (itemDef.tags != null) {
                    for (String tagStr : itemDef.tags) {
                        addTagEntry(tagData, "item", tagStr, ExampleMod.MODID + ":" + itemDef.id);
                    }
                }
            }

            for (BlockDefinition blockDef : blockDefinitions.values()) {
                if (blockDef.tags != null) {
                    for (String tagStr : blockDef.tags) {
                        addTagEntry(tagData, "block", tagStr, ExampleMod.MODID + ":" + blockDef.id);
                    }
                }
            }

            for (FluidDefinition fluidDef : fluidDefinitions.values()) {
                if (fluidDef.tags != null) {
                    for (String tagStr : fluidDef.tags) {
                        addTagEntry(tagData, "fluid", tagStr, ExampleMod.MODID + ":" + fluidDef.id);
                        addTagEntry(tagData, "fluid", tagStr, ExampleMod.MODID + ":flowing_" + fluidDef.id);
                    }
                }
            }

            Path dataDir = generatedPackDir.resolve("data");
            for (Map.Entry<String, Map<String, List<String>>> nsEntry : tagData.entrySet()) {
                String namespace = nsEntry.getKey();
                for (Map.Entry<String, List<String>> tagEntry : nsEntry.getValue().entrySet()) {
                    String relPath = tagEntry.getKey();
                    List<String> values = tagEntry.getValue();

                    Path tagFilePath = dataDir.resolve(namespace).resolve("tags").resolve(relPath + ".json");
                    Files.createDirectories(tagFilePath.getParent());

                    JsonObject tagJson = new JsonObject();
                    tagJson.addProperty("replace", false);
                    com.google.gson.JsonArray valArray = new com.google.gson.JsonArray();
                    for (String v : values) {
                        valArray.add(v);
                    }
                    tagJson.add("values", valArray);

                    Files.writeString(tagFilePath, GSON.toJson(tagJson), StandardCharsets.UTF_8);
                }
            }

            // Write lang JSONs
            for (String lang : langs) {
                Files.writeString(langDir.resolve(lang + ".json"), GSON.toJson(langJsonMap.get(lang)), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error generating dynamic resource pack", e);
        }
    }

    public Path getGeneratedPackPath() {
        return generatedPackDir;
    }

    public Map<String, ItemDefinition> getItemDefinitions() {
        return itemDefinitions;
    }

    public Map<String, BlockDefinition> getBlockDefinitions() {
        return blockDefinitions;
    }

    public Map<String, FluidDefinition> getFluidDefinitions() {
        return fluidDefinitions;
    }

    public Map<String, DynamicItem> getDynamicItems() {
        return dynamicItems;
    }

    public Map<String, DynamicBlock> getDynamicBlocks() {
        return dynamicBlocks;
    }

    public Map<String, BlockItem> getDynamicBlockItems() {
        return dynamicBlockItems;
    }

    public Map<String, DynamicFluidHolder> getDynamicFluids() {
        return dynamicFluids;
    }

    private void processTextureDirectory(Path sourceDir, Path targetDir) {
        if (!Files.exists(sourceDir)) return;
        try (Stream<Path> stream = Files.walk(sourceDir, 1)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String filename = p.getFileName().toString();
                try {
                    if (filename.endsWith(".png") || filename.endsWith(".mcmeta")) {
                        Files.copy(p, targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                    } else if (filename.endsWith(".gif")) {
                        String baseName = filename.substring(0, filename.length() - 4);
                        convertGifToSpriteSheet(p, targetDir.resolve(baseName + ".png"), targetDir.resolve(baseName + ".png.mcmeta"));
                    }
                } catch (Exception e) {
                    LOGGER.error("[ExampleMod] Error processing texture file: {}", p, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error reading texture directory: {}", sourceDir, e);
        }
    }

    private void convertGifToSpriteSheet(Path gifPath, Path outPngPath, Path outMcmetaPath) {
        try {
            var readers = javax.imageio.ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return;
            var reader = readers.next();
            try (var inStream = javax.imageio.ImageIO.createImageInputStream(gifPath.toFile())) {
                reader.setInput(inStream);
                int numFrames = reader.getNumImages(true);
                if (numFrames <= 0) return;

                java.awt.image.BufferedImage firstFrame = reader.read(0);
                int width = firstFrame.getWidth();
                int height = firstFrame.getHeight();

                java.awt.image.BufferedImage spriteSheet = new java.awt.image.BufferedImage(
                        width, height * numFrames, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = spriteSheet.createGraphics();

                for (int i = 0; i < numFrames; i++) {
                    java.awt.image.BufferedImage frame = reader.read(i);
                    g.drawImage(frame, 0, i * height, null);
                }
                g.dispose();

                javax.imageio.ImageIO.write(spriteSheet, "PNG", outPngPath.toFile());

                if (!Files.exists(outMcmetaPath)) {
                    int frametimeTicks = 2;
                    try {
                        var metadata = reader.getImageMetadata(0);
                        String formatName = metadata.getNativeMetadataFormatName();
                        if (formatName != null) {
                            var tree = (org.w3c.dom.Node) metadata.getAsTree(formatName);
                            for (int i = 0; i < tree.getChildNodes().getLength(); i++) {
                                var node = tree.getChildNodes().item(i);
                                if ("GraphicControlExtension".equalsIgnoreCase(node.getNodeName())) {
                                    var delayAttr = node.getAttributes() != null ? node.getAttributes().getNamedItem("delayTime") : null;
                                    if (delayAttr != null) {
                                        int delay10ms = Integer.parseInt(delayAttr.getNodeValue());
                                        if (delay10ms > 0) {
                                            frametimeTicks = Math.max(1, Math.round(delay10ms / 5.0f));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    JsonObject mcmeta = new JsonObject();
                    JsonObject anim = new JsonObject();
                    anim.addProperty("frametime", frametimeTicks);
                    anim.addProperty("interpolate", true);
                    mcmeta.add("animation", anim);
                    Files.writeString(outMcmetaPath, GSON.toJson(mcmeta), StandardCharsets.UTF_8);
                }
                LOGGER.info("[ExampleMod] Successfully converted animated GIF '{}' ({} frames) to sprite sheet PNG + mcmeta",
                        gifPath.getFileName(), numFrames);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Failed to convert GIF texture: {}", gifPath, e);
        }
    }

    private void generateOrUpdateMcmeta(Path mcmetaPath, int frametime, boolean interpolate) {
        try {
            JsonObject mcmeta = new JsonObject();
            JsonObject anim = new JsonObject();
            anim.addProperty("frametime", Math.max(1, frametime));
            anim.addProperty("interpolate", interpolate);
            mcmeta.add("animation", anim);
            Files.writeString(mcmetaPath, GSON.toJson(mcmeta), StandardCharsets.UTF_8);
            LOGGER.info("[ExampleMod] Configured animation metadata at {} (frametime={}, interpolate={})",
                    mcmetaPath.getFileName(), frametime, interpolate);
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error writing mcmeta file at {}", mcmetaPath, e);
        }
    }

    private void generateFluidBucketTexture(FluidDefinition fluidDef, Path outBucketPngPath, Path outBucketMcmetaPath) {
        try {
            java.awt.image.BufferedImage templateImage = null;
            java.awt.image.BufferedImage emptyBucketImage = null;

            try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/template/bucket_template.png")) {
                if (in != null) templateImage = javax.imageio.ImageIO.read(in);
            } catch (Exception ignored) {}

            try (InputStream in = ExampleMod.class.getResourceAsStream("/assets/examplemod/textures/template/bucket_empty.png")) {
                if (in != null) emptyBucketImage = javax.imageio.ImageIO.read(in);
            } catch (Exception ignored) {}

            if (templateImage == null) {
                Path diskTemplate = rootDir.resolve("templates").resolve("bucket_template.png");
                if (Files.exists(diskTemplate)) templateImage = javax.imageio.ImageIO.read(diskTemplate.toFile());
            }

            if (emptyBucketImage == null) {
                Path diskEmpty = rootDir.resolve("templates").resolve("bucket_empty.png");
                if (Files.exists(diskEmpty)) emptyBucketImage = javax.imageio.ImageIO.read(diskEmpty.toFile());
            }

            if (templateImage == null) {
                LOGGER.warn("[ExampleMod] Could not load bucket_template.png for fluid bucket generation.");
                return;
            }

            int w = templateImage.getWidth();
            int h = templateImage.getHeight();

            // Smoothly rescale emptyBucketImage to match template dimensions (e.g. 16x16)
            if (emptyBucketImage != null && (emptyBucketImage.getWidth() != w || emptyBucketImage.getHeight() != h)) {
                java.awt.Image scaled = emptyBucketImage.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
                java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = resized.createGraphics();
                g2d.drawImage(scaled, 0, 0, null);
                g2d.dispose();
                emptyBucketImage = resized;
            }

            String flowTexName = fluidDef.rendering.flow_texture != null ? fluidDef.rendering.flow_texture : fluidDef.id + "_flow";
            Path fluidTexPngPath = generatedPackDir.resolve("assets").resolve(ExampleMod.MODID).resolve("textures").resolve("block").resolve(flowTexName + ".png");
            if (!Files.exists(fluidTexPngPath)) {
                String stillTexName = fluidDef.rendering.still_texture != null ? fluidDef.rendering.still_texture : fluidDef.id + "_still";
                fluidTexPngPath = generatedPackDir.resolve("assets").resolve(ExampleMod.MODID).resolve("textures").resolve("block").resolve(stillTexName + ".png");
            }

            java.awt.image.BufferedImage fluidTexture = null;
            if (Files.exists(fluidTexPngPath)) {
                fluidTexture = javax.imageio.ImageIO.read(fluidTexPngPath.toFile());
            }

            int tintARGB = parseColor(fluidDef.rendering);
            int tintA = (tintARGB >> 24) & 0xFF;
            int tintR = (tintARGB >> 16) & 0xFF;
            int tintG = (tintARGB >> 8) & 0xFF;
            int tintB = tintARGB & 0xFF;

            boolean isAnimated = Boolean.TRUE.equals(fluidDef.bucket.animated_bucket) && fluidTexture != null && fluidTexture.getHeight() > h;
            int numFrames = isAnimated ? fluidTexture.getHeight() / h : 1;

            java.awt.image.BufferedImage bucketSheet = new java.awt.image.BufferedImage(
                    w, h * numFrames, java.awt.image.BufferedImage.TYPE_INT_ARGB);

            for (int f = 0; f < numFrames; f++) {
                int frameOffsetY = f * h;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int templatePixel = templateImage.getRGB(x, y);
                        int tA = (templatePixel >> 24) & 0xFF;
                        int tR = (templatePixel >> 16) & 0xFF;
                        int tG = (templatePixel >> 8) & 0xFF;
                        int tB = templatePixel & 0xFF;

                        boolean isBackground = (tA == 0) || (tR >= 255 && tG >= 255 && tB >= 255);
                        boolean isGreenMask = !isBackground && (tG > (tR + 20) && tG > (tB + 20));

                        if (isBackground) {
                            bucketSheet.setRGB(x, y + frameOffsetY, 0);
                        } else if (isGreenMask) {
                            int fluidPixel = 0xFFFFFFFF;
                            if (fluidTexture != null) {
                                int sampleY = (y + (f * h)) % fluidTexture.getHeight();
                                int sampleX = x % fluidTexture.getWidth();
                                fluidPixel = fluidTexture.getRGB(sampleX, sampleY);
                            }
                            int fA = (fluidPixel >> 24) & 0xFF;
                            int fR = (fluidPixel >> 16) & 0xFF;
                            int fG = (fluidPixel >> 8) & 0xFF;
                            int fB = fluidPixel & 0xFF;

                            int fgA = (fA * tintA) / 255;
                            int fgR = (fR * tintR) / 255;
                            int fgG = (fG * tintG) / 255;
                            int fgB = (fB * tintB) / 255;

                            int emptyPixel = (emptyBucketImage != null) ? emptyBucketImage.getRGB(x, y) : templatePixel;
                            int bgA = (emptyPixel >> 24) & 0xFF;
                            int bgR = (emptyPixel >> 16) & 0xFF;
                            int bgG = (emptyPixel >> 8) & 0xFF;
                            int bgB = emptyPixel & 0xFF;

                            if (bgA == 0 || (bgR > 230 && bgG > 230 && bgB > 230)) {
                                bgA = 255;
                                bgR = 0x48;
                                bgG = 0x48;
                                bgB = 0x48;
                            }

                            float srcA = fgA / 255.0f;
                            float dstA = (bgA / 255.0f) * (1.0f - srcA);
                            float outA = srcA + dstA;

                            int finalR = bgR;
                            int finalG = bgG;
                            int finalB = bgB;
                            int finalA = (int) (outA * 255.0f);

                            if (outA > 0.001f) {
                                finalR = Math.min(255, Math.max(0, Math.round((fgR * srcA + bgR * dstA) / outA)));
                                finalG = Math.min(255, Math.max(0, Math.round((fgG * srcA + bgG * dstA) / outA)));
                                finalB = Math.min(255, Math.max(0, Math.round((fgB * srcA + bgB * dstA) / outA)));
                            }

                            int argb = (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
                            bucketSheet.setRGB(x, y + frameOffsetY, argb);
                        } else {
                            int drawPixel = templatePixel;
                            if (emptyBucketImage != null) {
                                int emptyPixel = emptyBucketImage.getRGB(x, y);
                                int eA = (emptyPixel >> 24) & 0xFF;
                                int eR = (emptyPixel >> 16) & 0xFF;
                                int eG = (emptyPixel >> 8) & 0xFF;
                                int eB = emptyPixel & 0xFF;
                                if (eA > 0 && !(eR > 230 && eG > 230 && eB > 230)) {
                                    drawPixel = emptyPixel;
                                }
                            }
                            bucketSheet.setRGB(x, y + frameOffsetY, drawPixel);
                        }
                    }
                }
            }

            javax.imageio.ImageIO.write(bucketSheet, "PNG", outBucketPngPath.toFile());

            if (isAnimated) {
                Integer flowFt = fluidDef.rendering.flow_frametime != null ? fluidDef.rendering.flow_frametime : fluidDef.rendering.frametime;
                int frametimeTicks = flowFt != null ? flowFt : 2;
                Boolean interp = fluidDef.rendering.interpolate != null ? fluidDef.rendering.interpolate : true;
                generateOrUpdateMcmeta(outBucketMcmetaPath, frametimeTicks, interp);
            }

            LOGGER.info("[ExampleMod] Dynamically generated {} fluid bucket texture (animated={}, blended=true)", fluidDef.id, isAnimated);
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error generating bucket texture for {}", fluidDef.id, e);
        }
    }

    private int parseColor(FluidDefinition.Rendering rendering) {
        String hex = rendering.tint_color;
        int baseColor = 0xFFFFFFFF;
        if (hex != null && !hex.isBlank()) {
            try {
                String clean = hex.startsWith("#") ? hex.substring(1) : hex;
                if (clean.length() == 6) {
                    baseColor = (int) (0xFF000000L | Long.parseLong(clean, 16));
                } else if (clean.length() == 8) {
                    baseColor = (int) Long.parseLong(clean, 16);
                }
            } catch (Exception ignored) {}
        }

        Float op = rendering.opacity;
        if (op == null && rendering.transparency != null) {
            float t = rendering.transparency > 1.0f ? rendering.transparency / 100.0f : rendering.transparency;
            op = 1.0f - t;
        }

        if (op != null) {
            float norm = op > 1.0f ? (op / 100.0f) : op;
            int alpha = Math.max(0, Math.min(255, (int) (norm * 255.0f)));
            baseColor = (alpha << 24) | (baseColor & 0x00FFFFFF);
        }
        return baseColor;
    }

    private void addTagEntry(Map<String, Map<String, List<String>>> tagMap, String defaultCategory, String tagString, String elementId) {
        if (tagString == null || tagString.isBlank()) return;
        try {
            ResourceLocation loc = ResourceLocation.parse(tagString);
            String namespace = loc.getNamespace();
            String path = loc.getPath();

            String fullTagKey = defaultCategory + "s/" + path;
            tagMap.computeIfAbsent(namespace, k -> new HashMap<>())
                  .computeIfAbsent(fullTagKey, k -> new ArrayList<>())
                  .add(elementId);
        } catch (Exception ignored) {}
    }

    public String getItemDefinitionsJson() {
        return GSON.toJson(this.itemDefinitions);
    }

    public String getBlockDefinitionsJson() {
        return GSON.toJson(this.blockDefinitions);
    }

    public String getFluidDefinitionsJson() {
        return GSON.toJson(this.fluidDefinitions);
    }

    public void applyServerSyncedConfig(String itemsJson, String blocksJson, String fluidsJson) {
        try {
            LOGGER.info("[ExampleMod] Applying server-synced configuration payload...");

            java.lang.reflect.Type itemType = new com.google.gson.reflect.TypeToken<Map<String, ItemDefinition>>(){}.getType();
            java.lang.reflect.Type blockType = new com.google.gson.reflect.TypeToken<Map<String, BlockDefinition>>(){}.getType();
            java.lang.reflect.Type fluidType = new com.google.gson.reflect.TypeToken<Map<String, FluidDefinition>>(){}.getType();

            Map<String, ItemDefinition> syncedItems = GSON.fromJson(itemsJson, itemType);
            Map<String, BlockDefinition> syncedBlocks = GSON.fromJson(blocksJson, blockType);
            Map<String, FluidDefinition> syncedFluids = GSON.fromJson(fluidsJson, fluidType);

            if (syncedItems != null) {
                this.itemDefinitions.putAll(syncedItems);
            }
            if (syncedBlocks != null) {
                this.blockDefinitions.putAll(syncedBlocks);
            }
            if (syncedFluids != null) {
                this.fluidDefinitions.putAll(syncedFluids);
                for (Map.Entry<String, FluidDefinition> entry : syncedFluids.entrySet()) {
                    DynamicFluidHolder holder = dynamicFluids.get(entry.getKey());
                    if (holder != null) {
                        holder.updateDefinition(entry.getValue());
                    }
                }
            }

            LOGGER.info("[ExampleMod] Successfully applied server-synced configuration! ({} items, {} blocks, {} fluids)",
                    syncedItems != null ? syncedItems.size() : 0,
                    syncedBlocks != null ? syncedBlocks.size() : 0,
                    syncedFluids != null ? syncedFluids.size() : 0);
        } catch (Exception e) {
            LOGGER.error("[ExampleMod] Error applying server-synced configuration payload", e);
        }
    }
}
