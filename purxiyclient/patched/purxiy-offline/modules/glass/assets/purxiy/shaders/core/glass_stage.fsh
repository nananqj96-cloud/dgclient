#version 330

// The stage of Glass's preview in the menu (see dev.purxiy.glass.GlassPreview): the night-lilac studio of the other
// previews, with plenty for the glass figure in front of it to bend: a planet with its ring, a floor
// of fine lines running off to a glowing horizon, and a few quiet stars. Designed in lilac, drawn in the accent colour.

#moj_import <purxiy:theme.glsl>

layout(std140) uniform GlassStage {
    vec4 StageInfo;   // xy = size (px), z = time (s)
};

in vec2 uv;

out vec4 fragColor;

const vec3 LILAC = vec3(0.749, 0.561, 0.969);
const float HORIZON = 0.36;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453);
}

void main() {
    vec2 size = StageInfo.xy;
    float aspect = size.x / size.y;
    vec2 p = vec2(uv.x * aspect, uv.y);
    float px = 1.0 / size.y;
    float time = StageInfo.z;
    vec3 lilac = prx_theme(LILAC);

    // the sky: deep violet above, a lilac glow along the horizon
    vec3 top = prx_theme(vec3(0.066, 0.052, 0.118));
    vec3 low = prx_theme(vec3(0.235, 0.160, 0.400));
    float lift = smoothstep(HORIZON, 1.0, uv.y);
    vec3 color = mix(low, top, pow(lift, 0.7));
    color += lilac * 0.28 * exp(-abs(uv.y - HORIZON) * 16.0);

    // stars, twinkling slowly
    vec2 cell = floor(p * 38.0);
    float star = hash(cell);
    if (star > 0.93 && uv.y > HORIZON + 0.06) {
        vec2 at = (cell + 0.25 + 0.5 * vec2(hash(cell + 3.1), hash(cell + 7.7))) / 38.0;
        float d = length(p - at) / px;
        float twinkle = 0.6 + 0.4 * sin(time * (0.8 + star * 2.0) + star * 40.0);
        color += prx_theme(vec3(0.85, 0.8, 1.0)) * exp(-d * d * 0.35) * twinkle * (star - 0.93) * 12.0;
    }

    // the planet, up and to the right, lit from the left, with a halo and a thin tilted ring
    vec2 centre = vec2(aspect * 0.77, 0.69);
    float radius = 0.19;
    vec2 q = p - centre;
    float d = length(q);
    color += lilac * 0.22 * exp(-max(d - radius, 0.0) * 9.0);
    // the ring behind the planet
    vec2 r = vec2(q.x * 0.94 + q.y * 0.34, -q.x * 0.34 + q.y * 0.94);
    float ring = length(vec2(r.x, r.y * 3.4));
    float band = smoothstep(px * 2.0, 0.0, abs(ring - radius * 1.55) - 0.006);
    bool behind = r.y > 0.0;
    if (behind) {
        color = mix(color, prx_theme(vec3(0.95, 0.85, 1.0)), band * 0.75);
    }
    float body = smoothstep(radius + px, radius - px, d);
    if (body > 0.0) {
        vec3 n = vec3(q / radius, sqrt(max(1.0 - dot(q, q) / (radius * radius), 0.0)));
        float lit = clamp(dot(n, normalize(vec3(-0.65, 0.45, 0.6))), 0.0, 1.0);
        vec3 surface = mix(prx_theme(vec3(0.22, 0.13, 0.38)), prx_theme(vec3(0.93, 0.74, 1.0)), pow(lit, 0.8));
        // soft bands across it
        surface *= 0.9 + 0.1 * sin((q.y + q.x * 0.35) * 48.0);
        surface += lilac * 0.35 * pow(1.0 - n.z, 3.0);
        color = mix(color, surface, body);
    }
    if (!behind) {
        color = mix(color, prx_theme(vec3(0.97, 0.88, 1.0)), band * 0.85);
    }

    // the floor: fine lines running off to the horizon, fading as they go
    if (uv.y < HORIZON) {
        float below = HORIZON - uv.y;
        float z = 0.16 / below;
        float x = (p.x - aspect * 0.5) * z;
        float lx = abs(fract(x * 1.6) - 0.5) / (fwidth(x * 1.6) + 1.0e-5);
        float lz = abs(fract(z * 1.1 - time * 0.05) - 0.5) / (fwidth(z * 1.1) + 1.0e-5);
        float line = max(1.0 - smoothstep(0.0, 1.5, lx), 1.0 - smoothstep(0.0, 1.5, lz));
        float fade = smoothstep(0.0, 0.2, below) * exp(-z * 0.12);
        vec3 floorColor = mix(prx_theme(vec3(0.10, 0.075, 0.17)), prx_theme(vec3(0.05, 0.04, 0.09)), smoothstep(0.0, HORIZON, below));
        color = mix(color, floorColor, smoothstep(0.0, 0.015, below));
        color += lilac * line * fade * 0.55;
        // a soft pool of light where the figure stands
        vec2 pool = vec2((p.x - aspect * 0.46) * 1.6, (uv.y - HORIZON * 0.42) * 5.5);
        color += lilac * 0.12 * exp(-dot(pool, pool) * 3.0);
    }

    // a little dither keeps the gradients from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    fragColor = vec4(color + (dither - 0.5) / 255.0, 1.0);
}
