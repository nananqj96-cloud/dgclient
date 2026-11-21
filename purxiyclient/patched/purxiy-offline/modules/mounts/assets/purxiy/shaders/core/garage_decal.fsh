#version 330

// The ride's mark on the world, read from the scene's depth so it lies on the real blocks, steps and slabs.
layout(std140) uniform GarageDecal {
    mat4 ClipToWorld;
    mat4 WorldToCar;
    vec4 Box;        // half width, half length, centre z, shadow strength
    vec4 SunCar;     // sun direction in car space, w strength
    vec4 Map;        // the ground picture: lo x, lo z, 1 / size x, 1 / size z
    vec4 Head[4];    // two lamps: position + level, direction
    vec4 Rear;       // hi z, lo z, tail, brake
    vec4 Misc;       // reverse, height
};
uniform sampler2D SceneDepth;
#ifdef SHADOW
uniform sampler2D GroundMap;
#endif

in vec2 texCoord;
out vec4 fragColor;

#ifdef SHADOW
vec4 ground(vec2 p) {
    vec2 uv = (p - Map.xy) * Map.zw;
    if (uv.x <= 0.0 || uv.y <= 0.0 || uv.x >= 1.0 || uv.y >= 1.0) return vec4(0.0);
    return texture(GroundMap, uv);
}
#endif

void main() {
    float depth = texture(SceneDepth, texCoord).r;
    if (depth >= 1.0) discard;
    vec4 p = ClipToWorld * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 world = p.xyz / p.w;
    vec3 q = (WorldToCar * vec4(world, 1.0)).xyz;
    if (abs(q.z - Box.z) > Box.y + 18.0 || abs(q.x) > Box.x + 18.0) discard;
#ifdef SHADOW
    if (q.y > 1.3 || q.y < -1.6) discard;
    // what covers this point, straight above: the darkness under the body and around the tyres
    float level = 1.0 - smoothstep(0.0, 0.9, abs(q.y));
    float occl = ground(q.xz).r * level;
    // the shadow the sun throws, from three heights of the body
    float sun = 0.0;
    if (SunCar.y > 0.08 && SunCar.w > 0.01) {
        vec2 lean = SunCar.xz / SunCar.y;
        float s1 = ground(q.xz + lean * max(0.2 - q.y, 0.0)).g;
        float s2 = ground(q.xz + lean * max(0.68 - q.y, 0.0)).b;
        float s3 = ground(q.xz + lean * max(Misc.y * 0.92 - q.y, 0.0)).a;
        sun = max(s1, max(s2, s3)) * SunCar.w;
    }
    float amount = 1.0 - (1.0 - occl * occl * 0.9 * Box.w) * (1.0 - sun * 0.46);
    if (amount <= 0.003) discard;
    fragColor = vec4(vec3(0.02, 0.022, 0.035), clamp(amount, 0.0, 0.93));
#else
    vec3 n = normalize(cross(dFdx(q), dFdy(q)));
    vec3 light = vec3(0.0);
    for (int i = 0; i < 2; i++) {
        float level = Head[i * 2].w;
        if (level <= 0.0) continue;
        vec3 from = Head[i * 2].xyz, dir = normalize(Head[i * 2 + 1].xyz);
        vec3 l = q - from;
        float dist = length(l);
        l /= max(dist, 0.001);
        float cone = smoothstep(0.86, 0.975, dot(l, dir));
        float reach = 1.0 / (1.0 + dist * dist * 0.055);
        float facing = max(dot(n, -l) * (dot(n, vec3(0.0, 1.0, 0.0)) > -0.5 ? 1.0 : 0.0), 0.0) * 0.8 + 0.2;
        light += vec3(1.0, 0.92, 0.78) * cone * reach * facing * level * 0.6;
    }
    // tail and brake lights colour the ground behind, reversing lights whiten it
    float behind = Rear.y - q.z;
    if (behind > -0.4 && behind < 4.5) {
        float spread = exp(-q.x * q.x * 0.5) * exp(-max(behind, 0.0) * 0.75) * max(n.y, 0.0) * smoothstep(-0.4, 0.8, behind);
        light += vec3(0.8, 0.02, 0.02) * spread * (Rear.z * 0.10 + Rear.w * 0.5);
        light += vec3(0.9) * spread * Misc.x * 0.22;
    }
    fragColor = vec4(light, 0.0);
#endif
}
