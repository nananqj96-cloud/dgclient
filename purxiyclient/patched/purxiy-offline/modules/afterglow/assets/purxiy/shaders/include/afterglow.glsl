#version 330

// Afterglow (see dev.purxiy.afterglow.PurxiyAfterglow): the fallen dissolve into light, texel by texel. Every
// dissolving entity is drawn with render types of its own, whose texture matrix carries its dissolve instead of a
// texture transform:
//
//   TextureMat[0]  xyz = the entity's feet (camera-relative), w = the front (see afterglowField)
//   TextureMat[1]  x = height of the model, y = yaw, z = flip (rad; see LivingEntityRenderer#setupRotations), w = seed
//   TextureMat[2]  xyz = the palette's first, second and accent colour (0xRRGGBB as floats), w = glow
//   TextureMat[3]  x = charge (the body fills with light), y = flash (the moment it falls), z = bottom of the model,
//                  w = unused
//
// The field and the noise are mirrored in dev.purxiy.afterglow.AfterglowField: light is released where texels go.

// How much the front frays (noise), and how scattered single texels go (their own random nudge).
const float FIELD_NOISE = 0.3;
const float TEXEL_JITTER = 0.12;
const float NOISE_SCALE = 3.1;

uint afterglowHash(ivec3 c, uint seed) {
    uint h = uint(c.x) * 0x8DA6B343u + uint(c.y) * 0xD8163841u + uint(c.z) * 0xCB1AB31Fu + seed * 0x9E3779B9u;
    h ^= h >> 16;
    h *= 0x7FEB352Du;
    h ^= h >> 15;
    h *= 0x846CA68Bu;
    h ^= h >> 16;
    return h;
}

float afterglowRandom(ivec3 c, uint seed) {
    return float(afterglowHash(c, seed) >> 8) / 16777215.0;
}

// Smooth value noise, 0..1.
float afterglowNoise(vec3 p, uint seed) {
    vec3 i = floor(p);
    vec3 f = p - i;
    vec3 u = f * f * (3.0 - 2.0 * f);
    ivec3 c = ivec3(i);
    float x00 = mix(afterglowRandom(c, seed), afterglowRandom(c + ivec3(1, 0, 0), seed), u.x);
    float x10 = mix(afterglowRandom(c + ivec3(0, 1, 0), seed), afterglowRandom(c + ivec3(1, 1, 0), seed), u.x);
    float x01 = mix(afterglowRandom(c + ivec3(0, 0, 1), seed), afterglowRandom(c + ivec3(1, 0, 1), seed), u.x);
    float x11 = mix(afterglowRandom(c + ivec3(0, 1, 1), seed), afterglowRandom(c + ivec3(1, 1, 1), seed), u.x);
    return mix(mix(x00, x10, u.y), mix(x01, x11, u.y), u.z);
}

// From camera-relative axes around the feet into the body's own frame: the yaw and the fall undone, y running from the
// feet (0) to the top of the head (height).
vec3 afterglowBody(vec3 local, float yaw, float flip) {
    float cy = cos(yaw), sy = sin(yaw);
    vec3 p = vec3(local.x * cy - local.z * sy, local.y, local.x * sy + local.z * cy);
    float cf = cos(flip), sf = sin(flip);
    return vec3(p.x * cf + p.y * sf, -p.x * sf + p.y * cf, p.z);
}

// When a point of the body goes (0..1): from the top of the model down, the front frayed by noise. A texel goes once
// the front passes afterglowField * (1 - TEXEL_JITTER) + its random nudge * TEXEL_JITTER.
float afterglowField(vec3 body, float bottom, float height, uint seed) {
    float h = clamp((body.y - bottom) / max(height, 0.1), 0.0, 1.0);
    float n = afterglowNoise(body * NOISE_SCALE, seed);
    return clamp((1.0 - h) * (1.0 - FIELD_NOISE) + n * FIELD_NOISE, 0.0, 1.0);
}

// Integers up to 2^24 travel exactly in a float, but adding 0.5 to one that large would round it to the next even
// number: round() instead.
uint afterglowInt(float value) {
    return uint(round(value));
}

vec3 afterglowColor(float rgb) {
    uint c = afterglowInt(rgb);
    return vec3(float((c >> 16) & 255u), float((c >> 8) & 255u), float(c & 255u)) / 255.0;
}
