package plopp.pipecraft.gui.viaductlinker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.linker.PacketCancelScan;
import plopp.pipecraft.Network.linker.PacketUpdateSortedPositions;

public class ViaductLinkerScreen extends AbstractContainerScreen<ViaductLinkerMenu> {

    private static final ResourceLocation VIADUCT_LINKER_GUI = ResourceLocation.fromNamespaceAndPath("logisticpipes", "textures/gui/viaduct_linker.png");
    private static final int maxVisibleButtons = 9;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private static ViaductLinkerScreen currentInstance;
    private SortMode currentSortMode = SortMode.DISTANCE_ASCENDING;
    private boolean alphabeticalSortEnabled = false;
    private boolean customSortEnabled = false; 
    private boolean isDraggingCustom = false;     
    private int draggedIndex = -1;                 
    private int dragOffsetY = 0;                      
    private boolean sentPacket = false;
    
    private enum SortMode {
        DISTANCE_ASCENDING,
        DISTANCE_DESCENDING;

        public SortMode toggle() {
            return this == DISTANCE_ASCENDING ? DISTANCE_DESCENDING : DISTANCE_ASCENDING;
        }
    }
    @Override
    public void onClose() {
        super.onClose();

        if (sentPacket) return; 
        sentPacket = true;

        if (!menu.getCustomSortedLinkers().isEmpty()) {
            List<BlockPos> sorted = menu.getCustomSortedLinkers().stream()
                .map(DataEntryRecord::pos)
                .toList();

            menu.blockEntity.setSortedTargetPositions(sorted);

            PacketUpdateSortedPositions packet = new PacketUpdateSortedPositions(menu.blockEntity.getBlockPos(), sorted);
            NetworkHandler.sendToServer(packet);
        }

        NetworkHandler.sendToServer(new PacketCancelScan(menu.blockEntity.getBlockPos()));
    }
    
    public ViaductLinkerScreen(ViaductLinkerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 215;
        currentInstance = this;
        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
    }
    
    public void updateLinkers(List<DataEntryRecord> newLinkers) {
        if (newLinkers == null || newLinkers.isEmpty()) {
            menu.setLinkers(Collections.emptyList());
            return;
        }
        BlockPos playerPos = menu.blockEntity.getBlockPos();

        if (customSortEnabled && !menu.getCustomSortedLinkers().isEmpty()) {
            menu.setLinkers(new ArrayList<>(menu.getCustomSortedLinkers()));
            return;
        }

        List<DataEntryRecord> sortedList = new ArrayList<>(newLinkers);

        Comparator<DataEntryRecord> comparator;

        if (alphabeticalSortEnabled) {
            comparator = Comparator.comparing(
                (Function<DataEntryRecord, String>) e -> e.name().toString(),
                String.CASE_INSENSITIVE_ORDER
            );
        } else {
        	double px = playerPos.getX() + 0.5;
        	double py = playerPos.getY() + 0.5;
        	double pz = playerPos.getZ() + 0.5;

        	comparator = Comparator.comparingDouble(
        		    e -> e.pos().distToCenterSqr(px, py, pz)
        		);
        }

        if (currentSortMode == SortMode.DISTANCE_DESCENDING) {
            comparator = comparator.reversed();
        }

        sortedList.sort(comparator);

        if (customSortEnabled) {
            menu.setCustomSortedLinkers(new ArrayList<>(sortedList));
        }

        menu.setLinkers(sortedList);

        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }
 
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (menu.hasNewData()) {
            updateLinkers(menu.getLatestLinkers());
        }

        int btnSize = 9;
        int left = leftPos + 174;

        boolean isLoading = menu.isAsyncScanInProgress();

        int distBtnX = left;
        int distBtnY = topPos + 4;
        if (mouseX >= distBtnX && mouseX < distBtnX + btnSize &&
            mouseY >= distBtnY && mouseY < distBtnY + btnSize) {
            if (isLoading) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_loading"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_distance"), mouseX, mouseY);
            }
        }

        int alphaBtnX = left;
        int alphaBtnY = topPos + 16;
        if (mouseX >= alphaBtnX && mouseX < alphaBtnX + btnSize &&
            mouseY >= alphaBtnY && mouseY < alphaBtnY + btnSize) {
            if (isLoading) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_loading"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_alphabet"), mouseX, mouseY);
            }
        }

        int customBtnX = left;
        int customBtnY = topPos + 28;
        if (mouseX >= customBtnX && mouseX < customBtnX + btnSize &&
            mouseY >= customBtnY && mouseY < customBtnY + btnSize) {
            if (isLoading) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_loading"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("gui.linkerscreen.sortmode_custom"), mouseX, mouseY);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.hasNewData()) {
            updateLinkers(menu.getLatestLinkers());
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, this.title, 7, 6, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, VIADUCT_LINKER_GUI);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(VIADUCT_LINKER_GUI, x, y, 0, 0, imageWidth, imageHeight,256, 320  );

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

        guiGraphics.blit(VIADUCT_LINKER_GUI, scrollbarX, scrollbarY, u, 1, scrollbarW, scrollbarH, 256, 320);

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

            if (customSortEnabled && isDraggingCustom && index == draggedIndex) {
                btnY = (int)(mouseY - dragOffsetY);
            }

            BlockPos entryPos = menu.getLinkers().get(index).pos();
            BlockPos currentPos = menu.blockEntity.getBlockPos();

            boolean isCurrent = entryPos.equals(currentPos);

            if (isCurrent) {
                // eigener (aktueller) Connector → immer ausgegraut
                guiGraphics.blit(VIADUCT_LINKER_GUI, btnX, btnY, 1, 258, 129, 14, 256, 320);
            } else {
                // normaler Button
                guiGraphics.blit(VIADUCT_LINKER_GUI, btnX, btnY, btnU, btnV, btnWidth, btnHeight, 256, 320);

                // nur bei anderen Buttons Hover-Effekt anzeigen
                boolean isHovering = mouseX >= btnX && mouseX < btnX + btnWidth
                                  && mouseY >= btnY && mouseY < btnY + btnHeight;
                if (isHovering) {
                    int hoverU = 1;
                    int hoverV = 241;
                    guiGraphics.blit(VIADUCT_LINKER_GUI, btnX, btnY, hoverU, hoverV, btnWidth, btnHeight, 256, 320);
                }
            }

            // Icon wird immer gerendert
            int iconX = btnX - iconSize;
            int iconY = btnY + (btnHeight - iconSize) / 4;
            guiGraphics.blit(VIADUCT_LINKER_GUI, iconX, iconY, iconU, iconV, iconSize, iconSize, 256, 320);

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

        boolean asyncBusy = menu.isAsyncScanInProgress();

        int distBtnX = leftPos + 174;
        int distBtnY = topPos + 4;
        boolean hoveredDist = mouseX >= distBtnX && mouseX < distBtnX + 9 &&
                              mouseY >= distBtnY && mouseY < distBtnY + 9;

        boolean distActive = alphabeticalSortEnabled || (!alphabeticalSortEnabled && !customSortEnabled);

        int distTexX, distTexY;
        if (asyncBusy) {
            distTexX = 214;
            distTexY = (currentSortMode == SortMode.DISTANCE_ASCENDING) ? 31 : 20;
        } else {
            distTexX = hoveredDist ? 202 : (distActive ? 178 : 190);
            distTexY = (currentSortMode == SortMode.DISTANCE_ASCENDING) ? 31 : 19;
        }
        guiGraphics.blit(VIADUCT_LINKER_GUI, distBtnX, distBtnY, distTexX, distTexY, 9, 9, 256, 320);

        int alphaBtnX = leftPos + 174;
        int alphaBtnY = topPos + 16;
        boolean hoveredAlpha = mouseX >= alphaBtnX && mouseX < alphaBtnX + 9 &&
                               mouseY >= alphaBtnY && mouseY < alphaBtnY + 9;

        int alphaTexX, alphaTexY;
        if (asyncBusy) {
            alphaTexX = 214;
            alphaTexY = 43;
        } else {
            alphaTexX = alphabeticalSortEnabled ? (hoveredAlpha ? 202 : 178) : (hoveredAlpha ? 202 : 190);
            alphaTexY = 43;
        }
        guiGraphics.blit(VIADUCT_LINKER_GUI, alphaBtnX, alphaBtnY, alphaTexX, alphaTexY, 9, 9, 256, 320);

        int customBtnX = leftPos + 174;
        int customBtnY = topPos + 28;
        boolean hoveredCustom = mouseX >= customBtnX && mouseX < customBtnX + 9 &&
                               mouseY >= customBtnY && mouseY < customBtnY + 9;

        int customTexX, customTexY;
        if (asyncBusy) {
            customTexX = 214;
            customTexY = 55;
        } else {
            customTexX = customSortEnabled ? (hoveredCustom ? 202 : 178) : (hoveredCustom ? 202 : 190);
            customTexY = 55;
        }
        guiGraphics.blit(VIADUCT_LINKER_GUI, customBtnX, customBtnY, customTexX, customTexY, 9, 9, 256, 320);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int scrollbarX = leftPos + 161;
        int scrollbarW = 5;
        int scrollbarY = getScrollbarY();
        int scrollbarH = 15;

        if (button == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + scrollbarW &&
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

                if (relMouseX >= btnX && relMouseX < btnX + btnWidth && relMouseY >= btnY && relMouseY < btnY + btnHeight) {
                    BlockPos start = menu.blockEntity.getBlockPos();
                    BlockPos target = menu.getLinkers().get(index).pos();

                    if (start.equals(target)) {
  
                        minecraft.player.playSound(SoundEvents.ALLAY_THROW, 1f, 2f);
                        return true;
                    }
                    
                    NetworkHandler.sendTravelStartPacket(start, target);
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
                    this.onClose();
                    return true;
                }
            }
        }
        
        int btnSize = 9;
        int distBtnX = leftPos + 175;
        int distBtnY = topPos + 4;

        if (button == 0 &&
            mouseX >= distBtnX && mouseX < distBtnX + btnSize &&
            mouseY >= distBtnY && mouseY < distBtnY + btnSize) {

            customSortEnabled = false;
            currentSortMode = currentSortMode.toggle();
            updateLinkers(menu.getLinkers());
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
            return true;
        }

        int alphaBtnX = leftPos + 174;
        int alphaBtnY = topPos + 16;

        if (button == 0 &&
            mouseX >= alphaBtnX && mouseX < alphaBtnX + 9 &&
            mouseY >= alphaBtnY && mouseY < alphaBtnY + 9) {

            customSortEnabled = false;
            alphabeticalSortEnabled = !alphabeticalSortEnabled;
            updateLinkers(menu.getLinkers());
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
            return true;
        }

        int customBtnX = leftPos + 174;
        int customBtnY = topPos + 28;

        if (button == 0 &&
            mouseX >= customBtnX && mouseX < customBtnX + btnSize &&
            mouseY >= customBtnY && mouseY < customBtnY + btnSize) {

            if (customSortEnabled) {
                customSortEnabled = false;
                if (!menu.getCustomSortedLinkers().isEmpty()) {
                    List<BlockPos> sorted = menu.getCustomSortedLinkers().stream()
                        .map(DataEntryRecord::pos)
                        .toList();
                    menu.blockEntity.setSortedTargetPositions(sorted);
                }
                updateLinkers(menu.getCustomSortedLinkers());
            } else {
                customSortEnabled = true;
                alphabeticalSortEnabled = false;
                currentSortMode = SortMode.DISTANCE_ASCENDING;

                List<BlockPos> savedOrder = menu.blockEntity.getSortedTargetPositions();
                if (!savedOrder.isEmpty()) {
                    List<DataEntryRecord> sortedList = new ArrayList<>();
                    List<DataEntryRecord> unsorted = new ArrayList<>(menu.getLinkers());

                    for (BlockPos pos : savedOrder) {
                        for (Iterator<DataEntryRecord> it = unsorted.iterator(); it.hasNext(); ) {
                            DataEntryRecord entry = it.next();
                            if (entry.pos().equals(pos)) {
                                sortedList.add(entry);
                                it.remove();
                                break;
                            }
                        }
                    }
                    sortedList.addAll(unsorted);
                    menu.setCustomSortedLinkers(sortedList);
                } else {
                    menu.setCustomSortedLinkers(new ArrayList<>(menu.getLinkers()));
                }

                updateLinkers(menu.getCustomSortedLinkers());
            }
            
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
            return true;
        }

        if (button == 1 && customSortEnabled) {
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

                    isDraggingCustom = true;
                    draggedIndex = index;
                    dragOffsetY = relMouseY - btnY;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && isDraggingCustom) {
            isDraggingCustom = false;
            draggedIndex = -1;
            return true;
        }

        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingCustom && customSortEnabled) {
            List<DataEntryRecord> list = menu.getCustomSortedLinkers();
            if (draggedIndex >= 0 && draggedIndex < list.size()) {
                int relMouseY = (int)(mouseY - (topPos + 24));
                int btnHeight = 14;
                int spacing = btnHeight + 6;

                int targetIndex;
                if (maxScroll > 0) {
                    targetIndex = scrollOffset + Math.floorDiv(relMouseY, spacing);
                } else {
                    targetIndex = Math.floorDiv(relMouseY, spacing);
                }

                targetIndex = Mth.clamp(targetIndex, 0, list.size() - 1);

                if (targetIndex != draggedIndex) {
                    DataEntryRecord draggedItem = list.remove(draggedIndex);
                    list.add(targetIndex, draggedItem);
                    draggedIndex = targetIndex;
                    menu.setCustomSortedLinkers(list);
                    menu.setLinkers(list);
                }
                return true;
            }
        }

        if (maxScroll <= 0) return false;

        if (isDraggingScrollbar) {
        }
        
        if (isDraggingScrollbar) {
            int scrollTrackStart = topPos + 18;
            int scrollTrackEnd = topPos + 204;
            int scrollTrackHeight = scrollTrackEnd - scrollTrackStart;

            float relativeY = (float)(mouseY - scrollTrackStart) / scrollTrackHeight;
            relativeY = Mth.clamp(relativeY, 0f, 1f);

            scrollOffset = (int)(relativeY * maxScroll);
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) return false;

        scrollOffset -= (int) Math.signum(scrollY);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        return true;
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