#version 330

#moj_import <purxiy:ambience.glsl>

// The particles of the ambience (see dev.purxiy.ambience.PurxiyAmbience), premultiplied: a body covers what is
// behind it, glow only adds light.
//
//   orb      a mote: a crisp dot far away, a soft sphere of light up close, white-hot in its heart, in a soft glow
//   sparkle  a tiny glint with four long rays and four short ones, flaring up now and then
//   flake    a soft snowflake that shows its six-fold crystal up close
//   petal    a flat blossom petal in the world, deeper in colour at its base, with a notch in its tip
//
// Up close everything melts into bokeh. Depth is tested here against the scene, so particles fade where they come
// close to a surface instead of being cut off by it.

uniform sampler2D DepthSampler;

in vec2 vLocal;
flat in vec4 vBody;
flat in vec4 vGlow;
flat in vec4 vSize;
flat in vec4 vShape;
flat in float vFade;

out vec4 fragColor;

float linearDepth(float depth) {
    float n = AmbDepth.x, f = AmbDepth.y;
    return 2.0 * n * f / (f + n - (depth * 2.0 - 1.0) * (f - n));
}

vec2 turn(vec2 p, float angle) {
    float c = cos(angle), s = sin(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// The strength of the glow: none at a Glow of 0%, bright at 100%.
float glowStrength() {
    return AmbLook.x * 1.6 * vGlow.a;
}

// Out of focus: a soft, translucent disc of light, a touch brighter towards its rim, like bokeh in a photograph. It
// takes over as the blur grows past the size of the particle itself, and spreads the particle's light over its area.
vec4 bokeh(vec4 sharp, float r) {
    float body = vSize.x, radius = vSize.y;
    float amount = smoothstep(1.4, 3.2, radius / body);
    if (amount <= 0.0) {
        return sharp;
    }
    float disc = 1.0 - smoothstep(radius * 0.7, radius, r);
    float rim = smoothstep(radius * 0.3, radius * 0.85, r) * disc;
    float light = max(vBody.a, vGlow.a) * clamp(pow(body * 1.35 / radius, 0.8), 0.12, 1.0);
    vec4 soft = vec4(mix(vBody.rgb, vGlow.rgb, 0.55) * (disc * 0.8 + rim * 0.12) * light, disc * light * 0.08);
    return mix(sharp, soft, amount);
}

// ---------------------------------------------------------------------------------------------------------- shapes

vec4 orb(vec2 p) {
    float body = vSize.x;
    float r = length(p);
    // small ones are crisp dots; larger ones soften into a sphere of light, white-hot in its heart
    float core = mix(clamp(body - r + 0.5, 0.0, 1.0), exp(-r * r / (body * body * 0.55)), smoothstep(2.5, 8.0, body));
    float heart = exp(-r * r / (body * body * 0.22));
    // the glow: tight and bright around the body with a wide, faint tail, gone where its reach ends
    float g = ambienceGlowSize(body);
    float reach = ambienceReach(ORB, body, 0.0);
    float glow = (exp(-r * r / (g * g * 3.5)) * 1.1 + exp(-r / (reach * 0.3)) * 0.45) * (1.0 - smoothstep(reach * 0.55, reach, r));
    // the glow gives way to the body, so the body keeps its colour instead of burning out to white
    glow *= 1.0 - core * 0.5;
    vec3 rgb = mix(vBody.rgb, vec3(1.0), heart * 0.35) * core * vBody.a + vGlow.rgb * glow * glowStrength();
    return bokeh(vec4(rgb, core * vBody.a * 0.8), r);
}

vec4 sparkle(vec2 p) {
    float body = vSize.x, flare = vShape.z;
    float r = length(p);
    vec2 q = turn(p, vShape.x);
    float width = max(0.55, body * 0.28);
    float len = ambienceReach(SPARKLE, body, flare);
    // four long rays that taper to nothing at their ends, and four short ones between them
    vec2 a = abs(q);
    vec2 d = abs(vec2(q.x + q.y, q.x - q.y)) * 0.70710678;
    float rays = exp(-a.x / width - a.y * 2.5 / len) * (1.0 - smoothstep(0.6, 1.0, a.y / len))
        + exp(-a.y / width - a.x * 2.5 / len) * (1.0 - smoothstep(0.6, 1.0, a.x / len))
        + 0.3 * (exp(-d.x / width - d.y * 6.0 / len) + exp(-d.y / width - d.x * 6.0 / len));
    float core = clamp(body - r + 0.5, 0.0, 1.0);
    float heart = exp(-r * r / (body * body * 0.8));
    float halo = exp(-r / (body * 1.8)) * (1.0 - smoothstep(len * 0.4, len * 0.8, r)) * 0.35;
    float bright = 1.0 + flare;
    vec3 rgb = mix(vBody.rgb, vec3(1.0), 0.5) * (core * 0.7 + heart * 0.5) * vBody.a * bright
        + vGlow.rgb * (rays * 0.8 + halo) * glowStrength() * bright;
    return bokeh(vec4(rgb, core * vBody.a * 0.8), r);
}

// A stellar snow crystal of radius 1: six arms with two pairs of branches each, around a small hexagonal plate.
float crystal(vec2 q, float px) {
    float r = length(q);
    if (r > 1.15) {
        return 0.0;
    }
    // fold into one twelfth: six arms, each mirrored
    const float SECTOR = 1.04719755;
    float a = atan(q.y, q.x);
    a = mod(a + SECTOR * 0.5, SECTOR) - SECTOR * 0.5;
    vec2 f = vec2(cos(a), abs(sin(a))) * r;
    float d = segment(f, vec2(0.0), vec2(1.0, 0.0)) - 0.06;
    d = min(d, segment(f, vec2(0.42, 0.0), vec2(0.58, 0.28)) - 0.045);
    d = min(d, segment(f, vec2(0.70, 0.0), vec2(0.80, 0.17)) - 0.04);
    float hexagon = dot(f, vec2(0.8660254, 0.5));
    d = min(d, hexagon - 0.12);
    d = min(d, abs(hexagon - 0.25) - 0.025);
    return clamp(0.5 - d / px, 0.0, 1.0);
}

vec4 flake(vec2 p) {
    float body = vSize.x, glint = 1.0 + vShape.z;
    float r = length(p);
    // far away a soft round flake; up close its crystal
    float shape = max(clamp(body * 0.8 - r + 0.5, 0.0, 1.0) * 0.8, exp(-r * r / (body * body * 0.7)));
    float detail = smoothstep(5.0, 12.0, body);
    if (detail > 0.0) {
        shape = mix(shape, crystal(turn(p, vShape.x) / body, 1.0 / body), detail);
    }
    float reach = ambienceReach(FLAKE, body, 0.0);
    float halo = exp(-r / (ambienceGlowSize(body) * 1.1)) * (1.0 - smoothstep(reach * 0.55, reach, r)) * 0.35;
    vec3 rgb = vBody.rgb * shape * vBody.a * glint + vGlow.rgb * halo * glowStrength();
    return bokeh(vec4(rgb, shape * vBody.a * 0.9), r);
}

// Half the width of a petal at y (0 at the base, 1 at the tip): narrow at the base, widest two thirds up, rounded
// towards the tip.
float petalWidth(float y) {
    if (y <= 0.0 || y >= 1.0) {
        return -0.05;
    }
    float grow = pow(sin(1.5707963 * min(y / 0.64, 1.0)), 0.85);
    float rounded = y > 0.64 ? sqrt(max(1.0 - pow((y - 0.64) / 0.36, 2.0), 0.0)) : 1.0;
    return 0.95 * grow * rounded;
}

vec4 petal(vec2 p) {
    // p runs along the petal from its base (y = -1) to its tip (y = 1); across it, 1 is the widest point
    float y = p.y * 0.5 + 0.5;
    float d = abs(p.x) - petalWidth(y);
    // the notch in the tip of a blossom petal
    d = max(d, 0.14 - length(vec2(p.x * 0.85, y - 1.03)));
    // an edge of one pixel, softer out of focus
    float cover = clamp(0.5 - d / (fwidth(d) + vSize.z * 0.35 + 1.0e-4), 0.0, 1.0);

    // deeper at the base and lighter towards the rim, with a faint vein; brighter as it turns towards you, and a
    // glint when it faces you squarely
    vec3 rgb = mix(vGlow.rgb * 0.85, vBody.rgb, smoothstep(0.0, 0.8, y));
    rgb += vBody.rgb * 0.1 * exp(-abs(p.x) / 0.07) * (1.0 - y);
    rgb = mix(rgb, vBody.rgb * 1.1, smoothstep(-0.2, 0.0, d) * 0.3);
    float facing = vShape.w;
    rgb *= (0.82 + 0.18 * facing) * (gl_FrontFacing ? 1.0 : 0.9);
    rgb += vec3(pow(facing, 14.0) * AmbLook.z * 0.3);

    vec4 color = vec4(rgb, 1.0) * cover * vBody.a;
    color.rgb += vGlow.rgb * exp(-max(d, 0.0) * 10.0) * (1.0 - cover) * 0.7 * glowStrength();
    // out of focus it softens and thins
    return color * (1.0 - vSize.z * 0.45);
}

// ---------------------------------------------------------------------------------------------------------- main

void main() {
    int shape = int(vSize.w + 0.5);
    // shapes first: the petal's edge needs derivatives, which a discard before it would spoil
    vec4 color;
    if (shape == ORB) {
        color = orb(vLocal);
    } else if (shape == SPARKLE) {
        color = sparkle(vLocal);
    } else if (shape == FLAKE) {
        color = flake(vLocal);
    } else {
        color = petal(vLocal);
    }

    // hidden behind the world, and fading where it comes close to a surface (a petal lies right on it)
    float scene = linearDepth(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float mine = linearDepth(gl_FragCoord.z);
    float visible = shape == PETAL ? step(mine, scene + 0.04) : clamp((scene - mine) / AmbDepth.z, 0.0, 1.0);
    color *= visible * vFade;

    float light = max(color.r, max(color.g, color.b));
    if (light < 0.5 / 255.0 && color.a < 0.5 / 255.0) {
        discard;
    }
    // dither away banding in the soft glows
    color.rgb += (hash12(gl_FragCoord.xy) - 0.5) * (1.0 / 255.0) * step(1.0 / 255.0, light);
    // Fabulous graphics composites layers by alpha and skips pixels without any: pure glow keeps a trace of it
    color.a = max(color.a, 1.0 / 255.0);
    fragColor = max(color, vec4(0.0));
}
