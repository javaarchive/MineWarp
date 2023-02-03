package net.fabricmc.example.mixin;

import net.fabricmc.example.GameStreamSystem;
import net.fabricmc.example.VirtualInputManager;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(at = @At("HEAD"), method = "swapBuffers")
    private void onRenderFinish(CallbackInfo ci){
        if(GameStreamSystem.INSTANCE.checkInitalized()){
            // TODO: run at start of frame?
            VirtualInputManager.sync();
            GameStreamSystem.INSTANCE.capture();
        }
    }
}
