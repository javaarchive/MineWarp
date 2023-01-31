package net.fabricmc.example.mixin;

import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.GameStreamSystem;
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
		 	GameStreamSystem.INSTANCE.init();
		}
	}
}
