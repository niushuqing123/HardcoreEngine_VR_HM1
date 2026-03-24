package homework1;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class RasterCanvas extends JPanel {
    public static final int RENDER_W = 320;
    public static final int RENDER_H = 240;

    private final BufferedImage image;
    private final int[] pixels;
    private final float[] zBuffer;

    public RasterCanvas() {
        image = new BufferedImage(RENDER_W, RENDER_H, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        zBuffer = new float[pixels.length];
        setBackground(new java.awt.Color(0x1E1E1E));
    }

    public void clearBuffers(int hexColor) {
        Arrays.fill(pixels, hexColor);
        Arrays.fill(zBuffer, Float.MAX_VALUE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}
