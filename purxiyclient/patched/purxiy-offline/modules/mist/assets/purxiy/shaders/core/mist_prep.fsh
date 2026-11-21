#version 330

#moj_import <purxiy:mist.glsl>

// The mist's field, prepared from the terrain map (R ground, G canopy, B water, A known; heights relative to the base) in
// two passes over the whole wrap-around map. First (BLUR_X) the ground, water and what is known are blurred along x;
// then along z, and the sunlight's shade is found: per column, the height below which the sun (or the moon) is hidden by
// a canopy or a hill towards it. The blurred ground is the floor the mist lies on: lower than a hilltop, so hills rise
// out of it, and higher than the bottom of a hollow, so mist pools there. Ground far above or below the column's own
// (a floating platform, a tower, the far side of a cliff) counts for little, so it does not lift the floor into the sky.
//
// Out: R the floor (relative to the base, /255), G the shade (likewise; after BLUR_X: how much is known), B water, A how
// much of the floor is known (after BLUR_X: how much R stands for).

#ifdef BLUR_X
uniform sampler2D TerrainSampler;
#else
uniform sampler2D BlurSampler;
uniform sampler2D TerrainSampler;
#endif

out vec4 fragColor;

const int RADIUS = 6;
const float SPACING = 2.0;
// how different (blocks) a column's ground may be from this one's and still count fully
const float RANGE = 12.0;

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);
    int size = int(MIST_SIZE);
#ifdef BLUR_X
    ivec2 axis = ivec2(1, 0);
    vec4 own = texelFetch(TerrainSampler, texel, 0);
    float centre = own.r * 255.0;
    bool known = own.a > 0.01;
#else
    ivec2 axis = ivec2(0, 1);
    vec4 own = texelFetch(BlurSampler, texel, 0);
    float centre = own.r * 255.0;
    bool known = own.a > 0.004;
#endif
    float ground = 0.0, weight = 0.0, water = 0.0, seen = 0.0, total = 0.0;
    for (int i = -RADIUS; i <= RADIUS; i++) {
        ivec2 at = (texel + axis * int(float(i) * SPACING) + size) % size;
        float x = float(i) / 3.2;
        float w = exp(-0.5 * x * x);
#ifdef BLUR_X
        vec4 v = texelFetch(TerrainSampler, at, 0);
        float g = v.r * 255.0, k = v.a, s = v.a;
#else
        vec4 v = texelFetch(BlurSampler, at, 0);
        float g = v.r * 255.0, k = v.a, s = v.g;
#endif
        // the centre column counts its neighbours' ground only as far as it lies near its own
        float d = (g - centre) / RANGE;
        float near = known ? exp(-d * d) : 1.0;
        ground += g * k * near * w;
        weight += k * near * w;
        water += v.b * w;
        seen += s * w;
        total += w;
    }
#ifdef BLUR_X
    float mean = weight > 1.0e-4 ? ground / weight : centre;
    fragColor = vec4(clamp(mean / 255.0, 0.0, 1.0), seen / total, water / total, weight / total);
#else
    float floorHeight = weight > 1.0e-4 ? ground / weight : centre;
    // the sunlight's shade: the highest a canopy towards the light reaches, less how far the light climbs on the way
    vec2 column = vec2(texel) + 0.5;
    float shade = texelFetch(TerrainSampler, texel, 0).g * 255.0;
    vec3 light = MistSunDir.xyz;
    float across = length(light.xz);
    if (light.y > 0.0 && across > 0.001) {
        vec2 toward = light.xz / across;
        float climb = light.y / across;
        float k = 1.0;
        for (int i = 0; i < 40; i++) {
            float canopy = texture(TerrainSampler, (column + toward * k) / MIST_SIZE).g * 255.0;
            shade = max(shade, canopy - climb * k);
            if (climb * k > 255.0) {
                break;
            }
            k += i < 16 ? 1.0 : 2.0;
        }
    }
    fragColor = vec4(clamp(floorHeight / 255.0, 0.0, 1.0), clamp(shade / 255.0, 0.0, 1.0), water / total, seen / total);
#endif
}
