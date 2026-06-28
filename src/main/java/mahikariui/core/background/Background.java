/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.resource.ResourceManager
 */
package mahikariui.core.background;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import mahikariui.core.Constants;

public class Background {
    private final String name;
    private final Identifier[] framePaths;
    private static final Pattern FRAME_NUMBER_PATTERN = Pattern.compile("(\\d+)");

    protected Background(String name, Identifier[] framePaths) {
        this.name = name;
        this.framePaths = framePaths;
    }

    public String getName() {
        return this.name;
    }

    public Identifier[] getFramePaths() {
        return this.framePaths;
    }

    public boolean isAnimated() {
        return this.framePaths.length > 1;
    }

    public static Identifier[] createFramePaths(String basePath, int frameCount) {
        Identifier[] paths = new Identifier[frameCount];
        for (int i = 0; i < frameCount; ++i) {
            String path = basePath + (i + 1) + ".png";
            paths[i] = Identifier.of((String)"mahikariui", (String)path);
        }
        return paths;
    }

    public static String[] getAvailableBackgrounds() {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        HashSet<String> backgrounds = new HashSet<>();
        resourceManager.findResources("background", location -> {
            String path = location.getPath().toLowerCase();
            return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
        }).forEach((location, resource) -> {
            String path = location.getPath();
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                String bgName = parts[1].toLowerCase();
                if (bgName.endsWith(".png") || bgName.endsWith(".jpg") || bgName.endsWith(".jpeg")) {
                    bgName = bgName.substring(0, bgName.lastIndexOf('.'));
                }
                backgrounds.add(bgName);
            }
        });
        
        java.io.File bgDir = new java.io.File(Constants.backgroundDir);
        if (!bgDir.exists()) bgDir.mkdirs();
        java.io.File[] extFiles = bgDir.listFiles();
        if (extFiles != null) {
            for (java.io.File f : extFiles) {
                String name = f.getName().toLowerCase();
                if (f.isDirectory()) {
                    backgrounds.add(name);
                } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".mp4")) {
                    backgrounds.add(name.substring(0, name.lastIndexOf('.')));
                }
            }
        }
        
        return backgrounds.toArray(new String[0]);
    }

    private static int extractFrameNumber(String filename) {
        Matcher matcher = FRAME_NUMBER_PATTERN.matcher(filename);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            }
            catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static java.io.InputStream getFrameStream(Identifier id) throws Exception {
        String path = id.getPath();
        if (path.startsWith("external_")) {
            return new java.io.FileInputStream(new java.io.File(Constants.backgroundDir, path.substring(9)));
        }
        return ((net.minecraft.resource.Resource)MinecraftClient.getInstance().getResourceManager().getResource(id).orElseThrow()).getInputStream();
    }

    // ULTRA-FAST PIXEL INJECTION FOR JPG DECODING
    public static net.minecraft.client.texture.NativeImage decodeFrame(Identifier id) throws Exception {
        String path = id.getPath().toLowerCase();
        java.io.InputStream rawStream = getFrameStream(id);
        
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(rawStream);
            rawStream.close();
            
            // Allocate NativeImage in GPU RAM
            net.minecraft.client.texture.NativeImage nativeImg = new net.minecraft.client.texture.NativeImage(bimg.getWidth(), bimg.getHeight(), false);
            
            // Ultra-fast ABGR pixel injection
            for (int y = 0; y < bimg.getHeight(); y++) {
                for (int x = 0; x < bimg.getWidth(); x++) {
                    int argb = bimg.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImg.setColor(x, y, abgr);
                }
            }
            return nativeImg;
        } else {
            net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(rawStream);
            rawStream.close();
            return img;
        }
    }

    private static Identifier[] registerPreview(Identifier[] frames) {
        if (frames == null || frames.length == 0) return frames;
        Identifier firstFrame = frames[0];
        String path = firstFrame.getPath().toLowerCase();
        
        // Prevent Minecraft's internal renderer from crashing when drawing JPG previews
        if (!path.startsWith("external_") && !path.endsWith(".jpg") && !path.endsWith(".jpeg")) return frames;
        
        net.minecraft.client.texture.TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        try {
            net.minecraft.client.texture.NativeImage img = decodeFrame(firstFrame);
            textureManager.registerTexture(firstFrame, new net.minecraft.client.texture.NativeImageBackedTexture(() -> "mahikariui_bg_preview", img));
        } catch (Exception e) {
            Constants.LOG.error("Failed to load texture preview: " + path, e);
        }
        return frames;
    }

    /** Returns the .mp4 file in the backgrounds dir for the given name, or null if not present. */
    public static java.io.File findVideoFile(String backgroundName) {
        if (backgroundName == null || backgroundName.isEmpty()) return null;
        String name = backgroundName.toLowerCase();
        if (name.endsWith(".mp4")) name = name.substring(0, name.length() - 4);
        java.io.File f = new java.io.File(Constants.backgroundDir, name + ".mp4");
        return f.isFile() ? f : null;
    }

    public static Identifier[] loadAnimatedFramePaths(String backgroundName) {
        if (backgroundName == null || backgroundName.isEmpty()) {
            Constants.LOG.warn("Background name is null or empty. Falling back to default background.", new Object[0]);
            return new DefaultBackground().getFramePaths();
        }
        backgroundName = backgroundName.toLowerCase();
        if (backgroundName.endsWith(".png") || backgroundName.endsWith(".jpg") || backgroundName.endsWith(".jpeg") || backgroundName.endsWith(".mp4")) {
            backgroundName = backgroundName.substring(0, backgroundName.lastIndexOf('.'));
        }

        // Video backgrounds: return a single sentinel frame path. BackgroundBuilder swaps in a
        // VideoFrameDecoder when it sees this; the path itself is just a placeholder.
        java.io.File videoFile = findVideoFile(backgroundName);
        if (videoFile != null) {
            Identifier marker = Identifier.of((String) "mahikariui", (String) ("video_" + backgroundName + ".mp4"));
            return new Identifier[] { marker };
        }

        ArrayList<Identifier> frameList = new ArrayList<Identifier>();

        java.io.File bgDir = new java.io.File(Constants.backgroundDir);
        java.io.File extFolder = new java.io.File(bgDir, backgroundName);
        if (extFolder.exists() && extFolder.isDirectory()) {
            java.io.File[] frames = extFolder.listFiles();
            if (frames != null) {
                for (java.io.File f : frames) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        frameList.add(Identifier.of((String)"mahikariui", (String)("external_" + backgroundName + "/" + f.getName())));
                    }
                }
                frameList.sort(Comparator.comparingInt(location -> Background.extractFrameNumber(location.getPath())));
                if (!frameList.isEmpty()) return registerPreview(frameList.toArray(new Identifier[0]));
            }
        }
        
        java.io.File[] staticExts = {new java.io.File(bgDir, backgroundName + ".png"), new java.io.File(bgDir, backgroundName + ".jpg"), new java.io.File(bgDir, backgroundName + ".jpeg")};
        for (java.io.File extFile : staticExts) {
            if (extFile.exists() && !extFile.isDirectory()) {
                frameList.add(Identifier.of((String)"mahikariui", (String)("external_" + extFile.getName())));
                return registerPreview(frameList.toArray(new Identifier[0]));
            }
        }

        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        String folder = "background/" + backgroundName.toLowerCase();
        resourceManager.findResources("background", location -> {
            String path = location.getPath().toLowerCase();
            return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
        }).forEach((location, resource) -> {
            String path = location.getPath().toLowerCase();
            if (path.startsWith(folder + "/") || path.equals(folder + ".png") || path.equals(folder + ".jpg") || path.equals(folder + ".jpeg")) {
                frameList.add((Identifier)location);
            }
        });
        if (frameList.isEmpty()) {
            Constants.LOG.warn("No frames found in '%1$s'. Falling back to default background.", folder);
            return new DefaultBackground().getFramePaths();
        }
        frameList.sort(Comparator.comparingInt(location -> {
            String filename = location.getPath().substring(location.getPath().lastIndexOf(47) + 1);
            return Background.extractFrameNumber(filename);
        }));
        return registerPreview(frameList.toArray(new Identifier[0]));
    }

    public static class DefaultBackground
    extends Background {
        public DefaultBackground() {
            super("DEFAULT", DefaultBackground.createFramePaths("background/default/default", 1));
        }
    }

    public static class AnimatedBackground
    extends Background {
        public AnimatedBackground(String name, Identifier[] framePaths) {
            super(name, framePaths);
        }
    }

    /**
     * Video background backed by an MP4 file in the backgrounds dir.
     * Its framePaths array holds a single sentinel; actual frames are streamed by VideoFrameDecoder.
     */
    public static class VideoBackground
    extends Background {
        private final java.io.File videoFile;

        public VideoBackground(String name, Identifier[] framePaths, java.io.File videoFile) {
            super(name, framePaths);
            this.videoFile = videoFile;
        }

        public java.io.File getVideoFile() {
            return this.videoFile;
        }

        @Override
        public boolean isAnimated() {
            return true;
        }
    }
}

