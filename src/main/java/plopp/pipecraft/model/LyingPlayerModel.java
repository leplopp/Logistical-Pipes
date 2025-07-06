package plopp.pipecraft.model;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import plopp.pipecraft.logic.ViaductTravel;

public class LyingPlayerModel<T extends AbstractClientPlayer> extends PlayerModel<T> {
    public LyingPlayerModel(ModelPart part, boolean slim) {
        super(part, slim);
    }

    @Override
    public void setupAnim(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (ViaductTravel.isTravelActive(player)) {
            float yaw = ViaductTravel.getTravelYaw(player.getUUID());
            float radiansYaw = (float) Math.toRadians(yaw);

            // Körper
            this.body.xRot = (float) Math.toRadians(90);
            this.body.yRot = radiansYaw;

            // Kopf – unabhängig von Kamera
            this.head.xRot = 0;
            this.head.yRot = radiansYaw;

            // Arme
            this.rightArm.xRot = (float) Math.toRadians(270);
            this.rightArm.yRot = radiansYaw;
            this.rightArm.zRot = (float) Math.toRadians(-10);
            this.rightArm.y = 0;
            this.rightArm.z = 0;

            this.leftArm.xRot = (float) Math.toRadians(270);
            this.leftArm.yRot = radiansYaw;
            this.leftArm.zRot = (float) Math.toRadians(10);
            this.leftArm.y = 0;
            this.leftArm.z = 0;

            // Beine
            this.leftLeg.xRot = (float) Math.toRadians(90);
            this.leftLeg.yRot = radiansYaw;
            this.leftLeg.y = 0F;
            this.leftLeg.z = 12.0F;

            this.rightLeg.xRot = (float) Math.toRadians(90);
            this.rightLeg.yRot = radiansYaw;
            this.rightLeg.y = 0F;
            this.rightLeg.z = 12.0F;

            // Skin-Layer deaktivieren
            this.hat.visible = false;
            this.jacket.visible = false;
            this.leftSleeve.visible = false;
            this.rightSleeve.visible = false;
            this.leftPants.visible = false;
            this.rightPants.visible = false;
        }
    }
}