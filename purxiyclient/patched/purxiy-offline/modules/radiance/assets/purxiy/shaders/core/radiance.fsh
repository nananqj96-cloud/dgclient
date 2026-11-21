#version 330

#moj_import <purxiy:radiance.glsl>

// Radiance: the light around what radiates, in one pass over the rectangles where there can be any (see
// dev.purxiy.radiance.PurxiyRadiance). The distance field tells every pixel the way to the nearest edge, so:
//
//   line      a crisp line along the outside of each silhouette, anti-aliased at any width
//   halo      soft light around it, falling off with the true distance
//   wisps     (Aura) light that rises off them, longest where they face up, flowing like heat
//   inside    a soft tint over them, lit along their edges (Fill)
//   colours   the palette flowing around each of them, a sheen of light that orbits them, a gentle breath
//   sparkles  tiny stars that twinkle on the line
//   events    a flash and a ripple of light when they are hit; a front of light rising over them as they appear
//
// Where something stands in front of something else, its light is drawn over the one behind it.

uniform sampler2D MaskSampler;
uniform sampler2D FieldSampler;

out vec4 fragColor;

const float TAU = 6.28318530718;
const vec3 WHITE = vec3(1.0);

vec4 slotRect(int s) {
    return RadianceSlots[s * 4];
}

vec4 slotTint(int s) {
    return RadianceSlots[s * 4 + 1];
}

vec4 slotLife(int s) {
    return RadianceSlots[s * 4 + 2];
}

vec4 slotShape(int s) {
    return RadianceSlots[s * 4 + 3];
}

// ---------------------------------------------------------------------------------------------------------- noise

// Hashes by Dave Hoskins (MIT).
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash32(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yzz) * p3.zyx);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = p - i;
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x), mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

// ---------------------------------------------------------------------------------------------------------- colour

// The palette as a loop: first, second, accent and back to the first.
vec3 palette(float t) {
    t = fract(t) * 3.0;
    float f = smoothstep(0.0, 1.0, fract(t));
    if (t < 1.0) {
        return mix(RadianceFirst.rgb, RadianceSecond.rgb, f);
    }
    if (t < 2.0) {
        return mix(RadianceSecond.rgb, RadianceAccent.rgb, f);
    }
    return mix(RadianceAccent.rgb, RadianceFirst.rgb, f);
}

// Where a point lies around a slot: the angle around its centre (0..1), in a frame stretched to its rectangle, so the
// colours spread evenly around tall and wide things alike.
float around(int s, vec2 p) {
    vec4 r = slotRect(s);
    vec2 d = (p - (r.xy + r.zw) * 0.5) / max(r.zw - r.xy, vec2(1.0));
    return atan(d.y, d.x) / TAU;
}

// The colour of a slot at an angle around it: the palette flowing around, tinted (health, rarity).
vec3 colourAt(int s, float angle, float shift) {
    vec3 c = palette(angle + RadianceScreen.w * 0.09 + slotShape(s).z + shift);
    vec4 tint = slotTint(s);
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return mix(c, tint.rgb * (0.78 + 0.3 * luma), tint.w);
}

// A narrow sheen of light that orbits each of them, once every few seconds.
float sheen(int s, float angle) {
    float band = 0.5 + 0.5 * cos(TAU * (angle - RadianceScreen.w * 0.15 - slotShape(s).z * 3.0));
    return pow(band, 30.0);
}

// How much of a slot shows at p: its presence, and while it appears, a front of light rising over it (below the front
// it shows; at the front it glows, which front receives).
float shown(int s, vec2 p, out float front) {
    float presence = slotLife(s).x * RadianceFirst.w;
    float amount = presence;
    front = 0.0;
    float reveal = slotShape(s).x;
    if (reveal < 0.999) {
        vec4 r = slotRect(s);
        float h = max(r.w - r.y, 1.0);
        float soft = 0.08 * h + 6.0 * RadianceScreen.z;
        float y = mix(r.y - soft, r.w + soft, reveal);
        amount *= 1.0 - smoothstep(y - soft, y, p.y);
        float x = (p.y - (y - soft * 0.5)) / soft;
        front = exp(-x * x * 2.0) * (1.0 - reveal) * presence;
    }
    return amount;
}

// ---------------------------------------------------------------------------------------------------------- shapes

// Light of the halo at distance e beyond the line: a soft core with a longer tail, gone at the reach.
float haloFalloff(float e, float reach) {
    float x = max(e, 0.0) / max(reach, 1.0);
    return (0.55 * exp(-x * x * 6.0) + 0.45 * exp(-x * 4.0)) * (1.0 - smoothstep(0.62, 1.0, x));
}

// Tiny four-pointed stars that flare now and then on the line: one per cell of a grid around the slot's centre (so
// they travel with it), each on a clock of its own.
float sparkles(int s, vec2 p, float e, float px) {
    vec4 r = slotRect(s);
    vec2 q = (p - (r.xy + r.zw) * 0.5) / px;
    const float CELL = 18.0;
    vec2 cell = floor(q / CELL);
    float seed = slotShape(s).z * 97.0;
    float light = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 id = cell + vec2(x, y);
            vec3 h = hash32(id + seed);
            float life = fract(RadianceScreen.w * (0.2 + 0.18 * h.z) + h.x * 7.0);
            float on = smoothstep(0.0, 0.06, life) * (1.0 - smoothstep(0.06, 0.26, life));
            if (on <= 0.0) {
                continue;
            }
            vec2 d = q - (id + 0.2 + 0.6 * h.xy) * CELL;
            float size = 3.4 + 3.0 * h.z;
            float arms = max(exp(-abs(d.y) * 1.5 - abs(d.x) / size), exp(-abs(d.x) * 1.5 - abs(d.y) / size));
            light = max(light, (arms + 1.2 * exp(-dot(d, d) * 0.4)) * on);
        }
    }
    // they sit on the line
    float band = (e - RadianceLook.x * 0.5) / (5.5 * px);
    return light * exp(-band * band);
}

// ---------------------------------------------------------------------------------------------------------- light

// Premultiplied "src over dst".
vec4 over(vec4 dst, vec4 src) {
    return src + dst * (1.0 - src.a);
}

// The light of slot s at p, e pixels outside its edge, away the unit direction from it.
vec4 outside(int s, vec2 p, float e, vec2 away, float px) {
    float front;
    float amount = shown(s, p, front);
    if (amount <= 0.001 && front <= 0.001) {
        return vec4(0.0);
    }
    vec4 life = slotLife(s);
    vec4 shape = slotShape(s);
    float scale = shape.y;
    float focus = life.y;
    float flash = life.z;
    float angle = around(s, p);
    vec3 colour = colourAt(s, angle, 0.0);
    float lit = sheen(s, angle);
    float breath = 0.9 + 0.1 * sin(RadianceScreen.w * 1.8 + shape.z * TAU);
    float strength = RadianceGlow.z * breath * (1.0 + 0.35 * focus + 1.4 * flash);
    float width = RadianceLook.x * mix(0.7, 1.0, scale);

    vec4 result = vec4(0.0);

    // the halo
    float halo = RadianceLook.z * haloFalloff(e - width * 0.5, RadianceGlow.x * scale) * strength;
    if (halo > 0.0005) {
        vec3 c = mix(colourAt(s, angle, 0.06), WHITE, 0.1 + 0.3 * lit + 0.3 * flash);
        float a = min(halo * 0.95, 1.0);
        result = over(result, vec4(c * a, a * 0.5));
    }

    // wisps: tongues of light that rise off them, longest on top, flowing up like heat, hot at the edge and cooling into
    // the accent at their tips
    if (RadianceLook.w > 0.001) {
        vec4 r = slotRect(s);
        vec2 q = (p - (r.xy + r.zw) * 0.5) / (px * max(scale, 0.4));
        float t = RadianceScreen.w;
        float n = noise(vec2(q.x * 0.055 + shape.z * 31.0, q.y * 0.03 - t * 1.25)) * 0.6
            + noise(vec2(q.x * 0.14 - shape.z * 17.0, q.y * 0.075 - t * 2.3)) * 0.4;
        n = smoothstep(0.18, 0.86, n);
        float up = max(away.y, 0.0);
        float reach = RadianceGlow.y * scale * (0.25 + n) * mix(0.55, 1.25, up) * (1.0 + 0.3 * flash);
        float x = max(e, 0.0) / max(reach, 1.0);
        float tongue = exp(-x * x * 2.4) * (0.3 + 0.7 * n);
        float core = exp(-max(e, 0.0) / (2.4 * px));
        float w = RadianceLook.w * (tongue + core * 0.6) * strength;
        vec3 cool = mix(colourAt(s, angle, 0.1), RadianceAccent.rgb, smoothstep(0.2, 0.9, x) * 0.6);
        vec3 c = mix(cool, mix(RadianceSecond.rgb, WHITE, 0.6), core * 0.75 + lit * 0.3);
        float a = min(w * 1.05, 1.0);
        result = over(result, vec4(c * a, a * 0.45));
    }

    // a ripple: a ring of light that runs out when they are hit
    float ripple = life.w;
    if (ripple < 0.999) {
        // quick at first, then slowing as it fades
        float run = 1.0 - (1.0 - ripple) * (1.0 - ripple);
        float radius = width + run * 46.0 * px * max(scale, 0.5);
        float ring = (e - radius) / (3.4 * px);
        float a = min(exp(-ring * ring) * pow(1.0 - ripple, 1.3) * 1.15, 1.0);
        result = over(result, vec4(mix(RadianceAccent.rgb, WHITE, 0.5) * a, a * 0.5));
    }

    // the line
    float cover = clamp(width + 0.5 - e, 0.0, 1.0) * RadianceLook.y;
    if (cover > 0.0) {
        vec3 c = mix(colour, WHITE, 0.28 + 0.5 * lit + 0.55 * flash + 0.15 * focus);
        float a = cover * min(1.0, 0.85 + 0.15 * strength);
        result = over(result, vec4(c * a, a * 0.9));
    }

    // sparkles on the line
    if (RadianceAccent.w > 0.001 && e < width + 10.0 * px) {
        float spark = sparkles(s, p, e, px) * RadianceAccent.w;
        if (spark > 0.001) {
            float a = min(spark, 1.0);
            result = over(result, vec4(mix(colour, WHITE, 0.7) * a, a * 0.6));
        }
    }

    result *= amount;
    // the front of light rising over them as they appear
    if (front > 0.001) {
        float a = min(front * (cover + halo * 0.8) * 1.2, 1.0);
        result = over(result, vec4(mix(colour, WHITE, 0.6) * a, a * 0.5));
    }
    return result;
}

// The inside of slot s at p, e pixels from its edge (rim false where that edge is not its own): a soft tint, lit along
// its edges, and the flash of a hit.
vec4 inside(int s, vec2 p, float e, bool rim, float px) {
    float front;
    float amount = shown(s, p, front);
    if (amount <= 0.001) {
        return vec4(0.0);
    }
    vec4 life = slotLife(s);
    vec3 colour = colourAt(s, around(s, p), 0.2);
    float edge = rim ? exp(-e / (3.0 * px)) : 0.0;
    float soft = rim ? exp(-e / (22.0 * px)) : 0.35;
    float a = (RadianceGlow.w * (0.1 + 0.3 * soft + 0.75 * edge) + life.z * 0.3) * amount;
    if (a <= 0.001) {
        return vec4(0.0);
    }
    a = min(a, 1.0);
    return vec4(mix(colour, WHITE, 0.15 + 0.4 * life.z) * a, a * 0.35);
}

// ---------------------------------------------------------------------------------------------------------- main

void main() {
    vec2 p = gl_FragCoord.xy;
    ivec2 ip = ivec2(p);
    float px = RadianceScreen.z;
    int own = radianceByte(texelFetch(MaskSampler, ip, 0).r);
    vec4 texel = texelFetch(FieldSampler, ip, 0);
    bool near = texel.w > 0.5;
    if (own == 0 && !near) {
        discard;
    }

    int slot = near ? radianceByte(texel.z) : 0;
    vec2 offset = near ? radianceOffset(texel) : vec2(0.0);
    // where the line is drawn, one more step of the flood makes the way to the edge exact
    float band = RadianceLook.x + 4.0 * px;
    float best = dot(offset, offset);
    if (near && best < band * band) {
        ivec2 limit = ivec2(RadianceScreen.xy) - 1;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                ivec2 q = ip + ivec2(x, y);
                if ((x == 0 && y == 0) || q.x < 0 || q.y < 0 || q.x > limit.x || q.y > limit.y) {
                    continue;
                }
                vec4 other = texelFetch(FieldSampler, q, 0);
                vec2 o = radianceOffset(other) + vec2(x, y);
                float d = dot(o, o);
                if (other.w > 0.5 && d < best) {
                    best = d;
                    offset = o;
                    slot = radianceByte(other.z);
                }
            }
        }
    }
    float dist = sqrt(best);

    vec4 result = vec4(0.0);
    if (own != 0) {
        // its own tint, lit along its edge where the nearest edge is its own
        result = inside(own, p, dist + 0.5, near && slot == own, px);
    }
    // the light of the nearest edge: around it, and over whatever stands behind it
    if (near && (own == 0 || (slot != own && radianceDepth(slot) < radianceDepth(own)))) {
        vec2 away = dist > 0.001 ? -offset / dist : vec2(0.0, 1.0);
        result = over(result, outside(slot, p, dist - 0.5, away, px));
    }

    if (result.a <= 0.002 && max(max(result.r, result.g), result.b) <= 0.002) {
        discard;
    }
    // a little dither keeps the soft light from banding over dark scenes
    float dither = fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
