#version 330

// Signed distance helpers shared by the Purxiy shaders. All distances are negative inside.

// Rounded box centred on the origin. b = half size, r = corner radii (top-left, top-right, bottom-right, bottom-left).
// The coordinate system is y-down, matching the UI.
float prx_roundBox(vec2 p, vec2 b, vec4 r) {
    float rad = p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x);
    rad = min(rad, min(b.x, b.y));
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

// Arc around the origin on radius r, starting at angle a0 (radians, clockwise on screen) and running for sweep radians.
// ht = half the stroke thickness.
float prx_arc(vec2 p, float r, float a0, float sweep, float ht, bool roundCaps) {
    if (sweep >= 6.28318) {
        return abs(length(p) - r) - ht;
    }
    float mid = a0 + sweep * 0.5;
    float c = cos(mid);
    float s = sin(mid);
    // rotate p by -mid so the arc is symmetric around +x
    vec2 q = vec2(c * p.x + s * p.y, -s * p.x + c * p.y);
    q.y = abs(q.y);
    float halfSweep = sweep * 0.5;
    vec2 sc = vec2(cos(halfSweep), sin(halfSweep));
    float band = abs(length(q) - r) - ht;
    if (roundCaps) {
        return (sc.y * q.x > sc.x * q.y) ? band : length(q - sc * r) - ht;
    }
    // butt caps: intersect the ring with the wedge
    float cut = -(q.x * sc.y - q.y * sc.x);
    return max(band, cut);
}

// ---------------------------------------------------------------------------------------------
// Gaussian blurred rounded rectangle (Evan Wallace, "Fast Rounded Rectangle Shadows", CC0).

float prx_gaussian(float x, float sigma) {
    const float PI = 3.14159265;
    return exp(-(x * x) / (2.0 * sigma * sigma)) / (sqrt(2.0 * PI) * sigma);
}

vec2 prx_erf(vec2 x) {
    vec2 s = sign(x);
    vec2 a = abs(x);
    x = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    x *= x;
    return s - s / (x * x);
}

float prx_shadowX(float x, float y, float sigma, float corner, vec2 halfSize) {
    float delta = min(halfSize.y - corner - abs(y), 0.0);
    float curved = halfSize.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
    vec2 integral = 0.5 + 0.5 * prx_erf((x + vec2(-curved, curved)) * (sqrt(0.5) / sigma));
    return integral.y - integral.x;
}

// Box centred on the origin with the given half size.
float prx_roundBoxShadow(vec2 p, vec2 halfSize, float sigma, float corner) {
    corner = min(corner, min(halfSize.x, halfSize.y));
    float low = p.y - halfSize.y;
    float high = p.y + halfSize.y;
    float start = clamp(-3.0 * sigma, low, high);
    float end = clamp(3.0 * sigma, low, high);
    float stepSize = (end - start) / 4.0;
    float y = start + stepSize * 0.5;
    float value = 0.0;
    for (int i = 0; i < 4; i++) {
        value += prx_shadowX(p.x, p.y - y, sigma, corner, halfSize) * prx_gaussian(y, sigma) * stepSize;
        y += stepSize;
    }
    return value;
}

// ---------------------------------------------------------------------------------------------

// Cheap hash for dithering and grain (Dave Hoskins, MIT).
float prx_hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
