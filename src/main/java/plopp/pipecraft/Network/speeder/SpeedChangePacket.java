package plopp.pipecraft.Network.speeder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.logic.SpeedLevel;
import plopp.pipecraft.logic.SpeedManager;

public class SpeedChangePacket implements CustomPacketPayload {
	  public static final CustomPacketPayload.Type<SpeedChangePacket> TYPE =
		        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "speed_change"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpeedChangePacket> CODEC = new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, SpeedChangePacket pkt) {
                buf.writeBlockPos(pkt.pos);
                buf.writeInt(pkt.delta);
            }

            @Override
            public SpeedChangePacket decode(RegistryFriendlyByteBuf buf) {
                return new SpeedChangePacket(buf.readBlockPos(), buf.readInt());
            }
        };

        private final BlockPos pos;
        private final int delta;

        public SpeedChangePacket(BlockPos pos, int delta) {
            this.pos = pos;
            this.delta = delta;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpeedChangePacket pkt, IPayloadContext context) {
            System.out.println("[SpeedChangePacket] Empfang auf Server für pos=" + pkt.pos + " delta=" + pkt.delta);

            if (!(context.player() instanceof ServerPlayer player)) {
                System.out.println("[SpeedChangePacket] Spieler ist kein ServerPlayer!");
                return;
            }

            ServerLevel level = player.serverLevel();

            context.enqueueWork(() -> {
                BlockState state = level.getBlockState(pkt.pos);
                BlockEntity be = level.getBlockEntity(pkt.pos);

                if (!(state.getBlock() instanceof BlockViaductSpeed) || !(be instanceof BlockEntityViaductSpeed speedBe)) {
                    System.out.println("[SpeedChangePacket] Kein SpeedBlock/BE an Pos " + pkt.pos);
                    return;
                }

                SpeedLevel current = state.getValue(BlockViaductSpeed.SPEED);
                int currentIndex = current.ordinal();
                int newIndex = Mth.clamp(currentIndex + pkt.delta, 0, SpeedLevel.values().length - 1);
                SpeedLevel newVal = SpeedLevel.values()[newIndex];

                ItemStack idStack = speedBe.getIdStack();

                if (!idStack.isEmpty()) {
                    // bestehende globale Update-Logik für Item-SpeedBlocks
                    SpeedManager.setSpeed(idStack, newVal);
                    for (BlockEntityViaductSpeed otherBe : BlockEntityViaductSpeed.getAll()) {
                        ItemStack otherStack = otherBe.getIdStack();
                        if (!otherStack.isEmpty() && otherStack.is(idStack.getItem())) {
                            BlockPos pos = otherBe.getBlockPos();
                            BlockState state1 = level.getBlockState(pos);
                            if (state1.getBlock() instanceof BlockViaductSpeed) {
                                level.setBlock(pos, state1.setValue(BlockViaductSpeed.SPEED, newVal), 3);
                                otherBe.setChanged();
                            }
                        }
                    }
                } else {
                    // Einzelblock: nur diesen SpeedBlock updaten
                    BlockState state1 = level.getBlockState(pkt.pos);
                    if (state1.getBlock() instanceof BlockViaductSpeed) {
                        level.setBlock(pkt.pos, state1.setValue(BlockViaductSpeed.SPEED, newVal), 3);
                        // Item entfernen, damit Renderer nichts mehr zeigt
                        speedBe.setIdStack(ItemStack.EMPTY);
                        speedBe.setChanged();
                        level.sendBlockUpdated(pkt.pos, state1, state1, 3);
                    }
                }

                System.out.println("[SpeedChangePacket] Alter Speed: " + current + " → Neuer Speed: " + newVal + " für idStack=" + idStack);
            });
        }
}