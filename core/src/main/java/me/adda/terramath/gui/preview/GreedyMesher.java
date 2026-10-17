package me.adda.terramath.gui.preview;

import java.util.Arrays;

import static me.adda.terramath.gui.preview.VoxelField.GY;
import static me.adda.terramath.gui.preview.VoxelField.WY_MAX;
import static me.adda.terramath.gui.preview.VoxelField.WY_MIN;

public final class GreedyMesher {
    /** Triangle vertices as 7 floats each: x, y, z, r, g, b, a. */
    public record Mesh(float[] verts, int floatCount) {
        public static final Mesh EMPTY = new Mesh(new float[0], 0);
    }

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

    private GreedyMesher() {}

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

    public static Mesh build(VoxelField field) {
        long[] bits = field.bits;
        int gx = field.gx, gz = field.gz;

        if (bits.length == 0 || gx == 0 || gz == 0)
            return Mesh.EMPTY;

        double hw = field.halfWidth;
        float vsx = (float) ((hw * 2.0) / gx);
        float vsy = (WY_MAX - WY_MIN) / GY;
        float vsz = (float) ((hw * 2.0) / gz);
        int sX = GY * gz;
        int sY = gz;

        // Начинаем с разумного буфера; рост по необходимости
        float[] meshVerts = new float[1 << 18];
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
                case 0: case 1: uDim = gx; vDim = gz; nDim = GY; break; // ±Y
                case 2: case 3: uDim = GY; vDim = gz; nDim = gx; break; // ±X
                default: uDim = gx; vDim = GY; nDim = gz; break; // ±Z
            }

            for (int layer = 0; layer < nDim; layer++) {

                // Заполняем маску для этого слоя
                Arrays.fill(mask, 0, uDim * vDim, 0);

                for (int u = 0; u < uDim; u++) {
                    for (int v = 0; v < vDim; v++) {

                        // Координаты текущего и соседнего вокселя
                        int ix, iy, iz, nx, ny, nz;
                        switch (dir) {
                            case 0: ix = u; iy = layer; iz = v; nx = u; ny = layer+1; nz = v; break;
                            case 1: ix = u; iy = layer; iz = v; nx = u; ny = layer-1; nz = v; break;
                            case 2: ix = layer; iy = u; iz = v; nx = layer+1; ny = u; nz = v; break;
                            case 3: ix = layer; iy = u; iz = v; nx = layer-1; ny = u; nz = v; break;
                            case 4: ix = u; iy = v; iz = layer; nx = u; ny = v; nz = layer+1; break;
                            default: ix = u; iy = v; iz = layer; nx = u; ny = v; nz = layer-1; break;
                        }

                        boolean solid = ix>=0 && ix<gx && iy>=0 && iy<GY && iz>=0 && iz<gz
                                && VoxelField.bitGet(bits, sX, sY, ix, iy, iz);
                        boolean neighborSolid = nx>=0 && nx<gx && ny>=0 && ny<GY && nz>=0 && nz<gz
                                && VoxelField.bitGet(bits, sX, sY, nx, ny, nz);

                        if (solid && !neighborSolid) {
                            // Открытая грань - определяем тип цвета
                            float wyMid;
                            switch (dir) {
                                case 0: case 1: wyMid = WY_MIN + (layer + 0.5f) * vsy; break;
                                case 2: case 3: wyMid = WY_MIN + (u + 0.5f) * vsy; break;
                                default: wyMid = WY_MIN + (v + 0.5f) * vsy; break;
                            }
                            boolean belowSea = wyMid < 63.0f;
                            // isGrass: грань dir=0 (+Y, верхняя) и не под водой
                            boolean isGrass = (dir == 0) && !belowSea;
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
                                q0x=(float)(-hw+u*vsx); q0y=y1; q0z=(float)(-hw+(v+w2)*vsz);
                                q1x=(float)(-hw+(u+h2)*vsx); q1y=y1; q1z=(float)(-hw+(v+w2)*vsz);
                                q2x=(float)(-hw+(u+h2)*vsx); q2y=y1; q2z=(float)(-hw+v*vsz);
                                q3x=(float)(-hw+u*vsx); q3y=y1; q3z=(float)(-hw+v*vsz);
                                break;
                            }
                            case 1: {
                                float y0 = WY_MIN + layer * vsy;
                                q0x=(float)(-hw+(u+h2)*vsx); q0y=y0; q0z=(float)(-hw+(v+w2)*vsz);
                                q1x=(float)(-hw+u*vsx); q1y=y0; q1z=(float)(-hw+(v+w2)*vsz);
                                q2x=(float)(-hw+u*vsx); q2y=y0; q2z=(float)(-hw+v*vsz);
                                q3x=(float)(-hw+(u+h2)*vsx); q3y=y0; q3z=(float)(-hw+v*vsz);
                                break;
                            }
                            case 2: { // +X
                                float x1 = (float)(-hw+(layer+1)*vsx);
                                q0x=x1; q0y=WY_MIN+u*vsy; q0z=(float)(-hw+v*vsz);
                                q1x=x1; q1y=WY_MIN+(u+h2)*vsy; q1z=(float)(-hw+v*vsz);
                                q2x=x1; q2y=WY_MIN+(u+h2)*vsy; q2z=(float)(-hw+(v+w2)*vsz);
                                q3x=x1; q3y=WY_MIN+u*vsy; q3z=(float)(-hw+(v+w2)*vsz);
                                break;
                            }
                            case 3: { // -X
                                float x0 = (float)(-hw+layer*vsx);
                                q0x=x0; q0y=WY_MIN+(u+h2)*vsy; q0z=(float)(-hw+v*vsz);
                                q1x=x0; q1y=WY_MIN+u*vsy; q1z=(float)(-hw+v*vsz);
                                q2x=x0; q2y=WY_MIN+u*vsy; q2z=(float)(-hw+(v+w2)*vsz);
                                q3x=x0; q3y=WY_MIN+(u+h2)*vsy; q3z=(float)(-hw+(v+w2)*vsz);
                                break;
                            }
                            case 4: { // +Z
                                float z1 = (float)(-hw+(layer+1)*vsz);
                                q0x=(float)(-hw+(u+h2)*vsx); q0y=WY_MIN+v*vsy; q0z=z1;
                                q1x=(float)(-hw+(u+h2)*vsx); q1y=WY_MIN+(v+w2)*vsy; q1z=z1;
                                q2x=(float)(-hw+u*vsx); q2y=WY_MIN+(v+w2)*vsy; q2z=z1;
                                q3x=(float)(-hw+u*vsx); q3y=WY_MIN+v*vsy; q3z=z1;
                                break;
                            }
                            default: { // -Z
                                float z0 = (float)(-hw+layer*vsz);
                                q0x=(float)(-hw+u*vsx); q0y=WY_MIN+v*vsy; q0z=z0;
                                q1x=(float)(-hw+u*vsx); q1y=WY_MIN+(v+w2)*vsy; q1z=z0;
                                q2x=(float)(-hw+(u+h2)*vsx); q2y=WY_MIN+(v+w2)*vsy; q2z=z0;
                                q3x=(float)(-hw+(u+h2)*vsx); q3y=WY_MIN+v*vsy; q3z=z0;
                                break;
                            }
                        }

                        float r = (argb >> 16) & 0xFF;
                        float g = (argb >> 8) & 0xFF;
                        float b = argb & 0xFF;
                        float a = (argb >>> 24) & 0xFF;

                        // Quad → 2 треугольника (6 вершин × 7 float = 42 float)
                        if (vertFloats + 42 > meshVerts.length) {
                            meshVerts = Arrays.copyOf(meshVerts,
                                    Math.max(meshVerts.length * 2, vertFloats + 42));
                        }
                        float[] vt = meshVerts;
                        int o = vertFloats;
                        // tri 0: q0,q1,q2
                        vt[o]=q0x;vt[o+1]=q0y;vt[o+2]=q0z;vt[o+3]=r;vt[o+4]=g;vt[o+5]=b;vt[o+6]=a;
                        vt[o+7]=q1x;vt[o+8]=q1y;vt[o+9]=q1z;vt[o+10]=r;vt[o+11]=g;vt[o+12]=b;vt[o+13]=a;
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

        return new Mesh(meshVerts, vertFloats);
    }
}
