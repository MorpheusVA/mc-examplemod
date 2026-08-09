package com.example.examplemod.content;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.data.BlockDefinition;
import com.example.examplemod.content.data.FluidDefinition;
import com.example.examplemod.content.data.ItemDefinition;
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
                itemDef.name = "Example Item";
                itemDef.description = "An enchanted crystal item loaded dynamically from external config. Restores hunger quickly.";
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
                blockDef.name = "Example Block";
                blockDef.description = "A sturdy runic stone block loaded dynamically from external config.";
                blockDef.destroy_time = 1.5f;
                blockDef.explosion_resistance = 6.0f;
                blockDef.light_emission = 5;
                blockDef.sound_type = "stone";
                blockDef.map_color = "color_cyan";
                blockDef.waila_info = "⚡ Infused with Cyan Energy (External Config)";
                blockDef.has_item = true;

                try (Writer writer = Files.newBufferedWriter(defaultBlockJson, StandardCharsets.UTF_8)) {
                    GSON.toJson(blockDef, writer);
                }
            }

            Path defaultFluidJson = fluidsDir.resolve("acid.json");
            if (!Files.exists(defaultFluidJson)) {
                FluidDefinition fluidDef = new FluidDefinition();
                fluidDef.id = "acid";
                fluidDef.name = "Ácido Corrosivo";
                fluidDef.description = "Um líquido verde e borbulhante altamente corrosivo. Provoca envenenamento e cegueira em contato.";
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

            JsonObject langJson = new JsonObject();
            langJson.addProperty("itemGroup.examplemod", "Example Mod Tab");
            langJson.addProperty("config.jade.plugin_examplemod.dynamic_block_provider", "Dynamic Block Info");

            // 2. Generate item models and lang
            for (ItemDefinition itemDef : itemDefinitions.values()) {
                JsonObject itemModel = new JsonObject();
                itemModel.addProperty("parent", "item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", ExampleMod.MODID + ":item/" + itemDef.id);
                itemModel.add("textures", textures);

                Files.writeString(modelsItemDir.resolve(itemDef.id + ".json"), GSON.toJson(itemModel), StandardCharsets.UTF_8);

                langJson.addProperty("item." + ExampleMod.MODID + "." + itemDef.id, itemDef.name != null ? itemDef.name : itemDef.id);
                if (itemDef.description != null) {
                    langJson.addProperty("jei." + ExampleMod.MODID + "." + itemDef.id + ".description", itemDef.description);
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

                langJson.addProperty("block." + ExampleMod.MODID + "." + blockDef.id, blockDef.name != null ? blockDef.name : blockDef.id);
                if (blockDef.description != null) {
                    langJson.addProperty("jei." + ExampleMod.MODID + "." + blockDef.id + ".description", blockDef.description);
                }
                if (blockDef.waila_info != null) {
                    langJson.addProperty("jade." + ExampleMod.MODID + "." + blockDef.id + ".info", blockDef.waila_info);
                }
            }

            // 4. Generate fluid bucket models and lang
            for (FluidDefinition fluidDef : fluidDefinitions.values()) {
                if (fluidDef.bucket.has_bucket) {
                    JsonObject bucketModel = new JsonObject();
                    bucketModel.addProperty("parent", "item/generated");
                    JsonObject textures = new JsonObject();
                    String bucketTex = fluidDef.bucket.texture != null ? fluidDef.bucket.texture : fluidDef.id + "_bucket";
                    textures.addProperty("layer0", ExampleMod.MODID + ":item/" + bucketTex);
                    bucketModel.add("textures", textures);

                    Files.writeString(modelsItemDir.resolve(fluidDef.id + "_bucket.json"), GSON.toJson(bucketModel), StandardCharsets.UTF_8);

                    langJson.addProperty("item." + ExampleMod.MODID + "." + fluidDef.id + "_bucket", fluidDef.name != null ? "Balde de " + fluidDef.name : "Balde de " + fluidDef.id);
                    if (fluidDef.description != null) {
                        langJson.addProperty("jei." + ExampleMod.MODID + "." + fluidDef.id + "_bucket.description", fluidDef.description);
                    }
                }

                langJson.addProperty("fluid_type." + ExampleMod.MODID + "." + fluidDef.id, fluidDef.name != null ? fluidDef.name : fluidDef.id);
                langJson.addProperty("block." + ExampleMod.MODID + "." + fluidDef.id, fluidDef.name != null ? fluidDef.name : fluidDef.id);

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

            // Write lang JSON
            Files.writeString(langDir.resolve("en_us.json"), GSON.toJson(langJson), StandardCharsets.UTF_8);
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
}
