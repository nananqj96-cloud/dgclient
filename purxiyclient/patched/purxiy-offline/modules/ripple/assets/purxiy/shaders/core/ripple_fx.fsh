#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:ripple_fx.glsl>

// The light Ripple sets free (see dev.purxiy.ripple.RippleFx), premultiplied: bodies of light cover a little, glow adds.
//
//   star     a critical hit: four long rays and four short ones around a white-hot dot, pink-white, flying off
//   sparkle  an enchanted hit: a small glint in the palette that twinkles as it drifts up
//   pillar   a kill: a beam that shoots up into the sky from where they fell, a white core in a soft glow, then thins
//   ring     a kill: a ring of light that rolls out over the ground and fades
//   flare    a kill: a soft burst of light where the body stood
//
// Depth is tested here, against the scene's depth texture, so what stands in front covers the light softly.

uniform sampler2D DepthSampler;
#ifdef MASKED
// your hands as they are now (with a shader pack, whose image already holds them and whose depth is its own)
uniform sampler2D HandMaskSampler;
#endif

in vec2 local;
in vec3 world;
flat in int kind;
flat in vec4 piece;
flat in float scale;

out vec4 fragColor;

const float TAU = 6.28318530718;

// How much of something this far away the scene leaves visible here, soft over {@code soft} blocks.
float visibility(float away, float soft) {
    vec2 ndc = gl_FragCoord.xy / FxScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = FxInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    float sceneDistance = length(scene.xyz / scene.w);
    return clamp((sceneDistance - away) / soft + 1.0, 0.0, 1.0);
}

vec2 turn(vec2 p, float angle) {
    float c = cos(angle), s = sin(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// The palette at a point around a ring (turns): three soft lobes, the accent a little quieter.
vec3 paletteAt(float turns) {
    float a = turns * TAU;
    vec3 w = max(vec3(cos(a), cos(a - TAU / 3.0), cos(a + TAU / 3.0)), 0.0);
    w *= w * vec3(1.0, 1.0, 0.75);
    return (FxFirst.rgb * w.x + FxSecond.rgb * w.y + FxAccent.rgb * w.z) / (w.x + w.y + w.z);
}

// Four long rays and four short ones around a dot, in pixels: the dot is {@code core}, the rays {@code tint}.
vec4 glint(vec2 p, float body, float len, vec3 core, vec3 tint) {
    float r = length(p);
    float width = max(0.55, body * 0.3);
    vec2 a = abs(p);
    vec2 d = abs(vec2(p.x + p.y, p.x - p.y)) * 0.70710678;
    float rays = exp(-a.x / width - a.y * 2.5 / len) * (1.0 - smoothstep(0.6, 1.0, a.y / len))
        + exp(-a.y / width - a.x * 2.5 / len) * (1.0 - smoothstep(0.6, 1.0, a.x / len))
        + 0.3 * (exp(-d.x / width - d.y * 6.0 / len) + exp(-d.y / width - d.x * 6.0 / len));
    float dot = clamp(body - r + 0.5, 0.0, 1.0);
    float heart = exp(-r * r / (body * body * 0.8));
    float halo = exp(-r / (body * 1.8)) * (1.0 - smoothstep(len * 0.4, len * 0.8, r)) * 0.35;
    vec3 rgb = core * (dot * 0.75 + heart * 0.55) + tint * (rays * 0.85 + halo);
    return vec4(rgb, dot * 0.7);
}

vec4 star() {
    float life = piece.y;
    float flare = piece.w;
    // pops out bright with long rays, then shrinks and fades as it slows
    float pop = smoothstep(0.0, 0.08, life);
    float size = (0.55 + 0.45 * pop) * (1.0 - 0.4 * life);
    float body = max(piece.x * scale, 0.9) * size;
    float len = body * (4.4 + 2.6 * flare) * (0.75 + 0.5 * (1.0 - life));
    float twinkle = 0.85 + 0.15 * sin(life * 21.0 + flare * 6.2831) * FxLook.y;
    float bright = (1.0 - smoothstep(0.5, 1.0, life)) * twinkle * (1.0 + 0.8 * (1.0 - pop));
    vec3 core = mix(FxAccent.rgb, vec3(1.0), 0.72);
    vec3 tint = mix(FxAccent.rgb, FxFirst.rgb, 0.3 + 0.3 * flare);
    return glint(turn(local, piece.z), body, len, core, tint) * bright;
}

vec4 sparkle() {
    float life = piece.y;
    float flare = piece.w;
    float body = max(piece.x * scale, 0.8);
    float len = body * (3.4 + 1.6 * flare);
    float twinkle = 0.7 + 0.3 * sin(life * 17.0 + flare * 6.2831) * FxLook.y;
    float bright = pow(sin(3.14159265 * clamp(life, 0.0, 1.0)), 0.7) * twinkle;
    vec3 core = mix(FxFirst.rgb, vec3(1.0), 0.6);
    vec3 tint = mix(FxFirst.rgb, FxSecond.rgb, flare);
    return glint(turn(local, piece.z), body, len, core, tint) * bright * 0.9;
}

vec4 flare() {
    float life = piece.y;
    float radius = max(piece.x * scale, 1.0);
    float r = length(local);
    float bright = smoothstep(0.0, 0.06, life) * pow(1.0 - life, 2.0);
    float core = exp(-r * r / (radius * radius * 0.07));
    float glow = exp(-r * r / (radius * radius * 0.55)) * (1.0 - smoothstep(radius * 1.6, radius * 2.4, r));
    vec3 rgb = vec3(1.0) * core * 0.9 + mix(FxFirst.rgb, FxAccent.rgb, 0.25) * glow * 0.8;
    return vec4(rgb, core * 0.25 + glow * 0.08) * bright;
}

vec4 pillar() {
    float height = piece.x;
    float width = piece.y;
    float life = piece.z;
    float age = life * PILLAR_LIFE;
    float y = local.y;
    float x = abs(local.x);
    // it shoots up fast and slows as it reaches the sky
    float shoot = clamp(age / 0.34, 0.0, 1.0);
    float top = height * (1.0 - pow(1.0 - shoot, 4.0));
    if (y > top + 3.0) {
        return vec4(0.0);
    }
    // a white core in a soft glow, thinning as it fades
    float thin = 1.0 - 0.6 * smoothstep(0.25, 1.0, life);
    float coreWidth = (0.04 + 0.05 * width) * thin;
    float glowWidth = (0.2 + 0.25 * width) * (0.75 + 0.25 * thin);
    float halfWidth = 0.55 + width * 0.9;
    float core = exp(-x * x / (coreWidth * coreWidth));
    float glow = exp(-x / glowWidth) * (1.0 - smoothstep(halfWidth * 0.55, halfWidth, x));
    // brightest low, fading towards the sky; soft at its top and where it meets the ground
    float along = mix(1.0, 0.3, smoothstep(0.0, height * 0.8, y));
    float end = 1.0 - smoothstep(top - 4.0, top + 0.5, y);
    float ground = smoothstep(0.0, 0.35, y);
    float alive = smoothstep(0.0, 0.03, age) * (1.0 - smoothstep(0.3, 1.0, life));
    float flow = 1.0 + 0.16 * sin(y * 1.8 - age * 15.0) * FxLook.y;
    float body = along * end * ground * flow * alive;
    // the head of the beam as it shoots up
    float tip = exp(-pow((y - top) / 1.6, 2.0)) * (1.0 - smoothstep(0.18, 0.45, age)) * 1.1 * alive;
    // a kill is usually right in front of you: up close the beam is quieter, so it never blinds
    float near = mix(0.55, 1.0, smoothstep(2.0, 9.0, scale));
    float coreLight = core * (0.85 * body + tip) * near;
    float glowLight = glow * (0.6 * body + tip) * near;
    vec3 coreColor = mix(FxFirst.rgb, vec3(1.0), 0.45);
    vec3 glowColor = mix(mix(FxFirst.rgb, FxSecond.rgb, 0.5), FxSecond.rgb, smoothstep(0.0, height * 0.5, y));
    glowColor = mix(glowColor, FxAccent.rgb, 0.25 * (0.5 + 0.5 * sin(y * 0.35 - age * 2.5)));
    return vec4(coreColor * coreLight + glowColor * glowLight, coreLight * 0.3);
}

vec4 ring() {
    float reach = piece.x;
    float life = piece.y;
    float eased = 1.0 - pow(1.0 - life, 3.0);
    float radius = reach * (0.06 + 0.94 * eased);
    float r = length(local);
    float d = r - radius;
    float px = max(fwidth(r), 1.0e-5);
    // the line: never thinner than a pixel; thinner lines fade instead of breaking up
    float halfWidth = mix(0.075, 0.022, eased);
    float hw = halfWidth / px;
    float line = clamp(max(hw, 0.5) + 0.5 - abs(d) / px, 0.0, 1.0) * min(hw * 2.0, 1.0);
    float gx = max(abs(d) - halfWidth, 0.0) / 0.5;
    float glow = (0.6 * exp(-gx * gx * 9.0) + 0.4 * exp(-gx * 4.5)) * (1.0 - smoothstep(0.6, 1.0, gx));
    float inside = max(-d - halfWidth, 0.0) / max(radius * 0.35, 1.0e-3);
    float wake = d < 0.0 ? pow(max(1.0 - inside, 0.0), 2.0) * (1.0 - life) * (1.0 - life) : 0.0;
    float fade = pow(1.0 - life, 1.3) * smoothstep(0.0, 0.03, life);
    vec3 tint = paletteAt(atan(local.y, local.x) / TAU + piece.z + 0.15 * eased * FxLook.y);
    float lineAlpha = line * 0.95;
    float light = glow + wake * 0.6;
    vec4 result = vec4(mix(tint, vec3(1.0), 0.35) * lineAlpha, lineAlpha) + vec4(tint * light, light * 0.5) * (1.0 - lineAlpha);
    return result * fade;
}

void main() {
#ifdef MASKED
    if (texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    vec4 result;
    float away = length(world);
    float soft = 0.05 + away * 0.004;
    if (kind == STAR) {
        result = star();
        soft = 0.15 + away * 0.004;
    } else if (kind == SPARKLE) {
        result = sparkle();
        soft = 0.15 + away * 0.004;
    } else if (kind == PILLAR) {
        result = pillar();
        soft = 0.08 + away * 0.004;
    } else if (kind == RING) {
        result = ring();
    } else {
        result = flare();
        soft = 0.6 + away * 0.004;
    }
    result *= FxLook.x * visibility(away, soft);
    if (result.a <= 0.0 && result.r + result.g + result.b <= 0.001) {
        discard;
    }

    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(world), fog_cylindrical_distance(world), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
