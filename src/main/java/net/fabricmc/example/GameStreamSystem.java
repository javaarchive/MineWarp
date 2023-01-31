package net.fabricmc.example;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.freedesktop.gstreamer.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GameStreamSystem implements Pad.PROBE {

    int width, height;
    public static GameStreamSystem INSTANCE = new GameStreamSystem();

    public Pipeline pipeline;

    public volatile ByteBuffer lastRenderedFrame;

    public Framebuffer targetFb;
    boolean initalized = false;
    boolean initStart = false;
    public boolean checkInitalized(){
        return initalized;
    }

    public boolean checkInitStarted(){
        return initStart;
    }

    public void init(){
        initStart = true;

        // GStreamer native libs
        GStreamerExampleUtils.configurePaths();
        // Gst.init(Version.BASELINE, "MineWarp");

        Gst.init(Version.of(1, 16), "MineWarp");

        MinecraftClient mc = MinecraftClient.getInstance();

        targetFb = mc.getFramebuffer();

        Window w = mc.getWindow();

        width = w.getFramebufferWidth();
        height = w.getFramebufferHeight();

        System.out.println("Allocating frame for gamestream " + width + "x" + height);

        lastRenderedFrame = BufferUtils.createByteBuffer(w.getFramebufferWidth() * w.getFramebufferHeight() * 4);

        System.out.println("Creating pipeline");

        String caps = "video/x-raw, width=" + w.getFramebufferWidth() + ", height=" + w.getFramebufferHeight()
                + ", pixel-aspect-ratio=1/1, "
                + (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
                ? "format=BGRx" : "format=xRGB");
        pipeline = (Pipeline) Gst.parseLaunch("autovideosrc ! videoconvert ! videoscale ! "
                + caps + " ! identity name=identity ! videoflip method=vertical-flip ! videoconvert ! autovideosink");

        Element identity = pipeline.getElementByName("identity");
        identity.getStaticPad("sink").addProbe(PadProbeType.BUFFER, this);

        // at this point I have no idea what exactly happens
        System.out.println("Attaching to pipeline messages");
        pipeline.getBus().connect((Bus.ERROR) ((source, code, message) -> {
            System.out.println(message);
            Gst.quit();
        }));
        pipeline.getBus().connect((Bus.EOS) (source) -> Gst.quit());
        System.out.println("Playing pipeline");
        pipeline.play();
        System.out.println("Run GST Main");
        // pops up new window and waits so we run i nthread
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Gst.main();
            }
        });

        t.start();


        System.out.println("Initialize gamestream success");

        initalized = true;
    }

    public void capture(){
        targetFb.beginRead();
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, lastRenderedFrame);
        targetFb.endRead();
        lastRenderedFrame.rewind();
    }

    @Override
    public PadProbeReturn probeCallback(Pad pad, PadProbeInfo info) {
        Buffer gstBuffer = info.getBuffer();
        if(gstBuffer.isWritable()){
            ByteBuffer target = gstBuffer.map(true);
            target.rewind();
            target.put(lastRenderedFrame);
            lastRenderedFrame.rewind();
            gstBuffer.unmap();
        }
        return PadProbeReturn.OK;
    }
}
