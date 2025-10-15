package plopp.pipecraft.Network.facade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Facade.BlockFacadeTileEntity;
import plopp.pipecraft.logic.FacadeOverlayManager;

public class FacadeOverlayPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FacadeOverlayPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "facade_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeOverlayPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, FacadeOverlayPacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeBoolean(pkt.remove);
            if (!pkt.remove) {
                buf.writeEnum(pkt.color);
                buf.writeBoolean(pkt.transparent);
            }
        }

        @Override
        public FacadeOverlayPacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            boolean remove = buf.readBoolean();
            if (remove) {
                return new FacadeOverlayPacket(pos, DyeColor.WHITE, false, true);
            } else {
                DyeColor color = buf.readEnum(DyeColor.class);
                boolean transparent = buf.readBoolean();
                return new FacadeOverlayPacket(pos, color, transparent, false);
            }
        }
    };

    private final BlockPos pos;
    private final DyeColor color;
    private final boolean transparent;
    private final boolean remove;

    public FacadeOverlayPacket(BlockPos pos, DyeColor color, boolean transparent, boolean remove) {
        this.pos = pos;
        this.color = color;
        this.transparent = transparent;
        this.remove = remove;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(FacadeOverlayPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (pkt.isRemove()) {
                FacadeOverlayManager.removeFacade(pkt.getPos());
                System.out.println("[FacadeOverlayPacket] Facade entfernt: " + pkt.getPos());
            } else {
                FacadeOverlayManager.addFacade(pkt.getPos(), pkt.getColor(), pkt.isTransparent());
                System.out.println("[FacadeOverlayPacket] Facade hinzugefügt: " + pkt.getPos() +
                                   " Farbe=" + pkt.getColor() + " transparent=" + pkt.isTransparent());
            }
        });
    }

    // Getter für pos, color, transparent, remove
    public BlockPos getPos() { return pos; }
    public DyeColor getColor() { return color; }
    public boolean isTransparent() { return transparent; }
    public boolean isRemove() { return remove; }
}