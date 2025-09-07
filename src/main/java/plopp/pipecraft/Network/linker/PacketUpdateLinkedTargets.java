package plopp.pipecraft.Network.linker;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class PacketUpdateLinkedTargets {
	  private List<LinkedTargetEntry> linkedTargets;

	    public PacketUpdateLinkedTargets() {}

	    public PacketUpdateLinkedTargets(List<LinkedTargetEntry> linkedTargets) {
	        this.linkedTargets = linkedTargets;
	    }

	    public void encode(FriendlyByteBuf buf) {
	        buf.writeInt(linkedTargets.size());
	        for (LinkedTargetEntry entry : linkedTargets) {
	            buf.writeBlockPos(entry.pos);
	            buf.writeUtf(entry.name);
	        }
	    }
	    
	    public static PacketUpdateLinkedTargets decode(FriendlyByteBuf buf) {
	        int size = buf.readInt();
	        List<LinkedTargetEntry> list = new ArrayList<>(size);
	        for (int i = 0; i < size; i++) {
	            BlockPos pos = buf.readBlockPos();
	            String name = buf.readUtf(32767);
	            list.add(new LinkedTargetEntry(pos, name));
	        }
	        return new PacketUpdateLinkedTargets(list);
	    }

	    public List<LinkedTargetEntry> getLinkedTargets() {
	        return linkedTargets;
	    }
}
