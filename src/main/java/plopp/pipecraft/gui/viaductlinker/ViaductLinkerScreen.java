package plopp.pipecraft.gui.viaductlinker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
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
import plopp.pipecraft.Network.PacketUpdateSortedPositions;


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

    private boolean isDraggingCustom = false;         // Ob gerade per rechter Maustaste gezogen wird
    private int draggedIndex = -1;                      // Index des aktuell gezogenen Buttons
    private int dragOffsetY = 0;                        // Abstand Maus zum oberen Rand des Buttons beim Drag Start

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

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer(); // ruft normalerweise removed() auf
        }

        // Fallback: zur Sicherheit speichern
        if (!menu.getCustomSortedLinkers().isEmpty()) {
            List<BlockPos> sorted = menu.getCustomSortedLinkers().stream()
                .map(LinkedTargetEntryRecord::pos)
                .toList();

            menu.blockEntity.setSortedTargetPositions(sorted); // lokal setzen

            // ➕ Jetzt an Server senden
            PacketUpdateSortedPositions packet = new PacketUpdateSortedPositions(menu.blockEntity.getBlockPos(), sorted);
            NetworkHandler.sendToServer(packet);
            System.out.println("[Screen] PacketUpdateSortedPositions gesendet: " + sorted);
        }
    }
    
    public ViaductLinkerScreen(ViaductLinkerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 215;
        currentInstance = this;
        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
    }
    
    public void updateLinkers(List<LinkedTargetEntryRecord> newLinkers) {
        if (newLinkers == null || newLinkers.isEmpty()) {
            menu.setLinkers(Collections.emptyList());
            return;
        }
        BlockPos playerPos = menu.blockEntity.getBlockPos();

        // Wenn CustomSort an und Liste nicht leer, einfach diese nutzen, keine Sortierung neu machen
        if (customSortEnabled && !menu.getCustomSortedLinkers().isEmpty()) {
            // Setzt die Custom-Sortierte Liste als aktive Liste
            menu.setLinkers(new ArrayList<>(menu.getCustomSortedLinkers()));
            return;
        }

        // Ansonsten neue Liste kopieren und sortieren
        List<LinkedTargetEntryRecord> sortedList = new ArrayList<>(newLinkers);

        Comparator<LinkedTargetEntryRecord> comparator;

        if (alphabeticalSortEnabled) {
            comparator = Comparator.comparing(
                (Function<LinkedTargetEntryRecord, String>) e -> e.name().toString(),
                String.CASE_INSENSITIVE_ORDER
            );
        } else {
            comparator = Comparator.comparingDouble(
                (ToDoubleFunction<LinkedTargetEntryRecord>) e -> e.pos().distToCenterSqr(playerPos.getCenter())
            );
        }

        if (currentSortMode == SortMode.DISTANCE_DESCENDING) {
            comparator = comparator.reversed();
        }

        sortedList.sort(comparator);

        if (customSortEnabled) {
            // Initialisiere Custom-Sort-Liste, falls noch nicht vorhanden
            menu.setCustomSortedLinkers(new ArrayList<>(sortedList));
        }

        // Setze die sortierte Liste in menu
        menu.setLinkers(sortedList);

        // Scrollgrenzen neu berechnen
        maxScroll = Math.max(0, menu.linkedNames.size() - maxVisibleButtons);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Erstmalige Anzeige korrekt initialisieren
        if (customSortEnabled && !menu.getCustomSortedLinkers().isEmpty() && menu.getLinkers() != menu.getCustomSortedLinkers()) {
            menu.setLinkers(menu.getCustomSortedLinkers());
        }

        if (menu.hasNewData()) {
            updateLinkers(menu.getLatestLinkers());
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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

        guiGraphics.blit(VIADUCT_LINKER_GUI, x, y, 0, 0, imageWidth, imageHeight);

        // Scrollbar
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

        // Buttons rendern
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

            // Wenn der Button aktuell gezogen wird, an Maus Y verschieben
            if (customSortEnabled && isDraggingCustom && index == draggedIndex) {
                btnY = (int)(mouseY - dragOffsetY);
            }

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

        // Distanz-Sortierbutton
        int distBtnX = leftPos + 174;
        int distBtnY = topPos + 4;
        boolean hoveredDist = mouseX >= distBtnX && mouseX < distBtnX + 9 &&
                              mouseY >= distBtnY && mouseY < distBtnY + 9;

        boolean distActive = alphabeticalSortEnabled || (!alphabeticalSortEnabled && !customSortEnabled);

        int distTexX = hoveredDist ? 202 : (distActive ? 178 : 190);
        int distTexY = (currentSortMode == SortMode.DISTANCE_ASCENDING) ? 31 : 19;

        guiGraphics.blit(VIADUCT_LINKER_GUI, distBtnX, distBtnY, distTexX, distTexY, 9, 9);

        // Alphabet-Button
        int alphaBtnX = leftPos + 174;
        int alphaBtnY = topPos + 16;
        boolean hoveredAlpha = mouseX >= alphaBtnX && mouseX < alphaBtnX + 9 &&
                               mouseY >= alphaBtnY && mouseY < alphaBtnY + 9;

        int alphaTexX = alphabeticalSortEnabled ? (hoveredAlpha ? 202 : 178) : (hoveredAlpha ? 202 : 190);
        int alphaTexY = 43;

        guiGraphics.blit(VIADUCT_LINKER_GUI, alphaBtnX, alphaBtnY, alphaTexX, alphaTexY, 9, 9);

        // Custom Sort Button
        int customBtnX = leftPos + 174;
        int customBtnY = topPos + 28;
        boolean hoveredCustom = mouseX >= customBtnX && mouseX < customBtnX + 9 &&
                               mouseY >= customBtnY && mouseY < customBtnY + 9;

        int customTexX = customSortEnabled ? (hoveredCustom ? 202 : 178) : (hoveredCustom ? 202 : 190);
        int customTexY = 55;

        guiGraphics.blit(VIADUCT_LINKER_GUI, customBtnX, customBtnY, customTexX, customTexY, 9, 9);
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

        int btnSize = 9;
        int sortBtnX = leftPos + 175;
        int sortBtnY = topPos + 4;

        if (button == 0 &&
            mouseX >= sortBtnX && mouseX < sortBtnX + btnSize &&
            mouseY >= sortBtnY && mouseY < sortBtnY + btnSize) {

            customSortEnabled = false;
            currentSortMode = currentSortMode.toggle();
            updateLinkers(menu.getLinkers());
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
            return true;
        }

        int customBtnX = leftPos + 174;
        int customBtnY = topPos + 28;

        if (button == 0 &&
        	    mouseX >= customBtnX && mouseX < customBtnX + btnSize &&
        	    mouseY >= customBtnY && mouseY < customBtnY + btnSize) {

        	if (customSortEnabled) {
        	    // CustomSort aus
        	    customSortEnabled = false;

        	    System.out.println("[Screen] CustomSort deaktiviert, nutze gespeicherte Sortierung:");

        	    // NEU: Direkt beim Ausschalten CustomSort speichern
        	    if (!menu.getCustomSortedLinkers().isEmpty()) {
        	        List<BlockPos> sorted = menu.getCustomSortedLinkers().stream()
        	            .map(LinkedTargetEntryRecord::pos)
        	            .toList();
        	        menu.blockEntity.setSortedTargetPositions(sorted);
        	        System.out.println("[Screen] Direkt gespeichert beim Ausschalten: " + sorted);
        	    }

        	    updateLinkers(menu.getCustomSortedLinkers());
        	} else {
        	    	
        	        customSortEnabled = true;
        	        alphabeticalSortEnabled = false;
        	        currentSortMode = SortMode.DISTANCE_ASCENDING;

        	        List<BlockPos> savedOrder = menu.blockEntity.getSortedTargetPositions();
        	        System.out.println("[Screen] CustomSort aktiviert, geladene gespeicherte Reihenfolge: " + savedOrder);

        	        if (!savedOrder.isEmpty()) {
        	            List<LinkedTargetEntryRecord> sortedList = new ArrayList<>();
        	            List<LinkedTargetEntryRecord> unsorted = new ArrayList<>(menu.getLinkers());

        	            for (BlockPos pos : savedOrder) {
        	                for (Iterator<LinkedTargetEntryRecord> it = unsorted.iterator(); it.hasNext(); ) {
        	                    LinkedTargetEntryRecord entry = it.next();
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

        // Link-Button Klick (links)
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
            List<LinkedTargetEntryRecord> list = menu.getCustomSortedLinkers();
            if (draggedIndex >= 0 && draggedIndex < list.size()) {
                int relMouseY = (int)(mouseY - (topPos + 24));
                int btnHeight = 14;
                int spacing = btnHeight + 6;

                // maxScroll == 0 wenn weniger als maxVisibleButtons Einträge => kein Scroll
                int targetIndex;
                if (maxScroll > 0) {
                    targetIndex = scrollOffset + Math.floorDiv(relMouseY, spacing);
                } else {
                    targetIndex = Math.floorDiv(relMouseY, spacing);
                }

                targetIndex = Mth.clamp(targetIndex, 0, list.size() - 1);

                // Debugausgabe um zu prüfen, was berechnet wird:
                System.out.println("[Drag] draggedIndex: " + draggedIndex + ", targetIndex: " + targetIndex + ", relMouseY: " + relMouseY);

                if (targetIndex != draggedIndex) {
                    LinkedTargetEntryRecord draggedItem = list.remove(draggedIndex);
                    list.add(targetIndex, draggedItem);
                    draggedIndex = targetIndex;
                    menu.setCustomSortedLinkers(list);
                    menu.setLinkers(list);
                }
                return true;
            }
        }

        // Scrollbar etc. bleibt unverändert
        if (maxScroll <= 0) return false;

        if (isDraggingScrollbar) {
            // deine Scrollbar Logik hier
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

        // scrollY ist der vertikale Scrollbetrag (z.B. +1 oder -1)
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