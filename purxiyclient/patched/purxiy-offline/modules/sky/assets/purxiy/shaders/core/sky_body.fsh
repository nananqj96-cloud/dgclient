#version 330

#moj_import <purxiy:sky.glsl>
#moj_import <purxiy:planet.glsl>

// Bounded sky passes, premultiplied. Planet geometry and materials live in planet.glsl.
in vec3 vRay;
flat in int vBody;
out vec4 fragColor;

// The singularity's disk: its axis (nearly in the plane of the sky, so we see the disk almost edge-on) and two axes in
// its plane, the second pointing away from us. Radii in shadow radii.
const vec3 DISK_AXIS = vec3(0.100, 0.978, 0.182);
const vec3 DISK_A = vec3(0.9948, -0.1017, 0.0);
const vec3 DISK_B = vec3(0.0185, 0.1811, -0.9831);
const float DISK_INNER = 2.6;
const float DISK_OUTER = 8.0;
const float JET_LENGTH = 14.0;

// ---------------------------------------------------------------------------------------------------------- singularity

// Light of the disk at radius r (in shadow radii), `around` its unit direction in the disk plane (DISK_A, DISK_B).
// The gas orbits faster close in; as a flow map (two phases, cross-faded) the spiral never winds up too tight.
vec3 diskLight(float r, vec2 around, float flow, float life) {
    if (r < DISK_INNER * 0.9 || r > DISK_OUTER) {
        return vec3(0.0);
    }
    float speed = 0.9 * pow(r / DISK_INNER, -1.5) * (0.5 + 0.5 * life);
    float phase = fract(flow / 20.0);
    float lr = log(r) * 7.0;
    float a = noise3(vec3(rotate2(around, speed * phase * 20.0) * 4.0, lr));
    float b = noise3(vec3(rotate2(around, speed * fract(phase + 0.5) * 20.0) * 4.0, lr + 13.0));
    float streaks = mix(b, a, 1.0 - abs(1.0 - 2.0 * phase));
    float profile = smoothstep(DISK_INNER * 0.9, DISK_INNER * 1.05, r) * pow(DISK_INNER / r, 2.0) * (1.0 - smoothstep(DISK_OUTER * 0.55, DISK_OUTER, r));
    vec3 col = mix(ColSun.rgb, ColGlow.rgb, smoothstep(DISK_INNER, DISK_INNER * 1.8, r));
    col = mix(col, ColNebulaB.rgb, smoothstep(DISK_INNER * 1.8, DISK_OUTER * 0.8, r));
    // the side coming towards us shines brighter
    float beaming = 1.0 + 0.65 * around.x;
    return col * profile * beaming * (0.45 + 0.9 * streaks);
}

// Two thin beams along the disk's axis, widening as they go, with knots of light racing outwards. The one leaning
// towards us is brighter.
vec3 jets(vec2 q, float pxq, float flow, float life) {
    vec2 axis = normalize(DISK_AXIS.xy);
    float along = dot(q, axis);
    float a = abs(along);
    if (a < 1.1 || a > JET_LENGTH) {
        return vec3(0.0);
    }
    float across = dot(q, vec2(-axis.y, axis.x));
    float width = 0.08 + a * 0.035 + pxq * 0.7;
    float beam = exp(-across * across / (width * width));
    if (beam < 0.002) {
        return vec3(0.0);
    }
    float side = along > 0.0 ? 1.0 : -1.0;
    float knots = noise3(vec3(a * 0.9 - flow * 1.6, side * 7.0, across / width));
    float reach = smoothstep(1.1, 2.4, a) * (1.0 - smoothstep(JET_LENGTH * 0.35, JET_LENGTH, a));
    float towards = side * DISK_AXIS.z > 0.0 ? 1.0 : 0.55;
    vec3 col = mix(ColAuroraB.rgb, ColStar.rgb, 0.35 + 0.4 * exp(-a * 0.25));
    return col * beam * reach * (0.25 + 0.75 * knots * knots) * towards * (0.5 + 0.5 * life) * 0.55;
}

// The singularity, premultiplied: black where its shadow is, light added everywhere else.
vec4 singularity(vec3 d, float px, float flow, float life) {
    float cd = dot(d, SkyHole.xyz);
    if (cd < 0.8) {
        return vec4(0.0);
    }
    vec3 v = d / cd - SkyHole.xyz;
    vec2 q = vec2(dot(v, SkyHoleRight.xyz), dot(v, SkyHoleUp.xyz)) / SkyHole.w;
    float pxq = px / SkyHole.w;
    float rho = length(q);
    float shadow = clamp((1.0 - rho) / pxq + 0.5, 0.0, 1.0);
    vec3 light = jets(q, pxq, flow, life);

    if (rho < DISK_OUTER * 1.3) {
        // the disk itself, straight to the eye: its far half is hidden behind the shadow
        vec3 X = vec3(q, -(q.x * DISK_AXIS.x + q.y * DISK_AXIS.y) / DISK_AXIS.z);
        float r = length(X);
        if (X.z > 0.0 || rho > 1.0) {
            light += diskLight(r, vec2(dot(X, DISK_A), dot(X, DISK_B)) / r, flow, life) * (X.z > 0.0 ? 1.0 : 1.0 - shadow);
        }
        // the far side of the disk, bent up over the top of the shadow and under its bottom
        if (rho > 1.0) {
            vec2 dir = q / rho;
            float r2 = DISK_INNER * 0.95 + (rho - 1.05) * 3.4;
            float arc = smoothstep(1.0, 1.1, rho) * (1.0 - smoothstep(1.8, 3.0, rho));
            float side = dir.y > 0.0 ? 1.0 : 0.55;
            light += diskLight(r2, vec2(dir.x, abs(dir.y)), flow, life) * arc * side * pow(abs(dir.y), 0.6) * 1.25;
        }
        // light that orbits just outside the shadow
        light += ColSun.rgb * exp(-(rho - 1.05) * (rho - 1.05) / pow(0.014 + pxq * 0.7, 2.0)) * 0.9;
    }
    // and a soft glow around it all
    light += ColGlow.rgb * exp(-max(rho - 1.0, 0.0) * 0.6) * 0.07 * smoothstep(1.0 - pxq, 1.0 + pxq, rho);
    return vec4(light, shadow);
}

void main() {
    vec3 d = normalize(vRay);
    float px = max(length(dFdx(d)), length(dFdy(d)));
    float flow = SkyTime.x;
    float life = SkyAmount.w;
    float night = 1.0 - SkyState.x;
    vec4 col;
    if (vBody == 0) {
        float vis = SkyPlanetRight.w * mix(0.6, 1.0, night);
        col = planet(d, px, flow, life) * vis;
        // by day a little of the sky's light lies over it
        col.a *= 1.0 - 0.3 * SkyState.x;
    } else {
        float vis = SkyHoleRight.w * mix(0.35, 1.0, night);
        col = singularity(d, px, flow, life) * vis;
    }
    col.rgb += (hash12(gl_FragCoord.xy) - 0.5) * (1.0 / 255.0) * col.a;
    fragColor = col * SkyMisc.w;
}
