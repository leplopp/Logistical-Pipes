package plopp.pipecraft.gui.pipes;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import plopp.pipecraft.PipeCraftIndex;

public class PipeExtractScreen extends AbstractContainerScreen<PipeExtractMenu> {

	private EditBox textFieldID;
	private EditBox textFieldTAG;
	private boolean sliderModActive = false;
	private boolean sliderTagActive = false;

	private static final ResourceLocation PIPE_EXTRACOTR_GUI = ResourceLocation
			.fromNamespaceAndPath(PipeCraftIndex.MODID, "textures/gui/pipe_extractor_gui.png");

	public PipeExtractScreen(PipeExtractMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 177;
		this.imageHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		textFieldID = new EditBox(font, leftPos + 52, topPos + 16, 118, 16, Component.literal(""));
		textFieldTAG = new EditBox(font, leftPos + 52, topPos + 35, 118, 16, Component.literal(""));
		textFieldID.setResponder(this::onTextChanged);
		textFieldTAG.setResponder(this::onTextChanged);
		addWidget(textFieldID);
		addWidget(textFieldTAG);

		textFieldID.setEditable(false);
		textFieldTAG.setEditable(false);
	}

	private void onTextChanged(String newText) {
		menu.setCustomName(newText);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(PIPE_EXTRACOTR_GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		renderButton(guiGraphics, mouseX, mouseY, 152, 59, 14, 14, 213, 3, 230, 3); // OK
		renderButton(guiGraphics, mouseX, mouseY, 134, 59, 14, 14, 213, 37, 196, 37); // Redstone
		renderButton(guiGraphics, mouseX, mouseY, 52, 53, 18, 18, 200, 54, 179, 54); // Chest
		renderButton(guiGraphics, mouseX, mouseY, 170, 5, 9, 9, 191, 75, 203, 75); // FAQ

		renderToggleButton(guiGraphics, mouseX, mouseY, 170, 17, 14, 14, 196, 20, 179, 20, sliderModActive, 230, 20,
				213, 20); // Mod Slider
		renderToggleButton(guiGraphics, mouseX, mouseY, 170, 36, 14, 14, 196, 20, 179, 20, sliderTagActive, 230, 20,
				213, 20); // Tag Slider

		if (sliderModActive)
			renderButton(guiGraphics, mouseX, mouseY, 184, 17, 14, 14, 213, 87, 230, 87);
		if (sliderTagActive)
			renderButton(guiGraphics, mouseX, mouseY, 184, 36, 14, 14, 213, 87, 230, 87);
	}

	private void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int relX, int relY, int w, int h,
			int normalU, int normalV, int hoverU, int hoverV) {
		int x = leftPos + relX;
		int y = topPos + relY;
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		guiGraphics.blit(PIPE_EXTRACOTR_GUI, x, y, hover ? hoverU : normalU, hover ? hoverV : normalV, w, h, 256, 256);
	}

	private void renderToggleButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int relX, int relY, int w, int h,
			int normalU, int normalV, int hoverU, int hoverV, boolean active, int activeU, int activeV,
			int activeHoverU, int activeHoverV) {
		int x = leftPos + relX;
		int y = topPos + relY;
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

		int texU, texV;

		if (active) {
			if (hover) {
				texU = activeHoverU;
				texV = activeHoverV;
			} else {
				texU = activeU;
				texV = activeV;
			}
		} else {
			if (hover) {
				texU = hoverU;
				texV = hoverV;
			} else {
				texU = normalU;
				texV = normalV;
			}
		}

		guiGraphics.blit(PIPE_EXTRACOTR_GUI, x, y, texU, texV, w, h, 256, 256);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);

		textFieldID.render(guiGraphics, mouseX, mouseY, partialTicks);
		textFieldTAG.render(guiGraphics, mouseX, mouseY, partialTicks);

		renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 152, 59, 14, 14, "screen.pipecraft.guisbtn.close");
		renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 170, 5, 9, 9, "screen.pipecraft.pipeextract.faq");
		renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 52, 53, 18, 18, "screen.pipecraft.pipeextract.filter");
		if (sliderModActive)
			renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 184, 17, 14, 14, "screen.pipecraft.guisbtn.clear");
		if (sliderTagActive)
			renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 184, 36, 14, 14, "screen.pipecraft.guisbtn.clear");
		renderTooltipIfHovered(guiGraphics, mouseX, mouseY, 134, 59, 14, 14,
				"screen.pipecraft.pipeextract.redstonebtn");
	}

	private void renderTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY, int relX, int relY, int w,
			int h, String translationKey) {
		int x = leftPos + relX;
		int y = topPos + relY;
		if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
			guiGraphics.renderTooltip(font, Component.translatable(translationKey), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		textFieldID.mouseClicked(mouseX, mouseY, button);
		textFieldTAG.mouseClicked(mouseX, mouseY, button);

		// OK Button
		if (isInside(mouseX, mouseY, 152, 59, 14, 14)) {
			this.onClose();
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1f);
			return true;
		}

		// Slider MOD toggle
		if (isInside(mouseX, mouseY, 170, 17, 14, 14)) {
			sliderModActive = !sliderModActive;
			textFieldID.setEditable(sliderModActive);
			textFieldID.setFocused(sliderModActive);
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, sliderModActive ? 1f : 0.8f);
			return true;
		}

		// Slider TAG toggle
		if (isInside(mouseX, mouseY, 170, 36, 14, 14)) {
			sliderTagActive = !sliderTagActive;
			textFieldTAG.setEditable(sliderTagActive);
			textFieldTAG.setFocused(sliderTagActive);
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, sliderTagActive ? 1f : 0.8f);
			return true;
		}

		// Clear MOD
		if (sliderModActive && isInside(mouseX, mouseY, 184, 17, 14, 14)) {
			textFieldID.setValue("");
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
			return true;
		}

		// Clear TAG
		if (sliderTagActive && isInside(mouseX, mouseY, 184, 36, 14, 14)) {
			textFieldTAG.setValue("");
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean isInside(double mouseX, double mouseY, int relX, int relY, int w, int h) {
		int x = leftPos + relX;
		int y = topPos + relY;
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return (textFieldID.isFocused() && textFieldID.charTyped(codePoint, modifiers))
				|| (textFieldTAG.isFocused() && textFieldTAG.charTyped(codePoint, modifiers))
				|| super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return (textFieldID.isFocused() && textFieldID.keyPressed(keyCode, scanCode, modifiers))
				|| (textFieldTAG.isFocused() && textFieldTAG.keyPressed(keyCode, scanCode, modifiers))
				|| super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {

		if (!textFieldID.isMouseOver(mouseX, mouseY)) {
			textFieldID.setFocused(false);
		} else if (!textFieldTAG.isMouseOver(mouseX, mouseY)) {
			textFieldTAG.setFocused(false);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void removed() {
		super.removed();
	}
}
