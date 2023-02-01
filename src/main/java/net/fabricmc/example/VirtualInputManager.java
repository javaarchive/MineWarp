package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

public class VirtualInputManager {

    static int curx = 0,cury = 0;

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static void setPos(int x, int y){
        Window w = MinecraftClient.getInstance().getWindow();
        curx = (int) clamp(x,0,w.getWidth());
        cury = (int) clamp(y,0,w.getHeight());
    }

    public static void sync(){
        Window w = MinecraftClient.getInstance().getWindow();
        if(MinecraftClient.getInstance().currentScreen == null && MinecraftClient.getInstance().world != null){

        }else {
            GLFW.glfwSetCursorPos(w.getHandle(), curx, cury);
        }

    }
}
