package io.javaarchive.minewarp.mixin;

import io.javaarchive.minewarp.GameStreamSystem;
import io.javaarchive.minewarp.VirtualInputManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.*;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.DoubleBuffer;

@Mixin(value = GLFW.class, priority = 5)
public class GLFWMixin {

    // please match signature exactly so nullable and more importantly NativeType annotations needed

    @Inject(at = @At("HEAD"), method = "glfwSetKeyCallback", remap = false)
    private static void onRegisterKeyboardHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWkeyfun") GLFWKeyCallbackI cbfun, CallbackInfoReturnable<GLFWKeyCallback> cir){
        VirtualInputManager.keyboardInputCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwSetCharCallback", remap = false)
    private static void onRegisterCharacterHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWcharfun") GLFWCharCallbackI cbfun, CallbackInfoReturnable<GLFWCharCallback> cir){
        VirtualInputManager.keyboardCharCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwSetCharModsCallback", remap = false)
    private static void onRegisterCharacterHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWcharmodsfun") GLFWCharModsCallbackI cbfun, CallbackInfoReturnable<GLFWCharModsCallback> cir){
        VirtualInputManager.keyboardCharModCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwSetCursorPosCallback", remap = false)
    private static void onRegisterMouseMotionHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWcursorposfun") GLFWCursorPosCallbackI cbfun, CallbackInfoReturnable<GLFWCursorPosCallback> cir){
        VirtualInputManager.mouseMotionInputCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("HEAD"), method = "glfwSetMouseButtonCallback", remap = false)
    private static void onRegisterMouseMotionHandler(@NativeType("GLFWwindow *") long window, @Nullable @NativeType("GLFWmousebuttonfun") GLFWMouseButtonCallbackI cbfun, CallbackInfoReturnable<GLFWMouseButtonCallback> cir){
        VirtualInputManager.mouseButtonInputCallbacksByWindow.put(window, cbfun);
    }

    @Inject(at = @At("TAIL"), method = "glfwGetCursorPos(J[D[D)V", remap = false)
    private static void onGetCursorPosSimple(long window, double[] xpos, double[] ypos, CallbackInfo ci){
        if(!GameStreamSystem.INSTANCE.hasConnectedUser) return; // no modify!
        xpos[0] = VirtualInputManager.getMouseX();
        ypos[0] = VirtualInputManager.getMouseY();
    }

    @Inject(at = @At("TAIL"), method = "glfwGetCursorPos(JLjava/nio/DoubleBuffer;Ljava/nio/DoubleBuffer;)V", remap = false)
    private static void onGetCursorPosSimple(long window, DoubleBuffer xpos, DoubleBuffer ypos, CallbackInfo ci){
        if(!GameStreamSystem.INSTANCE.hasConnectedUser) return; // no modify
        xpos.rewind();
        xpos.put((double) VirtualInputManager.getMouseX());
        ypos.rewind();
        ypos.put((double) VirtualInputManager.getMouseY());
    }

    @Inject(at = @At("TAIL"), method = "glfwGetKey", remap = false, cancellable = true)
    private static void onGetCursorPosSimple(long window, int key, CallbackInfoReturnable<Integer> cir){
        if(cir.getReturnValue() == GLFW.GLFW_RELEASE){
            cir.setReturnValue(VirtualInputManager.getVirtualKeyState(key));
        }
    }
}
