package plopp.pipecraft.gui.viaductlinker;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import plopp.pipecraft.Network.NetworkHandler;

public class ViaductLinkerIDScreen extends AbstractContainerScreen<ViaductLinkerIDMenu> {
	
	private static final ResourceLocation VIADUCT_LINKER_IDGUI = 
			ResourceLocation.fromNamespaceAndPath("logisticpipes", "textures/gui/viaduct_linker_idgui.png");
	private EditBox textField;
		
	  public ViaductLinkerIDScreen(ViaductLinkerIDMenu menu, Inventory inv, Component title) {
	        super(menu, inv, title);
	        this.imageWidth  = 177;
	        this.imageHeight = 166; 
	    }

	  @Override
	  protected void init() {
	      super.init();
	      textField = new EditBox(font, leftPos + 7, topPos + 20, 162, 16, Component.literal(""));
	      textField.setMaxLength(32);
	      textField.setValue(menu.getCustomName()); 
	      textField.setResponder(this::onTextChanged);
	      addWidget(textField);
	      textField.setFocused(true);
	  }
	  
	  private void onTextChanged(String newText) {
		    menu.setCustomName(newText);
		}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		 	RenderSystem.setShader(GameRenderer::getPositionTexShader);
	        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_IDGUI);

	        int x = (width  - imageWidth ) / 2;
	        int y = (height - imageHeight) / 2;

	        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_IDGUI);
	        guiGraphics.blit(VIADUCT_LINKER_IDGUI, x, y, 0, 0, imageWidth, imageHeight);

	    }
	   
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        textField.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    
	    @Override
	    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);
	    }  
	    
	    @Override
	    public boolean charTyped(char codePoint, int modifiers) {
	        if (textField.isFocused()) {
	            return textField.charTyped(codePoint, modifiers);
	        }
	        return super.charTyped(codePoint, modifiers);
	    }

	    @Override
	    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
	        if (textField.isFocused()) {
	            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
	                this.onClose();
	                return true;
	            }
	            return textField.keyPressed(keyCode, scanCode, modifiers);
	        }

	        return super.keyPressed(keyCode, scanCode, modifiers);
	    }

	    @Override
	    public boolean mouseClicked(double mouseX, double mouseY, int button) {
	        if (textField.mouseClicked(mouseX, mouseY, button)) {
	            return true;
	        }
	        return super.mouseClicked(mouseX, mouseY, button);
	    }
	    @Override
	    public void onClose() {
	    	
	        String value = textField.getValue();
	        menu.setCustomName(value);

	        if (minecraft.player != null && minecraft.level != null && minecraft.level.isClientSide) {
	            NetworkHandler.sendNameToServer(menu.blockEntity.getBlockPos(), value);
	        }

	        super.onClose();
	    }
	    
	    @Override
	    public void removed() {
	        super.removed();
	    }
}