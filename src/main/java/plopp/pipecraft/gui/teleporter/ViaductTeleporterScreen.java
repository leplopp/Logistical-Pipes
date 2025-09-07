package plopp.pipecraft.gui.teleporter;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import plopp.pipecraft.Network.teleporter.PacketUpdateTeleporterNames;

public class ViaductTeleporterScreen extends AbstractContainerScreen<ViaductTeleporterMenu> {
	
    private EditBox startNameField;
    private EditBox targetNameField;
    private boolean toggleState;

    
    private static final ResourceLocation VIADUCT_TELEPORTER_IDGUI = 
        ResourceLocation.fromNamespaceAndPath("logisticpipes", "textures/gui/viaduct_teleporter.png");

    public ViaductTeleporterScreen(ViaductTeleporterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 177;
        this.imageHeight = 166;
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
 
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, VIADUCT_TELEPORTER_IDGUI);

        int x = (width  - imageWidth ) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(VIADUCT_TELEPORTER_IDGUI, x, y, 0, 0, imageWidth, imageHeight);

        int buttonX = leftPos + 59;
        int buttonY = topPos + 47;
        int buttonW = 14;
        int buttonH = 14;

        boolean hovered = mouseX >= buttonX && mouseX < buttonX + buttonW &&
                          mouseY >= buttonY && mouseY < buttonY + buttonH;

        int texX, texY;
        if (toggleState) {
            texX = hovered ? 196 : 179;
            texY = 3;
        } else {
            texX = hovered ? 230 : 213;
            texY = 3;
        }

        guiGraphics.blit(VIADUCT_TELEPORTER_IDGUI, buttonX, buttonY, texX, texY, buttonW, buttonH);
        
        
    }
    
    @Override
    protected void init() {
        super.init();
        this.toggleState = menu.getToggleState();
        
        int x = leftPos;
        int y = topPos;

        // Felder initialisieren
        startNameField = new EditBox(font, x + 7, y + 20, 80, 16, Component.empty());
        startNameField.setMaxLength(20);
        startNameField.setValue(normalize(menu.getStartName(), "Teleport Start"));
        startNameField.setHint(Component.literal("Teleport Start"));
        startNameField.setResponder(menu::setStartName);
        addRenderableWidget(startNameField);

        targetNameField = new EditBox(font, x + 89, y + 20, 80, 16, Component.empty());
        targetNameField.setMaxLength(20);
        targetNameField.setValue(normalize(menu.getTargetName(), "Teleport Goal"));
        targetNameField.setHint(Component.literal("Teleport Goal"));
        targetNameField.setResponder(menu::setTargetName);
        addRenderableWidget(targetNameField);
    }

    private String normalize(String name, String fallback) {
        return (name == null || name.isEmpty() || name.equals(fallback)) ? "" : name;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        startNameField.render(guiGraphics, mouseX, mouseY, partialTicks);
        targetNameField.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Toggle-Button Tooltip
        if (mouseX >= leftPos + 59 && mouseX < leftPos + 73 &&
            mouseY >= topPos + 47 && mouseY < topPos + 61) {
        	 guiGraphics.renderTooltip(font, Component.literal(toggleState ? "Aktiviert" : "Deaktiviert"), mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void removed() {
        super.removed();
        menu.setStartName(startNameField.getValue());
        menu.setTargetName(targetNameField.getValue());
        menu.saveChanges();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (startNameField.isFocused()) return startNameField.charTyped(codePoint, modifiers);
        if (targetNameField.isFocused()) return targetNameField.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (startNameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            return startNameField.keyPressed(keyCode, scanCode, modifiers);
        }
        if (targetNameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            return targetNameField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (startNameField.mouseClicked(mouseX, mouseY, button)) {
            startNameField.setFocused(true);
            return true;
        }
        if (targetNameField.mouseClicked(mouseX, mouseY, button)) {
            targetNameField.setFocused(true);
            return true;
        }

        int buttonX = leftPos + 59;
        int buttonY = topPos + 47;
        int buttonW = 14;
        int buttonH = 14;

        if (mouseX >= buttonX && mouseX < buttonX + buttonW &&
        	    mouseY >= buttonY && mouseY < buttonY + buttonH) {
        	this.toggleState = !this.toggleState;
            menu.setToggleState(this.toggleState); // <-- speichert ins BlockEntity
            return true;
        	}
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!startNameField.isMouseOver(mouseX, mouseY)) {
            menu.setStartName(startNameField.getValue());
            startNameField.setFocused(false);
        }
        if (!targetNameField.isMouseOver(mouseX, mouseY)) {
            menu.setTargetName(targetNameField.getValue());
            targetNameField.setFocused(false);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void onClose() {
        super.onClose();

        String startNameValue = startNameField.getValue();
        String targetNameValue = targetNameField.getValue();
        String targetIdValue = menu.getTargetId();
        
        menu.setToggleState(toggleState);
        menu.setStartName(startNameValue);
        menu.setTargetName(targetNameValue);
        menu.setTargetId(targetIdValue);

        if (minecraft.player != null && minecraft.level != null && minecraft.level.isClientSide) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new PacketUpdateTeleporterNames(
                    menu.blockEntity.getBlockPos(),
                    startNameValue,
                    targetNameValue,
                    targetIdValue 
                ));
            }
        }
    }   
}