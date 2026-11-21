#version 330

#moj_import <purxiy:frame.glsl>

in vec4 PosLocal; // xy = position in UI units (already transformed), zw = coordinates in the shape's own space
in vec4 UvSize;   // xy = texture coordinates, zw = half size of the shape
in vec4 Radii;    // rounded box: corner radii (tl, tr, br, bl) | arc: (start, sweep, half thickness, -)
in vec4 Params;   // x = mode, y = border / weight, z = softness / px range / opacity, w = flags
in vec4 Clip;     // clip rectangle in UI units (x0, y0, x1, y1)
in vec4 Extra;    // x = clip radius, y = shape units per device pixel, zw = mode specific
in vec4 Color;
in vec4 Color2;

out vec2 vLocal;
out vec2 vUv;
out vec2 vScreen;
out vec4 vColor;
out vec4 vColor2;
flat out vec2 vHalf;
flat out vec4 vRadii;
flat out vec4 vParams;
flat out vec4 vClip;
flat out vec4 vExtra;

void main() {
    gl_Position = ProjMat * vec4(PosLocal.xy, 0.0, 1.0);

    vScreen = PosLocal.xy;
    vLocal = PosLocal.zw;
    vUv = UvSize.xy;
    vHalf = UvSize.zw;
    vRadii = Radii;
    vParams = Params;
    vClip = Clip;
    vExtra = Extra;

    // Premultiply before interpolation so gradients blend like CSS does.
    vColor = vec4(Color.rgb * Color.a, Color.a);
    vColor2 = vec4(Color2.rgb * Color2.a, Color2.a);
}
