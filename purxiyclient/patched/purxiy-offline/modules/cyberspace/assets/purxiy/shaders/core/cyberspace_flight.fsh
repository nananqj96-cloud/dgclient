#version 330

#moj_import <purxiy:cyberspace.glsl>
#moj_import <purxiy:cyberspace_common.glsl>
#moj_import <purxiy:cyberspace_flight.glsl>

// A packet of data or a bit (see dev.purxiy.cyberspace.DataPackets), drawn after the picture was redrawn. The head is a
// real little cube, found per pixel (the ray from your eye against its box, turned with it): its edges are lines of
// light, its faces glass, and a glow around it. A trail is a ribbon of light that fades towards its end. Both are
// hidden behind whatever stands in front of them (the scene's depth is read here, the pass has none of its own).

uniform sampler2D DepthSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

in vec3 position;
in vec2 local;
flat in int object;
flat in int part;

out vec4 fragColor;

const float RIBBON = 0.3;

float sceneDistance() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    float depth = texelFetch(DepthSampler, px, 0).r;
    if (depth >= 1.0) {
        return 1.0e9;
    }
    vec2 ndc = gl_FragCoord.xy / CyScreen.xy * 2.0 - 1.0;
    vec4 p = CyInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return length(p.xyz / p.w);
}

mat3 turn(float angle) {
    // about a slanted axis, so it tumbles
    vec3 axis = normalize(vec3(0.6, 1.0, 0.35));
    float s = sin(angle);
    float c = cos(angle);
    float k = 1.0 - c;
    return mat3(
        c + axis.x * axis.x * k, axis.y * axis.x * k + axis.z * s, axis.z * axis.x * k - axis.y * s,
        axis.x * axis.y * k - axis.z * s, c + axis.y * axis.y * k, axis.z * axis.y * k + axis.x * s,
        axis.x * axis.z * k + axis.y * s, axis.y * axis.z * k - axis.x * s, c + axis.z * axis.z * k);
}

void main() {
#ifdef MASKED
    if (texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    vec4 head = FlObjects[object * 4];
    vec4 bend = FlObjects[object * 4 + 1];
    vec4 end = FlObjects[object * 4 + 2];
    vec4 look = FlObjects[object * 4 + 3];
    float visible = bend.w;
    float pixelAngle = 2.0 / (CyScreen.z * CyScreen.y);
    float glow = CyScreen.w;
    vec3 light = mix(CyFirst.rgb, CyAccent.rgb, look.x);
    vec3 white = mix(light, vec3(1.0), 0.6);
    float sceneDist = sceneDistance();

    vec3 color = vec3(0.0);
    float cover = 0.0;
    if (part == 0) {
        vec3 ray = normalize(position);
        vec3 centre = head.xyz;
        float s = head.w;
        mat3 frame = look.y > 0.5 ? turn(look.z) : mat3(1.0);
        vec3 ro = frame * -centre;
        vec3 rd = frame * ray;
        vec3 inv = 1.0 / rd;
        vec3 t0 = (-s - ro) * inv;
        vec3 t1 = (s - ro) * inv;
        vec3 tmin = min(t0, t1);
        vec3 tmax = max(t0, t1);
        float tn = max(max(tmin.x, tmin.y), tmin.z);
        float tf = min(min(tmax.x, tmax.y), tmax.z);
        if (tf > max(tn, 0.0) && tn < sceneDist) {
            vec3 a = abs(ro + rd * tn);
            vec2 other = a.x >= a.y && a.x >= a.z ? a.yz : a.y >= a.z ? a.xz : a.xy;
            float edge = s - max(other.x, other.y);
            float fw = tn * pixelAngle;
            float wire = line(edge, s * 0.14, fw);
            color = mix(light * 0.55, white * 1.4, wire);
            cover = mix(0.35, 1.0, wire);
        }
        // a glow around it, where it is not hidden
        float along = dot(ray, centre);
        float miss = length(ray * along - centre);
        if (along < sceneDist) {
            float halo = exp(-max(miss - s, 0.0) / (s * 1.1)) * (0.35 + 0.5 * glow) * (1.0 - 0.8 * CyLook.x);
            color += light * halo * (1.0 - cover);
        }
        color *= look.w;
        cover *= look.w;
    } else {
        vec3 a = part == 1 ? head.xyz : bend.xyz;
        vec3 b = part == 1 ? bend.xyz : end.xyz;
        float first = distance(head.xyz, bend.xyz);
        float total = first + distance(bend.xyz, end.xyz);
        float from = part == 1 ? local.x * first : first + local.x * (total - first);
        float fade = total > 1.0e-4 ? pow(1.0 - clamp(from / total, 0.0, 1.0), 1.6) : 0.0;
        float dist = length(position);
        if (dist > sceneDist + 0.05) {
            discard;
        }
        float across = abs(local.y) * RIBBON;
        float fw = dist * pixelAngle;
        float core = line(across, 0.026, fw);
        float halo = exp(-across / 0.07) * (0.35 + 0.5 * glow) * (1.0 - 0.6 * CyLook.x);
        // right past your eye a trail would fill the view: it thins out there
        float close = smoothstep(0.8, 4.0, dist);
        color = mix(CySecond.rgb, white, fade * 0.8) * (core * 1.2 + halo) * fade * end.w * close;
        cover = core * fade * 0.5 * close;
    }
    // fading in and out, near the eye and far away
    float near = length(head.xyz);
    visible *= smoothstep(0.6, 2.0, near) * (1.0 - smoothstep(38.0, 52.0, near));
    color *= visible;
    cover *= visible;
    if (max(color.r, max(color.g, color.b)) < 0.002 && cover < 0.002) {
        discard;
    }
    // light adds on midnight; on porcelain it is drawn as ink
    vec3 ink = mix(pow(CySecond.rgb, vec3(1.6)), CySecond.rgb, 0.35) * 0.8;
    float inkCover = clamp(luma(color) * 1.3 + cover, 0.0, 0.95);
    vec4 night = vec4(color, cover * 0.4);
    vec4 day = vec4(ink * inkCover, inkCover);
    fragColor = mix(night, day, CyLook.x);
}
