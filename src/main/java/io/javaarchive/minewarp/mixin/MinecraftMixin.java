package io.javaarchive.minewarp.mixin;

import io.javaarchive.minewarp.GameStreamSystem;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftClient.class, priority = 5)
public abstract class MinecraftMixin {
    @Shadow private boolean windowFocused; // i love mc dev extension

    @Shadow public abstract void onWindowFocusChanged(boolean focused);

    @Inject(at = @At("RETURN"), method = "isWindowFocused", cancellable = true)
    private void fakeWindowFocused(CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValue()){
            // hmmm it got changed back to false back to true pls
            this.onWindowFocusChanged(true);
        }
        if(GameStreamSystem.INSTANCE.hasConnectedUser){
            this.windowFocused = true;
            cir.setReturnValue(true);
        }
    }
}
