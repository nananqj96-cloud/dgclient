#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:cyberspace.glsl>
#moj_import <purxiy:cyberspace_common.glsl>

// Cyberspace (see dev.purxiy.cyberspace.Cyberspace): the picture redrawn as a digital dimension. For every pixel the
// scene's surface is found again from the depth (where it is, and from its neighbours which way it faces):
//
//   block faces   (facing along an axis, on a sixteenth of a block) become dark cells: their borders are lines of light,
//                 the grid; circuits run over them in tiles of a quarter block. Which sides of a tile are wired is
//                 decided by where that side is in the world, so tiles agree with their neighbours, on the same face
//                 and around edges: traces run on, bend at 45 degrees, meet at junctions and end in vias. Data runs
//                 along them in pulses, in bursts that drift through the world
//   anything else (creatures, players, plants, items) becomes a hologram of itself: its own brightness in the palette,
//                 a rim of light and fine rising scan lines
//   edges         where a surface ends in front of what lies behind it, or folds away, a line of light
//   the sky       an endless grid far below the horizon, stars of data above, and the sun and the moon as rings
//
// The real picture shows through as much as Reality says. Scan waves and the fronts that switch the world over are
// spheres of light: they light up the surfaces they pass, and their wire shells are seen out in the open, in 3D.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

// circuit tiles per block side, and how fast data runs along the traces (pulses per second)
const float TILES = 4.0;
const float DATA_SPEED = 2.2;
// keep in sync with Cyberspace.SCAN_REACH
const float SCAN_REACH = 72.0;

// ------------------------------------------------------------------------------------------------ helpers

vec3 scenePoint(ivec2 px) {
    float depth = texelFetch(DepthSampler, px, 0).r;
    vec2 ndc = (vec2(px) + 0.5) / CyScreen.xy * 2.0 - 1.0;
    vec4 p = CyInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec3 viewRay(vec2 frag) {
    vec2 ndc = frag / CyScreen.xy * 2.0 - 1.0;
    vec4 p = CyInverse * vec4(ndc, 1.0, 1.0);
    return normalize(p.xyz / p.w);
}

// distance from p to the segment ab, and where along it (0..1) the nearest point is
vec2 segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return vec2(length(pa - ba * h), h);
}

// Smooth noise through the world (a lattice point every 8 blocks), drifting in time. Exact far from the origin: whole
// blocks stay integers.
float worldNoise(ivec3 block, vec3 inBlock, float time) {
    ivec3 cell = block >> 3;
    vec3 f = (vec3(block & 7) + inBlock) / 8.0;
    f = f * f * (3.0 - 2.0 * f);
    float slice = floor(time);
    float k = fract(time);
    k = k * k * (3.0 - 2.0 * k);
    float result = 0.0;
    for (int s = 0; s < 2; s++) {
        uvec3 z = uvec3(uint(int(slice) + s) * 2654435761u, 0u, uint(int(slice) + s) * 2246822519u);
        float c000 = hashf(uvec3(cell + ivec3(0, 0, 0)) ^ z);
        float c100 = hashf(uvec3(cell + ivec3(1, 0, 0)) ^ z);
        float c010 = hashf(uvec3(cell + ivec3(0, 1, 0)) ^ z);
        float c110 = hashf(uvec3(cell + ivec3(1, 1, 0)) ^ z);
        float c001 = hashf(uvec3(cell + ivec3(0, 0, 1)) ^ z);
        float c101 = hashf(uvec3(cell + ivec3(1, 0, 1)) ^ z);
        float c011 = hashf(uvec3(cell + ivec3(0, 1, 1)) ^ z);
        float c111 = hashf(uvec3(cell + ivec3(1, 1, 1)) ^ z);
        float v = mix(mix(mix(c000, c100, f.x), mix(c010, c110, f.x), f.y), mix(mix(c001, c101, f.x), mix(c011, c111, f.x), f.y), f.z);
        result += v * (s == 0 ? 1.0 - k : k);
    }
    return result;
}

// ------------------------------------------------------------------------------------------------ circuits

// the ports in the middle of a tile's sides: 0 left, 1 right, 2 bottom, 3 top
vec2 portAt(int side) {
    return side == 0 ? vec2(0.0, 0.5) : side == 1 ? vec2(1.0, 0.5) : side == 2 ? vec2(0.5, 0.0) : vec2(0.5, 1.0);
}

// the tile turned so that a side's port is on the left
vec2 toLeft(vec2 f, int side) {
    return side == 0 ? f : side == 1 ? 1.0 - f : side == 2 ? vec2(f.y, 1.0 - f.x) : vec2(1.0 - f.y, f.x);
}

// A port, keyed by where it is in the world (sixteenths of a block): bit 0 = wired, bit 1 = which way data flows.
uint portHash(int axis, int plane, ivec2 at) {
    ivec3 w = axis == 0 ? ivec3(plane, at.y, at.x) : axis == 1 ? ivec3(at.x, plane, at.y) : ivec3(at.x, at.y, plane);
    return hashu(uvec3(w) + uvec3(0x68E31DA4u, 0xB5297A4Du, 0x1B56C4E9u));
}

// The circuit of a tile at f (0..1): x = distance to its traces, y = where along the flow of data (see below),
// z = its via or junction (coverage, anti-aliased over tfw), w = 1 where there is a trace at all.
//
// Every wired side has a half-path from its port in to the middle (to a via when it is the only one, to a solder
// junction when there are three or four), and two wired sides at a corner are joined with a 45 degree bend. Along a
// half-path u runs from 0 at the port to 1 inside; data flows in through some ports and out through others (the port
// says which, for both tiles that share it), so g = u where it flows in and -u where it flows out runs on across ports
// (0 there) and through the middle (1 and -1 are one period of 2 apart).
vec4 circuit(vec2 f, int axis, int plane, ivec2 tile, float tfw) {
    ivec2 o = tile * 4;
    uint h0 = portHash(axis, plane, o + ivec2(0, 2));
    uint h1 = portHash(axis, plane, o + ivec2(4, 2));
    uint h2 = portHash(axis, plane, o + ivec2(2, 0));
    uint h3 = portHash(axis, plane, o + ivec2(2, 4));
    bool on0 = (h0 & 3u) == 0u || (h0 & 3u) == 1u;
    bool on1 = (h1 & 3u) == 0u || (h1 & 3u) == 1u;
    bool on2 = (h2 & 3u) == 0u || (h2 & 3u) == 1u;
    bool on3 = (h3 & 3u) == 0u || (h3 & 3u) == 1u;
    int count = int(on0) + int(on1) + int(on2) + int(on3);
    if (count == 0) {
        return vec4(1.0e3, 0.0, 0.0, 0.0);
    }
    bool corner = count == 2 && !(on0 && on1) && !(on2 && on3);
    float nearest = 1.0e3;
    float g = 0.0;
    for (int side = 0; side < 4; side++) {
        uint h = side == 0 ? h0 : side == 1 ? h1 : side == 2 ? h2 : h3;
        bool on = side == 0 ? on0 : side == 1 ? on1 : side == 2 ? on2 : on3;
        if (!on) {
            continue;
        }
        vec2 p = toLeft(f, side);
        vec2 s;
        if (corner) {
            // bend towards the other wired side
            int other = 0;
            for (int k = 0; k < 4; k++) {
                bool o2 = k == 0 ? on0 : k == 1 ? on1 : k == 2 ? on2 : on3;
                if (o2 && k != side) {
                    other = k;
                }
            }
            if (toLeft(portAt(other), side).y < 0.5) {
                p.y = 1.0 - p.y;
            }
            const float c = 0.3;
            const float l1 = 0.5 - c;
            const float l2 = c * 0.70710678;
            vec2 a = segment(p, vec2(0.0, 0.5), vec2(l1, 0.5));
            vec2 b = segment(p, vec2(l1, 0.5), vec2(0.5 - c * 0.5, 0.5 + c * 0.5));
            s = a.x < b.x ? vec2(a.x, a.y * l1 / (l1 + l2)) : vec2(b.x, (l1 + b.y * l2) / (l1 + l2));
        } else {
            s = segment(p, vec2(0.0, 0.5), vec2(count == 1 ? 0.36 : 0.5, 0.5));
        }
        if (s.x < nearest) {
            nearest = s.x;
            // this tile lies on the positive side of its left and bottom ports, on the negative side of the others
            bool bit = ((h >> 1u) & 1u) == 1u;
            bool leaving = side == 1 || side == 3 ? bit : !bit;
            g = leaving ? -s.y : s.y;
        }
    }
    float pad = 0.0;
    float r = length(f - 0.5);
    if (count == 1) {
        pad = line(abs(r - 0.14), 0.035, tfw);
    } else if (count >= 3) {
        pad = clamp((0.1 - r) / tfw + 0.5, 0.0, 1.0);
    }
    return vec4(nearest, g, pad, 1.0);
}

// ------------------------------------------------------------------------------------------------ the sky

vec3 digitalSky(vec3 ray, float pixelAngle) {
    vec3 color = skyBase(ray);
    float h = ray.y;
    float time = CyCamFract.w;
    float glow = CyScreen.w;
    if (h < 0.0) {
        // the endless grid far below: every 8 blocks, whole blocks exact
        float camY = float(CyCamFloor.y) + CyCamFract.y;
        float floorY = min(-64.0, camY - 64.0);
        float t = (floorY - camY) / h;
        vec2 hit = ray.xz * t;
        vec2 grid = (vec2(CyCamFloor.xz & 63) + CyCamFract.xz + hit) / 8.0;
        float fw = t * pixelAngle / max(-h, 0.02) / 8.0;
        vec2 d = abs(fract(grid + 0.5) - 0.5);
        float lines = max(line(d.x, 0.01, fw), line(d.y, 0.01, fw));
        float fade = exp(-t / 700.0) * (1.0 - smoothstep(0.05, 0.3, fw));
        color = lightOn(color, CyFirst.rgb, lines * fade * (0.35 + 0.35 * glow));
    } else {
        // stars of data: little squares in some cells of a cube around you
        vec3 a = abs(ray);
        vec2 face;
        uint side;
        if (a.x >= a.y && a.x >= a.z) {
            face = ray.yz / a.x;
            side = ray.x > 0.0 ? 0u : 1u;
        } else if (a.y >= a.z) {
            face = ray.xz / a.y;
            side = ray.y > 0.0 ? 2u : 3u;
        } else {
            face = ray.xy / a.z;
            side = ray.z > 0.0 ? 4u : 5u;
        }
        const float CELLS = 48.0;
        vec2 g = face * CELLS;
        vec2 c = floor(g);
        vec2 f = g - c - 0.5;
        uint hs = hashu(uvec3(uvec2(ivec2(c) + 4096), side));
        float stars = 0.0;
        if ((hs & 255u) < 9u) {
            vec2 offset = (vec2(float((hs >> 8u) & 255u), float((hs >> 16u) & 255u)) / 255.0 - 0.5) * 0.5;
            float size = 0.03 + 0.045 * float(hs >> 24u) / 255.0;
            float cpp = CELLS * pixelAngle * 1.3;
            vec2 q = abs(f - offset);
            float square = line(max(q.x, q.y), size, cpp);
            float twinkle = 0.55 + 0.45 * sin(time * (1.0 + float(hs & 7u) * 0.35) + float(hs >> 20u));
            stars = square * twinkle * smoothstep(0.0, 0.12, h);
        }
        color = lightOn(color, mix(CyFirst.rgb, vec3(1.0), 0.45), stars * 0.8);
        // a faint dome of longitudes and latitudes, turning slowly
        float azimuth = atan(ray.z, ray.x) / 6.2831853 * 24.0 + time * 0.01;
        float elevation = asin(clamp(h, 0.0, 1.0)) / 1.5707963 * 9.0;
        float fwAz = pixelAngle / 6.2831853 * 24.0 / max(sqrt(1.0 - h * h), 0.05);
        float fwEl = pixelAngle / 1.5707963 * 9.0;
        float dome = max(line(abs(fract(azimuth + 0.5) - 0.5), 0.01, fwAz) * (1.0 - smoothstep(0.35, 0.9, h)),
            line(abs(fract(elevation + 0.5) - 0.5), 0.01, fwEl));
        color = lightOn(color, CySecond.rgb, dome * 0.05 * (1.0 - smoothstep(0.5, 1.0, h)));
    }
    // the sun and the moon: rings of light, the sun with a four-pointed star
    if (CySun.w > 0.5) {
        vec3 sun = CySun.xyz;
        float cosA = dot(ray, sun);
        float angle = acos(clamp(cosA, -1.0, 1.0));
        float above = smoothstep(-0.03, 0.02, h);
        float ring = line(abs(angle - 0.07), 0.0035, pixelAngle);
        vec3 side = normalize(cross(sun, abs(sun.y) > 0.99 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
        vec3 up = cross(side, sun);
        vec2 local = vec2(dot(ray, side), dot(ray, up));
        float star = 0.0;
        if (cosA > 0.0) {
            // four spikes that taper to a point, inside the ring
            vec2 l = abs(local);
            float spikeX = l.y - 0.0045 * pow(max(1.0 - l.x / 0.052, 0.0), 2.2);
            float spikeY = l.x - 0.0045 * pow(max(1.0 - l.y / 0.052, 0.0), 2.2);
            star = clamp(-min(spikeX, spikeY) / pixelAngle + 0.5, 0.0, 1.0);
        }
        float halo = exp(-angle * 14.0) * 0.3 * glow;
        color = lightOn(color, mix(CyFirst.rgb, vec3(1.0), 0.4), (ring + star * 0.9 + halo) * above);
        vec3 moon = CyMoon.xyz;
        float moonAngle = acos(clamp(dot(ray, moon), -1.0, 1.0));
        float moonRing = line(abs(moonAngle - 0.04), 0.0025, pixelAngle) + line(moonAngle, 0.012, pixelAngle) * 0.35;
        color = lightOn(color, mix(CySecond.rgb, vec3(1.0), 0.3), (moonRing * 0.8 + exp(-moonAngle * 22.0) * 0.15 * glow) * smoothstep(-0.03, 0.02, h));
    }
    return color;
}

// ------------------------------------------------------------------------------------------------ spheres of light

// A sphere of light around c (camera-relative) with radius r, seen along the ray in front of the scene (at sceneDist):
// its wire shell (latitudes and longitudes, every few blocks) and a sheen where it is seen edge-on.
float shell(vec3 ray, vec3 c, float r, float sceneDist, float pixelAngle) {
    // close around you the wire would crowd into a moire: it comes in as the sphere grows
    float grown = smoothstep(4.0, 14.0, r);
    float b = dot(ray, c);
    float cc = dot(c, c) - r * r;
    float disc = b * b - cc;
    if (disc < 0.0 || r <= 0.0) {
        return 0.0;
    }
    float s = sqrt(disc);
    // from inside the far side ahead of you, from outside the near side
    float t = cc < 0.0 ? b + s : b - s;
    if (t <= 0.0 || t > sceneDist) {
        return 0.0;
    }
    vec3 n = normalize(ray * t - c);
    // (clamped: seen from its very middle this is 1 give or take rounding, and a power of a negative is undefined)
    float facing = clamp(abs(dot(n, ray)), 0.0, 1.0);
    const float lines = 36.0;
    float lon = atan(n.z, n.x) / 6.2831853 * lines;
    float lat = asin(clamp(n.y, -1.0, 1.0)) / 3.1415927 * lines * 0.5;
    float stretch = t * pixelAngle / max(facing, 0.08) / r;
    float fwLon = stretch / 6.2831853 * lines / max(sqrt(1.0 - n.y * n.y), 0.05);
    float fwLat = stretch / 3.1415927 * lines * 0.5;
    float wire = max(line(abs(fract(lon + 0.5) - 0.5), 0.014, fwLon) * (1.0 - smoothstep(0.85, 0.97, abs(n.y))),
        line(abs(fract(lat + 0.5) - 0.5), 0.014, fwLat));
    float sheen = pow(1.0 - facing, 5.0);
    return wire * grown * (0.35 + 0.65 * (1.0 - facing)) + sheen * 0.8;
}

// ------------------------------------------------------------------------------------------------ the glitch

ivec2 glitched(ivec2 px, float strength) {
    float seed = CyLook.w;
    // slices of the picture shifted sideways
    float row = floor(float(px.y) / 14.0);
    float r = hashf(uvec3(uint(row), uint(seed), 7u));
    ivec2 at = px;
    if (r < 0.35 * strength) {
        float shift = (hashf(uvec3(uint(row), uint(seed), 11u)) - 0.5) * 90.0 * strength;
        at.x += int(shift);
    }
    // and a few blocks of it coarse, like a signal breaking up
    vec2 block = floor(vec2(px) / vec2(48.0, 24.0));
    float b = hashf(uvec3(uvec2(block), uint(seed) + 101u));
    if (b < 0.1 * strength) {
        at = ivec2(floor(vec2(at) / 8.0) * 8.0);
    }
    return clamp(at, ivec2(0), ivec2(CyScreen.xy) - 1);
}

// ------------------------------------------------------------------------------------------------ main

void main() {
    ivec2 size = ivec2(CyScreen.xy);
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec3 original = texelFetch(SceneSampler, px, 0).rgb;
#ifdef MASKED
    if (CyCamFloor.w == 1 && texelFetch(HandMaskSampler, px, 0).r > 0.5) {
        fragColor = vec4(original, 1.0);
        return;
    }
#endif
    float glitch = CyLook.z;
    ivec2 src = glitch > 0.001 ? glitched(px, glitch) : px;
    vec3 scene = texelFetch(SceneSampler, src, 0).rgb;
    float depth = texelFetch(DepthSampler, src, 0).r;
    vec3 ray = viewRay(vec2(src) + 0.5);
    float pixelAngle = 2.0 / (CyScreen.z * CyScreen.y);
    float time = CyCamFract.w;
    float glow = CyScreen.w;
    float reality = CyFirst.w;
    vec3 deep = deepColor();

    vec3 color;
    float digital;
    float fronts = 0.0;
    float sceneDist = 1.0e9;
    if (depth >= 1.0) {
        color = mix(scene, digitalSky(ray, pixelAngle), CyAccent.w);
        color = mix(color, scene, reality * 0.35);
        digital = CyFront2.y;
    } else {
        ivec2 edge = size - 1;
        vec3 p = scenePoint(src);
        vec3 right = scenePoint(min(src + ivec2(1, 0), edge));
        vec3 left = scenePoint(max(src - ivec2(1, 0), ivec2(0)));
        vec3 above = scenePoint(min(src + ivec2(0, 1), edge));
        vec3 below = scenePoint(max(src - ivec2(0, 1), ivec2(0)));
        vec3 dx = distance(right, p) < distance(p, left) ? right - p : p - left;
        vec3 dy = distance(above, p) < distance(p, below) ? above - p : p - below;
        vec3 n = normalize(cross(dx, dy));
        if (dot(n, p) > 0.0) {
            n = -n;
        }
        float dist = length(p);
        sceneDist = dist;
        // blocks per pixel, across the view at this distance
        float pixel = dist * pixelAngle;
        float facing = clamp(dot(n, -ray), 0.0, 1.0);

        // edges: a neighbour well behind this surface (it ends here, in front of what lies behind, or folds away)
        float noise = 0.003 + dist * dist * 2.5e-6;
        float tolerance = max(0.35 * pixel, noise);
        float gapR = -dot(n, right - p);
        float gapL = -dot(n, left - p);
        float gapA = -dot(n, above - p);
        float gapB = -dot(n, below - p);
        float behind = max(max(gapR, gapL), max(gapA, gapB));
        float silhouette = smoothstep(tolerance, tolerance * 2.0, behind);
        // seen through a gap (between leaves, past an edge): the lines here stay quiet, or they speckle
        float front = max(max(-gapR, -gapL), max(-gapA, -gapB));
        float clear = 1.0 - 0.8 * smoothstep(tolerance * 2.0, tolerance * 4.0, front);
        if (silhouette > 0.0) {
            // a hole (in leaves, between the stalks of grass) rather than an edge: a few pixels on, the surface is back
            ivec2 away = behind == gapR ? ivec2(3, 0) : behind == gapL ? ivec2(-3, 0) : behind == gapA ? ivec2(0, 3) : ivec2(0, -3);
            vec3 beyond = scenePoint(clamp(src + away, ivec2(0), edge));
            silhouette *= smoothstep(tolerance, tolerance * 3.0, abs(dot(n, beyond - p)));
        }

        // a block face: facing along an axis, on a sixteenth of a block
        vec3 an = abs(n);
        int axis = an.x > an.y ? (an.x > an.z ? 0 : 2) : (an.y > an.z ? 1 : 2);
        vec3 q = CyCamFract.xyz + p;
        float along = q[axis];
        float snapped = floor(along * 16.0 + 0.5) / 16.0;
        float off = abs(along - snapped);
        // on a sixteenth of a block, facing along an axis: a block's own face, which carries circuits; other surfaces
        // facing straight up (water) only get the grid and the shading
        float sixteenth = step(0.94, an[axis]) * (1.0 - smoothstep(noise + 0.002, noise * 2.0 + 0.006, off));
        // (a shader pack sways leaves and plants a little off the grid: there anything roughly facing an axis counts)
        float block = max(sixteenth, CyTarget2.z > 0.5 ? step(0.9, an[axis]) : step(0.94, n.y));

        // the scan waves and the fronts where they cross the surface
        float wave = 0.0;
        float waveFront = 0.0;
        for (int i = 0; i < 4; i++) {
            vec4 s = CyScans[i];
            if (s.w < 0.0) {
                continue;
            }
            float x = s.w - distance(p, s.xyz);
            float fade = 1.0 - smoothstep(0.3, 1.0, s.w / SCAN_REACH);
            waveFront += exp(-x * x / 0.05) * fade;
            wave += (x > 0.0 ? exp(-x / 4.0) : 0.0) * fade;
        }
        float d0 = distance(p, CyFront.xyz);
        digital = smoothstep(CyFront.w + 0.6, CyFront.w - 0.6, d0) * smoothstep(CyFront2.x - 0.6, CyFront2.x + 0.6, d0);
        float toOuter = (d0 - CyFront.w) / 0.4;
        float toInner = (d0 - CyFront2.x) / 0.4;
        fronts = exp(-toOuter * toOuter) + exp(-toInner * toInner);

        float lum = luma(scene);
        float boost = 1.0 + 2.2 * wave;
        // a speck: a pixel whose nearest neighbours on a side are far off (a hole in leaves a pixel wide, a single
        // pixel of something thin). Which way it faces cannot be told, so it stays a dark, quiet gap
        float reach = max(length(dx), length(dy));
        float speck = smoothstep(7.0, 10.0, reach / max(pixel, 1.0e-4)) * step(0.08, reach);
        vec3 surface;
        if (speck > 0.5) {
            surface = mix(voidColor() + deep * 0.035, scene, reality);
            silhouette = 0.0;
        } else if (block > 0.5) {
            // ---------------------------------------------------------------- a block face
            vec2 uv;
            ivec2 base;
            vec2 du;
            vec2 dv;
            if (axis == 0) {
                uv = q.zy;
                base = CyCamFloor.zy;
                du = dx.zy;
                dv = dy.zy;
            } else if (axis == 1) {
                uv = q.xz;
                base = CyCamFloor.xz;
                du = dx.xz;
                dv = dy.xz;
            } else {
                uv = q.xy;
                base = CyCamFloor.xy;
                du = dx.xy;
                dv = dy.xy;
            }
            vec2 fw = max(abs(du) + abs(dv), vec2(1.0e-5));
            int plane = int(floor(along * 16.0 + 0.5)) + CyCamFloor[axis] * 16;
            // on a face of a whole block (or a slab, a stair) edges of light only run along whole and half blocks: the
            // holes of leaves and the like would otherwise light up as noise. Smaller shapes (fences, panes) keep theirs.
            if (sixteenth < 0.5 || abs(snapped * 2.0 - floor(snapped * 2.0 + 0.5)) < 0.01) {
                vec2 half2 = abs(fract(uv * 2.0 + 0.5) - 0.5) * 0.5 / fw;
                silhouette *= 1.0 - smoothstep(0.9, 1.5, min(half2.x, half2.y));
            }
            // and not where a face lies a hair above another (leaf litter, petals, carpets on the ground), nor far
            // away, where holes are smaller than a pixel and the grid draws the shapes anyway
            silhouette *= smoothstep(0.07, 0.12, behind) * (1.0 - smoothstep(0.045, 0.1, max(fw.x, fw.y)));
            // the block this face belongs to
            vec3 inward = vec3(0.0);
            inward[axis] = n[axis] > 0.0 ? -0.01 : 0.01;
            ivec3 cell = CyCamFloor.xyz + ivec3(floor(q + inward));

            // the cell: lit by which way it faces, the real texture etched into it as a relief of its brightness
            float shade = axis == 1 ? (n.y > 0.0 ? 1.0 : 0.45) : (axis == 0 ? 0.66 : 0.8);
            vec3 night = voidColor() + deep * (0.055 + 0.08 * pow(lum, 0.8)) * shade;
            vec3 day = voidColor() * mix(0.74, 1.0, shade) * (0.94 + 0.08 * pow(lum, 0.8));
            surface = mix(night, day, CyLook.x);
            surface = mix(surface, scene, reality);

            // the grid: the borders of blocks
            vec2 g = abs(fract(uv + 0.5) - 0.5);
            vec2 visible = 1.0 - smoothstep(0.07, 0.22, fw);
            float grid = max(line(g.x, 0.012, fw.x) * visible.x, line(g.y, 0.012, fw.y) * visible.y);
            vec2 haloFade = 1.0 - smoothstep(0.02, 0.09, fw);
            float halo = max(exp(-g.x / 0.045) * haloFade.x, exp(-g.y / 0.045) * haloFade.y) * 0.22 * glow;

            // the block you look at: its cell brighter, filling as you break it
            float selected = 0.0;
            if (CyTarget.w == 1 && all(equal(cell, CyTarget.xyz))) {
                float appear = smoothstep(0.0, 0.15, CyTarget2.y);
                selected = appear * (0.8 + 0.2 * sin(time * 5.0));
                vec2 inCell = fract(uv);
                float sweep = CyTarget2.x > 0.0 ? step(inCell.y, CyTarget2.x) : 0.0;
                surface = lightOn(surface, CyFirst.rgb, (0.05 + 0.2 * sweep) * appear);
            }

            // the circuits
            float traceLight = 0.0;
            float padLight = 0.0;
            float pulseLight = 0.0;
            float circuitVisible = (1.0 - smoothstep(0.012, 0.03, max(fw.x, fw.y))) * CySecond.w * sixteenth;
            if (circuitVisible > 0.001) {
                vec2 t = uv * TILES;
                vec2 tileFloor = floor(t);
                ivec2 tile = ivec2(tileFloor) + base * int(TILES);
                float tfw = max(fw.x, fw.y) * TILES;
                vec4 c = circuit(t - tileFloor, axis, plane, tile, tfw);
                if (c.w > 0.0) {
                    float trace = line(c.x, 0.045, tfw);
                    traceLight = trace * circuitVisible;
                    padLight = c.z * circuitVisible;
                    // the data: a pulse every tile, in bursts that drift through the world
                    if (CyLook.y < 0.5) {
                        float x = fract(c.y * 0.5 - time * DATA_SPEED * 0.5);
                        float pulse = smoothstep(0.7, 0.97, x) * (1.0 - smoothstep(0.97, 1.0, x));
                        if (pulse > 0.0) {
                            float burst = smoothstep(0.62, 0.86, worldNoise(cell, fract(q), time * 0.35));
                            pulseLight = pulse * max(burst, wave * 0.8) * line(c.x, 0.07, tfw) * circuitVisible;
                        }
                    }
                }
            }

            float lines = (grid * 0.62 + halo) * boost * clear;
            traceLight *= clear;
            padLight *= clear;
            pulseLight *= clear;
            vec3 lit = surface;
            lit = lightOn(lit, CySecond.rgb, traceLight * 0.3 * (1.0 + 1.8 * wave));
            lit = lightOn(lit, CyFirst.rgb, padLight * 0.42);
            lit = lightOn(lit, CyFirst.rgb, lines * (1.0 + selected * 1.4) * (1.0 - 0.45 * reality));
            lit = lightOn(lit, mix(CyFirst.rgb, vec3(1.0), 0.45), pulseLight * (0.75 + 0.55 * glow));
            surface = lit;
        } else {
            // ---------------------------------------------------------------- a hologram
            float rim = pow(1.0 - facing, 2.0);
            vec3 night = mix(deep * 0.15, mix(CyFirst.rgb, vec3(1.0), 0.15), pow(lum, 0.85)) * 0.78 + CyFirst.rgb * rim * 0.55 * (0.5 + glow);
            vec3 day = mix(CySecond.rgb * 0.55, mix(vec3(1.0), CyFirst.rgb, 0.2), pow(lum, 0.85)) - (vec3(1.0) - CyFirst.rgb) * rim * 0.3;
            vec3 holo = mix(night, day, CyLook.x);
            float yfw = abs(dx.y) + abs(dy.y) + 1.0e-5;
            float scan = line(abs(fract(q.y * 10.0 - time * 0.35) - 0.5) * 0.1, 0.012, yfw);
            holo = lightOn(holo, CyFirst.rgb, scan * 0.35 * (1.0 - smoothstep(0.01, 0.05, yfw)));
            surface = mix(holo, scene, max(reality, 0.22));
            // things lying flat a hair above the ground (leaf litter, petals) keep no edge of their own
            silhouette *= smoothstep(0.035, 0.08, behind);
        }

        // edges of light
        vec3 edgeLight = mix(CyFirst.rgb, vec3(1.0), 0.25);
        surface = lightOn(surface, edgeLight, silhouette * (0.45 + 0.3 * glow) * boost * (block > 0.5 ? 0.8 : 0.85));
        // the scan wave's front: a thin band of light over everything it crosses
        surface = lightOn(surface, mix(CyFirst.rgb, CyAccent.rgb, 0.3), waveFront * 0.5 + wave * 0.03);

        // far away, the world fades into the digital sky; fog of water, lava and the like tints it
        float fogFar = linear_fog_value(fog_cylindrical_distance(p), FogRenderDistanceStart, FogRenderDistanceEnd);
        float fogNear = linear_fog_value(fog_spherical_distance(p), FogEnvironmentalStart, FogEnvironmentalEnd);
        surface = mix(surface, skyBase(ray), fogFar);
        surface = mix(surface, mix(voidColor(), FogColor.rgb, 0.45), fogNear * FogColor.a);
        color = surface;
    }

    // the wire shells of the scan waves and the fronts, out in the open in front of whatever lies behind
    float shells = 0.0;
    for (int i = 0; i < 4; i++) {
        vec4 s = CyScans[i];
        if (s.w > 0.0) {
            shells += shell(ray, s.xyz, s.w, sceneDist, pixelAngle) * (1.0 - smoothstep(0.25, 1.0, s.w / SCAN_REACH)) * 0.22;
        }
    }
    float frontShells = 0.0;
    if (CyFront.w < CyFront2.z) {
        frontShells += shell(ray, CyFront.xyz, CyFront.w, sceneDist, pixelAngle);
    }
    if (CyFront2.x > 0.0) {
        frontShells += shell(ray, CyFront.xyz, CyFront2.x, sceneDist, pixelAngle);
    }
    color = lightOn(color, mix(CyFirst.rgb, CyAccent.rgb, 0.25), shells * (0.6 + 0.6 * glow));

    // the real picture where the world is not digital (yet, or any more), and the fronts glowing where they are
    vec3 result = mix(scene, color, digital);
    result = lightOn(result, mix(CyFirst.rgb, vec3(1.0), 0.35), fronts * 0.9 + frontShells * 0.55);
    // a glitch tints the picture and splits its colours a little
    if (glitch > 0.001) {
        float band = hashf(uvec3(uint(gl_FragCoord.y / 3.0), uint(CyLook.w), 3u));
        result = mix(result, result.gbr * vec3(0.8, 0.9, 1.2), glitch * 0.25 * step(0.8, band));
        result += CyAccent.rgb * glitch * 0.08 * step(0.93, band);
    }
    // a little dither keeps the dark gradients from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result = max(result + (dither - 0.5) / 255.0, 0.0);
    fragColor = vec4(result, 1.0);
}
