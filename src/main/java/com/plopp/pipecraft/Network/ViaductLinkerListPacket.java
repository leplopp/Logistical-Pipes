package com.plopp.pipecraft.Network;

import java.util.ArrayList;
import java.util.List;
import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.gui.viaductlinker.ViaductLinkerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ViaductLinkerListPacket implements CustomPacketPayload {
    private List<LinkedTargetEntryRecord> linkers;

    public static final CustomPacketPayload.Type<ViaductLinkerListPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct_linker_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ViaductLinkerListPacket> CODEC = new StreamCodec<>() {
    	@Override
    	public void encode(RegistryFriendlyByteBuf buf, ViaductLinkerListPacket pkt) {
    	    buf.writeInt(pkt.linkers.size());
    	    for (LinkedTargetEntryRecord entry : pkt.linkers) {
    	        buf.writeBlockPos(entry.pos());
    	        buf.writeUtf(entry.name());

    	        CompoundTag tag = new CompoundTag();
    	        var encodeResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, entry.icon());
    	        if (encodeResult.result().isPresent()) {
    	            tag = (CompoundTag) encodeResult.result().get();
    	        }
    	        buf.writeNbt(tag);
    	    }
    	}

    	@Override
    	public ViaductLinkerListPacket decode(RegistryFriendlyByteBuf buf) {
    	    int size = buf.readInt();
    	    List<LinkedTargetEntryRecord> linkers = new ArrayList<>();
    	    for (int i = 0; i < size; i++) {
    	        BlockPos pos = buf.readBlockPos();
    	        String name = buf.readUtf(32767);

    	        CompoundTag tag = buf.readNbt();
    	        ItemStack icon = ItemStack.EMPTY;
    	        if (tag != null) {
    	            var decodeResult = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag);
    	            if (decodeResult.result().isPresent()) {
    	                icon = decodeResult.result().get();
    	            }
    	        }
    	        linkers.add(new LinkedTargetEntryRecord(pos, name, icon));
    	    }
    	    return new ViaductLinkerListPacket(linkers);
    	}
    };

    public ViaductLinkerListPacket(List<LinkedTargetEntryRecord> linkers) {
        this.linkers = linkers;
    }

    public static void handle(ViaductLinkerListPacket packet, IPayloadContext context) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof ViaductLinkerScreen screen) {
                screen.getMenu().setLinkers(packet.linkers);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}