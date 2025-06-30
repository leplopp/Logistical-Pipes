package com.plopp.pipecraft.gui.viaductlinker;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ViaductLinkerScreen extends AbstractContainerScreen<ViaductLinkerMenu> {
	private static final ResourceLocation VIADUCT_LINKER_GUI = 
			ResourceLocation.fromNamespaceAndPath("logisticpipes", "textures/gui/viaduct_linker.png");
		
		private int scrollOffset = 0;
		private int maxScroll = 0;
		private boolean isDraggingScrollbar = false;
		
	  public ViaductLinkerScreen(ViaductLinkerMenu menu, Inventory inv, Component title) {
	        super(menu, inv, title);
	        this.imageWidth  = 177;
	        this.imageHeight = 225; 
	    }

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		 	RenderSystem.setShader(GameRenderer::getPositionTexShader);
	        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_GUI);

	        int x = (width  - imageWidth ) / 2;
	        int y = (height - imageHeight) / 2;

	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_GUI);
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
	                           
	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_GUI);
	        guiGraphics.blit(VIADUCT_LINKER_GUI, scrollbarX, scrollbarY, u, 1, scrollbarW, scrollbarH);

	    }
	    
	    @Override
	    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
	        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
	    }

	    @Override
	    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	    }
	    
	    @Override
	    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
	    	if (maxScroll <= 0) return false;
	        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
	        return true;
	    }
	    @Override
	    public boolean mouseClicked(double mouseX, double mouseY, int button) {
	    	 if (maxScroll <= 0) return false;
	        int scrollbarX = leftPos + 161;
	        int scrollbarW = 5;
	        int scrollbarY = getScrollbarY();
	        int scrollbarH = 15;

	        if (mouseX >= scrollbarX && mouseX < scrollbarX + scrollbarW &&
	            mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarH) {
	            isDraggingScrollbar = true;
	            return true;
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
	            int scrollTrackEnd = topPos + 196;
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
	        int scrollTrackEnd = 181;
	        int scrollTrackHeight = scrollTrackEnd - scrollTrackStart;

	        float scrollRatio = maxScroll == 0 ? 0f : (float) scrollOffset / (float) maxScroll;
	        return topPos + scrollTrackStart + (int)(scrollRatio * scrollTrackHeight);
	    }
}

