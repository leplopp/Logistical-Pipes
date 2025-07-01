package plopp.pipecraft.gui.viaductlinker;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import plopp.pipecraft.Network.LinkedTargetEntryRecord;
import plopp.pipecraft.Network.NetworkHandler;

public class ViaductLinkerScreen extends AbstractContainerScreen<ViaductLinkerMenu> {
	
		private static final ResourceLocation VIADUCT_LINKER_GUI = ResourceLocation.fromNamespaceAndPath("logisticpipes", "textures/gui/viaduct_linker.png");
		private static final int maxVisibleButtons = 9;
		private int scrollOffset = 0;
		private int maxScroll = 0;
		private boolean isDraggingScrollbar = false;
		  private static ViaductLinkerScreen currentInstance;

		  
	  public ViaductLinkerScreen(ViaductLinkerMenu menu, Inventory inv, Component title) {
	        super(menu, inv, title);
	        this.imageWidth  = 175;
	        this.imageHeight = 215; 
	        currentInstance = this;
	        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
	    }
	  public void updateLinkers(List<LinkedTargetEntryRecord> newLinkers) {
		    menu.setLinkers(newLinkers);
		    maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
		}

	    @Override
	    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
	    	maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
	        RenderSystem.setShader(GameRenderer::getPositionTexShader);
	        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_GUI);

	        int x = (width - imageWidth) / 2;
	        int y = (height - imageHeight) / 2;

	        guiGraphics.blit(VIADUCT_LINKER_GUI, x, y, 0, 0, imageWidth, imageHeight);

	        int scrollbarX = x + 161;
	        int scrollbarY = getScrollbarY();
	        int scrollbarW = 5;
	        int scrollbarH = 15;

	        boolean hovering = mouseX >= scrollbarX && mouseX < scrollbarX + scrollbarW &&
	                           mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarH;

	        int u;
	        if (maxScroll <= 0) {
	            u = 194;
	        } else {
	            u = hovering ? 186 : 178;
	        }

	        guiGraphics.blit(VIADUCT_LINKER_GUI, scrollbarX, scrollbarY, u, 1, scrollbarW, scrollbarH);

	        int btnU = 1;
	        int btnV = 224;
	        int btnWidth = 129;
	        int btnHeight = 14;

	        int iconU = 133;
	        int iconV = 224;
	        int iconSize = 16;
	        
	        int spacing = btnHeight + 6;

	        for (int i = 0; i < maxVisibleButtons; i++) {
	            int index = i + scrollOffset;
	            if (index >= menu.linkedNames.size()) break;

	            int btnX = x + 28;
	            int btnY = y + 24 + i * spacing;

	            guiGraphics.blit(VIADUCT_LINKER_GUI, btnX, btnY, btnU, btnV, btnWidth, btnHeight);

	            int iconX = btnX - iconSize;
	            int iconY = btnY + (btnHeight - iconSize) / 4;

	            boolean isHovering = mouseX >= btnX && mouseX < btnX + btnWidth
	                      && mouseY >= btnY && mouseY < btnY + btnHeight;
	                      if (isHovering) {
	                    	  int hoverU = 1;
	                    	  int hoverV = 241;
	                    	  guiGraphics.blit(VIADUCT_LINKER_GUI, btnX, btnY, hoverU, hoverV, btnWidth, btnHeight);
	                      }
	            
	            guiGraphics.blit(VIADUCT_LINKER_GUI, iconX, iconY, iconU, iconV, iconSize, iconSize);

	            ItemStack stack = menu.linkedItems.size() > index ? menu.linkedItems.get(index) : ItemStack.EMPTY;
	            if (!stack.isEmpty()) {
	                float scale = 0.75f;
	                int scaledSize = (int)(iconSize * scale);
	                int offset = (iconSize - scaledSize) / 4;

	                guiGraphics.pose().pushPose();
	                guiGraphics.pose().translate(iconX + offset, iconY + offset, 0);
	                guiGraphics.pose().scale(scale, scale, 1f);
	                guiGraphics.renderItem(stack, 0, 0);
	                guiGraphics.pose().popPose();
	            }

	            Component label = menu.linkedNames.get(index);
	            int textX = btnX + btnWidth / 2;
	            int textY = btnY + (btnHeight - font.lineHeight) / 2 + 1;
	            guiGraphics.drawCenteredString(font, label, textX, textY, 0xFFFFFF);
	        }
	    }

	    @Override
	    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	        if (menu.hasNewData()) {
	            updateLinkers(menu.getLatestLinkers());
	        }
	        super.render(guiGraphics, mouseX, mouseY, partialTick);
	        this.renderTooltip(guiGraphics, mouseX, mouseY);
	    }

	    @Override
	    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	        guiGraphics.drawString(font, this.title, 7, 7, 0x404040, false);
	    }

	    @Override
	    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
	        if (maxScroll <= 0) return false;
	        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
	        return true;
	    }
	    
	    @Override
	    public boolean mouseClicked(double mouseX, double mouseY, int button) {

	        int scrollbarX = leftPos + 161;
	        int scrollbarW = 5;
	        int scrollbarY = getScrollbarY();
	        int scrollbarH = 15;

	        if (mouseX >= scrollbarX && mouseX < scrollbarX + scrollbarW &&
	            mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarH) {
	            isDraggingScrollbar = true;
	            return true;
	        }

	        if (button == 0 && !menu.linkedNames.isEmpty()) {
	            int relMouseX = (int)(mouseX - leftPos);
	            int relMouseY = (int)(mouseY - topPos);

	            int x = 28;
	            int y = 24;
	            int btnWidth = 129;
	            int btnHeight = 14;
	            int spacing = btnHeight + 6;

	            for (int i = 0; i < maxVisibleButtons; i++) {
	                int index = i + scrollOffset;
	                if (index >= menu.linkedNames.size()) break;

	                int btnX = x;
	                int btnY = y + i * spacing;

	                if (relMouseX >= btnX && relMouseX < btnX + btnWidth &&
	                    relMouseY >= btnY && relMouseY < btnY + btnHeight) {

	                    System.out.println("[GUI] Button " + index + " geklickt, Name: " + menu.linkedNames.get(index));

	                    BlockPos start = menu.blockEntity.getBlockPos();
	                    BlockPos target = menu.getLinkers().get(index).pos();

	                    System.out.println("[GUI] Sende TravelStartPacket: start=" + start + ", target=" + target);

	                    NetworkHandler.sendTravelStartPacket(start, target);

	                    this.minecraft.player.displayClientMessage(
	                        Component.literal("Starte Fahrt zum Link " + menu.linkedNames.get(index).getString()), true);

	                    return true;
	                }
	            }
	        }

	        return super.mouseClicked(mouseX, mouseY, button);
	    }
	    
	    @Override
	    public boolean mouseReleased(double mouseX, double mouseY, int button) {
	        isDraggingScrollbar = false;
	        
	        return super.mouseReleased(mouseX, mouseY, button);
	    }

	    @Override
	    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
	    	 if (maxScroll <= 0) return false;
	        if (isDraggingScrollbar) {
	            int scrollTrackStart = topPos + 18;
	            int scrollTrackEnd = topPos + 204;
	            int scrollTrackHeight = scrollTrackEnd - scrollTrackStart;

	            float relativeY = (float)(mouseY - scrollTrackStart) / (float)scrollTrackHeight;
	            relativeY = Mth.clamp(relativeY, 0f, 1f);
	            scrollOffset = Mth.clamp((int)(relativeY * maxScroll), 0, maxScroll);
	            return true;
	        }
	        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	    }
	    
	    private int getScrollbarY() {
	        int scrollTrackStart = 18;
	        int scrollTrackEnd = 189;
	        int scrollTrackHeight = scrollTrackEnd - scrollTrackStart;

	        float scrollRatio = maxScroll == 0 ? 0f : (float) scrollOffset / (float) maxScroll;
	        return topPos + scrollTrackStart + (int)(scrollRatio * scrollTrackHeight);
	    }

	    public static ViaductLinkerScreen instance() {
	        return currentInstance;
	    }
}