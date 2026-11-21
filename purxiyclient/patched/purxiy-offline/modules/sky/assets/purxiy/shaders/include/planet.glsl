#version 330

// Analytic 3D geometry in planet radii. The camera is at +Z: every surface, cloud shell, ring and moon is
// intersected by the same perspective ray and composited by its actual distance. No screen-space sphere mapping.
const vec3 AXIS = vec3(-0.32506, 0.89416, 0.30906);
const vec3 EQUATOR_A = vec3(0.93982, 0.34167, 0.0);
const vec3 EQUATOR_B = vec3(-0.10560, 0.29046, -0.95104);
const float RING_INNER = 1.34;
const float RING_OUTER = 2.30;
const float CLOUD_RADIUS = 1.022;
const float AIR_RADIUS = 1.075;
const float COAST = 0.493;
const vec3 STORM_EYE = vec3(0.621, 0.351, 0.701);

vec4 over(vec4 top, vec4 bottom) {
    return top + bottom * (1.0 - top.a);
}

vec3 turn(vec3 v, vec3 axis, float angle) {
    float s = sin(angle), c = cos(angle);
    return v * c + cross(axis, v) * s + axis * dot(axis, v) * (1.0 - c);
}

vec3 spin(vec3 p, float angle) {
    return turn(p, AXIS, angle);
}

// Negative discriminants are misses. Callers use the closest point only for subpixel silhouette coverage.
vec2 sphereHit(vec3 origin, vec3 ray, float radius) {
    float b = dot(origin, ray);
    vec3 closest = origin - ray * b;
    float h = radius * radius - dot(closest, closest);
    if (h < 0.0) return vec2(1.0e5, -1.0e5);
    float root = sqrt(h);
    return vec2(-b - root, -b + root);
}

float sphereCoverage(vec3 origin, vec3 ray, float radius, float footprint) {
    float distanceToRay = length(cross(origin, ray));
    return clamp((radius - distanceToRay) / max(footprint, 0.00001) + 0.5, 0.0, 1.0);
}

// Small octaves fade continuously before reaching the pixel size, including when viewed through Zoom.
vec2 detailNoise(vec3 p, float footprint, int octaves) {
    vec2 value = vec2(0.0);
    float weight = 0.5, norm = 0.0;
    for (int i = 0; i < 7; i++) {
        if (i >= octaves) break;
        float resolved = 1.0 - smoothstep(0.22, 0.65, footprint);
        value += weight * mix(vec2(0.5), noise3x2(p), resolved);
        norm += weight;
        p = OCTAVE * p * 2.03 + 0.17;
        footprint *= 2.03;
        weight *= 0.49;
    }
    return value / norm;
}

vec4 moonOrbit(int i, float flow) {
    float a = flow * TAU / (i == 0 ? 150.0 : 260.0) + (i == 0 ? 1.0 : 4.2);
    float radius = i == 0 ? 2.75 : 3.3;
    float tilt = i == 0 ? 0.10 : -0.16;
    return vec4((EQUATOR_A * cos(a) + EQUATOR_B * sin(a) + AXIS * sin(a) * tilt) * radius,
        i == 0 ? 0.115 : 0.085);
}

float ringDensity(float radius, float footprint) {
    float fw = max(footprint, 0.0003);
    float edge = smoothstep(RING_INNER - fw, RING_INNER + fw, radius)
        * (1.0 - smoothstep(RING_OUTER - fw, RING_OUTER + fw, radius));
    float radial = (radius - RING_INNER) / (RING_OUTER - RING_INNER);
    float broad = noise2(vec2(radius * 36.0, 7.5));
    float fine = mix(noise2(vec2(radius * 260.0, 21.0)), 0.5, smoothstep(0.002, 0.014, fw));
    float threads = mix(0.5 + 0.5 * sin(radius * 1150.0), 0.5, smoothstep(0.0008, 0.004, fw));
    float gap = smoothstep(0.018, 0.028 + fw, abs(radius - 1.94));
    gap *= mix(0.22, 1.0, smoothstep(0.004, 0.012 + fw, abs(radius - 2.17)));
    return edge * gap * (0.23 + 0.47 * broad + 0.23 * fine + 0.07 * threads)
        * mix(1.0, 0.42, smoothstep(0.72, 1.0, radial));
}

// Visibility of the light, shared by the ground, clouds and moons. Emission is never darkened by a shadow.
float planetShadow(vec3 p, vec3 light) {
    float along = dot(p, light);
    float separation = length(p - light * along);
    return along < 0.0 ? smoothstep(0.975, 1.025, separation) : 1.0;
}

float sceneShadow(vec3 p, vec3 light, vec4 m0, vec4 m1, float footprint) {
    float shade = 1.0;
    float denominator = dot(light, AXIS);
    if (abs(denominator) > 0.0001) {
        float t = -dot(p, AXIS) / denominator;
        if (t > 0.002) {
            float density = ringDensity(length(p + light * t), max(footprint, 0.006));
            shade *= exp(-density * 1.35 / max(abs(denominator), 0.18));
        }
    }
    for (int i = 0; i < 2; i++) {
        vec4 m = i == 0 ? m0 : m1;
        vec3 delta = m.xyz - p;
        float t = dot(delta, light);
        if (t > 0.0) {
            float penumbra = max(footprint, 0.004 + t * 0.008);
            shade *= mix(0.025, 1.0, smoothstep(m.w - penumbra, m.w + penumbra, length(delta - light * t)));
        }
    }
    return shade;
}

// x: elevation, y: humidity, z: folded mountain ridges. Warped continents have no UV seams or polar singularities.
vec3 terrainField(vec3 ground, float footprint) {
    vec3 p = ground * 2.35 + vec3(4.0, 1.0, 7.0);
    vec2 warp = fbm2(p * 0.63 + 3.0, 3) - 0.5;
    p += vec3(warp.x, warp.y, warp.x - warp.y) * 1.45;
    vec2 field = detailNoise(p, footprint * 2.35, QUALITY + 4);
    float ridges = 1.0 - abs(detailNoise(p * 3.7 + 19.0, footprint * 8.7, QUALITY + 2).x * 2.0 - 1.0);
    ridges = pow(ridges, 5.0);
    float mountains = smoothstep(COAST + 0.035, COAST + 0.14, field.x);
    return vec3(field.x + ridges * mountains * 0.038, field.y, ridges * mountains);
}

vec3 terrainNormal(vec3 n, vec3 terrain, float footprint) {
    vec3 tangent = normalize(cross(n, abs(n.y) < 0.9 ? vec3(0, 1, 0) : vec3(1, 0, 0)));
    vec3 bitangent = cross(n, tangent);
    float epsilon = max(0.0015, footprint * 0.65);
    float height = max(terrain.x - COAST, 0.0);
    vec3 tx = spin(normalize(n + tangent * epsilon), SkyPlanetUp.w);
    vec3 ty = spin(normalize(n + bitangent * epsilon), SkyPlanetUp.w);
    float dx = max(terrainField(tx, footprint).x - COAST, 0.0) - height;
    float dy = max(terrainField(ty, footprint).x - COAST, 0.0) - height;
    return normalize(n - (tangent * dx + bitangent * dy) * (0.075 / epsilon));
}

// Independent, elevated cloud shell. Both the visible layer and its projected ground shadow use this field.
vec2 cloudField(vec3 n, float flow, float footprint) {
    vec3 air = spin(n, SkyPlanetUp.w * 1.22 + 0.7);
    float eye = max(1.0 - dot(air, STORM_EYE), 0.0);
    float storm = exp(-eye / 0.032);
    float phase = fract(flow / 26.0);
    vec3 a = turn(air, STORM_EYE, (4.0 + phase * 2.5) * storm);
    vec3 b = turn(air, STORM_EYE, (4.0 + fract(phase + 0.5) * 2.5) * storm);
    vec2 weatherA = detailNoise(a * 5.8 + vec3(3, 7, 11), footprint * 5.8, QUALITY + 3);
    vec2 weatherB = detailNoise(b * 5.8 + vec3(3, 7, 11), footprint * 5.8, QUALITY + 3);
    vec2 weather = mix(weatherB, weatherA, 1.0 - abs(1.0 - phase * 2.0));
    float belt = 0.5 + 0.5 * cos(dot(n, AXIS) * 10.0);
    float wisps = noise3(air * 44.0 + weather.x * 3.0) - 0.5;
    weather.x += wisps * 0.07 * (1.0 - smoothstep(0.006, 0.025, footprint));
    float cover = smoothstep(0.51 - belt * 0.025 - storm * 0.09, 0.665, weather.x);
    cover *= smoothstep(0.00025, 0.0023, eye);
    return vec2(cover, weather.y);
}

vec3 shadeWorld(vec3 p, vec3 n, vec3 view, vec3 light, float flow, float life, float footprint, float shadow) {
    vec3 ground = spin(n, SkyPlanetUp.w);
    vec3 terrain = terrainField(ground, footprint);
    float elevation = terrain.x - COAST;
    float land = smoothstep(-0.0015 - footprint * 0.15, 0.002 + footprint * 0.15, elevation);
    float latitude = abs(dot(n, AXIS));
    float ice = smoothstep(0.85, 0.96, latitude + (terrain.y - 0.5) * 0.17);
    float sea = (1.0 - land) * (1.0 - ice);
    vec3 normal = terrainNormal(n, terrain, footprint);
    normal = normalize(mix(normal, n, 1.0 - land));

    vec3 ocean = mix(ColRingA.rgb * 0.44, ColPlanetA.rgb * 0.16, smoothstep(0.002, 0.065, -elevation));
    vec3 plains = mix(ColPlanetB.rgb, ColNebulaC.rgb, 0.16) * 0.68;
    vec3 forest = mix(ColPlanetA.rgb, ColNebulaB.rgb, 0.23) * 0.65;
    vec3 soil = mix(plains, forest, smoothstep(0.38, 0.61, terrain.y));
    soil = mix(soil, ColPlanetB.rgb * 0.86, terrain.z * 0.55);
    float snow = smoothstep(0.12, 0.19, elevation + latitude * 0.043);
    soil = mix(soil, mix(vec3(0.92), ColMoon.rgb, 0.35), snow);
    float grain = noise3(ground * 165.0);
    soil *= 0.90 + grain * 0.20 * (1.0 - smoothstep(0.002, 0.012, footprint));
    vec3 albedo = mix(ocean, soil, land);
    albedo = mix(albedo, ColMoon.rgb * 0.87, ice);

    float ndl = dot(n, light);
    float day = smoothstep(-0.07, 0.16, ndl);
    vec2 cloudShadowHit = sphereHit(p + n * 0.001, light, CLOUD_RADIUS);
    float cloudShade = 1.0;
    if (ndl > -0.08 && cloudShadowHit.y > 0.0) {
        vec3 cloudPoint = normalize(p + n * 0.001 + light * cloudShadowHit.y);
        cloudShade = exp(-cloudField(cloudPoint, flow, footprint * 1.6).x * 1.65);
    }
    float diffuse = max(dot(normal, light), 0.0) * shadow * cloudShade;
    vec3 ambient = mix(ColPlanetRim.rgb, ColMoon.rgb, 0.2) * (0.018 + 0.017 * max(n.y, 0.0));
    vec3 colour = albedo * (ambient + ColSun.rgb * diffuse * 1.35);

    // Fresnel ocean reflection: a tight sun glint over a broad, softer field of microfacets.
    vec3 halfVector = normalize(light + view);
    float fresnel = 0.025 + 0.975 * pow(1.0 - max(dot(n, view), 0.0), 5.0);
    float specular = pow(max(dot(n, halfVector), 0.0), 210.0) * 1.6
        + pow(max(dot(n, halfVector), 0.0), 34.0) * 0.12;
    colour += ColSun.rgb * specular * (0.3 + fresnel) * sea * day * shadow * cloudShade;

    float fine = 1.0 - smoothstep(0.006, 0.025, footprint);
    float coastal = exp(-elevation * elevation / 0.0007);
    vec2 settlements = noise3x2(ground * 105.0 + 31.0);
    float cities = smoothstep(0.70, 0.9, settlements.x) * smoothstep(0.42, 0.64, noise3(ground * 19.0));
    cities *= land * (1.0 - ice) * (0.25 + coastal * 0.75) * fine;
    float veins = pow(1.0 - abs(noise3(ground * 12.0 + 21.0) * 2.0 - 1.0), 18.0);
    veins *= land * smoothstep(0.50, 0.65, terrain.y) * fine;
    float pulse = 0.65 + 0.35 * sin(flow * 0.9 + terrain.y * 30.0);
    float tide = exp(-elevation * elevation / 0.000012) * (1.0 - ice) * 0.15;
    vec3 emission = mix(vec3(1.0, 0.68, 0.36), ColSun.rgb, 0.4) * cities * 1.7
        + ColAuroraA.rgb * veins * pulse * life * 0.22 + ColRingA.rgb * tide;
    colour += emission * (1.0 - day);
    return colour;
}

vec3 giantField(vec3 n, float flow, float life, float footprint) {
    float latitude = dot(n, AXIS);
    float wind = sin(latitude * 11.0) * 0.045 * (0.4 + 0.6 * life);
    float phase = fract(flow / 36.0);
    vec3 p0 = spin(n, SkyPlanetUp.w + wind * phase * 36.0);
    vec3 p1 = spin(n, SkyPlanetUp.w + wind * fract(phase + 0.5) * 36.0);
    vec2 turbulence = mix(detailNoise(p1 * 7.0 + 11.0, footprint * 7.0, QUALITY + 3),
        detailNoise(p0 * 7.0 + 11.0, footprint * 7.0, QUALITY + 3), 1.0 - abs(1.0 - phase * 2.0));
    float warped = latitude + (turbulence.x - 0.5) * 0.065;
    float bands = sin(warped * 36.0 + turbulence.y * 2.2);
    float filaments = sin(warped * 155.0 + turbulence.x * 14.0) * (1.0 - smoothstep(0.003, 0.022, footprint));
    float tone = clamp(0.49 + bands * 0.25 + filaments * 0.085 + (turbulence.y - 0.5) * 0.8, 0.0, 1.0);
    vec3 colour = mix(ColPlanetA.rgb * 0.7, mix(ColPlanetB.rgb, ColMoon.rgb, 0.23), tone);
    colour = mix(colour, mix(ColNebulaC.rgb, ColPlanetB.rgb, 0.45), smoothstep(0.3, 0.95, bands) * 0.28);

    vec3 turned = spin(n, SkyPlanetUp.w);
    vec3 stormCentre = normalize(EQUATOR_A * 0.57 + AXIS * 0.16 + EQUATOR_B * -0.77);
    vec3 stormTangent = normalize(cross(AXIS, stormCentre));
    vec3 stormUp = cross(stormCentre, stormTangent);
    vec2 uv = vec2(dot(turned, stormTangent) * 0.82, dot(turned, stormUp) * 1.7);
    float radius = length(uv);
    float mask = (1.0 - smoothstep(0.12, 0.30, radius)) * smoothstep(0.65, 0.9, dot(turned, stormCentre));
    if (mask > 0.001) {
        vec2 swirl = rotate2(uv, exp(-radius * 5.0) * 7.0 + flow * 0.07);
        float arms = noise3(vec3(swirl * 48.0, 7.0)) * 0.5 + 0.5 * sin(radius * 115.0 + noise3(vec3(swirl * 16.0, 2.0)) * 6.0);
        vec3 stormColour = mix(ColPlanetA.rgb * 0.65, mix(ColPlanetRim.rgb, ColMoon.rgb, 0.42), arms * 0.5 + 0.4);
        stormColour *= 1.0 - exp(-radius * radius / 0.0009) * 0.55;
        colour = mix(colour, stormColour, mask);
    }
    return colour;
}

float giantRelief(vec3 n, float footprint) {
    vec3 p = spin(n, SkyPlanetUp.w);
    float turbulence = noise3(p * 16.0 + 9.0);
    float belt = sin(dot(n, AXIS) * 82.0 + turbulence * 5.0);
    float small = (noise3(p * 65.0 + 3.0) - 0.5) * (1.0 - smoothstep(0.004, 0.018, footprint));
    return (belt * 0.6 + small) * 0.0022;
}

vec3 shadeGiant(vec3 n, vec3 light, float flow, float life, float footprint, float shadow) {
    vec3 albedo = giantField(n, flow, life, footprint);
    float ndl = dot(n, light);
    // Grazing light picks up changes in the cloud deck's normal, especially at the terminator.
    vec3 tangent = normalize(cross(n, abs(n.y) < 0.9 ? vec3(0, 1, 0) : vec3(1, 0, 0)));
    vec3 bitangent = cross(n, tangent);
    float epsilon = max(0.002, footprint * 0.75);
    float h = giantRelief(n, footprint);
    float dx = giantRelief(normalize(n + tangent * epsilon), footprint) - h;
    float dy = giantRelief(normalize(n + bitangent * epsilon), footprint) - h;
    vec3 normal = normalize(n - (tangent * dx + bitangent * dy) / epsilon);
    float diffuse = max(dot(normal, light), 0.0) * smoothstep(-0.05, 0.02, ndl);
    vec3 colour = albedo * (ColSun.rgb * diffuse * shadow * 1.27 + ColPlanetRim.rgb * 0.025);
    float flash = pow(max(sin(flow * 3.3 + noise3(n * 13.0) * 90.0), 0.0), 35.0);
    flash *= smoothstep(0.76, 0.93, noise3(spin(n, SkyPlanetUp.w) * 27.0 + 8.0));
    return colour + ColMoon.rgb * flash * (1.0 - smoothstep(-0.1, 0.2, ndl)) * life * 0.45;
}

// A shallow atmosphere is integrated along the actual ray. Its optical depth grows at the limb and the
// planet occludes sunlight on the night side; the terminator naturally glows through the elevated air.
vec4 atmosphere(vec3 origin, vec3 ray, float stop, vec3 light) {
    vec2 hit = sphereHit(origin, ray, AIR_RADIUS);
    if (hit.x > hit.y || hit.y < 0.0) return vec4(0.0);
    float start = max(hit.x, 0.0), end = min(hit.y, stop);
    if (end <= start) return vec4(0.0);
    const int STEPS = 4 + QUALITY * 3;
    float stepSize = (end - start) / float(STEPS);
    vec3 colour = vec3(0.0);
    float transmission = 1.0;
    float phase = 0.65 + 0.35 * pow(dot(-ray, light), 2.0);
    for (int i = 0; i < STEPS; i++) {
        vec3 p = origin + ray * (start + (float(i) + 0.5) * stepSize);
        float altitude = max(length(p) - 1.0, 0.0);
        float density = exp(-altitude / 0.014);
        float alpha = 1.0 - exp(-density * stepSize * 6.0);
        float lit = planetShadow(p, light);
        float sunset = exp(-pow((dot(normalize(p), light) + 0.025) / 0.17, 2.0));
        vec3 scatter = mix(mix(ColRingA.rgb, ColMoon.rgb, 0.40), ColPlanetRim.rgb, sunset * 0.8);
        colour += transmission * alpha * scatter * (0.035 + lit * phase * 1.45);
        transmission *= 1.0 - alpha;
    }
    return vec4(colour, 1.0 - transmission);
}

// Crater bowls and raised rims at three physical scales, fixed to each moon's rotating surface.
float craterField(vec3 n, float footprint) {
    float height = 0.0;
    float scale = 5.0;
    for (int octave = 0; octave < 2 + QUALITY / 2; octave++) {
        vec3 p = n * scale;
        vec3 cell = floor(p);
        float resolved = 1.0 - smoothstep(0.3, 0.8, footprint * scale);
        for (int z = -1; z <= 1; z++) for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
            vec3 id = cell + vec3(x, y, z);
            vec3 seed = hash3(id + 17.7);
            vec3 centre = id + 0.18 + seed * 0.64;
            float r = length(p - centre) / (0.22 + seed.x * 0.26);
            float bowl = -0.11 * (1.0 - smoothstep(0.0, 0.88, r));
            float rim = 0.035 * exp(-pow((r - 0.93) / 0.13, 2.0));
            height += (bowl + rim) * resolved / scale;
        }
        scale *= 2.7;
    }
    return height;
}

vec4 renderMoon(vec3 origin, vec3 ray, float footprint, vec4 m, int index, vec3 light, float flow, out float depth) {
    vec3 relative = origin - m.xyz;
    float cover = sphereCoverage(relative, ray, m.w, footprint);
    depth = 1.0e5;
    if (cover <= 0.0) return vec4(0.0);
    vec2 hit = sphereHit(relative, ray, m.w);
    depth = hit.x <= hit.y ? hit.x : -dot(relative, ray);
    if (depth < 0.0) return vec4(0.0);
    vec3 n = normalize(relative + ray * depth);
    vec3 p = m.xyz + n * m.w;
    vec3 ground = spin(n, flow * 0.018 + float(index) * 3.0);
    float fw = footprint / m.w;
    vec3 tangent = normalize(cross(n, abs(n.y) < 0.9 ? vec3(0, 1, 0) : vec3(1, 0, 0)));
    vec3 bitangent = cross(n, tangent);
    float epsilon = max(0.006, fw * 0.6);
    float h = craterField(ground, fw);
    float dx = craterField(spin(normalize(n + tangent * epsilon), flow * 0.018 + float(index) * 3.0), fw) - h;
    float dy = craterField(spin(normalize(n + bitangent * epsilon), flow * 0.018 + float(index) * 3.0), fw) - h;
    vec3 normal = normalize(n - (tangent * dx + bitangent * dy) * (2.8 / epsilon));
    float rock = detailNoise(ground * 7.0 + 5.0, fw * 7.0, QUALITY + 3).x;
    vec3 albedo = index == 0 ? mix(ColRingA.rgb, ColMoon.rgb, 0.78) : mix(ColPlanetA.rgb, ColMoon.rgb, 0.40);
    albedo *= (0.55 + rock * 0.6) * clamp(1.0 + h * 16.0, 0.5, 1.1);
    float lit = max(dot(normal, light), 0.0) * planetShadow(p, light);
    vec3 colour = albedo * (0.025 + lit * 1.3);
    if (index == 1) {
        float cracks = pow(1.0 - abs(noise3(ground * 8.0 + 31.0) * 2.0 - 1.0), 22.0);
        colour += mix(ColNebulaC.rgb, ColSun.rgb, 0.25) * cracks * (1.0 - smoothstep(-0.1, 0.25, dot(n, light))) * 0.35;
    }
    return vec4(colour * cover, cover);
}

void orderLayer(inout vec4 a, inout float da, inout vec4 b, inout float db) {
    if (db < da) {
        vec4 layer = a; a = b; b = layer;
        float depth = da; da = db; db = depth;
    }
}

vec4 planet(vec3 direction, float pixel, float flow, float life) {
    float cosine = dot(direction, SkyPlanet.xyz);
    if (cosine < 0.8) return vec4(0.0);
    float cameraDistance = sqrt(1.0 + 1.0 / (SkyPlanet.w * SkyPlanet.w));
    vec3 origin = vec3(0.0, 0.0, cameraDistance);
    vec3 ray = normalize(vec3(dot(direction, SkyPlanetRight.xyz), dot(direction, SkyPlanetUp.xyz), -cosine));
    vec3 light = SkyPlanetLight.xyz;
    float style = SkyPlanetLight.w;
    vec4 m0 = moonOrbit(0, flow), m1 = moonOrbit(1, flow);
    float footprint = max(pixel * cameraDistance, 0.00001);
    vec2 hit = sphereHit(origin, ray, 1.0);
    float cover = sphereCoverage(origin, ray, 1.0, footprint);
    float bodyDepth = 1.0e5;
    vec4 body = vec4(0.0);
    if (cover > 0.0) {
        bodyDepth = hit.x <= hit.y ? hit.x : -dot(origin, ray);
        vec3 n = normalize(origin + ray * bodyDepth);
        float surfaceFootprint = footprint / max(dot(n, -ray), 0.18);
        float shadow = sceneShadow(n, light, m0, m1, surfaceFootprint);
        vec3 colour = vec3(0.0);
        if (style < 0.999) colour += shadeGiant(n, light, flow, life, surfaceFootprint, shadow) * (1.0 - style);
        if (style > 0.001) colour += shadeWorld(n, n, -ray, light, flow, life, surfaceFootprint, shadow) * style;
        body = vec4(colour * cover, cover);
    }

    vec2 cloudHit = sphereHit(origin, ray, CLOUD_RADIUS);
    if (style > 0.001 && cloudHit.x < cloudHit.y) {
        vec3 p = origin + ray * cloudHit.x;
        vec3 n = normalize(p);
        float fw = footprint / max(dot(n, -ray), 0.22);
        vec2 weather = cloudField(n, flow, fw);
        float alpha = (1.0 - exp(-weather.x * 2.8 / max(dot(n, -ray), 0.3))) * style;
        alpha *= sphereCoverage(origin, ray, CLOUD_RADIUS, footprint);
        float ndl = dot(n, light);
        float shade = sceneShadow(p, light, m0, m1, fw) * planetShadow(p, light);
        vec3 white = mix(ColMoon.rgb, vec3(1.0), 0.72);
        float silver = pow(1.0 - max(dot(n, -ray), 0.0), 3.0) * smoothstep(-0.15, 0.4, ndl);
        vec3 colour = white * (0.025 + max(ndl + 0.08, 0.0) * shade * (0.85 + weather.y * 0.4));
        colour += ColPlanetRim.rgb * silver * 0.28;
        float flash = pow(max(sin(flow * 4.2 + weather.y * 150.0), 0.0), 40.0)
            * smoothstep(0.65, 0.85, weather.y) * (1.0 - smoothstep(-0.1, 0.2, ndl));
        colour += ColMoon.rgb * flash * life;
        body = over(vec4(colour * alpha, alpha), body);
    }

    vec4 air = atmosphere(origin, ray, cover > 0.0 ? bodyDepth : 1.0e5, light);
    body = over(air, body);
    // Polar curtains sit above the ground and below the outer atmosphere.
    if (cover > 0.0) {
        vec3 n = normalize(origin + ray * bodyDepth);
        float oval = exp(-pow((abs(dot(n, AXIS)) - 0.91) / 0.023, 2.0));
        float curtain = noise3(spin(n, SkyPlanetUp.w) * 34.0 + flow * 0.05);
        body.rgb += ColAuroraA.rgb * oval * curtain * life * (1.0 - smoothstep(-0.3, 0.2, dot(n, light))) * 0.25 * cover;
    }
    vec2 airHit = sphereHit(origin, ray, AIR_RADIUS);
    if (airHit.x < airHit.y) bodyDepth = min(bodyDepth, airHit.x);

    vec4 ring = vec4(0.0);
    float ringDepth = 1.0e5;
    float denominator = dot(ray, AXIS);
    if (abs(denominator) > 0.00001) {
        float t = -dot(origin, AXIS) / denominator;
        vec3 p = origin + ray * t;
        float radius = length(p);
        if (t > 0.0 && radius > RING_INNER - footprint * 4.0 && radius < RING_OUTER + footprint * 4.0) {
            float fw = footprint / max(abs(denominator), 0.08);
            float density = ringDensity(radius, fw);
            vec2 around = vec2(dot(p, EQUATOR_A), dot(p, EQUATOR_B)) / max(radius, 0.001);
            vec2 orbit = rotate2(around, flow * 0.045 * pow(radius / RING_INNER, -1.5));
            float clumps = noise3(vec3(orbit * 28.0, radius * 110.0));
            density *= mix(1.0, 0.8 + clumps * 0.4, 1.0 - smoothstep(0.007, 0.03, fw));
            float alpha = 1.0 - exp(-density * 0.78 / max(abs(denominator), 0.18));
            vec3 colour = mix(ColRingA.rgb, ColRingB.rgb, 0.5 + 0.5 * around.x);
            colour = mix(colour, ColMoon.rgb, (0.2 + 0.2 * noise2(vec2(radius * 80.0, 3.0))));
            float shade = planetShadow(p, light);
            colour *= (0.06 + shade * 0.94) * (0.65 + 0.35 * abs(dot(AXIS, light)));
            float glint = pow(noise3(vec3(orbit * 105.0, radius * 260.0)), 19.0)
                * (1.0 - smoothstep(0.004, 0.014, fw));
            colour += ColSun.rgb * glint * shade * 1.4;
            ring = vec4(colour * alpha, alpha);
            ringDepth = t;
        }
    }
    float d0, d1;
    vec4 moon0 = renderMoon(origin, ray, footprint, m0, 0, light, flow, d0);
    vec4 moon1 = renderMoon(origin, ray, footprint, m1, 1, light, flow, d1);
    orderLayer(body, bodyDepth, ring, ringDepth);
    orderLayer(moon0, d0, moon1, d1);
    orderLayer(body, bodyDepth, moon0, d0);
    orderLayer(ring, ringDepth, moon1, d1);
    orderLayer(ring, ringDepth, moon0, d0);
    return over(body, over(ring, over(moon0, moon1)));
}
