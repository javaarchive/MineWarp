package io.javaarchive.minewarp.mixin;

import io.javaarchive.minewarp.GameStreamSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "init()V")
	private void init(CallbackInfo info) {
		if(!GameStreamSystem.INSTANCE.checkInitStarted()){
		 	MinecraftClient.getInstance().execute(() -> {
				 GameStreamSystem.INSTANCE.init();
				 // Disable raw input
				MinecraftClient.getInstance().options.getRawMouseInput().setValue(false);
			});
		}
	}
}
