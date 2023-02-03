package net.fabricmc.example.mixin;

import net.fabricmc.example.VirtualInputManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GLFW.class)
public class GLFWMixin {
    @Inject(at = @At("HEAD"), method = "glfwSetKeyCallback")
    private static void onRegisterKeyboardHandler(long window, GLFWKeyCallbackI cbfun, CallbackInfoReturnable<GLFWKeyCallback> cir){
        VirtualInputManager.keyboardInputCallbacksByWindow.put(window, cbfun);
    }
}
