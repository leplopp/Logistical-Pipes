package plopp.pipecraft.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.logic.ViaductTravel;

@Mixin(LocalPlayer.class)
public abstract class MixinDisablePlayerInput extends AbstractClientPlayer {

    @Shadow @Nullable public Input input;

    private MixinDisablePlayerInput(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(MoverType type, Vec3 vec, CallbackInfo ci) {
        if (ViaductTravel.isTravelActive((Player) (Object) this)) {
            ci.cancel();
            ((Entity) (Object) this).setDeltaMovement(Vec3.ZERO);
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (ViaductTravel.isTravelActive((Player) (Object) this) && input != null) {
            input.shiftKeyDown = false;
            input.jumping = false; 
        }
    }
}