package com.example.examplemod.content.creature;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.data.CreatureDefinition;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class DynamicCreatureRenderer extends MobRenderer<DynamicCreatureEntity, EntityModel<DynamicCreatureEntity>> {
    private final CreatureDefinition definition;
    private final ResourceLocation textureLocation;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DynamicCreatureRenderer(EntityRendererProvider.Context context, CreatureDefinition definition) {
        super(context, createModel(context, definition), 0.5F);
        this.definition = definition;

        String texName = definition.texture != null ? definition.texture : definition.id;
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/" + texName + ".png");

        String base = definition.base_entity != null ? definition.base_entity.toLowerCase() : "zombie";
        if ("zombie".equals(base) || "skeleton".equals(base) || "humanoid".equals(base) || "husk".equals(base)) {
            if (this.model instanceof ZombieModel zombieModel) {
                this.addLayer(new HumanoidArmorLayer(
                        this,
                        new ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                        new ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                        context.getModelManager()
                ));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static EntityModel createModel(EntityRendererProvider.Context context, CreatureDefinition def) {
        String base = def.base_entity != null ? def.base_entity.toLowerCase() : "zombie";
        return switch (base) {
            case "skeleton", "stray" -> new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON));
            case "creeper" -> new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER));
            case "cow" -> new CowModel<>(context.bakeLayer(ModelLayers.COW));
            case "pig" -> new PigModel<>(context.bakeLayer(ModelLayers.PIG));
            case "spider" -> new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER));
            case "iron_golem" -> new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM));
            default -> new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE));
        };
    }

    @Override
    public ResourceLocation getTextureLocation(DynamicCreatureEntity entity) {
        return textureLocation;
    }
}
