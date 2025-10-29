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

public class TravelPlayerModel<T extends AbstractClientPlayer> extends PlayerModel<T> {
	
    private UUID playerUUID;
    
	@SuppressWarnings("unused")
	private final boolean slim;
	
    public final ModelPart leftSleeve, rightSleeve, leftPants, rightPants, jacket, cloak, ear;

    public TravelPlayerModel(ModelPart root, boolean slim) {
		super(root, slim);
        this.slim = slim;
        this.ear = root.getChild("ear");
        this.cloak = root.getChild("cloak");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.jacket = root.getChild("jacket");
    }
    
    private void setPoseStanding(T player, float yaw) {
        this.body.xRot = (float)Math.toRadians(0);
        this.body.yRot = (float)Math.toRadians(0);
        this.body.zRot = (float)Math.toRadians(0);
        
        this.head.xRot = (float)Math.toRadians(-90); this.head.yRot = 0; this.head.zRot = 0;
        this.head.z = -2f;
        this.head.y = -3f;
        
        this.rightArm.xRot = 0f; this.leftArm.xRot = 0f;
        this.rightArm.yRot = 0f; this.leftArm.yRot = 0f;
        this.rightArm.zRot = (float)Math.toRadians(180); this.leftArm.zRot = (float)Math.toRadians(180);
        this.rightArm.x = -7f; this.leftArm.x = 7f;
        this.rightArm.y = 1f; this.leftArm.y = 1f;

        this.rightLeg.xRot = 0f; this.leftLeg.xRot = 0f;
        this.rightLeg.y = 12f; this.leftLeg.y = 12f;
        this.rightLeg.z = 0f; this.leftLeg.z = 0f;
        
        ModelParts(false);
    }

    private void setPoseUpsideDown(T player, float yaw) {
        this.body.xRot = (float)Math.toRadians(180);
        this.body.yRot = (float)Math.toRadians(0);
        this.body.zRot = (float)Math.toRadians(0);
        
        this.head.xRot = (float)Math.toRadians(90); this.head.yRot = 0; this.head.zRot = 0;
        this.head.z = 2f;
        this.head.x = 0;
        this.head.y = 3f;
        
        this.rightArm.xRot = 0f; this.leftArm.xRot = 0f;
        this.rightArm.yRot = 0f; this.leftArm.yRot = 0f;
        this.rightArm.zRot = 0f; this.leftArm.zRot = 0f;
        this.rightArm.x = -5f; this.leftArm.x = 5f;
        this.rightArm.y = 2f; this.leftArm.y = 2f;

        this.rightLeg.xRot = (float)Math.toRadians(180);
        this.leftLeg.xRot = (float)Math.toRadians(180);
        this.rightLeg.y = -12f; this.leftLeg.y = -12f;
        
        ModelParts(false);
        
    }

    private void setPoseLieDown(T player, float yaw) {
        this.body.xRot = (float)Math.toRadians(90);
        this.body.yRot = (float)Math.toRadians(0);
        this.body.zRot = 0f;

        this.head.xRot = -0f; this.head.yRot = 0f; this.head.zRot = 0f;
        this.head.y = 2f;
        this.head.z = -3f;
        
        this.rightArm.xRot = (float)Math.toRadians(270);
        this.leftArm.xRot = (float)Math.toRadians(270);
        this.rightArm.yRot = 0f; this.leftArm.yRot = 0f;
        this.rightArm.x = -5f; this.leftArm.x = 5f;
        this.rightArm.y = 0f; this.leftArm.y = 0f;
        
        this.rightLeg.xRot = (float)Math.toRadians(90);
        this.leftLeg.xRot = (float)Math.toRadians(90);
        this.rightLeg.y = 0f; this.leftLeg.y = 0f;
        this.rightLeg.z = 12f; this.leftLeg.z = 12f;
        
        ModelParts(false);
    }
    
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.playerUUID = entity.getUUID(); 
  if (!ClientTravelDataManager.hasData(entity.getUUID())) return;
        
        TravelStatePacket travelData = ClientTravelDataManager.getTravelData(entity.getUUID());
        if (ViaductTravel.consumeResetModel(entity.getUUID())) {
            resetModelToDefaultPose();
            return;
        }

        if (travelData == null || !travelData.isActive()) return;
        
        if (entity.level().isClientSide() &&
                Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON &&
                		entity == Minecraft.getInstance().player) return;
        
        
        if (ViaductTravel.consumeResetModel(entity.getUUID())) {
            resetModelToDefaultPose();
            return;
        }

        VerticalDirection vDir = travelData.getVerticalDirection();
        float yaw = travelData.getTravelYaw();

        switch (vDir) {
            case NONE -> setPoseLieDown(entity, yaw);
            case UP -> setPoseStanding(entity, yaw);
            case DOWN -> setPoseUpsideDown(entity, yaw);
        }
    }

    public void resetModelToDefaultPose() {
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

      ModelParts(true);
  }
    
    public void ModelParts(Boolean visible) {
        this.hat.visible = visible;
        this.jacket.visible = visible;
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        TravelStatePacket travelData = ClientTravelDataManager.getTravelData(this.playerUUID);
        if (travelData != null && travelData.isActive()) {
            poseStack.translate(0, 1.1, 0);
        }
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }
}