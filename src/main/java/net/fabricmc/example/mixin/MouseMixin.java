package net.fabricmc.example.mixin;

import net.fabricmc.example.GameStreamSystem;
import net.fabricmc.example.VirtualInputManager;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.Window;
import org.json.JSONException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(at = @At("HEAD"), method = "lockCursor", cancellable = true)
    private void onCursorLockRequest(CallbackInfo ci){
        if(GameStreamSystem.INSTANCE.hasConnectedUser){
            try{
                GameStreamSystem.INSTANCE.requestMouseLockStateChange(true);
            }catch(JSONException je){
                je.printStackTrace();
            }
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "unlockCursor", cancellable = true)
    private void onCursorUnlockRequest(CallbackInfo ci){
        if(GameStreamSystem.INSTANCE.hasConnectedUser){
            try{
                GameStreamSystem.INSTANCE.requestMouseLockStateChange(false);
            }catch(JSONException je){
                je.printStackTrace();
            }
            ci.cancel();
        }
    }
}
