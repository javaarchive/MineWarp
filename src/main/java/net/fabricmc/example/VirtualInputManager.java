package net.fabricmc.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import java.util.HashMap;
import java.util.Map;

public class VirtualInputManager {

    static int curx = 0,cury = 0;

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    // TODO: stop memory leak when windows are spam created by mod?
    public static Map<Long, GLFWKeyCallbackI> keyboardInputCallbacksByWindow = new HashMap<>();

    public static Map<Long, GLFWCursorPosCallbackI> mouseMotionInputCallbacksByWindow = new HashMap<>();

    public static void setMousePos(int x, int y){
        Window w = MinecraftClient.getInstance().getWindow();
        curx = (int) clamp(x,0,w.getWidth());
        cury = (int) clamp(y,0,w.getHeight());
    }

    public static int getMouseX(){
        return curx;
    }

    public static int getMouseY(){
        return cury;
    }

    public static void sync(){
        Window w = MinecraftClient.getInstance().getWindow();
        if(MinecraftClient.getInstance().currentScreen == null && MinecraftClient.getInstance().world != null){
            
        }else {
            if(mouseMotionInputCallbacksByWindow.containsKey(w.getHandle())) {
                mouseMotionInputCallbacksByWindow.get(w.getHandle()).invoke(w.getHandle(), curx, cury);
            }
        }
    }

    public static void keyChange(int jsKey, int action, int mods){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window w = minecraftClient.getWindow();
        GLFWKeyCallbackI cb = keyboardInputCallbacksByWindow.get(w.getHandle());
        int glfwKeyCode = KeyMapper.convertVKtoGLFW(jsKey);
        if(glfwKeyCode == KeyMapper.NOT_SUPPORTED_KEY){
            return;
        }
        cb.invoke(w.getHandle(), glfwKeyCode,GLFW.glfwGetKeyScancode(glfwKeyCode), action, mods);
    }
}
