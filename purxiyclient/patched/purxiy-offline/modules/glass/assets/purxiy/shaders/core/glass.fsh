#version 330

#if defined(WORLD) && !defined(DEPTH)
#moj_import <minecraft:fog.glsl>
#endif
#moj_import <purxiy:glass.glsl>

// Glass (see dev.purxiy.glass.PurxiyGlass): a surface of a player or an item, drawn as glass over the image behind it
// (which never had the surface in it: vanilla left it out).
//
//   shape     only where its texture is see-through, so is the glass; nothing else of the texture shows
//   edges     near the edge of a shape (the side of a cube, the outline of an item's pixels) the surface rounds off, so
//             every block and every item looks cast in glass, with a thin bright rim where the edge is
//   behind    what stands behind is seen through it, bent by the surface (blue a little more than red), frosted as much
//             as the setting says, and tinted by the glass's colour; a little milky, lit by the light where it stands
//   mirror    the sky and a soft studio light reflected in it, most at grazing angles (Fresnel), and a glint of the sun
//   glow      its own light, brightest along its edges, as much as the setting says
//   shimmer   now and then a band of light glides over each body
//
// With BACK, only the far side of the glass (what faces away from you) is drawn, before the rest: its edges, faintly, which
// the glass in front then shows bent and frosted, like the inside of a block of glass. With DEPTH, only the depth is
// written, where it is what the scene shows: the glass is then drawn on the front-most surface only.

uniform sampler2D Sampler0;
#ifdef WORLD
uniform sampler2D DepthSampler;
#endif
#if !defined(DEPTH) && !defined(BACK)
uniform sampler2D SceneSampler;
uniform sampler2D FrostSampler;
#endif

in vec3 position;
in vec2 texCoord0;
in vec3 normal;
in vec3 lightColor;
in float skyLight;
flat in vec4 bounds;
flat in int body;
flat in int flags;

out vec4 fragColor;

// How the edges round off: how far in from the edge of a shape (texels) and how far they turn away right at the edge
// (radians).
const float BEVEL = 1.8;
const float TILT = 1.3;

#ifdef WORLD
// How far away the scene is at a point of the screen (blocks), from its depth.
float sceneDistance(vec2 uv) {
    float depth = textureLod(DepthSampler, uv, 0.0).r;
    vec4 scene = GlassInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return length(scene.xyz / scene.w);
}
#endif

#ifndef DEPTH
// Where the edge of the shape is, seen from t (texels): the distance to the nearest see-through texel or edge of the face
// (texels) and the way to it. Texels are cells: the distance runs to the nearest point of the nearest empty cell.
vec3 edgeField(vec2 t, vec2 lo, vec2 hi) {
    float best = t.x - lo.x;
    vec2 way = vec2(-1.0, 0.0);
    if (hi.x - t.x < best) {
        best = hi.x - t.x;
        way = vec2(1.0, 0.0);
    }
    if (t.y - lo.y < best) {
        best = t.y - lo.y;
        way = vec2(0.0, -1.0);
    }
    if (hi.y - t.y < best) {
        best = hi.y - t.y;
        way = vec2(0.0, 1.0);
    }
    ivec2 size = textureSize(Sampler0, 0);
    ivec2 cell = ivec2(floor(t));
    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            ivec2 c = cell + ivec2(x, y);
            vec2 low = vec2(c);
            // only the face's own cells: beyond them, its edge is nearer anyway
            if (low.x < lo.x - 0.01 || low.y < lo.y - 0.01 || low.x + 1.0 > hi.x + 0.01 || low.y + 1.0 > hi.y + 0.01) {
                continue;
            }
            if (texelFetch(Sampler0, clamp(c, ivec2(0), size - 1), 0).a >= 0.5) {
                continue;
            }
            vec2 d = t - clamp(t, low, low + 1.0);
            float len = length(d);
            if (len < best) {
                best = len;
                way = len > 1.0e-4 ? -d / len : way;
            }
        }
    }
    return vec3(best, way);
}

#ifndef BACK
// Where on the screen light from behind comes from, bent into the surface at this point.
vec2 bent(vec3 eye, vec3 n, float eta) {
    vec3 r = refract(eye, n, eta);
    vec4 clip = GlassClip * vec4(position + r * GlassLook.w, 1.0);
    return clamp(clip.xy / max(clip.w, 1.0e-4) * 0.5 + 0.5, vec2(0.0005), vec2(0.9995));
}
#endif
#endif

void main() {
    // how the surface runs across the screen, before anything is discarded
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 texel = texCoord0 * size;
    vec3 dpx = dFdx(position), dpy = dFdy(position);
    vec2 dtx = dFdx(texel), dty = dFdy(texel);

    vec4 surface = texture(Sampler0, texCoord0);
    if (surface.a < 0.1) {
        discard;
    }
    float away = length(position);
#ifdef WORLD
    // only where it is what the scene shows: not behind a wall
    float tolerance = 0.035 + away * 0.0025;
    if (away - sceneDistance(gl_FragCoord.xy / GlassScreen.xy) > tolerance) {
        discard;
    }
#endif

#ifdef DEPTH
    fragColor = vec4(0.0);
#elif defined(BACK)
    // the far side only (it faces away from you): its edges, faintly, seen through the glass in front of it
    vec3 eye = position / max(away, 1.0e-4);
    if (dot(normal, eye) < 0.0 || (flags & FLAG_EDGE) != 0) {
        discard;
    }
    vec2 lo = min(bounds.xy, bounds.zw) * size, hi = max(bounds.xy, bounds.zw) * size;
    float edgeDistance = edgeField(texel, lo, hi).x;
    float texelsPerPixel = max(length(dtx), length(dty));
    float rim = 1.0 - smoothstep(0.0, max(0.22, texelsPerPixel * 1.2), edgeDistance);
    float bright = glassLuma(max(lightColor, vec3(0.05)));
    vec3 color = (mix(vec3(1.0), GlassTint.rgb, 0.5) * 0.2 * mix(0.3, 1.0, bright) + GlassGlow.rgb * GlassGlow.w * 0.55) * rim;
#ifdef WORLD
    color *= 1.0 - total_fog_value(fog_spherical_distance(position), fog_cylindrical_distance(position), FogEnvironmentalStart,
        FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
#endif
    fragColor = vec4(color, 0.0);
#else
    vec3 eye = position / max(away, 1.0e-4);
    vec3 n0 = normalize(normal);
    if (dot(n0, eye) > 0.0) {
        n0 = -n0;
    }

    // the edges round off: the surface turns away towards the nearest edge of its shape
    vec3 n = n0;
    float edge = 0.0;
    float edgeDistance = 100.0;
    if ((flags & FLAG_EDGE) == 0) {
        vec2 lo = min(bounds.xy, bounds.zw) * size, hi = max(bounds.xy, bounds.zw) * size;
        vec3 field = edgeField(texel, lo, hi);
        vec2 extent = hi - lo;
        float radius = min(BEVEL, 0.45 * min(extent.x, extent.y));
        edgeDistance = field.x;
        edge = clamp(1.0 - field.x / max(radius, 1.0e-3), 0.0, 1.0);
        float det = dtx.x * dty.y - dtx.y * dty.x;
        if (edge > 0.0 && abs(det) > 1.0e-12) {
            // the surface's directions along the texture, from how both run across the screen
            vec3 alongU = (dpx * dty.y - dpy * dtx.y) / det;
            vec3 alongV = (dpy * dtx.x - dpx * dty.x) / det;
            vec3 outward = alongU * field.y + alongV * field.z;
            outward -= n0 * dot(outward, n0);
            float len = length(outward);
            if (len > 1.0e-8) {
                float tilt = pow(edge, 1.6) * TILT;
                n = normalize(n0 * cos(tilt) + outward / len * sin(tilt));
            }
        }
    }
    float cosine = clamp(dot(-eye, n), 0.0, 1.0);
    float fresnel = 0.04 + 0.96 * pow(1.0 - cosine, 5.0);

    // what stands behind, bent through the surface: blue a little more than red
    vec2 here = gl_FragCoord.xy / GlassScreen.xy;
    vec2 uvR = bent(eye, n, 1.0 / 1.46);
    vec2 uvG = bent(eye, n, 1.0 / 1.50);
    vec2 uvB = bent(eye, n, 1.0 / 1.55);
#ifdef WORLD
    // what stands in front of the glass is never seen through it: look straight through there instead
    if (sceneDistance(uvG) < away - 0.08) {
        uvR = here;
        uvG = here;
        uvB = here;
    }
#endif
    // the side of a thin sheet (the edge of a flat item) is one soft band of light: no rainbow, no detail
    bool strip = (flags & FLAG_EDGE) != 0;
    if (strip) {
        uvR = uvG;
        uvB = uvG;
    }
    vec3 sharp = vec3(texture(SceneSampler, uvR).r, texture(SceneSampler, uvG).g, texture(SceneSampler, uvB).b);
    vec3 soft = vec3(texture(FrostSampler, uvR).r, texture(FrostSampler, uvG).g, texture(FrostSampler, uvB).b);
    float frost = GlassLook.x;
    vec3 behind = mix(sharp, soft, strip ? 1.0 : clamp(frost * 1.5, 0.0, 1.0));

    // the light where it stands (vanilla's: day and night, torches), never quite black; how open it is to the sky
    vec3 lit = max(lightColor, vec3(0.05));
    float bright = glassLuma(lit);
    float open = mix(0.35, 1.0, skyLight);

    // light through the glass takes its colour, and loses a little where it runs through more glass (towards the
    // edges): that darker band is what shapes the glass
    vec3 tint = GlassTint.rgb;
    float thick = 0.35 + 0.65 * edge;
    vec3 through = behind * mix(vec3(1.0), tint, GlassTint.w * thick) * (1.0 - 0.3 * edge * edge);
    // and the glass itself is a little milky, lit where it stands
    vec3 color = mix(through, tint * lit, GlassLook.z);

    // the sky mirrored in it, most at grazing angles: bright where it faces up, dark where it faces the ground
    vec3 mirror = reflect(eye, n);
    float up = dot(mirror, GlassUp.xyz);
    vec3 skyColor = mix(GlassHorizon.rgb, GlassSky.rgb, smoothstep(0.0, 0.65, up));
    vec3 ground = GlassHorizon.rgb * 0.22;
    vec3 env = up >= 0.0 ? skyColor : mix(GlassHorizon.rgb * 0.7, ground, smoothstep(0.0, 0.25, -up));
    env *= open * mix(0.4, 1.0, bright);
    color = mix(color, env, fresnel * 0.75);

    // the studio: a key light up and to the left of the camera, a softer one from the right; and a glint of the sun
    vec3 toKey = normalize(GlassKey.xyz - position);
    vec3 toFill = normalize(GlassFill.xyz - position);
    float key = max(dot(n, normalize(toKey - eye)), 0.0);
    float fill = max(dot(n, normalize(toFill - eye)), 0.0);
    float sun = max(dot(mirror, GlassSun.xyz), 0.0);
    float shine = (pow(key, 420.0) * 1.6 + pow(key, 36.0) * 0.1) * GlassKey.w + pow(fill, 160.0) * 0.8 * GlassFill.w;
    // the sun glints along the rounded edges, never over a whole flat face at once
    vec3 glint = GlassSunColor.rgb * pow(sun, 1400.0) * 3.0 * GlassSun.w * open * (0.12 + 0.88 * edge);
    vec3 highlight = vec3(shine) * mix(0.3, 1.0, bright) + glint;
    color += highlight * (0.4 + 0.6 * fresnel);

    // a thin bright rim right along the edge (at least a pixel wide, so it never flickers far away)
    float texelsPerPixel = max(length(dtx), length(dty));
    float rimWidth = max(0.2, texelsPerPixel * 1.1);
    float rim = strip ? 0.55 : 1.0 - smoothstep(0.0, rimWidth, edgeDistance);
    color += mix(vec3(1.0), tint, 0.35) * rim * 0.3 * mix(0.35, 1.0, bright);

    // its own light, brightest along its edges
    float glow = GlassGlow.w;
    color += GlassGlow.rgb * glow * (0.06 + 0.95 * pow(edge, 1.5) + 0.35 * fresnel + 0.8 * rim);

    // now and then a band of light glides over each body, from its lower left to its upper right
    vec4 middle = GlassBodies[body * 2];
    vec4 info = GlassBodies[body * 2 + 1];
    if (GlassLook.y > 0.001 && middle.w > 0.0) {
        float phase = GlassSunColor.w >= 0.0 ? GlassSunColor.w * 0.3 : fract(GlassScreen.z / 6.5 + info.y);
        float sweep = phase / 0.3;
        if (sweep < 1.0) {
            vec3 axis = normalize(GlassUp.xyz * 0.8 + GlassRight.xyz * 0.6);
            float s = dot(position - middle.xyz, axis) / middle.w;
            float band = exp(-pow((s - mix(-1.4, 1.4, sweep)) / 0.14, 2.0));
            color += mix(vec3(1.0), tint, 0.25) * band * 0.3 * GlassLook.y * (0.4 + 0.6 * cosine) * mix(0.4, 1.0, bright);
        }
    }

    // enchanted: its glint as a violet sheen drifting through the glass
    if ((flags & FLAG_FOIL) != 0) {
        float wave = 0.5 + 0.5 * sin(dot(position, vec3(3.1, 4.3, 2.2)) * 2.6 - GlassScreen.z * 2.4);
        color += vec3(0.62, 0.44, 1.0) * pow(wave, 5.0) * 0.26 * mix(0.5, 1.0, bright);
    }

    // a hit: a moment of rose
    color = mix(color, vec3(1.0, 0.45, 0.58) * max(bright, 0.45), info.x * 0.32);

#ifdef WORLD
    // the world's fog: far away, the glass fades into what is behind it
    float fog = total_fog_value(fog_spherical_distance(position), fog_cylindrical_distance(position), FogEnvironmentalStart,
        FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    color = mix(color, texture(SceneSampler, here).rgb, fog);
#endif

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    fragColor = vec4(max(color + (dither - 0.5) / 255.0, 0.0), 1.0);
#endif
}
