#version 330

#moj_import <purxiy:trail.glsl>

// A quad of a trail (see dev.purxiy.trail.TrailGeometry).
//
//   line     a piece of line between two points of the curve: all four vertices carry the whole piece, and the quad is
//            laid around it on the screen, wide enough for its glow. The fragment shader measures the distance to it.
//   band     a quad of the path, lying on the ground: its corners are where they are.
//   mote     a soft light (Stardust, the previews' runner): the centre in all four vertices, turned to the camera here.
//
// Nothing of a line comes closer than about a pixel or grows into a blob up close: thin lines are drawn a pixel wide and
// fainter by as much, and near the camera their width stops growing.

in vec4 Head;   // xyz = a point (camera-relative, blocks), w = kind
in vec4 Tail;   // line: xyz = the other end, w = thread; band: x = across (-1..1), y = half width, z = its line's half width (blocks); mote: x = radius, y = phase
in vec4 Width;  // line: radius at each end, glow reach at each end (blocks)
in vec4 Life;   // line: age at each end (0 new .. 1 gone), distance from the head at each end; band: age, distance, shade, odometer; mote: progress
in vec4 Extra;  // line: shade at each end, odometer at each end
in vec4 Color;  // mote: its colour

flat out int vKind;
flat out vec4 vEnds;    // line: both ends on the screen (px)
flat out vec4 vRadii;   // line: radius at each end, glow reach at each end (px)
flat out vec4 vLife;    // line: as Life
flat out vec4 vExtra;   // line: as Extra
flat out vec4 vDepth;   // line: depth (clip w) at each end, coverage at each end
flat out vec3 vStart;   // line: its ends in the world (camera-relative)
flat out vec3 vEnd;
flat out float vThread;
out vec3 vWorld;        // band, mote: camera-relative position (a mote's centre)
out vec4 vLocal;        // band: across (-1..1), age, distance, shade; mote: corner (-1..1), progress, phase
out vec3 vBand;         // band: half width, its line's half width (blocks), odometer
out vec4 vColor;

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

void line() {
    vec3 p0 = Head.xyz, p1 = Tail.xyz;
    vec4 width = Width, life = Life, extra = Extra;
    vec4 c0 = TrailViewProj * vec4(p0, 1.0);
    vec4 c1 = TrailViewProj * vec4(p1, 1.0);
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
        life = clipFirst(life, f);
        extra = clipFirst(extra, f);
    } else if (c1.w < NEAR) {
        float f = (NEAR - c1.w) / (c0.w - c1.w);
        p1 = mix(p1, p0, f);
        c1 = mix(c1, c0, f);
        width = clipSecond(width, f);
        life = clipSecond(life, f);
        extra = clipSecond(extra, f);
    }
    vec2 screen = TrailScreen.xy;
    vec2 s0 = (c0.xy / c0.w * 0.5 + 0.5) * screen;
    vec2 s1 = (c1.xy / c1.w * 0.5 + 0.5) * screen;
    float focal = TrailRight.w;
    vec2 radius = vec2(width.x / c0.w, width.y / c1.w) * focal;
    vec2 reach = vec2(width.z / c0.w, width.w / c1.w) * focal;
    // up close the line stops growing, its glow with it
    vec2 shrink = min(vec2(1.0), TrailLimits.x / max(radius, vec2(1.0e-4)));
    radius *= shrink;
    reach = min(reach * shrink, vec2(TrailLimits.y));
    // never thinner than about a pixel: fainter instead
    vec2 cover = clamp(radius / TrailLimits.z, 0.0, 1.0);
    radius = max(radius, vec2(TrailLimits.z));
    reach = max(reach, radius);

    float extent = max(max(reach.x, reach.y), max(radius.x, radius.y)) + 1.5;
    vec2 d = s1 - s0;
    float len = length(d);
    vec2 dir = len > 1.0e-3 ? d / len : vec2(1.0, 0.0);
    vec2 across = vec2(-dir.y, dir.x);
    int corner = gl_VertexID & 3;
    vec2 at = (corner == 1 || corner == 2 ? s1 + dir * extent : s0 - dir * extent) + across * (corner >= 2 ? extent : -extent);
    gl_Position = vec4(at / screen * 2.0 - 1.0, 0.0, 1.0);

    vEnds = vec4(s0, s1);
    vRadii = vec4(radius, reach);
    vLife = life;
    vExtra = extra;
    vDepth = vec4(c0.w, c1.w, cover);
    vStart = p0;
    vEnd = p1;
    vThread = Tail.w;
}

void band() {
    vWorld = Head.xyz;
    vLocal = vec4(Tail.x, Life.x, Life.y, Life.z);
    vBand = vec3(Tail.yz, Life.w);
    gl_Position = TrailViewProj * vec4(Head.xyz, 1.0);
}

void mote(int kind) {
    vec3 centre = Head.xyz;
    vec4 c = TrailViewProj * vec4(centre, 1.0);
    if (c.w < NEAR) {
        hide();
        return;
    }
    // a mote shrinks a little as it fades
    float radius = Tail.x * (kind == KIND_MOTE ? 1.0 - 0.35 * clamp(Life.x, 0.0, 1.0) : 1.0) * TrailRight.w / c.w;
    float minimum = kind == KIND_ORB ? 2.5 : 1.6;
    float cover = 1.0;
    if (radius < minimum) {
        cover = (radius / minimum) * (radius / minimum);
        radius = minimum;
    }
    radius = min(radius, TrailLimits.y);
    int i = gl_VertexID & 3;
    vec2 corner = vec2(i == 1 || i == 2 ? 1.0 : -1.0, i >= 2 ? 1.0 : -1.0);
    vec2 centreNdc = c.xy / c.w;
    gl_Position = vec4(centreNdc + corner * radius / TrailScreen.xy * 2.0, 0.0, 1.0);
    vWorld = centre;
    vLocal = vec4(corner, Life.x, Tail.y);
    vColor = vec4(Color.rgb, Color.a * cover);
}

void main() {
    int kind = int(Head.w + 0.5);
    vKind = kind;
    vEnds = vec4(0.0);
    vRadii = vec4(0.0);
    vLife = vec4(0.0);
    vExtra = vec4(0.0);
    vDepth = vec4(1.0);
    vStart = vec3(0.0);
    vEnd = vec3(0.0);
    vThread = 0.0;
    vWorld = vec3(0.0);
    vLocal = vec4(0.0);
    vBand = vec3(0.0);
    vColor = vec4(1.0);
    if (kind == KIND_LINE) {
        line();
    } else if (kind == KIND_BAND) {
        band();
    } else {
        mote(kind);
    }
}
