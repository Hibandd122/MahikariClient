package mahikariui.core.background;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.client.gui.screen.TitleScreen;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.Constants;
import mahikariui.core.config.Config;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class BackgroundBuilder extends TitleScreen {
    private static Background currentBackground;
    private static int currentFrame;
    private static long lastFrameTime;
    
    private static final Identifier DYNAMIC_TEXTURE_ID = Identifier.of("mahikariui", "dynamic_bg_texture");
    private static ExecutorService decodeExecutor;
    private static LinkedBlockingQueue<NativeImage> decodedQueue = new LinkedBlockingQueue<>(2);
    private static boolean isStreaming = false;
    private static VideoFrameDecoder videoDecoder;

    public static void selectBackground(@NotNull String background) {
        stopStreaming();
        Identifier[] frames = new Identifier[0];
        if (background.isEmpty()) {
            background = "default";
        }
        if (background.equalsIgnoreCase("none")) {
            currentBackground = new Background.DefaultBackground() {
                @Override
                public String getName() {
                    return "none";
                }
            };
        } else {
            java.io.File videoFile = Background.findVideoFile(background);
            if (videoFile != null) {
                frames = Background.loadAnimatedFramePaths(background);
                currentBackground = new Background.VideoBackground(background, frames, videoFile);
            } else {
                currentBackground = (frames = Background.loadAnimatedFramePaths(background)) == null || frames.length == 0
                    ? new Background.DefaultBackground()
                    : (frames.length == 1 ? new Background(background, frames) : new Background.AnimatedBackground(background, frames));
            }
        }

        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();

        if (currentBackground.isAnimated() && !Config.getInstance().isLowQualityMode()) {
            if (currentBackground instanceof Background.VideoBackground vb) {
                startVideoStreaming(vb.getVideoFile());
            } else {
                startStreaming(frames);
            }
        }
    }

    private static void startStreaming(Identifier[] frames) {
        if (frames == null || frames.length <= 1) return;
        isStreaming = true;
        decodeExecutor = Executors.newSingleThreadExecutor();
        decodedQueue = new LinkedBlockingQueue<>(15); // Buffer up to 15 frames in RAM to prevent stuttering
        
        decodeExecutor.submit(() -> {
            int decodeIndex = 0;
            while (isStreaming && !Thread.currentThread().isInterrupted()) {
                try {
                    NativeImage img = Background.decodeFrame(frames[decodeIndex]);
                    
                    // Put blocks automatically if the queue hits 15 frames!
                    decodedQueue.put(img);
                    
                    decodeIndex = (decodeIndex + 1) % frames.length;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (java.nio.channels.ClosedByInterruptException e) {
                    // Normal interruption when stream stops, ignore silently.
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (!e.toString().contains("ClosedByInterruptException")) {
                        Constants.LOG.error("Failed to decode frame async: " + frames[decodeIndex], e);
                    }
                    try { Thread.sleep(50); } catch (Exception ignored) {} // Prevent error spam
                }
            }
        });
    }
    
    /** Threshold above which we consider the CPU too slow for live video and fall back to static. */
    private static final double SLOW_DECODE_MS_BUDGET = 80.0; // 80ms ≈ 12fps — anything worse and the menu feels laggy
    private static final int SLOW_DECODE_SAMPLE_AFTER = 20;   // wait this many frames before judging
    private static volatile boolean videoFallbackToStatic = false;

    private static void startVideoStreaming(java.io.File videoFile) {
        isStreaming = true;
        videoFallbackToStatic = false;
        // 3-frame buffer: ~10MB RAM at 720p vs ~120MB at 15 frames. Decode thread blocks on put()
        // when the menu isn't actively pulling frames, so CPU also drops to ~0 when idle.
        decodedQueue = new LinkedBlockingQueue<>(3);
        decodeExecutor = Executors.newSingleThreadExecutor();

        decodeExecutor.submit(() -> {
            try {
                videoDecoder = new VideoFrameDecoder(videoFile);
            } catch (Exception e) {
                Constants.LOG.error("Failed to open video background: " + videoFile.getName(), e);
                return;
            }
            while (isStreaming && !Thread.currentThread().isInterrupted()) {
                try {
                    NativeImage img = videoDecoder.nextFrame();
                    if (img == null) {
                        Thread.sleep(50);
                        continue;
                    }
                    decodedQueue.put(img); // blocks once buffer holds 3 frames → idle CPU when off-screen

                    // After a small sample window, judge whether this machine can actually keep up.
                    // If not, freeze on the latest frame and stop the decode loop — better than a stuttery menu.
                    if (videoDecoder.decodedFrameCount() == SLOW_DECODE_SAMPLE_AFTER
                            && videoDecoder.averageDecodeMillis() > SLOW_DECODE_MS_BUDGET) {
                        Constants.LOG.warn("Video background decode averaging {} ms/frame on this machine — falling back to static frame to protect FPS.",
                                String.format("%.1f", videoDecoder.averageDecodeMillis()));
                        videoFallbackToStatic = true;
                        isStreaming = false;
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Constants.LOG.error("Video decode loop error", e);
                    try { Thread.sleep(50); } catch (Exception ignored) {}
                }
            }
        });
    }

    private static void stopStreaming() {
        isStreaming = false;
        if (decodeExecutor != null) {
            decodeExecutor.shutdownNow();
            decodeExecutor = null;
        }
        if (videoDecoder != null) {
            try { videoDecoder.close(); } catch (Exception ignored) {}
            videoDecoder = null;
        }
        if (decodedQueue != null) {
            NativeImage img;
            while ((img = decodedQueue.poll()) != null) {
                img.close();
            }
        }
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
            // MinecraftClient.getInstance().getTextureManager().method_4618(DYNAMIC_TEXTURE_ID);
        }
    }

    public static Background getBackground() {
        if (currentBackground == null) {
            currentBackground = new Background.DefaultBackground();
        }
        if (currentBackground.isAnimated()) {
            BackgroundBuilder.updateFrame();
        }
        return currentBackground;
    }

    public static String getBackgroundPath() {
        if (currentBackground == null) {
            return "background/default";
        }
        return "background/" + currentBackground.getName().toLowerCase();
    }

    public static Identifier getCurrentFrame() {
        if (currentBackground == null) {
            currentBackground = new Background.DefaultBackground();
        }
        if (currentBackground.getName().equalsIgnoreCase("none")) {
            return null;
        }
        Identifier[] frames = currentBackground.getFramePaths();
        if (frames == null || frames.length == 0) {
            return new Background.DefaultBackground().getFramePaths()[0];
        }
        boolean videoStatic = (currentBackground instanceof Background.VideoBackground)
                && (Config.getInstance().isLowQualityMode() || videoFallbackToStatic);
        if (videoStatic) {
            // Low-quality OR auto-fallback after slow decode: pin the first frame as a static texture.
            ensureVideoPreviewRegistered((Background.VideoBackground) currentBackground);
            return frames[0];
        }
        if (Config.getInstance().isLowQualityMode()) {
            return frames[0];
        }
        if (currentBackground.isAnimated()) {
            BackgroundBuilder.updateFrame();
            return DYNAMIC_TEXTURE_ID;
        }
        return frames[0];
    }

    private static final java.util.Set<String> videoPreviewsRegistered = new java.util.HashSet<>();

    private static void ensureVideoPreviewRegistered(Background.VideoBackground vb) {
        String key = vb.getName();
        if (videoPreviewsRegistered.contains(key)) return;
        NativeImage img = VideoFrameDecoder.decodeFirstFrame(vb.getVideoFile());
        if (img == null) return;
        Identifier id = vb.getFramePaths()[0];
        net.minecraft.client.texture.TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        net.minecraft.client.texture.AbstractTexture old = textureManager.getTexture(id);
        if (old != null) old.close();
        textureManager.registerTexture(id, new NativeImageBackedTexture(() -> "mahikariui_video_static", img));
        videoPreviewsRegistered.add(key);
    }

    public static Identifier[] getAllFrames() {
        if (currentBackground == null) {
            currentBackground = new Background.DefaultBackground();
        }
        return currentBackground.getFramePaths();
    }

    public static String getBackgroundName() {
        if (currentBackground == null) {
            return "default";
        }
        return currentBackground.getName();
    }

    public static boolean isAnimated() {
        return currentBackground != null && currentBackground.isAnimated();
    }

    public static int getCurrentFrameIndex() {
        return currentFrame;
    }

    public static int getFrameCount() {
        if (currentBackground == null) {
            return 1;
        }
        Identifier[] frames = currentBackground.getFramePaths();
        return frames != null ? frames.length : 1;
    }

    private static void updateFrame() {
        int targetFps = Config.getInstance().getBackgroundFrame();
        if (Config.getInstance().isLowQualityMode()) {
            targetFps = Math.min(10, targetFps);
        }
        int frameDelay = 1000 / targetFps;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFrameTime >= frameDelay) {
            if (isStreaming) {
                NativeImage nextImg = decodedQueue.poll();
                if (nextImg != null) {
                    Identifier[] frames = currentBackground.getFramePaths();
                    currentFrame = (currentFrame + 1) % frames.length;
                    
                    net.minecraft.client.texture.TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
                    net.minecraft.client.texture.AbstractTexture oldTex = textureManager.getTexture(DYNAMIC_TEXTURE_ID);
                    
                    // REVERTED GPU OPTIMIZATION: Recreating the texture is required to prevent OpenGL border bleeding (GL_CLAMP_TO_EDGE reset bug)
                    if (oldTex != null) {
                        oldTex.close();
                    }
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "mahikariui_dynamic_bg", nextImg);
                    textureManager.registerTexture(DYNAMIC_TEXTURE_ID, (AbstractTexture)tex);
                    
                    lastFrameTime = currentTime;
                }
            }
        }
    }

    public static void reset() {
        stopStreaming();
        currentBackground = new Background.DefaultBackground();
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public static void forceNextFrame() {
        if (currentBackground != null && currentBackground.isAnimated()) {
            Identifier[] frames = currentBackground.getFramePaths();
            currentFrame = (currentFrame + 1) % frames.length;
            lastFrameTime = System.currentTimeMillis();
        }
    }

    public void removed() {
        super.removed();
    }

    public static void shutdown() {
        stopStreaming();
    }

    static {
        currentFrame = 0;
        lastFrameTime = 0L;
        String configBg = Config.getInstance().getBackground();
        BackgroundBuilder.selectBackground(configBg != null ? configBg : "default");
    }
}
