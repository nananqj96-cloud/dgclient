#version 330

#moj_import <purxiy:mist.glsl>
#moj_import <purxiy:mist_field.glsl>

// The mist laid over the picture, at full size. Each pixel takes the march of the grid texels around it whose distance
// matches its own (so a near edge keeps clear of the mist behind it); a pixel the grid does not see at all (a fence post
// or a blade of grass in front of the far distance) marches its own ray, with fewer steps. Players and mobs can be let
// through more clearly, and with a shader pack a mask keeps your hand as it is. Blended over the picture with
// premultiplied alpha: picture * (1 - a) + rgb.

uniform sampler2D DepthSampler;
uniform sampler2D MarchSampler;
#ifdef CREATURES
uniform sampler2D TerrainDepthSampler;
uniform sampler2D OpaqueDepthSampler;
#endif
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
#ifdef MASKED
    if (texelFetch(HandMaskSampler, p, 0).r > 0.5) {
        fragColor = vec4(0.0);
        return;
    }
#endif
    float depth = texelFetch(DepthSampler, p, 0).r;
    bool sky = depth >= 0.999999;
    float linear = mistLinear(depth);
    // right around you the air is clear (the mist there stays below one part in 400)
    if (!sky && linear < 0.3 * MistReach.x && int(MistDepth.w + 0.5) == 0) {
        fragColor = vec4(0.0);
        return;
    }

    // the grid texels around: texel q was marched for pixel 2q
    vec2 h = vec2(p) * 0.5;
    ivec2 b = ivec2(floor(h));
    vec2 f = h - vec2(b);
    ivec2 grid = ivec2(MistScreen.zw);
    ivec2 last = ivec2(MistScreen.xy) - 1;
    vec4 sum = vec4(0.0);
    float total = 0.0;
    float best = 1.0e9;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            // a tent a little wider than a bilinear filter (it smooths the fine grain of the march), sharpened by how well
            // the distance matches; texels it gives no weight are not read at all
            vec2 d = abs(vec2(b + ivec2(i, j)) - h);
            float tent = max(0.0, 1.5 - d.x) * max(0.0, 1.5 - d.y);
            if (tent <= 0.0) {
                continue;
            }
            ivec2 q = clamp(b + ivec2(i, j), ivec2(0), grid - 1);
            float qDepth = texelFetch(DepthSampler, min(q * 2, last), 0).r;
            float off = abs(mistLinear(qDepth) - linear) / max(linear, 0.5);
            if (sky) {
                off = qDepth >= 0.999999 ? 0.0 : 1.0;
            }
            best = min(best, off);
            float w = tent * exp(-off * off * 600.0);
            sum += texelFetch(MarchSampler, q, 0) * w;
            total += w;
        }
    }
    vec4 fog;
    bool own = best > 0.08 || total < 1.0e-3;
    if (own) {
        vec2 uv = (vec2(p) + 0.5) / MistScreen.xy;
        vec3 rel = mistPosition(uv, sky ? 0.9999 : depth);
        float distance = length(rel);
        float far = sky ? MistReach.y : min(distance, MistReach.y);
        fog = mistMarch(rel / max(distance, 1.0e-4), far, 12, mistDither(gl_FragCoord.xy));
    } else {
        fog = sum / total;
    }

#ifdef CREATURES
    // players and mobs: in front of the terrain, with nothing translucent in front of them, the mist is thinner
    float terrain = texelFetch(TerrainDepthSampler, p, 0).r;
    float opaque = texelFetch(OpaqueDepthSampler, p, 0).r;
    if (!sky && abs(depth - opaque) < 1.0e-7 && opaque < terrain - 1.0e-6 && fog.a > 0.001) {
        float through = 1.0 - fog.a;
        float clearer = 1.0 - pow(max(through, 1.0e-4), 1.0 - 0.7 * MistCreatures.x);
        fog *= clearer / fog.a;
    }
#endif

    int debug = int(MistDepth.w + 0.5);
    if (debug >= 4) {
        // development: what the mist knows of the land, at the surface each pixel shows (4 the mist's floor in bands of 8
        // blocks, red where unknown; 5 sunlit; 6 dry), or the maps themselves in the corner (7: ground, floor, shade)
        if (debug == 7 && gl_FragCoord.x < MIST_SIZE && gl_FragCoord.y < MIST_SIZE) {
            // the maps around the camera (it is in the middle; up is +z): ground relative to the camera (red above,
            // blue below, in bands), the mist's floor (green bands), unknown columns dark
            ivec2 at = ivec2(mod(floor(MistCamera.xz) + gl_FragCoord.xy - MIST_SIZE * 0.5, MIST_SIZE));
            vec4 t = texelFetch(TerrainSampler, at, 0);
            vec4 fl = texelFetch(FieldSampler, at, 0);
            float g = MistCamera.w + t.r * 255.0 - MistCamera.y;
            float f = MistCamera.w + fl.r * 255.0 - MistCamera.y;
            vec3 c = vec3(clamp(g / 32.0, 0.0, 1.0), 0.35 + 0.65 * fract(f / 8.0), clamp(-g / 32.0, 0.0, 1.0)) * t.a;
            if (length(gl_FragCoord.xy - MIST_SIZE * 0.5) < 2.0) {
                c = vec3(1.0);
            }
            fragColor = vec4(c, 1.0);
            return;
        }
        vec2 uv = (vec2(p) + 0.5) / MistScreen.xy;
        vec3 world = MistCamera.xyz + mistPosition(uv, min(depth, 0.9999));
        vec4 raw = texture(TerrainSampler, world.xz / MIST_SIZE);
        vec4 field = texture(FieldSampler, world.xz / MIST_SIZE);
        float floorHeight = MistCamera.w + field.r * 255.0;
        float ground = MistCamera.w + raw.r * 255.0 / max(raw.a, 0.02);
        float shade = MistCamera.w + field.g * 255.0;
        if (debug == 4) {
            fragColor = vec4(vec3(fract(floorHeight / 8.0)) * vec3(1.0, raw.a, raw.a), 1.0);
        } else if (debug == 5) {
            fragColor = vec4(vec3(smoothstep(shade - 1.2, shade + 0.8, world.y)), 1.0);
        } else if (debug == 6) {
            fragColor = vec4(vec3(smoothstep(ground - 2.5, ground + 0.35, world.y)), 1.0);
        }
#ifdef LIGHTS
        if (debug == 8) {
            fragColor = vec4(vec3(mistLantern(world, world - MistCamera.xyz)), 1.0);
            return;
        }
#endif
        if (debug <= 8) {
            return;
        }
    }
    if (debug == 1) {
        fragColor = vec4(vec3(fog.a), 1.0);
        return;
    } else if (debug == 2) {
        fragColor = vec4(fog.rgb, 1.0);
        return;
    } else if (debug == 3) {
        fragColor = vec4(own ? 1.0 : 0.0, fog.a, 0.0, 1.0);
        return;
    }
    // a little noise against banding (the grid holds 8 bits per channel)
    float grain = (mistDither(gl_FragCoord.xy + 17.0) - 0.5) / 255.0;
    fragColor = vec4(max(fog.rgb + grain, 0.0), fog.a);
}
