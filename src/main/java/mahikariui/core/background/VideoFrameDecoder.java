package mahikariui.core.background;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import mahikariui.core.Constants;
import net.minecraft.client.texture.NativeImage;

/**
 * Decodes frames from an MP4 file as Minecraft NativeImage (NativeImage).
 * Loops to the start when end-of-stream is reached so videos play continuously.
 *
 * Performance notes (matters for weak machines):
 *  - Bulk-read RGB instead of per-pixel getRGB() — the latter is ~50–100× slower.
 *  - Cap frame size at MAX_W × MAX_H. Anything larger gets bilinearly downscaled
 *    before NativeImage allocation, cutting both decode-side memcpy and GPU upload.
 *  - Tracks average decode time so callers can fall back to a static frame on weak CPUs.
 *
 * Not thread-safe — caller (BackgroundBuilder's decode thread) drives nextFrame() serially.
 */
public class VideoFrameDecoder implements AutoCloseable {
    /** Max texture size we render at. 1280×720 is plenty for a fullscreen background. */
    private static final int MAX_W = 1280;
    private static final int MAX_H = 720;

    private final File source;
    private SeekableByteChannel channel;
    private FrameGrab grab;

    private long totalDecodeNanos;
    private long decodedFrames;

    public VideoFrameDecoder(File source) throws IOException, JCodecException {
        this.source = source;
        open();
    }

    private void open() throws IOException, JCodecException {
        this.channel = NIOUtils.readableChannel(source);
        this.grab = FrameGrab.createFrameGrab(channel);
    }

    /** Decode the next frame, looping to the beginning on EOF. Returns null only on hard error. */
    public NativeImage nextFrame() {
        long t0 = System.nanoTime();
        try {
            Picture pic = grab.getNativeFrame();
            if (pic == null) {
                reopen();
                pic = grab.getNativeFrame();
                if (pic == null) return null;
            }
            BufferedImage bimg = AWTUtil.toBufferedImage(pic);
            bimg = downscale(bimg, MAX_W, MAX_H);
            NativeImage img = toNativeImage(bimg);

            totalDecodeNanos += System.nanoTime() - t0;
            decodedFrames++;
            return img;
        } catch (Exception e) {
            Constants.LOG.error("VideoFrameDecoder failed on " + source.getName(), e);
            return null;
        }
    }

    /** Average wall-clock ms per decoded frame so far. Returns 0 before the first frame. */
    public double averageDecodeMillis() {
        if (decodedFrames == 0) return 0;
        return (totalDecodeNanos / (double) decodedFrames) / 1_000_000.0;
    }

    public long decodedFrameCount() {
        return decodedFrames;
    }

    private void reopen() throws IOException, JCodecException {
        closeChannel();
        open();
    }

    private void closeChannel() {
        if (channel != null) {
            try { channel.close(); } catch (IOException ignored) {}
            channel = null;
        }
    }

    @Override
    public void close() {
        closeChannel();
    }

    /** Decode just the first frame (used by ConfigScreen carousel previews). */
    public static NativeImage decodeFirstFrame(File source) {
        try (VideoFrameDecoder d = new VideoFrameDecoder(source)) {
            return d.nextFrame();
        } catch (Exception e) {
            Constants.LOG.error("Failed to decode first frame of " + source.getName(), e);
            return null;
        }
    }

    private static BufferedImage downscale(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxW && h <= maxH) return src;
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        int newW = Math.max(1, (int) (w * scale));
        int newH = Math.max(1, (int) (h * scale));
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    private static NativeImage toNativeImage(BufferedImage bimg) {
        int w = bimg.getWidth();
        int h = bimg.getHeight();
        // Bulk-read all pixels in one call — per-pixel getRGB(x,y) is ~50× slower for HD frames.
        int[] argb = new int[w * h];
        bimg.getRGB(0, 0, w, h, argb, 0, w);

        NativeImage img = new NativeImage(w, h, false);
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = argb[i++];
                int a = (p >>> 24) & 0xFF;
                int r = (p >>> 16) & 0xFF;
                int gC = (p >>> 8) & 0xFF;
                int b = p & 0xFF;
                // NativeImage expects ABGR
                img.setColor(x, y, (a << 24) | (b << 16) | (gC << 8) | r);
            }
        }
        return img;
    }
}
