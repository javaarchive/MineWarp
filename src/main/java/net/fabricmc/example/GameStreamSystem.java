package net.fabricmc.example;


import com.google.gson.stream.MalformedJsonException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.util.Window;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GameStreamSystem implements Pad.PROBE {

    int width, height;
    public static GameStreamSystem INSTANCE = new GameStreamSystem();

    public Pipeline pipeline;

    public WebRTCBin webRTCBin;

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

    public volatile Socket socket = null;

    public String getConf(String key, String defaultValue){
        String val = System.getenv(key);
        if(val == null) return System.getProperty("mw.config." + key,defaultValue);
        return val;
    }

    public synchronized void handleMessage(JSONObject message, String origin){
        try {
            if (message.has("type")) {
                String type = message.getString("type");
                System.out.println("Recv message type "+ type + " T: " + Thread.currentThread().getId());
                if(type.equals("existence_check")){
                    System.out.println("Awking remote existence check. ");
                    JSONObject resp = new JSONObject();
                    resp.put("type", "existence_awk");
                    socket.emit("roomBroadcast", resp);
                    System.out.println("Create Offer on existence of peer. ");
                    webRTCBin.createOffer(onOfferCreated);
                }else if(type.equals("offer")){
                    this.processOffer(message);
                }else if(type.equals("offer_answer")){
                    this.processOfferAnswer(message);
                }else if(type.equals("ice")){
                    this.processIce(message);
                }else if(type.equals("start")){
                    this.start();
                }else if(type.equals("keyChange")){
                    this.processKeyChange(message);
                }else if(type.equals("mouseUpdate")){
                    this.processMouseInput(message);
                }
            }
        }catch(JSONException mje){
            mje.printStackTrace();
            System.out.println("RECV: Malformed wrapped message recieved. ");
        }
    }

    public void processMouseInput(JSONObject message) throws JSONException {
        if(message.has("locked") && message.getBoolean("locked")){

        }else {
            VirtualInputManager.setMousePos(message.getInt("x"), message.getInt("y"));
        }
    }

    public void processKeyChange(JSONObject message) throws JSONException {
        int action = message.getBoolean("isDown") ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
        if(message.has("repeat") && message.getBoolean("repeat")){
            action = GLFW.GLFW_REPEAT;
        }
        VirtualInputManager.keyChange(message.getInt("code"),action,0);
    }

    public void processOfferAnswer(JSONObject message) throws JSONException {
        JSONObject offer = message.getJSONObject("offer");
        String sdp = offer.getString("sdp");
        System.out.println("Got SDP answer \n " + sdp);
        SDPMessage sdpMsg = new SDPMessage();
        sdpMsg.parseBuffer(sdp);
        WebRTCSessionDescription desc = new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMsg);
        System.out.println("Setting remote description of peer (offer recv) ");
        webRTCBin.setRemoteDescription(desc);
    }

    public void processOffer(JSONObject message) throws JSONException {
        JSONObject offer = message.getJSONObject("offer");
        String sdp = offer.getString("sdp");
        System.out.println("Got SDP \n " + sdp);
        SDPMessage sdpMsg = new SDPMessage();
        sdpMsg.parseBuffer(sdp);
        WebRTCSessionDescription desc = new WebRTCSessionDescription(WebRTCSDPType.OFFER, sdpMsg);
        System.out.println("Setting remote description of peer (offer answer recv) ");
        webRTCBin.setRemoteDescription(desc);
        // Create offer's answer
        // System.out.println("Creating answer");
        // webRTCBin.createAnswer(this.onCreateAnswer);
    }

    public void processIce(JSONObject message) throws JSONException {
        try {
            JSONObject offer = message.getJSONObject("ice");
            String canidate = offer.getString("candidate");
            int index = offer.getInt("sdpMLineIndex");
            System.out.println("Got SDP \n " + canidate + " index " + index);
            webRTCBin.addIceCandidate(index, canidate);
        }catch(JSONException jie){
           System.out.println("Malformed ice canidate sent");
            jie.printStackTrace();
        }
    }

    public void initSocket(){
        try {
            GameStreamSystem gss = this;
            System.out.println("Initializing socket for remote " + Thread.currentThread().getId());
            String CONN_URL = "http://127.0.0.1:7331";
            System.out.println("Connecting to " + CONN_URL);
            this.socket = IO.socket(getConf("MW_REMOTE_URI", CONN_URL));
            this.socket.on("hello", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        if (args.length > 0) {
                            JSONObject hello = (JSONObject) args[0];
                            System.out.println("Socket registered with id " + hello.getString("id"));
                            socket.emit("setRoom", getConf("MW_ROOM", "defaultroom"));
                        } else {
                            System.out.println("RECV: Malformed room broadcast!");
                        }
                    }catch(JSONException mje){
                        System.out.println("RECV: Malformed server hello. ");
                        mje.printStackTrace();
                    }
                }
            });
            this.socket.on("roomRecv", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if(args.length > 0){
                        JSONObject messageInfo = (JSONObject) args[0];
                        try {
                            MinecraftClient.getInstance().executeSync(() -> {
                                try {
                                    gss.handleMessage(messageInfo.getJSONObject("message"), messageInfo.getString("origin"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    System.out.println("RECV (Inner) : Malformed message!");
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("RECV: Malformed message!");
                        }
                    }else{
                        System.out.println("RECV: Malformed room broadcast!");
                    }
                }
            });
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("Remote Socket (re)connected. ");
                }
            });
            socket.connect();
            System.out.println("Remote Socket Connection init mostly ok: " + socket.connected());
        }catch(URISyntaxException re){
            System.out.println("Invalid remote configuration specified. ");
            re.printStackTrace();
        }
    }

    public synchronized void init(){
        initStart = true;

        if(this.socket == null) this.initSocket();

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
                + caps + " ! identity name=identity ! videoflip method=vertical-flip ! videoconvert ! " +
                "queue ! vp9enc deadline=1 ! rtpvp9pay ! " +
                "webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302");

        pipeline.getElements().forEach(el -> System.out.println("Found pipeline el " + el.getName() + " " + el.getTypeName()));

        Element identity = pipeline.getElementByName("identity");
        identity.getStaticPad("sink").addProbe(PadProbeType.BUFFER, this);

        // at this point I have no idea what exactly happens
        System.out.println("Attaching to pipeline messages");
        pipeline.getBus().connect((Bus.ERROR) ((source, code, message) -> {
            System.out.println("ERROR: " + message);
            Gst.quit();
        }));

        pipeline.getBus().connect((source, old, current, pending) -> {
            if (source instanceof Pipeline) {
                System.out.println("Pipe state changed from " + old + " to " + current);
            }else{
                // System.out.println("NonPipe state changed from " + old + " to " + current);
            }
        });

        pipeline.getBus().connect((Bus.EOS) (source) -> Gst.quit());

        webRTCBin = (WebRTCBin) pipeline.getElementByName("webrtcbin");
        webRTCBin.connect(onNegotiationNeeded);
        webRTCBin.connect(onIceCandidate);

        System.out.println("Initialize gamestream success T: " + Thread.currentThread().getId());

        System.out.println("Run GST Main");
        // pops up new window and waits so we run i nthread
        /*Thread t = new Thread(new Runnable() {
            @Override

            public void run() {
                Gst.main();
            }
        });*/

        // t.start();

        // Gst.invokeLater(() -> Gst.main());

        System.out.println("Playing pipeline");
        pipeline.play();

        initalized = true;


    }

    private WebRTCBin.CREATE_ANSWER onCreateAnswer = answer -> {
        String sdp = answer.getSDPMessage().toString();
        System.out.println("Answer created, sending it over " + answer + " " + sdp);
        JSONObject obj = new JSONObject();

        webRTCBin.setLocalDescription(answer);

        try {
            obj.put("type", "offer_answer");
            obj.put("sdp", sdp);
            obj.put("origin", "server");
            obj.put("offer", sdp);
            this.socket.emit("roomBroadcast", obj);
        } catch (JSONException e) {
            System.out.println("Couldn't make offer json?");
            throw new RuntimeException(e);
        }
    };

    private WebRTCBin.CREATE_OFFER onOfferCreated = offer -> {
        String sdp = String.copyValueOf(offer.getSDPMessage().toString().toCharArray());
        System.out.println("Got local offer " + Thread.currentThread().getId() + " " + sdp);
        webRTCBin.setLocalDescription(offer);
        JSONObject obj = new JSONObject();

        try {
            obj.put("type", "offer");
            obj.put("sdp", sdp);
            obj.put("origin", "server");
            obj.put("offer", sdp);
            this.socket.emit("roomBroadcast", obj);
            System.out.println("Sent local offer to remotes");
        } catch (JSONException e) {
            System.out.println("Couldn't make offer json?");
            throw new RuntimeException(e);
        }
    };

    // https://github.com/gstreamer-java/gst1-java-examples/blob/master/WebRTCSendRecv/src/main/java/org/freedesktop/gstreamer/examples/WebRTCSendRecv.java
    private WebRTCBin.ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> {
        System.out.println("Negotiation starting " + Thread.currentThread().getId());
        // webRTCBin.createOffer(onOfferCreated);
    };

    private WebRTCBin.ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
        System.out.println("Got Ice Canidate from local");
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", "ice");
            obj.put("sdpMLineIndex", sdpMLineIndex);
            obj.put("candidate", candidate);
            obj.put("origin", "server");
            this.socket.emit("roomBroadcast", obj);
        } catch (JSONException e) {
            System.out.println("Couldn't make ice json?");
            throw new RuntimeException(e);
        }
    };

    public void debug(){
        // again ig
        webRTCBin.createOffer(onOfferCreated);
        System.out.println("WRTC " + webRTCBin.getConnectionState().intValue() + " " + webRTCBin.getStunServer() + " " + webRTCBin.getICEGatheringState());
    }

    public void start(){
        if(pipeline.isPlaying()) {
            debug();
            return;
        }

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
