package com.example.examplemod;

import org.slf4j.Logger;

import com.example.examplemod.content.ContentManager;
import com.example.examplemod.content.DynamicBlock;
import com.example.examplemod.content.DynamicDirectionalBlock;
import com.example.examplemod.content.DynamicHorizontalBlock;
import com.example.examplemod.content.DynamicInvertibleBlock;
import com.example.examplemod.content.DynamicItem;
import com.example.examplemod.content.DynamicPillarBlock;
import com.example.examplemod.content.data.BlockDefinition;
import com.example.examplemod.content.data.ItemDefinition;
import com.example.examplemod.content.fluid.DynamicFluidHolder;
import com.example.examplemod.content.fluid.DynamicFluidType;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Creative mode tab deferred register
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Dynamic Creative Tab holding all externally loaded items, blocks, and fluid buckets
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> {
                Item firstItem = ContentManager.getInstance().getDynamicItems().values().stream().findFirst().orElse(null);
                if (firstItem != null) return firstItem.getDefaultInstance();
                Item firstBlockItem = ContentManager.getInstance().getDynamicBlockItems().values().stream().findFirst().orElse(null);
                if (firstBlockItem != null) return firstBlockItem.getDefaultInstance();
                return Items.AIR.getDefaultInstance();
            })
            .displayItems((parameters, output) -> {
                // Add all loaded blocks
                for (BlockItem blockItem : ContentManager.getInstance().getDynamicBlockItems().values()) {
                    output.accept(blockItem);
                }
                // Add all loaded items
                for (DynamicItem item : ContentManager.getInstance().getDynamicItems().values()) {
                    output.accept(item);
                }
                // Add all fluid buckets
                for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                    if (fluidHolder.getBucketItem() != null) {
                        output.accept(fluidHolder.getBucketItem());
                    }
                }
                // Add all spawn eggs
                for (Item spawnEgg : ContentManager.getInstance().getDynamicSpawnEggs().values()) {
                    output.accept(spawnEgg);
                }
            }).build());

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[ExampleMod] Initializing Dynamic Content Mod...");

        // 1. Initialize external content manager (loads items, blocks, fluids, creatures and generates resources)
        ContentManager.getInstance().init();

        // 2. Register dynamic items, blocks, fluids, and entities on RegisterEvent
        modEventBus.addListener(this::registerContent);

        // 3. Register creature attributes
        modEventBus.addListener(this::registerEntityAttributes);

        // 4. Register creative mode tabs
        CREATIVE_MODE_TABS.register(modEventBus);

        // 5. Mod lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ClientModEvents::onRegisterClientExtensions);
            modEventBus.addListener(ClientModEvents::onRegisterEntityRenderers);
            modEventBus.addListener(ClientModEvents::onClientSetup);
        }

        // 6. Game & Server event bus
        NeoForge.EVENT_BUS.register(this);

        // 7. Register mod config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerContent(RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.FLUID_TYPES)) {
            // Register Fluid Types
            for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, fluidHolder.getDefinition().id);
                event.register(NeoForgeRegistries.Keys.FLUID_TYPES, loc, fluidHolder::getFluidType);
                LOGGER.info("[ExampleMod] Registered dynamic fluid type: {}", loc);
            }
        } else if (event.getRegistryKey().equals(Registries.FLUID)) {
            // Register Source & Flowing Fluids
            for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                ResourceLocation sourceLoc = ResourceLocation.fromNamespaceAndPath(MODID, fluidHolder.getDefinition().id);
                ResourceLocation flowingLoc = ResourceLocation.fromNamespaceAndPath(MODID, "flowing_" + fluidHolder.getDefinition().id);
                event.register(Registries.FLUID, sourceLoc, fluidHolder::getSourceFluid);
                event.register(Registries.FLUID, flowingLoc, fluidHolder::getFlowingFluid);
                LOGGER.info("[ExampleMod] Registered dynamic fluids: {} and {}", sourceLoc, flowingLoc);
            }
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            // Register Dynamic Blocks
            for (BlockDefinition blockDef : ContentManager.getInstance().getBlockDefinitions().values()) {
                String rot = blockDef.rotation != null ? blockDef.rotation.toLowerCase() : "none";
                DynamicBlock block = switch (rot) {
                    case "horizontal" -> new DynamicHorizontalBlock(blockDef);
                    case "directional", "all", "6ways" -> new DynamicDirectionalBlock(blockDef);
                    case "axis", "pillar" -> new DynamicPillarBlock(blockDef);
                    case "invertible", "half", "upside_down" -> new DynamicInvertibleBlock(blockDef);
                    default -> new DynamicBlock(blockDef);
                };
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, blockDef.id);
                event.register(Registries.BLOCK, loc, () -> block);
                ContentManager.getInstance().getDynamicBlocks().put(blockDef.id, block);
                LOGGER.info("[ExampleMod] Registered dynamic block: {} (rotation={})", loc, rot);
            }

            // Register Liquid Blocks for Fluids
            for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, fluidHolder.getDefinition().id);
                event.register(Registries.BLOCK, loc, fluidHolder::getBlock);
                LOGGER.info("[ExampleMod] Registered dynamic fluid block: {}", loc);
            }
        } else if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            // Register Dynamic Entity Types
            for (com.example.examplemod.content.data.CreatureDefinition creatureDef : ContentManager.getInstance().getCreatureDefinitions().values()) {
                net.minecraft.world.entity.EntityType<com.example.examplemod.content.creature.DynamicCreatureEntity> entityType = net.minecraft.world.entity.EntityType.Builder
                        .<com.example.examplemod.content.creature.DynamicCreatureEntity>of((type, level) -> new com.example.examplemod.content.creature.DynamicCreatureEntity(type, level, creatureDef), net.minecraft.world.entity.MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .build(creatureDef.id);
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, creatureDef.id);
                event.register(Registries.ENTITY_TYPE, loc, () -> entityType);
                ContentManager.getInstance().getDynamicEntityTypes().put(creatureDef.id, entityType);
                LOGGER.info("[ExampleMod] Registered dynamic entity type: {}", loc);
            }
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            // Register Dynamic Items
            for (ItemDefinition itemDef : ContentManager.getInstance().getItemDefinitions().values()) {
                DynamicItem item = new DynamicItem(itemDef);
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, itemDef.id);
                event.register(Registries.ITEM, loc, () -> item);
                ContentManager.getInstance().getDynamicItems().put(itemDef.id, item);
                LOGGER.info("[ExampleMod] Registered dynamic item: {}", loc);
            }

            // Register Block Items for Dynamic Blocks
            for (BlockDefinition blockDef : ContentManager.getInstance().getBlockDefinitions().values()) {
                if (blockDef.has_item) {
                    DynamicBlock block = ContentManager.getInstance().getDynamicBlocks().get(blockDef.id);
                    if (block != null) {
                        BlockItem blockItem = new BlockItem(block, new Item.Properties());
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, blockDef.id);
                        event.register(Registries.ITEM, loc, () -> blockItem);
                        ContentManager.getInstance().getDynamicBlockItems().put(blockDef.id, blockItem);
                        LOGGER.info("[ExampleMod] Registered dynamic block item: {}", loc);
                    }
                }
            }

            // Register Bucket Items for Fluids
            for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                if (fluidHolder.getBucketItem() != null) {
                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, fluidHolder.getDefinition().id + "_bucket");
                    event.register(Registries.ITEM, loc, fluidHolder::getBucketItem);
                    LOGGER.info("[ExampleMod] Registered dynamic fluid bucket item: {}", loc);
                }
            }

            // Register Spawn Eggs for Creatures
            for (com.example.examplemod.content.data.CreatureDefinition creatureDef : ContentManager.getInstance().getCreatureDefinitions().values()) {
                if (creatureDef.spawn_egg != null && creatureDef.spawn_egg.has_egg) {
                    var entityTypeHolder = ContentManager.getInstance().getDynamicEntityTypes().get(creatureDef.id);
                    if (entityTypeHolder != null) {
                        int primary = parseHexColor(creatureDef.spawn_egg.primary_color, 0x000000);
                        int secondary = parseHexColor(creatureDef.spawn_egg.secondary_color, 0xFFFFFF);
                        Item spawnEgg = new net.neoforged.neoforge.common.DeferredSpawnEggItem(() -> entityTypeHolder, primary, secondary, new Item.Properties());
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, creatureDef.id + "_spawn_egg");
                        event.register(Registries.ITEM, loc, () -> spawnEgg);
                        ContentManager.getInstance().getDynamicSpawnEggs().put(creatureDef.id, spawnEgg);
                        LOGGER.info("[ExampleMod] Registered dynamic spawn egg: {}", loc);
                    }
                }
            }
        }
    }

    private void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        for (com.example.examplemod.content.data.CreatureDefinition creatureDef : ContentManager.getInstance().getCreatureDefinitions().values()) {
            var entityType = ContentManager.getInstance().getDynamicEntityTypes().get(creatureDef.id);
            if (entityType != null) {
                event.put(entityType, com.example.examplemod.content.creature.DynamicCreatureEntity.createAttributes(creatureDef).build());
                LOGGER.info("[ExampleMod] Registered entity attributes for creature: {}", creatureDef.id);
            }
        }
    }

    private static int parseHexColor(String hex, int defaultColor) {
        if (hex == null || hex.isBlank()) return defaultColor;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return (int) Long.parseLong(clean, 16);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[ExampleMod] Common setup complete.");
        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (BlockItem blockItem : ContentManager.getInstance().getDynamicBlockItems().values()) {
                event.accept(blockItem);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (DynamicFluidHolder fluidHolder : ContentManager.getInstance().getDynamicFluids().values()) {
                if (fluidHolder.getBucketItem() != null) {
                    event.accept(fluidHolder.getBucketItem());
                }
            }
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (Item spawnEgg : ContentManager.getInstance().getDynamicSpawnEggs().values()) {
                event.accept(spawnEgg);
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[ExampleMod] Server starting.");
    }

    public static class ClientModEvents {
        public static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            for (DynamicFluidHolder holder : ContentManager.getInstance().getDynamicFluids().values()) {
                DynamicFluidType type = holder.getFluidType();
                event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return type.getStillTexture();
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return type.getFlowingTexture();
                    }

                    @Override
                    public int getTintColor() {
                        return type.getTintColor();
                    }

                    @Override
                    public int getTintColor(net.minecraft.world.level.material.FluidState state, net.minecraft.world.level.BlockAndTintGetter getter, net.minecraft.core.BlockPos pos) {
                        return type.getTintColor();
                    }
                }, type);
                LOGGER.info("[ExampleMod] Registered client fluid extensions for: {}", holder.getDefinition().id);
            }
        }

        public static void onRegisterEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            for (com.example.examplemod.content.data.CreatureDefinition creatureDef : ContentManager.getInstance().getCreatureDefinitions().values()) {
                var entityType = ContentManager.getInstance().getDynamicEntityTypes().get(creatureDef.id);
                if (entityType != null) {
                    event.registerEntityRenderer(entityType, ctx -> new com.example.examplemod.content.creature.DynamicCreatureRenderer(ctx, creatureDef));
                    LOGGER.info("[ExampleMod] Registered client renderer for creature: {}", creatureDef.id);
                }
            }
        }

        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("[ExampleMod] Client setup initialized.");
            event.enqueueWork(() -> {
                for (DynamicFluidHolder holder : ContentManager.getInstance().getDynamicFluids().values()) {
                    RenderType renderType = holder.getDefinition().rendering.is_translucent
                            ? RenderType.translucent()
                            : RenderType.solid();
                    ItemBlockRenderTypes.setRenderLayer(holder.getSourceFluid(), renderType);
                    ItemBlockRenderTypes.setRenderLayer(holder.getFlowingFluid(), renderType);
                }
            });
        }
    }
}
