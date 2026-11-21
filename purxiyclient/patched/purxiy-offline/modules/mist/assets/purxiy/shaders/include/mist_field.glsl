#version 330

// The mist itself, shared by the march (on a grid of half the screen's size) and the composite (which marches again, with
// fewer steps, for pixels the grid does not see: a fence post or a blade of grass in front of the far distance). Needs
// mist.glsl.
//
// The mist lies on the blurred ground (lower than a hilltop, higher than the bottom of a hollow), thins with height, never
// goes below the real ground (caves and houses stay clear), drifts in soft banks and wisps, is thicker over water and is
// kept clear around you. Each step adds the light the mist scatters towards you (the open sky's, the sun's where its
// beams reach, a lantern's) and takes away what lies behind it.

uniform sampler2D TerrainSampler;
uniform sampler2D FieldSampler;
uniform sampler2D NoiseSampler;
#ifdef LIGHTS
uniform sampler2D LightSampler;
#endif

// value noise from the noise texture: two slices per texel (R and G, G being R a slice further), smooth in all three axes
float mistNoise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xz + vec2(37.0, 17.0) * p.y + f.xz;
    vec2 rg = textureLod(NoiseSampler, (uv + 0.5) / 256.0, 0.0).rg;
    return mix(rg.x, rg.y, f.y);
}

// a second, independent noise (B and A)
float mistNoise2(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xz + vec2(37.0, 17.0) * p.y + f.xz;
    vec2 ba = textureLod(NoiseSampler, (uv + 0.5) / 256.0, 0.0).ba;
    return mix(ba.x, ba.y, f.y);
}

// The mean of exp(-k * max(h, 0)) while h goes evenly from a to b: how thick a layer that thins with height is, on average,
// along one step of a ray (exact, so a long step through it far away gives the same as many short ones would).
float mistBand(float a, float b, float k) {
    float lo = min(a, b), hi = max(a, b);
    float span = hi - lo;
    if (span < 1.0e-3) {
        return exp(-k * max(lo, 0.0));
    }
    float below = clamp(-lo, 0.0, span);
    float above = hi > 0.0 ? (exp(-k * max(lo, 0.0)) - exp(-k * hi)) / k : 0.0;
    return (below + above) / span;
}

// Henyey-Greenstein, scaled so that light scattered evenly is 1
float mistPhase(float c, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(max(1.0 + g2 - 2.0 * g * c, 1.0e-4), 1.5);
}

#ifdef LIGHTS
// Block light around the camera, 0..1, from the light volume (see MistLights): cells of 2 x 2 x 2 blocks, 64 across and 32
// layers up, laid out as 8 x 4 tiles of 66 x 66 texels (cells at 1..64, and at 65 cell 0 again, so the filter wraps). It
// reaches at least 56 blocks out and 24 up and down; p is the point relative to the camera.
float mistLantern(vec3 world, vec3 p) {
    float reach = (1.0 - smoothstep(48.0, 56.0, max(abs(p.x), abs(p.z)))) * (1.0 - smoothstep(18.0, 24.0, abs(p.y)));
    if (reach <= 0.0) {
        return 0.0;
    }
    vec2 inTile = mod(world.xz * 0.5 - 0.5, 64.0) + 1.5;
    float layer = world.y * 0.5 - 0.5;
    float l0 = floor(layer);
    float s0 = mod(l0, 32.0), s1 = mod(l0 + 1.0, 32.0);
    vec2 tile0 = vec2(mod(s0, 8.0), floor(s0 / 8.0)) * 66.0;
    vec2 tile1 = vec2(mod(s1, 8.0), floor(s1 / 8.0)) * 66.0;
    const vec2 atlas = vec2(528.0, 264.0);
    float a = textureLod(LightSampler, (tile0 + inTile) / atlas, 0.0).r;
    float b = textureLod(LightSampler, (tile1 + inTile) / atlas, 0.0).r;
    return mix(a, b, layer - l0) * reach;
}
#endif

// Where step boundary u (0..1) of a ray out to far blocks lies: most steps in the first 48 blocks, where the shape of the
// mist, its beams and wisps show (growing as u^1.5 there), the rest out to far (growing as u^2).
float mistStepAt(float u, float far) {
    const float NEAR = 48.0, SPLIT = 0.6;
    if (far <= NEAR) {
        return far * u * sqrt(u);
    }
    if (u <= SPLIT) {
        float s = u / SPLIT;
        return NEAR * s * sqrt(s);
    }
    float v = (u - SPLIT) / (1.0 - SPLIT);
    return NEAR + (far - NEAR) * v * v;
}

// The mist along a view ray from the camera, out to far blocks: the light it adds (rgb) and how much of what lies behind
// it it hides (a).
vec4 mistMarch(vec3 dir, float far, int steps, float jitter) {
    float presence = MistReach.w;
    float sigma = MistShape.x;
    float clear = max(MistReach.x, 0.5);
    float base = MistCamera.w;
    float regional = MistReach.z;

    // light towards the sun (or moon): a halo close around it and a soft, wide glow, the same along the whole ray
    float c = dot(dir, MistSunDir.xyz);
    float halo = mistPhase(c, 0.75) / mistPhase(1.0, 0.75);
    float wide = mistPhase(c, 0.3) / mistPhase(1.0, 0.3);
    vec3 sun = MistSun.rgb * (1.0 + MistSun.w * (MistCreatures.z * halo + MistCreatures.w * wide));

    float transmit = 1.0;
    vec3 light = vec3(0.0);
    // steps grow with distance: fine near you, where the shape of the mist shows (each step ends where the next begins)
    float next = mistStepAt(jitter / float(steps), far);
    for (int i = 0; i < steps; i++) {
        float t = next;
        next = min(mistStepAt(min((float(i) + 1.0 + jitter) / float(steps), 1.0), far), far);
        float dt = next - t;
        if (dt <= 0.0) {
            break;
        }
        vec3 p = dir * (t + 0.5 * dt);
        vec3 world = MistCamera.xyz + p;
        vec2 column = world.xz / MIST_SIZE;
        vec4 raw = texture(TerrainSampler, column);
        vec4 field = texture(FieldSampler, column);

        // the real ground (no mist below it) and the mist's own floor (the blurred ground); columns not known, and those
        // beyond the map (which wraps: there it holds other places), take the ground near you
        float inside = 1.0 - smoothstep(MIST_SIZE * 0.4, MIST_SIZE * 0.48, max(abs(p.x), abs(p.z)));
        float known = smoothstep(0.0, 0.6, raw.a) * inside;
        float ground = mix(regional, base + raw.r * 255.0 / max(raw.a, 0.02), known);
        float spread = smoothstep(0.0, 0.6, field.a) * inside;
        // the floor follows the land, but not far above the ground around you: mountains rise out of the mist
        float floorHeight = min(mix(regional, base + field.r * 255.0, spread), regional + 32.0);

        // how high above the floor the step starts and ends: the layer and the veil over the whole step
        float rise = dir.y * 0.5 * dt;
        float above = max(world.y - floorHeight, 0.0);
        float layer = mistBand(world.y - rise - floorHeight, world.y + rise - floorHeight, MistShape.y);
        float veil = MistShape.z * mistBand(world.y - rise - floorHeight, world.y + rise - floorHeight, MistShape.w);
        float dry = smoothstep(ground - 2.5, ground + 0.35, world.y);
        float near = 1.0 - exp(-(t * t) / (clear * clear));
        float base0 = sigma * (layer * 1.6 + veil) * dry * near * presence;
        if (base0 < 1.0e-6) {
            // no mist here (inside the ground, or right around you): nothing to look up
            continue;
        }

        // drifting banks and wisps (the wisps live in the layer; above it the mist is an even veil)
        // (scales across are whole fractions of the drift's wrap, so the noise never jumps when it wraps)
        vec3 q = MistDrift.xyz + p;
        // a step longer than the noise's own size sees its mean: far out, where the steps are long, the banks and wisps
        // fade into an even mist instead of into grain
        float banksDetail = 1.0 - smoothstep(10.0, 34.0, dt);
        float wispsDetail = (1.0 - smoothstep(2.5, 9.0, dt)) * step(0.02, layer);
        float banks = banksDetail > 0.0 ? mix(0.5, mistNoise(q * vec3(0.03125, 0.05, 0.03125)), banksDetail) : 0.5;
        float wisps = wispsDetail > 0.0 ? mix(0.5, mistNoise2(q * vec3(0.125, 0.16, 0.125) + vec3(0.0, MistDrift.w, 0.0)), wispsDetail) : 0.5;
        float shape = (0.45 + 1.1 * banks) * mix(1.0, 0.62 + 0.76 * wisps, layer);
        // over water the mist lies thicker, low over the surface
        float water = 1.0 + 0.5 * field.b * inside * exp(-above * 0.3);

        float density = sigma * (layer * shape * water + veil) * dry * near * presence;

        if (density > 1.0e-6) {
            // where the sun's beams reach: above the shade a canopy or hill throws towards it
            float shadeHeight = base + field.g * 255.0;
            // (softer the longer the step: far out, where one step spans many trees, their shade averages out)
            float soft = 0.6 * dt;
            float lit = mix(1.0, smoothstep(shadeHeight - 1.2 - soft, shadeHeight + 0.8 + soft, world.y), inside);
            // mist is thick enough to light its own shade a little (light scattered more than once), so beams stay soft
            vec3 scatter = MistAmbient.rgb * (1.0 - MistAmbient.w * (1.0 - lit)) + sun * mix(1.0, lit, 0.45 * MistSunDir.w);
#ifdef LIGHTS
            if (t < 64.0) {
                float lamp = mistLantern(world, p);
                scatter += MistLantern.rgb * (lamp * lamp);
            }
#endif
            float step = 1.0 - exp(-density * dt);
            light += transmit * step * scatter;
            transmit *= 1.0 - step;
            if (transmit < 0.003) {
                break;
            }
        }
    }
    return vec4(light, 1.0 - transmit);
}
