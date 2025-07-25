package plopp.pipecraft.Network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.logic.SpeedLevel;

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
                if (!(state.getBlock() instanceof BlockViaductSpeed)) {
                    System.out.println("[SpeedChangePacket] Kein SpeedBlock an Pos " + pkt.pos);
                    return;
                }

                SpeedLevel current = state.getValue(BlockViaductSpeed.SPEED);
                System.out.println("[SpeedChangePacket] Alter Speed: " + current);

                int currentIndex = current.ordinal();
                int newIndex = Mth.clamp(currentIndex + pkt.delta, 0, SpeedLevel.values().length - 1);
                SpeedLevel newVal = SpeedLevel.values()[newIndex];

                level.setBlock(pkt.pos, state.setValue(BlockViaductSpeed.SPEED, newVal), 3);

                System.out.println("[SpeedChangePacket] Neuer Speed: " + newVal);
            });
        }
    }