package plopp.pipecraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import plopp.pipecraft.logic.Travel.ViaductTravel;

@Mixin(LivingEntity.class)
public abstract class MixinPlayerDimensions  {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object)this instanceof Player player)) return;

        if (ViaductTravel.isTravelActive(player)) {
            cir.setReturnValue(EntityDimensions.fixed(0.6F, 0.6F));
            return;
        }
    }
}