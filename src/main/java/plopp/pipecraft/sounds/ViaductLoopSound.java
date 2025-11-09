package plopp.pipecraft.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import plopp.pipecraft.ClientConfig;
import plopp.pipecraft.Network.travel.TravelStatePacket;
import plopp.pipecraft.logic.Manager.ClientTravelDataManager;
import net.minecraft.util.Mth;

public class ViaductLoopSound extends AbstractTickableSoundInstance {

    private final Player player;
    private final float maxVolume;

    public ViaductLoopSound(Player player) {
        super(SoundRegister.VIADUCT_LOOP.value(), SoundSource.BLOCKS, RandomSource.create());
        this.player = player;
        this.looping = true;
        this.volume = 1f;
        this.maxVolume = ClientConfig.getViaductTravelVolume();
        this.pitch = 1.0f;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.attenuation = Attenuation.LINEAR; 
    }
    @Override
    public void tick() {
        if (!ClientTravelDataManager.isTravelActive(player.getUUID())) {
            this.volume -= 0.02f;
            if (this.volume <= 0f) this.stop();
            return;
        }

        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();

        TravelStatePacket data = ClientTravelDataManager.getTravelData(player.getUUID());
        if (data != null && data.getTicksPerChunk() > 0) {
            float speedFactor = data.getDefaultTicksPerChunk() / (float) data.getTicksPerChunk();
            float targetPitch = Mth.clamp(speedFactor, 0.3f, 2.0f);
            this.pitch += (targetPitch - this.pitch) * 0.1f;
        }
        
        Player local = Minecraft.getInstance().player;
        if (local != null) {
            double distance = player.distanceTo(local);
            this.volume = (float) (maxVolume * Math.max(0.0, 1.0 - distance / 32.0));
        }
    }
}