package plopp.pipecraft.model;

import java.util.UUID;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import plopp.pipecraft.Network.travel.ClientTravelDataManager;
import plopp.pipecraft.Network.travel.TravelStatePacket;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.logic.ViaductTravel.VerticalDirection;

public class LyingPlayerModel<T extends AbstractClientPlayer> extends PlayerModel<T> {
    private UUID playerUUID;

	public LyingPlayerModel(ModelPart part, boolean slim) {
        super(part, slim);
    }

    @Override
    public void setupAnim(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.playerUUID = player.getUUID(); 

        if (!ClientTravelDataManager.hasData(player.getUUID())) return;
        
        TravelStatePacket travelData = ClientTravelDataManager.getTravelData(player.getUUID());
        if (ViaductTravel.consumeResetModel(player.getUUID())) {
            resetModelToDefaultPose();
            return;
        }

        if (travelData == null || !travelData.isActive()) return;
        
        if (player.level().isClientSide() &&
                Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON &&
                player == Minecraft.getInstance().player) return;
        
        
        if (ViaductTravel.consumeResetModel(player.getUUID())) {
            resetModelToDefaultPose();
            return;
        }
        
        VerticalDirection vdir = ViaductTravel.getVerticalDirection(player.getUUID(),  player.level());
        
        if (vdir != VerticalDirection.NONE) {
            boolean upsideDown = vdir == VerticalDirection.DOWN;
            float yaw = ViaductTravel.getTravelYaw(player.getUUID(), player.level());

            float bodyYaw;
            if ((yaw >= 45 && yaw < 135) || (yaw >= 225 && yaw < 315)) {
                // Ost oder West → Yaw = 0
                bodyYaw = 0f;
            } else {
                // Nord oder Süd → Yaw = Travel-Yaw
                bodyYaw = yaw;
            }

            this.body.xRot = 0;
            this.body.yRot = (float) Math.toRadians(bodyYaw);
            this.body.zRot = upsideDown ? (float) Math.toRadians(180) : 0;

            // Kopf anpassen
            this.head.xRot = upsideDown ? (float) Math.toRadians(90) : (float) Math.toRadians(-90);
            this.head.yRot = 0;
            this.head.zRot = 0;
            this.head.x = 0f;
            this.head.y = 0f;
            this.head.z = upsideDown ? 1f : -1f;

            // Arme anpassen
            this.rightArm.xRot = upsideDown ? 0f : (float) Math.toRadians(180);
            this.rightArm.yRot = upsideDown ? 0f : (float) Math.toRadians(-10);
            this.rightArm.zRot = 0;
            this.rightArm.x = -5;
            this.rightArm.y = 0;
            this.rightArm.z = 0;

            this.leftArm.xRot = upsideDown ? 0f : (float) Math.toRadians(180);
            this.leftArm.yRot = upsideDown ? 0f : (float) Math.toRadians(10);
            this.leftArm.zRot = 0;
            this.leftArm.x = 5;
            this.leftArm.y = 0;
            this.leftArm.z = 0;

            // Beine anpassen
            this.leftLeg.xRot = upsideDown ? (float) Math.toRadians(180) : 0;
            this.leftLeg.y = upsideDown ? -12 : 12;
            this.leftLeg.z = 0;

            this.rightLeg.xRot = upsideDown ? (float) Math.toRadians(180) : 0;
            this.rightLeg.y = upsideDown ? -12 : 12;
            this.rightLeg.z = 0;

            // Kleidung verstecken
            this.hat.visible = false;
            this.jacket.visible = false;
            this.leftSleeve.visible = false;
            this.rightSleeve.visible = false;
            this.leftPants.visible = false;
            this.rightPants.visible = false;

            return;
        }
        
        if (travelData.isActive()) {
        	float yaw = ViaductTravel.getTravelYaw(player.getUUID(),  player.level());

            float correctedYaw = yaw - 180f;
            float radiansYaw = (float) Math.toRadians(correctedYaw);
            
            float normalizedYaw = (yaw % 360 + 360) % 360;
            int direction = Math.round(normalizedYaw / 90f) % 4;

            float rollRotation = 0f;
            switch (direction) {
                case 1: 
                case 3: 
                    rollRotation = (float) Math.toRadians(0);
                    break;
                default:
                    rollRotation = 0f;
                    break;
            }
            
            this.body.xRot = (float) Math.toRadians(90);
            this.body.yRot = 0;
            this.body.zRot = rollRotation;

            this.head.xRot = (float) Math.toRadians(0);
            this.head.yRot = (float) Math.toRadians(0);
            this.head.zRot =  (float) Math.toRadians(0);

            float rightArmZ = radiansYaw + (float) Math.toRadians(-10);
            float leftArmZ  = radiansYaw + (float) Math.toRadians(10);
            float armYOffsetR = 0f;
            float armYOffsetL = 0f;
            float armZOffset = 0f;
            float rightArmX = 0f;
            float leftArmX = 0f;

            switch (direction) {
                case 0: // south
                	rightArmX = -7;
                	leftArmX  = +7;
                    break;
                case 1: // west
                	rightArmX = -6;
                	leftArmX  = +6;
                    armYOffsetL = 1f;
                    armYOffsetR = -1f;
                    armZOffset = 0f;
                    break;
                case 2: // north
                	rightArmX = -5;
                	leftArmX  = +5;
                    break;
                case 3: // east
                	rightArmX = -6;
                	leftArmX  = +6;
                    armYOffsetL = -1f;
                    armYOffsetR = 1f;
                    armZOffset = 0.5f;
                    break;
            }
            
            this.rightArm.xRot = (float) Math.toRadians(270);
            this.rightArm.yRot = 0;
            this.rightArm.zRot = rightArmZ;
            this.rightArm.y = armYOffsetR;
            this.rightArm.z = armZOffset;
            this.rightArm.x = rightArmX;

            this.leftArm.xRot = (float) Math.toRadians(270);
            this.leftArm.yRot = 0;
            this.leftArm.zRot = leftArmZ;
            this.leftArm.y = armYOffsetL;
            this.leftArm.z = armZOffset;
            this.leftArm.x = leftArmX;

            this.leftLeg.xRot = (float) Math.toRadians(90);
            this.leftLeg.yRot = 0;
            this.leftLeg.zRot = radiansYaw;
            this.leftLeg.y = 0F;
            this.leftLeg.z = 12.0F;

            this.rightLeg.xRot = (float) Math.toRadians(90);
            this.rightLeg.yRot = 0;
            this.rightLeg.zRot = radiansYaw;
            this.rightLeg.y = 0F;
            this.rightLeg.z = 12.0F;

            this.hat.visible = false;
            this.jacket.visible = false;
            this.leftSleeve.visible = false;
            this.rightSleeve.visible = false;
            this.leftPants.visible = false;
            this.rightPants.visible = false;
        }
    }
    
    public void resetModelToDefaultPose() {
    	  // System.out.println("Reset Model Pose for: " + this.getClass().getSimpleName());
        this.body.xRot = 0f;
        this.body.yRot = 0f;
        this.body.zRot = 0f;
        this.body.x = 0f;
        this.body.y = 0f;
        this.body.z = 0f;

        this.head.xRot = 0f;
        this.head.yRot = 0f;
        this.head.zRot = 0f;
        this.head.x = 0f;
        this.head.y = 0f;
        this.head.z = 0f;
        
        this.rightArm.xRot = 0;
        this.rightArm.yRot = 0;
        this.rightArm.zRot = 0;
        this.rightArm.x = -5;
        this.rightArm.y = 2;
        this.rightArm.z = 0;

        this.leftArm.xRot = 0;
        this.leftArm.yRot = 0;
        this.leftArm.zRot = 0;
        this.leftArm.x = 5;
        this.leftArm.y = 2;
        this.leftArm.z = 0;

        this.leftLeg.xRot = 0;
        this.leftLeg.yRot = 0;
        this.leftLeg.zRot = 0;
        this.leftLeg.y = 12;
        this.leftLeg.z = 0;

        this.rightLeg.xRot = 0;
        this.rightLeg.yRot = 0;
        this.rightLeg.zRot = 0;
        this.rightLeg.y = 12;
        this.rightLeg.z = 0;

        this.hat.visible = true;
        this.jacket.visible = true;
        this.leftSleeve.visible = true;
        this.rightSleeve.visible = true;
        this.leftPants.visible = true;
        this.rightPants.visible = true;
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (ClientTravelDataManager.hasData(this.playerUUID)) {
            TravelStatePacket travelData = ClientTravelDataManager.getTravelData(this.playerUUID);
            if (travelData != null && travelData.isActive()) {
                poseStack.translate(0, 1.1, 0); 
            }
        }
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }
}