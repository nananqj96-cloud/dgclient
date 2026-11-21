#version 330

#moj_import <purxiy:aura.glsl>

// A quad of an aura (see dev.purxiy.auras.AuraGeometry).
//
//   line   a piece of a light's tail, or a line of a constellation: all four vertices carry the whole piece, and the quad
//          is laid around it on the screen, wide enough for its glow. The fragment shader measures the distance to it.
//   mote   a soft light (a comet's head, a firefly, a star): its centre in all four vertices, turned to the camera here.
//
// Nothing comes closer than about a pixel or grows into a blob up close: thin lines and small lights are drawn a pixel
// wide and fainter by as much, and near the camera they stop growing.

in vec4 Head;   // xyz = a point (camera-relative, blocks), w = kind
in vec4 Tail;   // line: xyz = the other end; mote: x = radius of its core, y = reach of its glow (blocks), z = sparkle (0..1), w = its turn (radians)
in vec4 Width;  // line: radius at each end, glow reach at each end (blocks)
in vec4 Shade;  // line: strength at each end, whiteness at each end; mote: strength, whiteness
in vec4 Color;  // the colour (of a line's first end)
in vec4 Color2; // the colour of a line's other end

flat out int vKind;
flat out vec4 vEnds;    // line: both ends on the screen (px); mote: its centre on the screen (px), core radius, glow reach (px)
flat out vec4 vRadii;   // line: radius at each end, glow reach at each end (px); mote: sparkle, length of its rays (px), its turn (cos, sin)
flat out vec4 vShade;   // as Shade
flat out vec4 vDepth;   // line: depth (clip w) at each end, coverage at each end; mote: coverage
flat out vec3 vStart;   // line: its ends in the world (camera-relative); mote: its centre
flat out vec3 vEnd;
flat out vec3 vColorA;
flat out vec3 vColorB;

const float NEAR = 0.05;

// moves the first (a) or second (b) end of a pair of values towards the other one
vec4 clipFirst(vec4 v, float f) {
    return vec4(mix(v.x, v.y, f), v.y, mix(v.z, v.w, f), v.w);
}

vec4 clipSecond(vec4 v, float f) {
    return vec4(v.x, mix(v.y, v.x, f), v.z, mix(v.w, v.z, f));
}

void hide() {
    // outside the clip volume: nothing is drawn
    gl_Position = vec4(0.0, 0.0, 2.0, 1.0);
}

vec2 toScreen(vec4 clip) {
    return (clip.xy / clip.w * 0.5 + 0.5) * AuraScreen.xy;
}

void line() {
    vec3 p0 = Head.xyz, p1 = Tail.xyz;
    vec4 width = Width, shade = Shade;
    vec3 c0rgb = Color.rgb, c1rgb = Color2.rgb;
    vec4 c0 = AuraViewProj * vec4(p0, 1.0);
    vec4 c1 = AuraViewProj * vec4(p1, 1.0);
    if (c0.w < NEAR && c1.w < NEAR) {
        hide();
        return;
    }
    // a piece that passes behind the camera is cut where it does
    if (c0.w < NEAR) {
        float f = (NEAR - c0.w) / (c1.w - c0.w);
        p0 = mix(p0, p1, f);
        c0 = mix(c0, c1, f);
        width = clipFirst(width, f);
        shade = clipFirst(shade, f);
        c0rgb = mix(c0rgb, c1rgb, f);
    } else if (c1.w < NEAR) {
        float f = (NEAR - c1.w) / (c0.w - c1.w);
        p1 = mix(p1, p0, f);
        c1 = mix(c1, c0, f);
        width = clipSecond(width, f);
        shade = clipSecond(shade, f);
        c1rgb = mix(c1rgb, c0rgb, f);
    }
    vec2 s0 = toScreen(c0);
    vec2 s1 = toScreen(c1);
    float focal = AuraLimits.w;
    vec2 radius = vec2(width.x / c0.w, width.y / c1.w) * focal;
    vec2 reach = vec2(width.z / c0.w, width.w / c1.w) * focal;
    // up close the line stops growing, its glow with it
    vec2 shrink = min(vec2(1.0), AuraLimits.x / max(radius, vec2(1.0e-4)));
    radius *= shrink;
    reach = min(reach * shrink, vec2(AuraLimits.y));
    // never thinner than about a pixel: fainter instead
    vec2 cover = clamp(radius / AuraLimits.z, 0.0, 1.0);
    radius = max(radius, vec2(AuraLimits.z));
    reach = max(reach, radius);

    float extent = max(max(reach.x, reach.y), max(radius.x, radius.y)) + 1.5;
    vec2 d = s1 - s0;
    float len = length(d);
    vec2 dir = len > 1.0e-3 ? d / len : vec2(1.0, 0.0);
    vec2 across = vec2(-dir.y, dir.x);
    int corner = gl_VertexID & 3;
    vec2 at = (corner == 1 || corner == 2 ? s1 + dir * extent : s0 - dir * extent) + across * (corner >= 2 ? extent : -extent);
    gl_Position = vec4(at / AuraScreen.xy * 2.0 - 1.0, 0.0, 1.0);

    vEnds = vec4(s0, s1);
    vRadii = vec4(radius, reach);
    vShade = shade;
    vDepth = vec4(c0.w, c1.w, cover);
    vStart = p0;
    vEnd = p1;
    vColorA = c0rgb;
    vColorB = c1rgb;
}

void mote() {
    vec3 centre = Head.xyz;
    vec4 c = AuraViewProj * vec4(centre, 1.0);
    if (c.w < NEAR) {
        hide();
        return;
    }
    float focal = AuraLimits.w;
    float core = Tail.x * focal / c.w;
    float reach = Tail.y * focal / c.w;
    // up close a light stops growing (a glow without a core is as large as the body it lies behind: it grows with it)
    float shrink = min(1.0, AuraLimits.x / max(core, 1.0e-4));
    core *= shrink;
    reach = min(reach * shrink, Tail.x > 0.0 ? AuraLimits.y : AuraLimits.y * 6.0);
    // never smaller than about a pixel: fainter instead (a glow without a core, like the haze behind you, has none)
    float cover = 1.0;
    if (Tail.x > 0.0) {
        cover = clamp(core / 1.2, 0.0, 1.0);
        core = max(core, 1.2);
        reach = max(reach, core * 2.5);
    } else {
        core = 0.0;
        reach = max(reach, 1.0);
    }
    // the sparkle's rays reach a little past the glow
    float rays = Tail.z > 0.0 ? max(reach * 1.15, core * 6.0) : 0.0;
    float extent = max(reach, rays) + 1.5;
    vec2 centrePx = toScreen(c);
    int i = gl_VertexID & 3;
    vec2 corner = vec2(i == 1 || i == 2 ? 1.0 : -1.0, i >= 2 ? 1.0 : -1.0);
    gl_Position = vec4((centrePx + corner * extent) / AuraScreen.xy * 2.0 - 1.0, 0.0, 1.0);
    vEnds = vec4(centrePx, core, reach);
    vRadii = vec4(Tail.z, rays, cos(Tail.w), sin(Tail.w));
    vShade = Shade;
    vDepth = vec4(cover, 0.0, 0.0, 0.0);
    vStart = centre;
    vEnd = centre;
    vColorA = Color.rgb;
    vColorB = Color.rgb;
}

void main() {
    int kind = int(Head.w + 0.5);
    vKind = kind;
    if (kind == KIND_LINE) {
        line();
    } else {
        mote();
    }
}
