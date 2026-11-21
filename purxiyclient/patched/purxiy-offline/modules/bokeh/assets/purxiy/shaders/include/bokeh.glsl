#version 330

// The frame of Bokeh (see dev.purxiy.bokeh.BokehRenderer); keep in sync with it. Import bokeh_shape.glsl before this.
layout(std140) uniform PurxiyBokeh {
    // clip space to the camera-relative world
    mat4 BkInverse;
    // x, y = the picture's size (px); z = picture pixels per texel of the working grid (2..4); w = time (s)
    vec4 BkScreen;
    // x, y = the working grid's size (texels); z, w = the grid of light cells (2 x 2 texels each)
    vec4 BkGrid;
    // xyz = which way the camera looks (camera-relative world); w = 1 / the focus distance (1 / blocks)
    vec4 BkForward;
    // x = blur radius per dioptre away from the focus (px); y = the largest blur radius (px); z = near blur (0..1);
    // w = tilt-shift (0..1)
    vec4 BkLens;
    // x = the sharp band's middle (0 bottom .. 1 top); y = half its height; z = how soft its edges are; w = the blur
    // outside it (px)
    vec4 BkTilt;
    // x = shape from, y = shape to, z = from .. to (0..1); w = swirl (0..1)
    vec4 BkShape;
    // x = highlights (0..1); y = fringe (0..1); z = how present the lens is (0..1); w = debug view (0 off)
    vec4 BkLook;
};

const float BOKEH_SKY = 0.999999;

float bokehLuma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// How far the surface at a pixel is in front of the camera (blocks, along the view), from the depth there; the sky is
// endlessly far.
float bokehDepth(vec2 fragPx, float depth) {
    if (depth >= BOKEH_SKY) {
        return 1.0e6;
    }
    vec2 ndc = fragPx / BkScreen.xy * 2.0 - 1.0;
    vec4 p = BkInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return max(dot(p.xyz / p.w, BkForward.xyz), 0.02);
}

// The blur of a surface at view distance z and height v in the picture (0 bottom .. 1 top): the radius of its circle of
// confusion in pixels, negative in front of the focus, positive behind it. A thin lens: the radius grows with how far
// the surface is from the focus in dioptres. Tilt-shift blurs by height instead: sharp in a band, softer above and below.
float bokehCoc(float z, float v) {
    float lens = BkLens.x * (BkForward.w - 1.0 / z);
    lens *= lens < 0.0 ? BkLens.z : 1.0;
    float off = v - BkTilt.x;
    float band = BkTilt.w * smoothstep(BkTilt.y, BkTilt.y + BkTilt.z, abs(off)) * (off < 0.0 ? -1.0 : 1.0);
    float coc = mix(lens, band, BkLens.w) * BkLook.z;
    return clamp(coc, -BkLens.y, BkLens.y);
}

// A blur radius in 8 bits (alpha of the working grid): 0.5 is sharp.
float bokehEncode(float coc) {
    return clamp(0.5 + 0.5 * coc / BkLens.y, 0.0, 1.0);
}

float bokehDecode(float a) {
    float coc = (a * 2.0 - 1.0) * BkLens.y;
    return abs(coc) < BkLens.y * 0.006 ? 0.0 : coc;
}

// The lens's shape at q (see bokehGauge), gliding from one shape to the next while you choose, and cut by the cat's eye
// towards the edges of the picture.
float bokehLensGauge(vec2 q, vec2 screen) {
    float d = bokehGauge(q, int(BkShape.x));
    if (BkShape.z > 0.0) {
        d = mix(d, bokehGauge(q, int(BkShape.y)), BkShape.z);
    }
    return max(d, bokehCatsEye(q, screen, BkScreen.x / BkScreen.y, BkShape.w));
}

float bokehLensArea() {
    return mix(bokehArea(int(BkShape.x)), bokehArea(int(BkShape.y)), BkShape.z);
}

// Interleaved gradient noise (Jimenez): a fine, even pattern per pixel.
float bokehNoise(vec2 frag) {
    return fract(52.9829189 * fract(dot(frag, vec2(0.06711056, 0.00583715))));
}
