package net.fabricmc.example.mixin;

import net.fabricmc.example.VirtualInputManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.*;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.DoubleBuffer;

@Mixin(GLFW.class)
public class GLFWMixin {

    // please match signature exactly so nullable and more importantly NativeType annotations needed

    @Inject(at = @At("HEAD"), method = "glfwSetKeyCallback", remap = false)
    private static void onRegisterKeyboardHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWkeyfun") GLFWKeyCallbackI cbfun, CallbackInfoReturnable<GLFWKeyCallback> cir){
        VirtualInputManager.keyboardInputCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwSetCursorPosCallback", remap = false)
    private static void onRegisterMouseMotionHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWcursorposfun") GLFWCursorPosCallbackI cbfun, CallbackInfoReturnable<GLFWCursorPosCallback> cir){
        VirtualInputManager.mouseMotionInputCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwGetCursorPos(J[D[D)V", remap = false)
    private static void onGetCursorPosSimple(long window, double[] xpos, double[] ypos, CallbackInfo ci){
        xpos[0] = VirtualInputManager.getMouseX();
        ypos[0] = VirtualInputManager.getMouseY();
    }

    @Inject(at = @At("HEAD"), method = "glfwGetCursorPos(JLjava/nio/DoubleBuffer;Ljava/nio/DoubleBuffer;)V", remap = false)
    private static void onGetCursorPosSimple(long window, DoubleBuffer xpos, DoubleBuffer ypos, CallbackInfo ci){
        xpos.rewind();
        xpos.put((double) VirtualInputManager.getMouseX());
        ypos.rewind();
        ypos.put((double) VirtualInputManager.getMouseY());
    }
}
