package me.adda.terramath.gui;

import me.adda.terramath.TerraMath;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.gui.preview.GreedyMesher;
import me.adda.terramath.gui.preview.VoxelField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.adda.terramath.gui.preview.VoxelField.WY_MAX;
import static me.adda.terramath.gui.preview.VoxelField.WY_MIN;

public class PreviewPanel {
    private static final double CAM_DIST = 450.0;
    private static final double CAM_SCALE = 1;
    public static final int CTRL_H = 26;

    // One recompute at a time, the parallel work inside it runs on VoxelField's own pool
    private static final ExecutorService COMPUTE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TerraMath-Preview-Compute");
        thread.setDaemon(true);
        return thread;
    });

    private String formulaText = "";
    private TerrainSettingsManager displaySettings = null;

    private record Built(VoxelField field, GreedyMesher.Mesh mesh) {}

    private VoxelField field = null;
    private GreedyMesher.Mesh mesh = GreedyMesher.Mesh.EMPTY;

    private VoxelField.Params lastParams = null;
    private volatile boolean computing = false;
    private volatile Built pending = null;

    private float yaw = 45f;
    private float pitch = 28f;
    private double lastMX, lastMY;
    private boolean dragging = false;

    public int halfWidth = 100;
    public float zoom = 1.5f;

    public void setDisplaySettings(TerrainSettingsManager settings) {
        displaySettings = settings;
    }

    public void setFormula(String formula) {
        formulaText = formula == null ? "" : formula.trim();
    }

    public void render(GuiGraphics graphics, int x, int y, int w, int h, Font font) {
        int viewH = h - CTRL_H;

        Built ready = pending;
        if (ready != null) {
            pending = null;
            field = ready.field();
            mesh = ready.mesh();
        }

        if (!computing) {
            VoxelField.Params params = currentParams();
            if (!params.equals(lastParams))
                startCompute(params);
        }

        graphics.fill(x, y, x + w, y + viewH, 0xBF0F0F0F);

        if (field != null && field.error != null) {
            graphics.drawCenteredString(font, field.error,
                    x + w / 2, y + viewH / 2 - 4, 0xFFFF5555);
        } else if (field != null && field.gx > 0) {
            if (mesh.floatCount() > 0) renderMesh3D(graphics, x, y, w, viewH);
            drawOverlays(graphics, font, x, y, w, viewH);
        }

        renderControls(graphics, font, x, y + viewH, w);
        graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, 0x80606060);
    }

    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int button) {
        int viewH = h - CTRL_H;
        if (button == 0 && mx >= x && mx < x + w && my >= y + viewH && my < y + h)
            return handleControlClick((int) mx, (int) my, x, y + viewH, w);
        if (button == 0 && mx >= x && mx < x + w && my >= y && my < y + viewH) {
            dragging = true; lastMX = mx; lastMY = my;
            return true;
        }
        return false;
    }

    public void mouseReleased() { dragging = false; }

    public boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        yaw -= (float) (mx - lastMX) * 0.5f;
        pitch = (float) Math.max(-89, Math.min(89, pitch + (my - lastMY) * 0.4));
        lastMX = mx; lastMY = my;
        return true;
    }

    private VoxelField.Params currentParams() {
        TerrainSettingsManager src = displaySettings != null
                ? displaySettings : TerrainSettingsManager.getInstance();
        return VoxelField.Params.of(formulaText, src, halfWidth);
    }

    private void startCompute(VoxelField.Params params) {
        lastParams = params;
        computing = true;
        COMPUTE.submit(() -> {
            try {
                long start = System.nanoTime();
                VoxelField computed = VoxelField.compute(params);
                long meshStart = System.nanoTime();
                GreedyMesher.Mesh built = computed.error == null ? GreedyMesher.build(computed) : GreedyMesher.Mesh.EMPTY;
                long end = System.nanoTime();
                TerraMath.LOGGER.debug("Preview {}: eval {} ms, blur {} ms, classify {} ms, mesh {} ms, total {} ms",
                        params.halfWidth(), computed.evalNanos / 1_000_000, computed.blurNanos / 1_000_000,
                        computed.classifyNanos / 1_000_000, (end - meshStart) / 1_000_000, (end - start) / 1_000_000);
                pending = new Built(computed, built);
            } finally {
                computing = false;
            }
        });
    }

    private void renderMesh3D(GuiGraphics graphics, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int screenH = mc.getWindow().getHeight();

        int vpX = (int) (x * scale);
        int vpY = (int) (screenH - (y + h) * scale);
        int vpW = Math.max(1, (int) (w * scale));
        int vpH = Math.max(1, (int) (h * scale));

        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        float cy = (WY_MIN + WY_MAX) * 0.5f;
        float ldx = (float) (Math.sin(yr) * Math.cos(pr));
        float ldy = (float) (-Math.sin(pr));
        float ldz = (float) (Math.cos(yr) * Math.cos(pr));
        float camX = -ldx * (float) CAM_DIST;
        float camY = cy - ldy * (float) CAM_DIST;
        float camZ = -ldz * (float) CAM_DIST;
        float udx = (float) (Math.sin(yr) * Math.sin(pr));
        float udy = (float) Math.cos(pr);
        float udz = (float) (Math.cos(yr) * Math.sin(pr));

        double halfExtX = (w * 0.5) * (CAM_SCALE / zoom);
        double halfExtY = (h * 0.5) * (CAM_SCALE / zoom);

        Matrix4f proj = new Matrix4f().ortho(
                (float) -halfExtX, (float) halfExtX,
                (float) -halfExtY, (float) halfExtY,
                -5000f, 5000f);
        Matrix4f view = new Matrix4f().lookAt(camX, camY, camZ, 0f, cy, 0f, udx, udy, udz);
        Matrix4f projView = proj.mul(view, new Matrix4f());
        Matrix4f modelView = new Matrix4f().translate(0f, 0f, (float) CAM_DIST).mul(view);
        PreviewGl.drawMesh(graphics, new PreviewFrame(mesh.verts(), mesh.floatCount(), projView, modelView, zoom,
                x, y, w, h, vpX, vpY, vpW, vpH));
    }

    private void drawOverlays(GuiGraphics graphics, Font font, int x, int y, int w, int h) {
        if (computing)
            graphics.drawString(font, "/", x + w - 16, y + 3, 0x88FFFFFF, false);
        graphics.drawString(font, " drag to rotate", x + 3, y + h - 10, 0x88FFFFFF, false);
        String bounds = String.format("XZ:±%d  Y:%.0f-%.0f",
                field.halfWidth, (double) WY_MIN, (double) WY_MAX);
        graphics.drawString(font, bounds, x + 3, y + h - 20, 0x66FFFFFF, false);
    }

    private void renderControls(GuiGraphics graphics, Font font, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + CTRL_H, 0xBF0F0F0F);
        int half = w / 2;
        String zoomLabel = (zoom == Math.floor(zoom))
                ? (int)zoom + "x"
                : String.format("%.2fx", zoom).replaceAll("0+$","").replaceAll("\\.$","");
        renderControl(graphics, font, x, y, half, "Zoom", zoomLabel, zoom>0.5f, zoom<7.0f);
        renderControl(graphics, font, x + half, y, w - half, "Width", halfWidth+" blk", halfWidth>50, halfWidth<200);
    }

    private void renderControl(GuiGraphics graphics, Font font,
                               int x, int y, int w, String label, String value,
                               boolean canDec, boolean canInc) {
        int cy = y + CTRL_H/2 - 4, cx = x + w/2;
        graphics.drawString(font, "◀", x + 3, cy, canDec ? 0xFFCCCCCC : 0xFF555555, false);
        graphics.drawCenteredString(font, label+": "+value, cx, cy, 0xFFFFFFFF);
        graphics.drawString(font, "▶", x + w - font.width("▶") - 3, cy,
                canInc ? 0xFFCCCCCC : 0xFF555555, false);
    }

    private boolean handleControlClick(int mx, int my, int x, int y, int w) {
        int half = w / 2;
        if (mx < x + half) {
            int cx = x + half / 2;
            if (mx < cx-10 && zoom>0.5f) zoom = Math.round((zoom-0.25f)*100)/100f;
            else if (mx > cx+10 && zoom<7.0f) zoom = Math.round((zoom+0.25f)*100)/100f;
        } else {
            int cx = x + half + (w-half)/2;
            if (mx < cx-10 && halfWidth>50) { halfWidth -= 50; }
            else if (mx > cx+10 && halfWidth<200) { halfWidth += 50; }
        }
        return true;
    }
}
