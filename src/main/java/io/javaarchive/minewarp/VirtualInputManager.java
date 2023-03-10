package io.javaarchive.minewarp;

import io.javaarchive.minewarp.mixin.MouseAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.*;

import java.util.HashMap;
import java.util.Map;

public class VirtualInputManager {

    static int curx = 0,cury = 0;

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    // TODO: stop memory leak when windows are spam created by mod?
    public static Map<Long, GLFWKeyCallbackI> keyboardInputCallbacksByWindow = new HashMap<>();

    public static Map<Long, GLFWCharCallbackI> keyboardCharCallbacksByWindow = new HashMap<>();

    public static Map<Long, GLFWCharModsCallbackI> keyboardCharModCallbacksByWindow = new HashMap<>();

    public static Map<Long, GLFWCursorPosCallbackI> mouseMotionInputCallbacksByWindow = new HashMap<>();

    public static Map<Long, GLFWMouseButtonCallbackI> mouseButtonInputCallbacksByWindow = new HashMap<>();

    public static Map<Integer, Integer> virtualKeyState = new HashMap<>(); // key code to action

    public static void setMousePos(int x, int y){
        Window w = MinecraftClient.getInstance().getWindow();
        curx = (int) clamp(x,0,w.getWidth());
        cury = (int) clamp(y,0,w.getHeight());
        // sync(); // may not do this later
    }

    public static void relMoveMouse(int x, int y){
        curx += x;
        cury += y;
    }

    public static int getMouseX(){
        return curx;
    }

    public static int getMouseY(){
        return cury;
    }

    static boolean cursorLocked = false;

    public static void sync(){
        // Debug start
        // double[] x = {0,0};
        // double[] y = {0,0};
        // GLFW.glfwGetCursorPos(MinecraftClient.getInstance().getWindow().getHandle(), x,y);
        // System.out.println("CX " + x[0] + " CY: " + y[0] + " " + GameStreamSystem.INSTANCE.hasConnectedUser + " " + MinecraftClient.getInstance().mouse.isCursorLocked());
        // Debug end
        if(!GameStreamSystem.INSTANCE.hasConnectedUser) return;
        ((MouseAccessor) (Object) MinecraftClient.getInstance().mouse).setCursorLocked(cursorLocked);

        Window w = MinecraftClient.getInstance().getWindow();
        if(MinecraftClient.getInstance().currentScreen == null && MinecraftClient.getInstance().world != null){
            if(mouseMotionInputCallbacksByWindow.containsKey(w.getHandle())) {
                mouseMotionInputCallbacksByWindow.get(w.getHandle()).invoke(w.getHandle(), curx, cury);
            }
        }else {
            if(mouseMotionInputCallbacksByWindow.containsKey(w.getHandle())) {
                mouseMotionInputCallbacksByWindow.get(w.getHandle()).invoke(w.getHandle(), curx, cury);
            }
        }
    }

    public static int getVirtualKeyState(int keyCode){
        if(!VirtualInputManager.virtualKeyState.containsKey(keyCode)){
            return GLFW.GLFW_RELEASE;
        }
        return VirtualInputManager.virtualKeyState.get(keyCode);
    }

    public static void keyChange(int jsKey, int action, int mods){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window w = minecraftClient.getWindow();
        if(!keyboardInputCallbacksByWindow.containsKey(w.getHandle())) return;
        GLFWKeyCallbackI cb = keyboardInputCallbacksByWindow.get(w.getHandle());
        if(cb == null) return;
        int glfwKeyCode = KeyMapper.convertVKtoGLFW(jsKey);
        if(glfwKeyCode == KeyMapper.NOT_SUPPORTED_KEY){
            return;
        }
        virtualKeyState.put(glfwKeyCode, action);
        cb.invoke(w.getHandle(), glfwKeyCode,GLFW.glfwGetKeyScancode(glfwKeyCode), action, mods);
    }

    public static void charTyped(char c, int mods) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window w = minecraftClient.getWindow();
        if (keyboardCharCallbacksByWindow.containsKey(w.getHandle())) {
            GLFWCharCallbackI cb = keyboardCharCallbacksByWindow.get(w.getHandle());
            cb.invoke(w.getHandle(), (int) c);
        }
        if (keyboardCharModCallbacksByWindow.containsKey(w.getHandle())) {
            GLFWCharModsCallbackI cb = keyboardCharModCallbacksByWindow.get(w.getHandle());
            cb.invoke(w.getHandle(), (int) c, mods);
        }
    }

    /*public static int getMouseButtonFromJS(int mouseBtnJS){
        if(mouseBtnJS == 0) return GLFW.MOUSE_
    }*/

    public static void mouseButtonChange(int button, int action, int mods){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window w = minecraftClient.getWindow();

        if(!mouseButtonInputCallbacksByWindow.containsKey(w.getHandle())) return;

        GLFWMouseButtonCallbackI cb = mouseButtonInputCallbacksByWindow.get(w.getHandle());
        cb.invoke(w.getHandle(), button, action, mods);
    }
}
