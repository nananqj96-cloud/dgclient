#version 330

// Shared by the passes of the Purxiy sky (see dev.purxiy.sky.PurxiySky): the soft layers at half resolution
// (sky_space.fsh), the sky itself (sky.fsh), and the planet and singularity on top (sky_body.fsh). QUALITY (0 low,
// 1 medium, 2 high) is compiled in.

uniform sampler2D NoiseSampler; // 256x256 value noise: r and b random, g = r and a = b shifted by (37, 17)

layout(std140) uniform PurxiySky {
    mat4 SkyRays;        // the view ray through a point of the screen: mat3(SkyRays) * vec3(ndc, 1)
    mat4 SkyCelestial;   // world direction -> celestial frame (turns with the time of day and the drift)
    vec4 SkySun;         // xyz = sun direction, w = sun and moon visibility
    vec4 SkyMoon;        // xyz = moon direction, w = phase angle (0 = full moon)
    vec4 SkyPlanet;      // xyz = planet direction, w = tangent of its angular radius
    vec4 SkyPlanetRight; // xyz = planet's right axis, w = planet visibility
    vec4 SkyPlanetUp;    // xyz = planet's up axis, w = spin (radians)
    vec4 SkyPlanetLight; // xyz = light direction in planet space (z towards the viewer), w = 0 gas giant .. 1 living world
    vec4 SkyPlanetRect;  // screen rectangle of the planet's quad (ndc x0, y0, x1, y1)
    vec4 SkyHole;        // xyz = direction of the singularity, w = angular radius of its shadow
    vec4 SkyHoleRight;   // xyz = its right axis, w = visibility
    vec4 SkyHoleUp;      // xyz = its up axis, w = Einstein radius (how far it bends light)
    vec4 SkyHoleRect;    // screen rectangle of the singularity's quad
    vec4 SkyComet;       // xyz = direction of the comet's head, w = brightness (0 = no comet)
    vec4 SkyCometTail;   // xyz = direction its tails stream in, at the head; w = tail length (radians)
    vec4 SkyShower;      // xyz = radiant of a meteor shower, w = its strength
    vec4 SkyTime;        // x = flow (s), y = morph (s), z = breathing phase (rad), w = shooting stars
    vec4 SkyAmount;      // x = nebula, y = stars, z = aurora, w = liveliness
    vec4 SkyState;       // x = daylight, y = dawn/dusk, z unused (the weather never changes the sky), w = lightning flash
    vec4 SkyMisc;        // xy = 1 / screen size (px), z = soft layers drawn, w = opacity
    vec4 SkyLife;        // x = vanilla sky fog (under water), y = aurora surge
    vec4 SkyStyles;      // eased weights: Aether, Nocturne, Amethyst, Opaline
    vec4 SkyWeatherStyles; // xyz = Rain, Snow, Fire; w unused; remainder of all style weights = Galaxy
    vec4 SkyStyleParams; // x = pattern intensity, y = brightness
    vec4 ColZenith;
    vec4 ColMid;
    vec4 ColHorizon;
    vec4 ColGlow;
    vec4 ColNebulaA;
    vec4 ColNebulaB;
    vec4 ColNebulaC;
    vec4 ColAuroraA;
    vec4 ColAuroraB;
    vec4 ColPlanetA;
    vec4 ColPlanetB;
    vec4 ColPlanetRim;
    vec4 ColRingA;
    vec4 ColRingB;
    vec4 ColStar;
    vec4 ColSun;
    vec4 ColMoon;
    vec4 SkyTint[14];    // the colours of Aether .. Fire, two per style (see skyTint in sky_styles.glsl); Galaxy's are above
};

const float TAU = 6.28318531;

// Normal of the great circle the nebula gathers around, in the celestial frame: close to the axis the sky turns
// around, so the river arches from east to west and sways a little over the day.
const vec3 RIVER = vec3(0.880, 0.440, 0.179);

// How much of deep space the horizon's haze hides.
float skyHaze(vec3 d) {
    return exp(-abs(d.y) * 9.0) * 0.92;
}

vec2 rotate2(vec2 v, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// Gravitational lensing by the singularity (a point lens, tapered off so that it ends smoothly): the direction the
// light seen along d comes from. Stars and nebula behind it are looked up through this.
vec3 lensed(vec3 d) {
    if (SkyHoleRight.w <= 0.0) {
        return d;
    }
    vec3 hole = SkyHole.xyz;
    float reach = SkyHole.w * 7.0;
    float cosTheta = dot(d, hole);
    if (cosTheta < cos(reach)) {
        return d;
    }
    vec3 across = d - hole * cosTheta;
    float s = length(across);
    if (s < 1.0e-6) {
        return d;
    }
    float theta = atan(s, cosTheta);
    float bend = SkyHoleUp.w * SkyHoleUp.w / theta * (1.0 - smoothstep(reach * 0.45, reach, theta)) * SkyHoleRight.w;
    float beta = theta - bend;
    return hole * cos(beta) + across / s * sin(beta);
}

// ---------------------------------------------------------------------------------------------------------- noise

// Smooth 3D value noise with a single texture fetch (the green channel holds the next z slice).
float noise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xy + vec2(37.0, 17.0) * p.z + f.xy;
    vec2 rg = textureLod(NoiseSampler, (uv + 0.5) / 256.0, 0.0).xy;
    return mix(rg.x, rg.y, f.z);
}

// Two independent 3D value noises from the same single fetch (red/green and blue/alpha).
vec2 noise3x2(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xy + vec2(37.0, 17.0) * p.z + f.xy;
    vec4 t = textureLod(NoiseSampler, (uv + 0.5) / 256.0, 0.0);
    return mix(t.xz, t.yw, f.z);
}

float noise2(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return textureLod(NoiseSampler, (p + f + 0.5) / 256.0, 0.0).z;
}

const mat3 OCTAVE = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

// Two independent fractal noises at once, each roughly 0..1 around 0.5.
vec2 fbm2(vec3 p, int octaves) {
    vec2 sum = vec2(0.0);
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) {
            break;
        }
        sum += amp * noise3x2(p);
        norm += amp;
        p = OCTAVE * p * 2.03;
        amp *= 0.5;
    }
    return sum / norm;
}

// Hash of a lattice point (Dave Hoskins, "Hash without Sine", MIT). Floats only: integer conversions run at a fraction
// of the speed on many GPUs, and the stars hash every pixel.
vec3 hash3(vec3 p) {
    vec3 p3 = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
