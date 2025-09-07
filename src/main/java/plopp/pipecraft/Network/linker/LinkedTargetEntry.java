package plopp.pipecraft.Network.linker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class LinkedTargetEntry {
	 public final BlockPos pos;
	    public final String name;

	    public LinkedTargetEntry(BlockPos pos, String name) {
	        this.pos = pos;
	        this.name = name;
	    }

	    public CompoundTag toNBT() {
	        CompoundTag tag = new CompoundTag();
	        tag.putInt("x", pos.getX());
	        tag.putInt("y", pos.getY());
	        tag.putInt("z", pos.getZ());
	        tag.putString("name", name);
	        return tag;
	    }

	    public static LinkedTargetEntry fromNBT(CompoundTag tag) {
	        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
	        String name = tag.getString("name");
	        return new LinkedTargetEntry(pos, name);
	    }
	    public BlockPos getPos() {
	        return pos;
	    }
}
