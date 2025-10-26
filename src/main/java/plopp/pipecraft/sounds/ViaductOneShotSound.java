package plopp.pipecraft.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public class ViaductOneShotSound extends AbstractTickableSoundInstance {
    private final Player player;
    private final float maxVolume;
    private boolean fadingOut = false; 
    private float fadeVolume = 1f;  

    public ViaductOneShotSound(Player player, SoundEvent sound, float volume) {
        super(sound, SoundSource.BLOCKS, RandomSource.create());
        this.player = player;
        this.looping = false;
        this.volume = volume;
        this.maxVolume = volume;
        this.pitch = 1f;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.attenuation = Attenuation.LINEAR;
    }

    public void startFadeOut() {
        this.fadingOut = true;
    }

    @Override
    public void tick() {
        Player local = Minecraft.getInstance().player;
        if (local != null) {
            double distance = player.distanceTo(local);
            float distanceFactor = (float)Math.max(0, 1 - distance / 32.0);
            if (fadingOut) {
                fadeVolume -= 0.02f;
                if (fadeVolume <= 0f) {
                    this.stop();
                    return;
                }
            }
            this.volume = maxVolume * distanceFactor * fadeVolume;
        }

        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();

        if (!Minecraft.getInstance().getSoundManager().isActive(this) && !fadingOut) {
            this.stop();
        }
    }
}