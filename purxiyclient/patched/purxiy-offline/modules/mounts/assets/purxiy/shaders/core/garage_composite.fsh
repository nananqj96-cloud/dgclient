#version 330

// Lays the ride over the world. A bilinear tap at the screen pixel's centre is a 2x2 box of the larger image; drawn at the
// screen's own size (from the driver's seat, or on Performance), a pass of edge smoothing takes the jaggies out instead.
uniform sampler2D CarColor;
uniform sampler2D CarDepth;

in vec2 texCoord;
out vec4 fragColor;

#ifdef SMOOTH
float luma(vec4 c) { return dot(c.rgb, vec3(0.299, 0.587, 0.114)) + 0.25 * c.a; }

vec4 smoothed(vec2 uv) {
    vec2 px = 1.0 / vec2(textureSize(CarColor, 0));
    vec4 m = texture(CarColor, uv);
    float lm = luma(m);
    float nw = luma(textureOffset(CarColor, uv, ivec2(-1, -1))), ne = luma(textureOffset(CarColor, uv, ivec2(1, -1)));
    float sw = luma(textureOffset(CarColor, uv, ivec2(-1, 1))), se = luma(textureOffset(CarColor, uv, ivec2(1, 1)));
    float low = min(lm, min(min(nw, ne), min(sw, se))), high = max(lm, max(max(nw, ne), max(sw, se)));
    if (high - low < max(0.05, high * 0.12)) return m;
    vec2 dir = vec2(-((nw + ne) - (sw + se)), (nw + sw) - (ne + se));
    float reduce = max((nw + ne + sw + se) * 0.03125, 0.0078125);
    dir = clamp(dir / (min(abs(dir.x), abs(dir.y)) + reduce), -8.0, 8.0) * px;
    vec4 a = 0.5 * (texture(CarColor, uv + dir * (1.0 / 3.0 - 0.5)) + texture(CarColor, uv + dir * (2.0 / 3.0 - 0.5)));
    vec4 b = 0.5 * a + 0.25 * (texture(CarColor, uv - dir * 0.5) + texture(CarColor, uv + dir * 0.5));
    float lb = luma(b);
    return lb < low || lb > high ? a : b;
}
#endif

void main() {
#ifdef SMOOTH
    vec4 color = smoothed(texCoord);
    if (color.a <= 0.0) discard;
    ivec2 size = textureSize(CarDepth, 0);
    ivec2 at = ivec2(floor(texCoord * vec2(size)));
    float nearest = 1.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) nearest = min(nearest, texelFetch(CarDepth, clamp(at + ivec2(x, y), ivec2(0), size - 1), 0).r);
    }
    gl_FragDepth = nearest;
    fragColor = color;
#else
    vec4 color = texture(CarColor, texCoord);
    if (color.a <= 0.0) discard;
    ivec2 size = textureSize(CarDepth, 0);
    int scale = max(1, int(round(dFdx(texCoord.x) * float(size.x))));
    ivec2 first = ivec2(floor(texCoord * vec2(size) - vec2(float(scale - 1) * 0.5) - 0.0001));
    float nearest = 1.0;
    for (int y = 0; y < scale; y++) {
        for (int x = 0; x < scale; x++) nearest = min(nearest, texelFetch(CarDepth, first + ivec2(x, y), 0).r);
    }
    gl_FragDepth = nearest;
    fragColor = color;
#endif
}
