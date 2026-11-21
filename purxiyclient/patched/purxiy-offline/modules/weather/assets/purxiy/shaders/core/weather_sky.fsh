#version 330

#moj_import <purxiy:weather_surface.glsl>

// The sky dome the wet ground mirrors where the scene has nothing to show (see dev.purxiy.weather.WeatherSurface):
// one texel per direction above the horizon. A direction in view takes the colour of the sky there; one out of view
// slowly takes its neighbours', so the sky you last saw spreads to where you cannot see it.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D PreviousSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

// keep in sync with weather_surface.fsh
const float DOME_W = 32.0;
const float DOME_H = 8.0;
const float TAU = 6.28318530718;

vec3 previous(ivec2 texel) {
    return texelFetch(PreviousSampler, ivec2((texel.x + int(DOME_W)) % int(DOME_W), clamp(texel.y, 0, int(DOME_H) - 1)), 0).rgb;
}

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);
    float azimuth = (gl_FragCoord.x / DOME_W - 0.5) * TAU;
    float elevation = (gl_FragCoord.y - 0.5) / (DOME_H - 1.0) * TAU * 0.25;
    vec3 dir = vec3(cos(elevation) * sin(azimuth), sin(elevation), -cos(elevation) * cos(azimuth));
    // the game's own sky colours: where the dome starts from, and where a direction long out of view drifts to
    vec3 game = mix(SurfaceHorizon.rgb, SurfaceZenith.rgb, smoothstep(0.0, 0.6, dir.y));
    vec3 old = SurfaceProbe.z > 0.5 ? game : previous(texel);

    // the sky where this direction shows on the screen, over a patch about the texel's size
    vec3 sum = vec3(0.0);
    float found = 0.0;
    vec4 c = SurfaceViewProj * vec4(dir, 0.0);
    if (c.w > 1.0e-4) {
        vec2 centre = c.xy / c.w * 0.5 + 0.5;
        vec2 spread = vec2(0.045 * SurfaceScreen.y / SurfaceScreen.x, 0.045);
        for (int k = 0; k < 9; k++) {
            vec2 uv = centre + (vec2(k % 3, k / 3) - 1.0) * spread;
            bool inside = all(greaterThan(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)));
#ifdef MASKED
            // your hand before the sky is not the sky
            inside = inside && textureLod(HandMaskSampler, uv, 0.0).r < 0.5;
#endif
            if (inside && textureLod(DepthSampler, uv, 0.0).r >= 1.0) {
                sum += textureLod(SceneSampler, uv, 0.0).rgb;
                found += 1.0;
            }
        }
    }
    vec3 color;
    if (found > 0.0) {
        color = mix(old, sum / found, SurfaceProbe.x * found / 9.0);
    } else {
        vec3 around = (previous(texel + ivec2(1, 0)) + previous(texel - ivec2(1, 0))
            + previous(texel + ivec2(0, 1)) + previous(texel - ivec2(0, 1))) * 0.25;
        color = mix(old, mix(around, game, 0.3), SurfaceProbe.z > 0.5 ? 0.0 : SurfaceProbe.y);
    }
    fragColor = vec4(color, 1.0);
}
