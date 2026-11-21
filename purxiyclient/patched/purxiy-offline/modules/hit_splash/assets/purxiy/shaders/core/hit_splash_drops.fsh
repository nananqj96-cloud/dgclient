#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:hit_splash.glsl>

// A droplet of Hit Splash in flight (see dev.purxiy.hitsplash.SplashRenderer): a small bead of light drawn out along
// the way it moves (as a camera sees something fast), dimmer towards where it was, in a glow of the palette. Or the flash
// where a hit struck: a thin ring of light that opens around a soft spark and fades. Tested against the scene's depth,
// softly, so bodies and blocks in front cover it.

uniform sampler2D DepthSampler;

flat in vec2 head;
flat in vec2 tail;
flat in float radius;
flat in float reach;
flat in float bright;
flat in float away;
flat in int flash;

out vec4 fragColor;

float sceneDistance() {
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec2 ndc = gl_FragCoord.xy / SplashScreen.xy * 2.0 - 1.0;
    vec4 scene = SplashInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return length(scene.xyz / scene.w);
}

void main() {
    vec2 pa = gl_FragCoord.xy - tail;
    vec2 ba = head - tail;
    float along = clamp(dot(pa, ba) / max(dot(ba, ba), 1.0e-4), 0.0, 1.0);
    float d = length(pa - ba * along);
    vec3 pearl = mix(SplashFirst.rgb, vec3(1.0), 0.5);
    vec3 halo = mix(SplashSecond.rgb, SplashFirst.rgb, 0.5);
    vec4 result;
    if (flash == 1) {
        // the ring opens as the flash fades; its spark is gone first
        float ring = radius * 1.7;
        float width = max(1.2, radius * 0.14);
        float line = clamp(width + 0.5 - abs(d - ring), 0.0, 1.0);
        float spark = exp(-d * d / (2.0 * radius * radius * 0.12)) * bright;
        float glow = exp(-pow(abs(d - ring) / (width * 3.0 + 2.0), 2.0)) * 0.45 + exp(-d * d / (2.0 * reach * reach)) * 0.35 * bright;
        vec3 rgb = mix(pearl, vec3(1.0), 0.3) * (line * 0.9 + spark) + halo * glow;
        result = vec4(rgb, (line * 0.9 + spark) * 0.7 + glow * 0.25) * bright;
    } else {
        float fade = mix(0.4, 1.0, along * along);
        float bead = clamp(radius + 0.5 - d, 0.0, 1.0) * fade;
        float glow = exp(-d * d / (2.0 * reach * reach)) * fade * (1.0 - bead);
        result = vec4(pearl * bead + halo * glow * 0.7, bead * 0.85 + glow * 0.22) * min(bright, 1.0);
    }
    float visible = clamp((sceneDistance() - away) / 0.12 + 1.0, 0.0, 1.0);
    result *= visible;
    if (result.a <= 0.002 && result.r + result.g + result.b <= 0.004) {
        discard;
    }
    float fog = total_fog_value(away, away, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);
    fragColor = result;
}
