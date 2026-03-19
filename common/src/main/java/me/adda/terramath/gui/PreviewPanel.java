package me.adda.terramath.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.adda.terramath.api.TerrainSettingsManager;
import org.joml.Matrix4fStack;
import me.adda.terramath.config.NoiseType;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PreviewPanel {
    private static final int    VOXEL_SIZE_XZ = 2;
    private static final int    GY            = 192;
    private static final float  WY_MIN        = -64f;
    private static final float  WY_MAX        = 320f;
    private static final double CAM_DIST      = 450.0;
    private static final double CAM_SCALE     = 1;
    public  static final int    CTRL_H        = 26;

    private static final ExecutorService VOXEL_POOL =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Предвычисленные цвета граней [тип][face]
    private static final int[] COLOR_GRASS = {
            0xFF5A9A3A, 0xFF3A6020, 0xFF4A8030, 0xFF4A8030, 0xFF3A6020, 0xFF3A6020
    };
    private static final int[] COLOR_WATER = {
            0xFF555555, 0xFF252525, 0xFF3A3A3A, 0xFF3A3A3A, 0xFF252525, 0xFF252525
    };
    private static final int[] COLOR_STONE = {
            0xFF888888, 0xFF444444, 0xFF666666, 0xFF666666, 0xFF444444, 0xFF444444
    };

    // State

    private String                 formulaText     = "";
    private TerrainSettingsManager displaySettings = null;

    // Voxels хранятся как bitset: 1 bit на воксель -> в 8x меньше памяти и bandwidth
    private long[] voxelBits   = new long[0]; // ceil(gx*GY*gz / 64) longs
    private int    voxelGX     = 0, voxelGZ = 0;
    private int    renderHalfWidth = 200;
    private String errorMessage    = null;
    private boolean voxelsDirty   = true;

    private volatile boolean computing        = false;
    private volatile long[]  pendingBits      = null;
    private volatile String  pendingError     = null;
    private volatile int     pendingHalfWidth = 200;
    private volatile int     pendingGX = 0, pendingGZ = 0;

    // Mesh: вершины треугольников (6 float на вершину: xyz + rgb packed)
    // Формат: x, y, z, r, g, b, a - 7 float/vert, 6 vert/quad (2 треугольника)
    private float[] meshVerts     = new float[1 << 18];
    private int     meshVertCount = 0; // число float-значений (не вершин)
    private boolean meshDirty     = true;

    private float   yaw     = 45f;
    private float   pitch   = 28f;
    private double  lastMX, lastMY;
    private boolean dragging = false;

    public int   halfWidth = 100;
    public float zoom      = 1.5f;

    private double    snapCoordScale      = Double.NaN;
    private boolean   snapDensityMode     = false;
    private double    snapBaseHeight      = Double.NaN;
    private double    snapHeightVariation = Double.NaN;
    private double    snapSmoothing       = Double.NaN;
    private NoiseType snapNoiseType       = null;
    private double    snapNoiseScaleX = Double.NaN, snapNoiseScaleY = Double.NaN;
    private double    snapNoiseScaleZ = Double.NaN, snapNoiseHeightScale = Double.NaN;
    private int       snapHalfWidth       = -1;

    /** Размер bitset в long[] для n бит */
    private static int bitsetSize(int n) { return (n + 63) >>> 6; }

    /** Установить бит [ix][iy][iz] в bitset */
    private static void bitSet(long[] bits, int sX, int sY, int ix, int iy, int iz) {
        int idx = ix * sX + iy * sY + iz;
        bits[idx >>> 6] |= (1L << (idx & 63));
    }

    /** Проверить бит [ix][iy][iz] */
    private static boolean bitGet(long[] bits, int sX, int sY, int ix, int iy, int iz) {
        int idx = ix * sX + iy * sY + iz;
        return (bits[idx >>> 6] & (1L << (idx & 63))) != 0;
    }

    /** Проверить бит по линейному индексу */
    private static boolean bitGetFlat(long[] bits, int idx) {
        return (bits[idx >>> 6] & (1L << (idx & 63))) != 0;
    }

    public void setDisplaySettings(TerrainSettingsManager settings) {
        displaySettings = settings;
    }

    public void setFormula(String formula) {
        String trimmed = formula == null ? "" : formula.trim();
        if (!trimmed.equals(formulaText)) {
            formulaText = trimmed;
            voxelsDirty = true;
            meshDirty   = true;
        }
    }

    public void render(GuiGraphics graphics, int x, int y, int w, int h, Font font) {
        int viewH = h - CTRL_H;

        if (pendingBits != null && !computing) {
            voxelBits       = pendingBits;
            voxelGX         = pendingGX;
            voxelGZ         = pendingGZ;
            errorMessage    = pendingError;
            renderHalfWidth = pendingHalfWidth;
            pendingBits     = null;
            pendingError    = null;
            meshDirty       = true;
        }

        if (!voxelsDirty && !computing && displaySettings != null && settingsChanged())
            voxelsDirty = true;
        if (!voxelsDirty && !computing && halfWidth != snapHalfWidth)
            voxelsDirty = true;
        if (voxelsDirty && !computing)
            startComputeAsync();

        graphics.fill(x, y, x + w, y + viewH, 0xBF0F0F0F);

        if (errorMessage != null) {
            graphics.drawCenteredString(font, errorMessage,
                    x + w / 2, y + viewH / 2 - 4, 0xFF5555);
        } else if (voxelGX > 0) {
            if (meshDirty) rebuildMesh();
            if (meshVertCount > 0) renderMesh3D(graphics, x, y, w, viewH);
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
        yaw   -= (float) (mx - lastMX) * 0.5f;
        pitch  = (float) Math.max(-89, Math.min(89, pitch + (my - lastMY) * 0.4));
        lastMX = mx; lastMY = my;
        return true;
    }

    private void renderMesh3D(GuiGraphics graphics, int x, int y, int w, int h) {
        Minecraft mc      = Minecraft.getInstance();
        double    scale   = mc.getWindow().getGuiScale();
        int       screenH = mc.getWindow().getHeight();

        int vpX = (int) (x * scale);
        int vpY = (int) (screenH - (y + h) * scale);
        int vpW = Math.max(1, (int) (w * scale));
        int vpH = Math.max(1, (int) (h * scale));

        double yr  = Math.toRadians(yaw);
        double pr  = Math.toRadians(pitch);
        float  cy  = (WY_MIN + WY_MAX) * 0.5f;
        float  ldx = (float) (Math.sin(yr) * Math.cos(pr));
        float  ldy = (float) (-Math.sin(pr));
        float  ldz = (float) (Math.cos(yr) * Math.cos(pr));
        float  camX = -ldx * (float) CAM_DIST;
        float  camY =  cy  - ldy * (float) CAM_DIST;
        float  camZ = -ldz * (float) CAM_DIST;
        float  udx  = (float) (Math.sin(yr) * Math.sin(pr));
        float  udy  = (float)  Math.cos(pr);
        float  udz  = (float) (Math.cos(yr) * Math.sin(pr));

        double halfExtX = (w * 0.5) * (CAM_SCALE / zoom);
        double halfExtY = (h * 0.5) * (CAM_SCALE / zoom);

        Matrix4f proj     = new Matrix4f().ortho(
                (float) -halfExtX, (float) halfExtX,
                (float) -halfExtY, (float) halfExtY,
                -5000f, 5000f);
        Matrix4f view     = new Matrix4f().lookAt(camX, camY, camZ, 0f, cy, 0f, udx, udy, udz);
        Matrix4f projView = proj.mul(view, new Matrix4f());

        Matrix4f      savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack mvs       = RenderSystem.getModelViewStack();

        try {
            RenderSystem.viewport(vpX, vpY, vpW, vpH);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableCull();

            RenderSystem.setProjectionMatrix(projView, RenderSystem.getVertexSorting());
            mvs.pushMatrix();
            mvs.identity();
            RenderSystem.applyModelViewMatrix();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            Tesselator    tess = Tesselator.getInstance();
            BufferBuilder bb   = tess.getBuilder();

            bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            float[] v     = meshVerts;
            int     total = meshVertCount; // уже в float, шаг 7
            for (int i = 0; i < total; i += 7) {
                bb.vertex(v[i], v[i+1], v[i+2])
                        .color((int)v[i+3], (int)v[i+4], (int)v[i+5], (int)v[i+6])
                        .endVertex();
            }
            tess.end();

        } finally {
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProj, RenderSystem.getVertexSorting());
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
    }

    // Greedy Meshing
    //
    // Алгоритм (классический greedy mesh по Mikola Lysenko):
    //   Для каждой из 6 осей/направлений:
    //     Для каждого слоя вдоль этой оси:
    //       Строим 2D маску открытых граней (face mask)
    //       Жадно объединяем прямоугольники в маске -> 1 quad
    //
    // Вместо N^3 квадов получаем O(поверхность) квадов.
    // На плоском рельефе это буквально 1 quad на весь слой.

    private void rebuildMesh() {
        long[] bits = voxelBits;
        int gx = voxelGX, gz = voxelGZ;

        if (bits.length == 0 || gx == 0 || gz == 0) {
            meshVertCount = 0;
            meshDirty     = false;
            return;
        }

        double hw  = renderHalfWidth;
        float  vsx = (float) ((hw * 2.0) / gx);
        float  vsy = (WY_MAX - WY_MIN) / GY;
        float  vsz = (float) ((hw * 2.0) / gz);
        int    sX  = GY * gz;
        int    sY  = gz;

        // Начинаем с разумного буфера; рост по необходимости
        if (meshVerts.length < 1 << 18) meshVerts = new float[1 << 18];
        int vertFloats = 0; // курсор в meshVerts (в float)

        // Временная маска для greedy: int чтобы хранить packed color-type
        // 0 = пусто, иначе индекс цветовой схемы (1=grass,2=water,3=stone)
        int[] mask = new int[Math.max(gx, GY) * Math.max(GY, gz)];

        // 6 направлений
        // dir: 0=+Y, 1=-Y, 2=+X, 3=-X, 4=+Z, 5=-Z
        for (int dir = 0; dir < 6; dir++) {

            // u,v - два измерения плоскости грани; n - нормаль
            // Для каждого dir определяем: (uAxis, vAxis, nAxis, nPos)
            // uDim,vDim - размеры маски; nDim - сколько слоёв
            int uDim, vDim, nDim;
            switch (dir) {
                case 0: case 1: uDim = gx;  vDim = gz;  nDim = GY; break;  // ±Y
                case 2: case 3: uDim = GY;  vDim = gz;  nDim = gx; break;  // ±X
                default:        uDim = gx;  vDim = GY;  nDim = gz; break;  // ±Z
            }

            for (int layer = 0; layer < nDim; layer++) {

                // Заполняем маску для этого слоя
                Arrays.fill(mask, 0, uDim * vDim, 0);

                for (int u = 0; u < uDim; u++) {
                    for (int v = 0; v < vDim; v++) {

                        // Координаты текущего и соседнего вокселя
                        int ix, iy, iz, nx, ny, nz;
                        switch (dir) {
                            case 0: ix = u;     iy = layer; iz = v; nx = u;     ny = layer+1; nz = v; break;
                            case 1: ix = u;     iy = layer; iz = v; nx = u;     ny = layer-1; nz = v; break;
                            case 2: ix = layer; iy = u;     iz = v; nx = layer+1; ny = u;   nz = v; break;
                            case 3: ix = layer; iy = u;     iz = v; nx = layer-1; ny = u;   nz = v; break;
                            case 4: ix = u;     iy = v;     iz = layer; nx = u; ny = v;   nz = layer+1; break;
                            default:ix = u;     iy = v;     iz = layer; nx = u; ny = v;   nz = layer-1; break;
                        }

                        boolean solid = ix>=0 && ix<gx && iy>=0 && iy<GY && iz>=0 && iz<gz
                                && bitGet(bits, sX, sY, ix, iy, iz);
                        boolean neighborSolid = nx>=0 && nx<gx && ny>=0 && ny<GY && nz>=0 && nz<gz
                                && bitGet(bits, sX, sY, nx, ny, nz);

                        if (solid && !neighborSolid) {
                            // Открытая грань - определяем тип цвета
                            float wyMid;
                            switch (dir) {
                                case 0: case 1: wyMid = WY_MIN + (layer + 0.5f) * vsy; break;
                                case 2: case 3: wyMid = WY_MIN + (u     + 0.5f) * vsy; break;
                                default:        wyMid = WY_MIN + (v     + 0.5f) * vsy; break;
                            }
                            boolean belowSea = wyMid < 63.0f;
                            // isGrass: грань dir=0 (+Y, верхняя) и не под водой
                            boolean isGrass  = (dir == 0) && !belowSea;
                            mask[u * vDim + v] = isGrass ? 1 : (belowSea ? 2 : 3);
                        }
                    }
                }

                boolean[] used = new boolean[uDim * vDim];
                for (int u = 0; u < uDim; u++) {
                    for (int v = 0; v < vDim; v++) {
                        int mIdx = u * vDim + v;
                        int colorType = mask[mIdx];
                        if (colorType == 0 || used[mIdx]) continue;

                        int w2 = 1;
                        while (v + w2 < vDim && mask[u * vDim + v + w2] == colorType
                                && !used[u * vDim + v + w2]) w2++;

                        int h2 = 1;
                        outer:
                        while (u + h2 < uDim) {
                            for (int dv = 0; dv < w2; dv++) {
                                int mi = (u + h2) * vDim + v + dv;
                                if (mask[mi] != colorType || used[mi]) break outer;
                            }
                            h2++;
                        }

                        for (int du = 0; du < h2; du++)
                            for (int dv = 0; dv < w2; dv++)
                                used[(u + du) * vDim + v + dv] = true;

                        int[] colors = colorType == 1 ? COLOR_GRASS
                                : colorType == 2 ? COLOR_WATER : COLOR_STONE;
                        int argb = colors[dir];

                        float q0x, q0y, q0z, q1x, q1y, q1z, q2x, q2y, q2z, q3x, q3y, q3z;
                        switch (dir) {
                            case 0: { // +Y
                                float y1 = WY_MIN + (layer + 1) * vsy;
                                q0x=(float)(-hw+u    *vsx); q0y=y1; q0z=(float)(-hw+(v+w2)*vsz);
                                q1x=(float)(-hw+(u+h2)*vsx); q1y=y1; q1z=(float)(-hw+(v+w2)*vsz);
                                q2x=(float)(-hw+(u+h2)*vsx); q2y=y1; q2z=(float)(-hw+v    *vsz);
                                q3x=(float)(-hw+u    *vsx); q3y=y1; q3z=(float)(-hw+v    *vsz);
                                break;
                            }
                            case 1: {
                                float y0 = WY_MIN + layer * vsy;
                                q0x=(float)(-hw+(u+h2)*vsx); q0y=y0; q0z=(float)(-hw+(v+w2)*vsz);
                                q1x=(float)(-hw+u    *vsx); q1y=y0; q1z=(float)(-hw+(v+w2)*vsz);
                                q2x=(float)(-hw+u    *vsx); q2y=y0; q2z=(float)(-hw+v    *vsz);
                                q3x=(float)(-hw+(u+h2)*vsx); q3y=y0; q3z=(float)(-hw+v    *vsz);
                                break;
                            }
                            case 2: { // +X
                                float x1 = (float)(-hw+(layer+1)*vsx);
                                q0x=x1; q0y=WY_MIN+u    *vsy; q0z=(float)(-hw+v    *vsz);
                                q1x=x1; q1y=WY_MIN+(u+h2)*vsy; q1z=(float)(-hw+v    *vsz);
                                q2x=x1; q2y=WY_MIN+(u+h2)*vsy; q2z=(float)(-hw+(v+w2)*vsz);
                                q3x=x1; q3y=WY_MIN+u    *vsy; q3z=(float)(-hw+(v+w2)*vsz);
                                break;
                            }
                            case 3: { // -X
                                float x0 = (float)(-hw+layer*vsx);
                                q0x=x0; q0y=WY_MIN+(u+h2)*vsy; q0z=(float)(-hw+v    *vsz);
                                q1x=x0; q1y=WY_MIN+u    *vsy; q1z=(float)(-hw+v    *vsz);
                                q2x=x0; q2y=WY_MIN+u    *vsy; q2z=(float)(-hw+(v+w2)*vsz);
                                q3x=x0; q3y=WY_MIN+(u+h2)*vsy; q3z=(float)(-hw+(v+w2)*vsz);
                                break;
                            }
                            case 4: { // +Z
                                float z1 = (float)(-hw+(layer+1)*vsz);
                                q0x=(float)(-hw+(u+h2)*vsx); q0y=WY_MIN+v    *vsy; q0z=z1;
                                q1x=(float)(-hw+(u+h2)*vsx); q1y=WY_MIN+(v+w2)*vsy; q1z=z1;
                                q2x=(float)(-hw+u    *vsx); q2y=WY_MIN+(v+w2)*vsy; q2z=z1;
                                q3x=(float)(-hw+u    *vsx); q3y=WY_MIN+v    *vsy; q3z=z1;
                                break;
                            }
                            default: { // -Z
                                float z0 = (float)(-hw+layer*vsz);
                                q0x=(float)(-hw+u    *vsx); q0y=WY_MIN+v    *vsy; q0z=z0;
                                q1x=(float)(-hw+u    *vsx); q1y=WY_MIN+(v+w2)*vsy; q1z=z0;
                                q2x=(float)(-hw+(u+h2)*vsx); q2y=WY_MIN+(v+w2)*vsy; q2z=z0;
                                q3x=(float)(-hw+(u+h2)*vsx); q3y=WY_MIN+v    *vsy; q3z=z0;
                                break;
                            }
                        }

                        float r = (argb >> 16) & 0xFF;
                        float g = (argb >>  8) & 0xFF;
                        float b =  argb        & 0xFF;
                        float a = (argb >>> 24) & 0xFF;

                        // Quad → 2 треугольника (6 вершин × 7 float = 42 float)
                        if (vertFloats + 42 > meshVerts.length) {
                            meshVerts = Arrays.copyOf(meshVerts,
                                    Math.max(meshVerts.length * 2, vertFloats + 42));
                        }
                        float[] vt = meshVerts;
                        int o = vertFloats;
                        // tri 0: q0,q1,q2
                        vt[o   ]=q0x;vt[o+1]=q0y;vt[o+2]=q0z;vt[o+3]=r;vt[o+4]=g;vt[o+5]=b;vt[o+6]=a;
                        vt[o+7 ]=q1x;vt[o+8]=q1y;vt[o+9]=q1z;vt[o+10]=r;vt[o+11]=g;vt[o+12]=b;vt[o+13]=a;
                        vt[o+14]=q2x;vt[o+15]=q2y;vt[o+16]=q2z;vt[o+17]=r;vt[o+18]=g;vt[o+19]=b;vt[o+20]=a;
                        // tri 1: q0,q2,q3
                        vt[o+21]=q0x;vt[o+22]=q0y;vt[o+23]=q0z;vt[o+24]=r;vt[o+25]=g;vt[o+26]=b;vt[o+27]=a;
                        vt[o+28]=q2x;vt[o+29]=q2y;vt[o+30]=q2z;vt[o+31]=r;vt[o+32]=g;vt[o+33]=b;vt[o+34]=a;
                        vt[o+35]=q3x;vt[o+36]=q3y;vt[o+37]=q3z;vt[o+38]=r;vt[o+39]=g;vt[o+40]=b;vt[o+41]=a;
                        vertFloats += 42;
                    }
                }
            }
        }

        meshVertCount = vertFloats;
        meshDirty     = false;
    }

    private void startComputeAsync() {
        voxelsDirty = false;
        computing   = true;

        TerrainSettingsManager src = displaySettings != null
                ? displaySettings : TerrainSettingsManager.getInstance();
        saveSnapshot(src);

        final String    fText = formulaText;
        final double    cs    = snapCoordScale;
        final boolean   dm    = snapDensityMode;
        final double    bh    = snapBaseHeight;
        final double    hv    = snapHeightVariation;
        final double    sf    = snapSmoothing;
        final NoiseType nt    = snapNoiseType;
        final double    nsx   = snapNoiseScaleX, nsy = snapNoiseScaleY;
        final double    nsz   = snapNoiseScaleZ, nhs = snapNoiseHeightScale;
        final int       hw    = snapHalfWidth;
        final int       gx    = Math.max(8, hw * 2 / VOXEL_SIZE_XZ);
        final int       gz    = gx;

        VOXEL_POOL.submit(() -> {
            if (fText.isEmpty()) {
                pendingError     = "No formula";
                pendingBits      = new long[bitsetSize(gx * GY * gz)];
                pendingHalfWidth = hw;
                pendingGX = gx; pendingGZ = gz;
                computing = false;
                return;
            }

            FormulaParser.CompiledFormula compiled;
            try {
                compiled = parseFormulaWithSettings(fText, nt, nsx, nsy, nsz, nhs);
            } catch (Exception e) {
                pendingError     = "Formula error";
                pendingBits      = new long[bitsetSize(gx * GY * gz)];
                pendingHalfWidth = hw;
                pendingGX = gx; pendingGZ = gz;
                computing = false;
                return;
            }

            boolean eq  = fText.contains("=");
            double  vsx = (hw * 2.0) / gx;
            double  vsy = (WY_MAX - WY_MIN) / GY;
            double  vsz = (hw * 2.0) / gz;
            int     sX  = GY * gz, sY = gz;
            int     total = gx * GY * gz;
            float[] raw = new float[total];

            // evaluate однопоточно (PerlinNoise не thread-safe)
            for (int ix = 0; ix < gx; ix++) {
                double wx = -hw + (ix + 0.5) * vsx;
                double fx = wx / cs;
                for (int iz = 0; iz < gz; iz++) {
                    double wz = -hw + (iz + 0.5) * vsz;
                    double fz = wz / cs;
                    for (int iy = 0; iy < GY; iy++) {
                        double wy = WY_MIN + (iy + 0.5) * vsy;
                        float v;
                        try { v = (float) compiled.evaluate(fx, wy / cs, fz); }
                        catch (Exception e) { v = 0f; }
                        raw[ix * sX + iy * sY + iz] = v;
                    }
                }
            }

            // blur параллельно (только float[], thread-safe)
            float[] processed = raw;
            if (sf > 0) {
                int rXZ = Math.max(1, (int) Math.round(sf * cs / vsx));
                int rY  = eq ? Math.max(1, (int) Math.round(sf * cs / vsy)) : 0;
                processed = separableBoxBlur(raw, gx, GY, gz, sX, sY, rXZ, rY);
            }

            // classify -> bitset (параллельно)
            final float[] finalRaw = processed;
            long[] result = new long[bitsetSize(total)];
            int nThreads = Runtime.getRuntime().availableProcessors();
            runParallel(nThreads, gx, ix -> {
                for (int iz = 0; iz < gz; iz++) {
                    for (int iy = 0; iy < GY; iy++) {
                        double wy = WY_MIN + (iy + 0.5) * vsy;
                        if (toDensity(finalRaw[ix * sX + iy * sY + iz],
                                wy / cs, cs, dm, eq, bh, hv) > 0) {
                            // атомарный OR для thread-safety записи в bitset
                            int flatIdx = ix * sX + iy * sY + iz;
                            int  word = flatIdx >>> 6;
                            long bit  = 1L << (flatIdx & 63);
                            // Разные ix -> разные word при sX=GY*gz >= 64 -> нет гонок
                            // (при gx≥8 и GY=96 это всегда выполняется)
                            result[word] |= bit;
                        }
                    }
                }
            });

            pendingError     = null;
            pendingBits      = result;
            pendingHalfWidth = hw;
            pendingGX = gx; pendingGZ = gz;
            computing = false;
        });
    }

    private FormulaParser.CompiledFormula parseFormulaWithSettings(
            String fText, NoiseType nt,
            double nsx, double nsy, double nsz, double nhs) throws Exception {
        TerrainSettingsManager gtsm = TerrainSettingsManager.getInstance();
        NoiseType savedNt  = gtsm.getNoiseType();
        double savedNsx    = gtsm.getNoiseScaleX(), savedNsy = gtsm.getNoiseScaleY();
        double savedNsz    = gtsm.getNoiseScaleZ(), savedNhs = gtsm.getNoiseHeightScale();
        gtsm.setNoiseType(nt != null ? nt : NoiseType.NONE);
        gtsm.setNoiseScaleX(nsx); gtsm.setNoiseScaleY(nsy);
        gtsm.setNoiseScaleZ(nsz); gtsm.setNoiseHeightScale(nhs);
        try {
            return FormulaParser.parse(fText);
        } finally {
            gtsm.setNoiseType(savedNt);
            gtsm.setNoiseScaleX(savedNsx); gtsm.setNoiseScaleY(savedNsy);
            gtsm.setNoiseScaleZ(savedNsz); gtsm.setNoiseHeightScale(savedNhs);
        }
    }

    private float[] separableBoxBlur(float[] src, int gx, int gy, int gz,
                                     int sX, int sY, int rXZ, int rY) {
        int nT = Runtime.getRuntime().availableProcessors();
        float[] tmp1 = new float[src.length];
        float[] tmp2 = new float[src.length];

        // X
        runParallel(nT, gy, iy -> {
            for (int iz = 0; iz < gz; iz++) {
                double sum = 0; int cnt = 0;
                for (int ix = 0; ix <= Math.min(rXZ, gx-1); ix++) { sum += src[ix*sX+iy*sY+iz]; cnt++; }
                for (int ix = 0; ix < gx; ix++) {
                    int a = ix+rXZ+1; if (a < gx)  { sum += src[a*sX+iy*sY+iz]; cnt++; }
                    int r = ix-rXZ-1; if (r >= 0)   { sum -= src[r*sX+iy*sY+iz]; cnt--; }
                    tmp1[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        // Z
        runParallel(nT, gy, iy -> {
            for (int ix = 0; ix < gx; ix++) {
                double sum = 0; int cnt = 0;
                for (int iz = 0; iz <= Math.min(rXZ, gz-1); iz++) { sum += tmp1[ix*sX+iy*sY+iz]; cnt++; }
                for (int iz = 0; iz < gz; iz++) {
                    int a = iz+rXZ+1; if (a < gz)  { sum += tmp1[ix*sX+iy*sY+a]; cnt++; }
                    int r = iz-rXZ-1; if (r >= 0)   { sum -= tmp1[ix*sX+iy*sY+r]; cnt--; }
                    tmp2[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        if (rY <= 0) return tmp2;

        float[] out = new float[src.length];
        // Y
        runParallel(nT, gx, ix -> {
            for (int iz = 0; iz < gz; iz++) {
                double sum = 0; int cnt = 0;
                for (int iy = 0; iy <= Math.min(rY, gy-1); iy++) { sum += tmp2[ix*sX+iy*sY+iz]; cnt++; }
                for (int iy = 0; iy < gy; iy++) {
                    int a = iy+rY+1; if (a < gy)  { sum += tmp2[ix*sX+a*sY+iz]; cnt++; }
                    int r = iy-rY-1; if (r >= 0)   { sum -= tmp2[ix*sX+r*sY+iz]; cnt--; }
                    out[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        return out;
    }

    @FunctionalInterface private interface RowTask { void run(int row); }

    private void runParallel(int nThreads, int rowCount, RowTask task) {
        CountDownLatch latch  = new CountDownLatch(nThreads);
        AtomicInteger  cursor = new AtomicInteger(0);
        for (int ti = 0; ti < nThreads; ti++) {
            VOXEL_POOL.submit(() -> {
                try {
                    int row;
                    while ((row = cursor.getAndIncrement()) < rowCount) task.run(row);
                } finally { latch.countDown(); }
            });
        }
        try { latch.await(); } catch (InterruptedException ignored) {}
    }

    private static double toDensity(double fv, double fy, double scale,
                                    boolean dm, boolean eq,
                                    double baseHeight, double heightVariation) {
        if (eq) {
            double d = dm ? (fv - (baseHeight-64.0)/scale) / Math.max(1e-6, heightVariation) : fv;
            return d >= 0 ? 1.0 : -1.0;
        }
        double tgt = dm ? baseHeight/scale + fv*heightVariation : 64.0/scale + fv;
        return tgt - fy >= 0 ? 1.0 : -1.0;
    }

    private void drawOverlays(GuiGraphics graphics, Font font, int x, int y, int w, int h) {
        if (computing)
            graphics.drawString(font, "/", x + w - 16, y + 3, 0x88FFFFFF, false);
        graphics.drawString(font, " drag to rotate", x + 3, y + h - 10, 0x88FFFFFF, false);
        String bounds = String.format("XZ:±%d  Y:%.0f-%.0f",
                renderHalfWidth, (double)WY_MIN, (double)WY_MAX);
        graphics.drawString(font, bounds, x + 3, y + h - 20, 0x66FFFFFF, false);
    }

    private void renderControls(GuiGraphics graphics, Font font, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + CTRL_H, 0xBF0F0F0F);
        int half = w / 2;
        String zoomLabel = (zoom == Math.floor(zoom))
                ? (int)zoom + "x"
                : String.format("%.2fx", zoom).replaceAll("0+$","").replaceAll("\\.$","");
        renderControl(graphics, font, x,        y, half,     "Zoom",  zoomLabel,        zoom>0.5f,     zoom<7.0f);
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
            if      (mx < cx-10 && zoom>0.5f) zoom = Math.round((zoom-0.25f)*100)/100f;
            else if (mx > cx+10 && zoom<7.0f) zoom = Math.round((zoom+0.25f)*100)/100f;
        } else {
            int cx = x + half + (w-half)/2;
            if      (mx < cx-10 && halfWidth>50)  { halfWidth -= 50; voxelsDirty = true; }
            else if (mx > cx+10 && halfWidth<200) { halfWidth += 50; voxelsDirty = true; }
        }
        return true;
    }

    private boolean settingsChanged() {
        return displaySettings.getCoordinateScale()     != snapCoordScale
                || displaySettings.isUseDensityMode()   != snapDensityMode
                || displaySettings.getBaseHeight()       != snapBaseHeight
                || displaySettings.getHeightVariation()  != snapHeightVariation
                || displaySettings.getSmoothingFactor()  != snapSmoothing
                || displaySettings.getNoiseType()        != snapNoiseType
                || displaySettings.getNoiseScaleX()      != snapNoiseScaleX
                || displaySettings.getNoiseScaleY()      != snapNoiseScaleY
                || displaySettings.getNoiseScaleZ()      != snapNoiseScaleZ
                || displaySettings.getNoiseHeightScale() != snapNoiseHeightScale;
    }

    private void saveSnapshot(TerrainSettingsManager s) {
        snapCoordScale       = s.getCoordinateScale();
        snapDensityMode      = s.isUseDensityMode();
        snapBaseHeight       = s.getBaseHeight();
        snapHeightVariation  = s.getHeightVariation();
        snapSmoothing        = s.getSmoothingFactor();
        snapNoiseType        = s.getNoiseType();
        snapNoiseScaleX      = s.getNoiseScaleX();
        snapNoiseScaleY      = s.getNoiseScaleY();
        snapNoiseScaleZ      = s.getNoiseScaleZ();
        snapNoiseHeightScale = s.getNoiseHeightScale();
        snapHalfWidth        = halfWidth;
    }
}