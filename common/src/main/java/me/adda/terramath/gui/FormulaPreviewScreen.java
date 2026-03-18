package me.adda.terramath.gui;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shows a 3D voxel preview of the current formula.
 *
 * Uses an orthographic ray-DDA renderer:
 *   - 160×100 internal pixel buffer, displayed 2×2 per pixel (= 320×200 on screen)
 *   - Three-face voxel shading: top / left-side / right-side
 *   - Mouse drag to rotate (yaw + pitch)
 *   - Buffer is rebuilt only when rotation changes
 */
public class FormulaPreviewScreen extends Screen {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Internal (low-res) buffer size. Each pixel is displayed as 2×2 on screen. */
    private static final int BUF_W  = 160;
    private static final int BUF_H  = 100;

    /** On-screen canvas size. */
    private static final int CANVAS_W = BUF_W * 2; // 320
    private static final int CANVAS_H = BUF_H * 2; // 200

    /** World-space bounds sampled by the voxel grid. */
    private static final float WX_MIN = -100f, WX_MAX = 100f;
    private static final float WY_MIN =  -64f, WY_MAX = 192f;
    private static final float WZ_MIN = -100f, WZ_MAX = 100f;

    /** Voxel grid dimensions. */
    private static final int GX = 40, GY = 48, GZ = 40;

    /** DDA step limit: large enough to cross all outside-grid cells + grid interior. */
    private static final int MAX_STEPS = 350;

    /** World units per internal pixel (scale of the orthographic projection). */
    private static final double CAM_SCALE = 1.6;

    /** Distance of the camera origin behind the scene (for orthographic setup). */
    private static final double CAM_DIST = 450.0;

    // Face IDs returned by DDA
    private static final int FACE_NONE = 0;
    private static final int FACE_X    = 1;
    private static final int FACE_Y    = 2;
    private static final int FACE_Z    = 3;

    // Palette (ARGB)
    private static final int COL_SKY        = 0xFF222222;
    private static final int COL_GRASS_TOP  = 0xFF5A9A3A;
    private static final int COL_GRASS_SIDE_X = 0xFF4A8030;
    private static final int COL_GRASS_SIDE_Z = 0xFF3A6020;
    private static final int COL_STONE_TOP  = 0xFF888888;
    private static final int COL_STONE_X    = 0xFF666666;
    private static final int COL_STONE_Z    = 0xFF444444;
    private static final int COL_DEEP_TOP   = 0xFF555555;
    private static final int COL_DEEP_X     = 0xFF3A3A3A;
    private static final int COL_DEEP_Z     = 0xFF252525;

    private final Screen parent;
    private final String formulaText;

    private final byte[] voxels = new byte[GX * GY * GZ];

    private final int[] buffer = new int[BUF_W * BUF_H];

    private boolean dataReady    = false;
    private boolean bufferDirty  = true;
    private String  errorMessage = null;

    private float yaw   = 45f;
    private float pitch = 28f;

    private double lastMX, lastMY;
    private boolean dragging = false;

    private int canvasX, canvasY;

    public FormulaPreviewScreen(Screen parent, String formulaText) {
        super(Component.translatable("terramath.preview.title"));
        this.parent = parent;
        this.formulaText = formulaText.trim();
    }

    @Override
    protected void init() {
        super.init();
        canvasX = (width  - CANVAS_W) / 2;
        canvasY = (height - CANVAS_H) / 2 - 15;

        if (!dataReady && errorMessage == null) {
            computeVoxels();
        }

        addRenderableWidget(
                Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                      .pos(width / 2 - 50, canvasY + CANVAS_H + 8)
                      .size(100, 20)
                      .build()
        );
    }

    private void computeVoxels() {
        if (formulaText.isEmpty()) {
            errorMessage = Component.translatable("terramath.preview.empty").getString();
            return;
        }

        FormulaParser.CompiledFormula compiled;
        try {
            compiled = FormulaParser.parse(formulaText);
        } catch (Exception e) {
            errorMessage = Component.translatable("terramath.preview.error").getString();
            return;
        }

        TerrainSettingsManager settings = TerrainSettingsManager.getInstance();
        double coordScale   = settings.getCoordinateScale();
        boolean densityMode = settings.isUseDensityMode();
        boolean equationMode = formulaText.contains("=");

        double vsx = (WX_MAX - WX_MIN) / GX;
        double vsy = (WY_MAX - WY_MIN) / GY;
        double vsz = (WZ_MAX - WZ_MIN) / GZ;

        for (int ix = 0; ix < GX; ix++) {
            double fx = (WX_MIN + (ix + 0.5) * vsx) / coordScale;
            for (int iz = 0; iz < GZ; iz++) {
                double fz = (WZ_MIN + (iz + 0.5) * vsz) / coordScale;
                for (int iy = 0; iy < GY; iy++) {
                    double wy = WY_MIN + (iy + 0.5) * vsy;
                    double fy = wy / coordScale;

                    double density;
                    try {
                        double fv = compiled.evaluate(fx, fy, fz);
                        density = toDensity(fv, fy, coordScale, densityMode, equationMode, settings);
                    } catch (Exception e) {
                        density = -1.0;
                    }

                    voxels[ix * GY * GZ + iy * GZ + iz] = density > 0 ? (byte) 1 : (byte) 0;
                }
            }
        }

        dataReady   = true;
        bufferDirty = true;
    }

    private static double toDensity(double fv, double fy, double scale,
            boolean densityMode, boolean equationMode, TerrainSettingsManager s) {
        if (equationMode) {
            return fv;
        } else if (!densityMode) {
            return fy < 64.0 / scale + fv ? 1.0 : -1.0;
        } else {
            double tgt  = s.getBaseHeight() / scale + fv * s.getHeightVariation();
            double dist = Math.abs(fy - tgt);
            return fy < tgt
                    ? 1.0 - (dist / tgt) * s.getSmoothingFactor()
                    : -((dist / (s.getHeightVariation() * scale)) * s.getSmoothingFactor());
        }
    }

    private void rebuildBuffer() {
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);

        double ldx =  Math.sin(yr) * Math.cos(pr);
        double ldy = -Math.sin(pr);
        double ldz =  Math.cos(yr) * Math.cos(pr);

        double rdx =  Math.cos(yr);
        // rdy = 0
        double rdz = -Math.sin(yr);

        double udx =  Math.sin(yr) * Math.sin(pr);
        double udy =  Math.cos(pr);
        double udz =  Math.cos(yr) * Math.sin(pr);

        double cx = (WX_MIN + WX_MAX) * 0.5;
        double cy = (WY_MIN + WY_MAX) * 0.5;
        double cz = (WZ_MIN + WZ_MAX) * 0.5;

        double camX = cx - ldx * CAM_DIST;
        double camY = cy - ldy * CAM_DIST;
        double camZ = cz - ldz * CAM_DIST;

        double vsx = (WX_MAX - WX_MIN) / GX;
        double vsy = (WY_MAX - WY_MIN) / GY;
        double vsz = (WZ_MAX - WZ_MIN) / GZ;

        for (int py = 0; py < BUF_H; py++) {
            double sdY = -(py - BUF_H * 0.5) * CAM_SCALE;
            for (int px = 0; px < BUF_W; px++) {
                double sdX = (px - BUF_W * 0.5) * CAM_SCALE;

                double ox = camX + rdx * sdX + udx * sdY;
                double oy = camY +              udy * sdY;
                double oz = camZ + rdz * sdX + udz * sdY;

                buffer[py * BUF_W + px] = rayCast(ox, oy, oz, ldx, ldy, ldz, vsx, vsy, vsz);
            }
        }

        bufferDirty = false;
    }

    private int rayCast(double ox, double oy, double oz,
                        double dx, double dy, double dz,
                        double vsx, double vsy, double vsz) {

        double gx = (ox - WX_MIN) / vsx;
        double gy = (oy - WY_MIN) / vsy;
        double gz = (oz - WZ_MIN) / vsz;

        int ix = (int) Math.floor(gx);
        int iy = (int) Math.floor(gy);
        int iz = (int) Math.floor(gz);

        int stepX = dx >= 0 ? 1 : -1;
        int stepY = dy >= 0 ? 1 : -1;
        int stepZ = dz >= 0 ? 1 : -1;

        double gdx = dx / vsx;
        double gdy = dy / vsy;
        double gdz = dz / vsz;

        double tMaxX = stepX > 0 ? (Math.floor(gx) + 1.0 - gx) / Math.abs(gdx)
                                 : (gx - Math.floor(gx))         / Math.abs(gdx);
        double tMaxY = stepY > 0 ? (Math.floor(gy) + 1.0 - gy) / Math.abs(gdy)
                                 : (gy - Math.floor(gy))         / Math.abs(gdy);
        double tMaxZ = stepZ > 0 ? (Math.floor(gz) + 1.0 - gz) / Math.abs(gdz)
                                 : (gz - Math.floor(gz))         / Math.abs(gdz);

        double tDX = 1.0 / Math.abs(gdx);
        double tDY = 1.0 / Math.abs(gdy);
        double tDZ = 1.0 / Math.abs(gdz);

        int face = FACE_NONE;

        for (int s = 0; s < MAX_STEPS; s++) {
            if (ix >= 0 && ix < GX && iy >= 0 && iy < GY && iz >= 0 && iz < GZ) {
                if (voxels[ix * GY * GZ + iy * GZ + iz] != 0) {
                    return voxelColor(ix, iy, iz, face);
                }
            } else {
                // Outside grid — stop if definitely moving away in any axis
                if ((ix < 0 && stepX < 0) || (ix >= GX && stepX > 0)) break;
                if ((iy < 0 && stepY < 0) || (iy >= GY && stepY > 0)) break;
                if ((iz < 0 && stepZ < 0) || (iz >= GZ && stepZ > 0)) break;
            }

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                ix += stepX; tMaxX += tDX; face = FACE_X;
            } else if (tMaxY <= tMaxZ) {
                iy += stepY; tMaxY += tDY; face = FACE_Y;
            } else {
                iz += stepZ; tMaxZ += tDZ; face = FACE_Z;
            }
        }

        return COL_SKY;
    }

    private int voxelColor(int ix, int iy, int iz, int face) {
        double wy = WY_MIN + (iy + 0.5) * (WY_MAX - WY_MIN) / GY;
        boolean belowSea = wy < 63.0;

        // Top face is exposed if the voxel directly above is air (or out of bounds)
        boolean topOpen = (iy + 1 >= GY) || (voxels[ix * GY * GZ + (iy + 1) * GZ + iz] == 0);
        boolean isGrass = topOpen && !belowSea;

        if (isGrass) {
            return switch (face) {
                case FACE_Y -> COL_GRASS_TOP;
                case FACE_X -> COL_GRASS_SIDE_X;
                case FACE_Z -> COL_GRASS_SIDE_Z;
                default     -> COL_GRASS_TOP;
            };
        }

        if (belowSea) {
            return switch (face) {
                case FACE_Y -> COL_DEEP_TOP;
                case FACE_X -> COL_DEEP_X;
                case FACE_Z -> COL_DEEP_Z;
                default     -> COL_DEEP_TOP;
            };
        }

        return switch (face) {
            case FACE_Y -> COL_STONE_TOP;
            case FACE_X -> COL_STONE_X;
            case FACE_Z -> COL_STONE_Z;
            default     -> COL_STONE_TOP;
        };
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Title
        graphics.drawCenteredString(font,
                Component.translatable("terramath.preview.title"),
                width / 2, canvasY - 20, 0xFFFFFF);

        // Formula text (truncated)
        String display = formulaText.length() > 60
                ? formulaText.substring(0, 57) + "..." : formulaText;
        graphics.drawCenteredString(font, display, width / 2, canvasY - 10, 0xAAAAAA);

        if (!dataReady) {
            graphics.fill(canvasX, canvasY, canvasX + CANVAS_W, canvasY + CANVAS_H, 0xFF222222);
            String msg = errorMessage != null ? errorMessage : "...";
            graphics.drawCenteredString(font, msg,
                    canvasX + CANVAS_W / 2, canvasY + CANVAS_H / 2 - 4, 0xFF5555);
        } else {
            if (bufferDirty) rebuildBuffer();
            renderBuffer(graphics);
            graphics.drawString(font, "drag to rotate",
                    canvasX + 3, canvasY + CANVAS_H - 10, 0x88FFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBuffer(GuiGraphics graphics) {
        for (int py = 0; py < BUF_H; py++) {
            int y0 = canvasY + py * 2;
            int runStart = 0;
            int runColor = buffer[py * BUF_W];

            for (int px = 1; px <= BUF_W; px++) {
                int color = (px < BUF_W) ? buffer[py * BUF_W + px] : (runColor ^ 1); // sentinel
                if (color != runColor) {
                    graphics.fill(canvasX + runStart * 2, y0,
                                  canvasX + px * 2,       y0 + 2, runColor);
                    runStart = px;
                    runColor = color;
                }
            }
        }
        graphics.renderOutline(canvasX - 1, canvasY - 1, CANVAS_W + 2, CANVAS_H + 2, 0xFF444444);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && dataReady
                && mx >= canvasX && mx < canvasX + CANVAS_W
                && my >= canvasY && my < canvasY + CANVAS_H) {
            dragging = true;
            lastMX   = mx;
            lastMY   = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button,
                                double dragX, double dragY) {
        if (dragging && button == 0) {
            yaw   += (float) (mx - lastMX) * 0.5f;
            pitch  = (float) Math.max(-89, Math.min(89,
                     pitch + (my - lastMY) * 0.4));
            lastMX = mx;
            lastMY = my;
            bufferDirty = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
