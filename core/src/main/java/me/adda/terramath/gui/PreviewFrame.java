package me.adda.terramath.gui;

import org.joml.Matrix4f;

record PreviewFrame(float[] verts, int floatCount, Matrix4f projView, Matrix4f modelView, float zoom,
                    int x, int y, int w, int h, int vpX, int vpY, int vpW, int vpH) {}
