package com.example.examplemod.content.fluid;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.data.FluidDefinition;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;

public class DynamicFluidType extends FluidType {
    private final FluidDefinition definition;
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final int tintColor;

    public DynamicFluidType(FluidDefinition definition) {
        super(createProperties(definition));
        this.definition = definition;

        this.stillTexture = parseTextureLocation(definition.rendering.use_vanilla_texture, definition.rendering.still_texture, definition.id + "_still", false);
        this.flowingTexture = parseTextureLocation(definition.rendering.use_vanilla_texture, definition.rendering.flow_texture, definition.id + "_flow", true);
        this.tintColor = parseColor(definition.rendering);
    }

    public static ResourceLocation parseTextureLocation(String useVanilla, String customTex, String defaultName, boolean isFlow) {
        if ("lava".equalsIgnoreCase(useVanilla) || "lava".equalsIgnoreCase(customTex)) {
            return ResourceLocation.withDefaultNamespace("block/lava_" + (isFlow ? "flow" : "still"));
        }
        if ("water".equalsIgnoreCase(useVanilla) || "water".equalsIgnoreCase(customTex)) {
            return ResourceLocation.withDefaultNamespace("block/water_" + (isFlow ? "flow" : "still"));
        }
        String tex = customTex != null ? customTex : defaultName;
        if (tex.contains(":")) {
            return ResourceLocation.parse(tex);
        }
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "block/" + tex);
    }

    public FluidDefinition getDefinition() {
        return definition;
    }

    public void updateDefinition(FluidDefinition newDef) {
        if (newDef != null) {
            this.definition.rendering = newDef.rendering;
            this.definition.physics = newDef.physics;
            this.definition.behaviors = newDef.behaviors;
            this.definition.interactions = newDef.interactions;
        }
    }

    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    public int getTintColor() {
        return tintColor;
    }

    private static FluidType.Properties createProperties(FluidDefinition def) {
        FluidType.Properties props = FluidType.Properties.create()
                .density(def.physics.density)
                .temperature(def.physics.temperature)
                .viscosity(def.physics.viscosity)
                .lightLevel(def.rendering.luminosity)
                .canDrown(def.behaviors.drowns_player)
                .canConvertToSource(def.physics.infinite_source)
                .canExtinguish(!def.behaviors.catch_fire)
                .canSwim(true)
                .canPushEntity(true)
                .motionScale(0.014D)
                .supportsBoating(true);

        return props;
    }

    private static int parseColor(FluidDefinition.Rendering rendering) {
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
}
