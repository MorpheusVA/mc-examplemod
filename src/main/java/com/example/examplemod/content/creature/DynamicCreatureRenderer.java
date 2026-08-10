package com.example.examplemod.content.creature;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.content.data.CreatureDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

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
        if ("zombie".equals(base) || "skeleton".equals(base) || "humanoid".equals(base) || "husk".equals(base) || "drowned".equals(base)) {
            this.addLayer(new HumanoidArmorLayer(
                    this,
                    new HumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                    new HumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                    context.getModelManager()
            ));
        }

        // Render held item (Crossbow, Sword, Firearm, Bow, etc.) in mainhand and offhand
        this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
    }

    private static EntityModel<DynamicCreatureEntity> createModel(EntityRendererProvider.Context context, CreatureDefinition def) {
        String base = def.base_entity != null ? def.base_entity.toLowerCase() : "zombie";
        return switch (base) {
            case "pillager", "illager" -> new DynamicIllagerModel(context.bakeLayer(ModelLayers.PILLAGER));
            case "skeleton", "stray" -> new DynamicHumanoidModel(context.bakeLayer(ModelLayers.SKELETON));
            case "creeper" -> new DynamicCreeperModel(context.bakeLayer(ModelLayers.CREEPER));
            case "cow" -> new DynamicQuadrupedModel(context.bakeLayer(ModelLayers.COW));
            case "pig" -> new DynamicQuadrupedModel(context.bakeLayer(ModelLayers.PIG));
            case "spider" -> new DynamicSpiderModel(context.bakeLayer(ModelLayers.SPIDER));
            case "iron_golem" -> new DynamicIronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM));
            default -> new DynamicHumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE));
        };
    }

    @Override
    public ResourceLocation getTextureLocation(DynamicCreatureEntity entity) {
        return textureLocation;
    }

    // Dynamic Illager / Pillager Model
    public static class DynamicIllagerModel extends EntityModel<DynamicCreatureEntity> implements ArmedModel {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public DynamicIllagerModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
            this.root.translateAndRotate(poseStack);
            if (arm == HumanoidArm.RIGHT) {
                this.rightArm.translateAndRotate(poseStack);
            } else {
                this.leftArm.translateAndRotate(poseStack);
            }
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);

            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

            if (entity.isChargingCrossbow()) {
                this.rightArm.yRot = -0.8F;
                this.rightArm.xRot = -0.97F;
                this.leftArm.xRot = -0.97F;
                this.leftArm.yRot = 0.85F;
            } else if (entity.isAggressive()) {
                this.rightArm.xRot = -1.05F;
                this.rightArm.yRot = -0.3F;
                this.leftArm.xRot = -0.9F;
                this.leftArm.yRot = 0.4F;
            } else {
                this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.rightArm.yRot = 0.0F;
                this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
                this.leftArm.yRot = 0.0F;
            }
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }

    // Dynamic Humanoid Biped Model (Zombie, Skeleton, Biped)
    public static class DynamicHumanoidModel extends HumanoidModel<DynamicCreatureEntity> {
        public DynamicHumanoidModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.isChargingCrossbow()) {
                this.rightArm.yRot = -0.8F;
                this.rightArm.xRot = -0.97F;
                this.leftArm.xRot = -0.97F;
                this.leftArm.yRot = 0.85F;
            } else if (entity.isAggressive()) {
                float f = Mth.sin(this.attackTime * (float) Math.PI);
                float f1 = Mth.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * (float) Math.PI);
                this.rightArm.zRot = 0.0F;
                this.leftArm.zRot = 0.0F;
                this.rightArm.yRot = -(0.1F - f * 0.6F);
                this.leftArm.yRot = 0.1F - f * 0.6F;
                this.rightArm.xRot = -((float) Math.PI / 2F);
                this.leftArm.xRot = -((float) Math.PI / 2F);
                this.rightArm.xRot -= f * 1.2F - f1 * 0.4F;
                this.leftArm.xRot -= f * 1.2F - f1 * 0.4F;
                AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);
            }
        }
    }

    // Dynamic Quadruped Model (Cow, Pig)
    public static class DynamicQuadrupedModel extends EntityModel<DynamicCreatureEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightHindLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftFrontLeg;

        public DynamicQuadrupedModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightFrontLeg = root.getChild("right_front_leg");
            this.leftFrontLeg = root.getChild("left_front_leg");
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);
            this.rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }

    // Dynamic Creeper Model
    public static class DynamicCreeperModel extends EntityModel<DynamicCreatureEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightHindLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftFrontLeg;

        public DynamicCreeperModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightFrontLeg = root.getChild("right_front_leg");
            this.leftFrontLeg = root.getChild("left_front_leg");
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);
            this.rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }

    // Dynamic Spider Model
    public static class DynamicSpiderModel extends EntityModel<DynamicCreatureEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightHindLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightMiddleHindLeg;
        private final ModelPart leftMiddleHindLeg;
        private final ModelPart rightMiddleFrontLeg;
        private final ModelPart leftMiddleFrontLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftFrontLeg;

        public DynamicSpiderModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightMiddleHindLeg = root.getChild("right_middle_hind_leg");
            this.leftMiddleHindLeg = root.getChild("left_middle_hind_leg");
            this.rightMiddleFrontLeg = root.getChild("right_middle_front_leg");
            this.leftMiddleFrontLeg = root.getChild("left_middle_front_leg");
            this.rightFrontLeg = root.getChild("right_front_leg");
            this.leftFrontLeg = root.getChild("left_front_leg");
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);

            float legSpeed = 0.6662F;

            this.rightHindLeg.zRot = -(Mth.cos(limbSwing * legSpeed * 2.0F + 0.0F) * 0.4F * limbSwingAmount);
            this.leftHindLeg.zRot = Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI) * 0.4F * limbSwingAmount;
            this.rightMiddleHindLeg.zRot = -(Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI / 2F) * 0.4F * limbSwingAmount);
            this.leftMiddleHindLeg.zRot = Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI * 1.5F) * 0.4F * limbSwingAmount;
            this.rightMiddleFrontLeg.zRot = -(Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI) * 0.4F * limbSwingAmount);
            this.leftMiddleFrontLeg.zRot = Mth.cos(limbSwing * legSpeed * 2.0F + 0.0F) * 0.4F * limbSwingAmount;
            this.rightFrontLeg.zRot = -(Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI * 1.5F) * 0.4F * limbSwingAmount);
            this.leftFrontLeg.zRot = Mth.cos(limbSwing * legSpeed * 2.0F + (float) Math.PI / 2F) * 0.4F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }

    // Dynamic Iron Golem Model
    public static class DynamicIronGolemModel extends EntityModel<DynamicCreatureEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public DynamicIronGolemModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(DynamicCreatureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }
}
