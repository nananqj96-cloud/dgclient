#version 330

// Weather skies are complete atmospheres, just like the abstract sky styles. Their clouds live in world-direction
// space, so the dome has no cube seams, polar pinching or texture that follows the camera. Each settled preset uses
// only its own branch. Extra quality adds cloud detail rather than changing the size or shape of the main banks.
const int WEATHER_OCTAVES = QUALITY > 1 ? 5 : (QUALITY > 0 ? 4 : 3);
const int WEATHER_LIGHT_OCTAVES = QUALITY > 0 ? 3 : 2;

// A gently flattened cloud shell: the horizon sees more distant folds while the zenith sees their broad undersides.
// The finite denominator also makes the lower hemisphere continuous when flying above the world or looking down.
vec3 weatherShell(vec3 d, float scale) {
    float elevation = sqrt(d.y * d.y + 0.025);
    return d * vec3(1.0, 0.72, 1.0) * (scale / (0.58 + elevation * 0.42));
}

// RAIN — overlapping slate nimbostratus, darker close scud and the diffuse daylight trapped behind the cloud deck.
// Neighbouring density along one light direction gives the billows real shaded faces without screen-space normals.
vec3 rainSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float morph = SkyTime.y * 0.085;
    float flow = SkyTime.x * 0.075;
    vec3 p = weatherShell(d, 3.8) + vec3(19.2 + flow, 8.3, 4.6 + flow * 0.38);
    vec2 banks = fbm2(p * 0.53 + vec3(0.0, morph * 0.18, -morph * 0.12), 3);
    vec3 q = p + vec3(banks.x - 0.5, banks.y - 0.5, banks.x - banks.y) * 1.9;
    q.y += morph * 0.17;
    q += styleCurrent(p, morph);
    vec2 cloud = fbm2(q, WEATHER_OCTAVES);
    float density = smoothstep(0.26, 0.72, cloud.x);
    float litDensity = fbm2(q + vec3(-0.24, 0.48, -0.17), WEATHER_LIGHT_OCTAVES).x;
    float lighting = clamp(0.40 + (cloud.x - litDensity) * 3.5, 0.04, 1.0);
    float depth = smoothstep(0.27, 0.76, banks.y);
    float transmission = exp(-density * 2.8);
    vec3 quiet = mix(vec3(0.105, 0.135, 0.175), vec3(0.38, 0.43, 0.48), day);
    vec3 darkCloud = mix(vec3(0.041, 0.057, 0.078), vec3(0.135, 0.165, 0.197), day);
    vec3 litCloud = mix(vec3(0.24, 0.29, 0.35), vec3(0.63, 0.67, 0.70), day);
    vec3 col = mix(darkCloud, litCloud, lighting * (0.58 + 0.42 * depth));
    // Thick distant sheets show through the openings; even the brightest gaps remain an overcast sky.
    vec3 farCloud = mix(quiet * 0.85, litCloud * 0.91, banks.x);
    col = mix(col, farCloud, transmission * 0.75);
    float shoulder = exp(-pow((cloud.x - 0.46) * 7.0, 2.0));
    col += litCloud * shoulder * lighting * (1.0 - density) * 0.16;

    // A lower, independently advected scud layer creates parallax between the rolls and deepens their undersides.
    vec3 nearPoint = p * vec3(1.72, 0.95, 1.72) + vec3(-flow * 2.10, morph * 0.24, flow * 0.58);
    vec2 scud = fbm2(nearPoint + vec3(banks.y, banks.x, banks.y) * 0.72, QUALITY > 0 ? 3 : 2);
    float lowerCloud = smoothstep(0.48, 0.74, scud.x + (banks.y - 0.5) * 0.12);
    col = mix(col, darkCloud * (0.78 + scud.y * 0.36), lowerCloud * 0.68);
    // Soft distant rain curtains sit under the deck. Fine foreground drops remain the Weather module's particles.
    float rainVeil = smoothstep(0.35, 0.70, noise3(p * vec3(3.0, 0.22, 3.0) + vec3(0.0, -morph, 0.0)));
    rainVeil *= exp(-abs(d.y - 0.10) * 7.0) * 0.10;
    col = mix(col, litCloud * 0.78, rainVeil);
    col *= 1.0 + SkyAmount.w * 0.025 * sin(SkyTime.z + banks.x * 4.0);
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.75, 0.86, 1.0), SkyTint[8], SkyTint[9]);
}

// SNOW — a luminous, milk-white winter ceiling. Broad pearl cloud blankets overlap cooler blue-grey folds; light
// diffuses through them instead of drawing a hard sun, so the sky remains soft and snowy even at midnight.
vec3 snowSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float morph = SkyTime.y * 0.065;
    float flow = SkyTime.x * 0.045;
    vec3 p = weatherShell(d, 3.0) + vec3(5.7 + flow, 24.4, 12.8 - flow * 0.27);
    vec2 folds = fbm2(p * 0.57 + vec3(morph * 0.11, -morph * 0.13, 0.0), 3);
    vec3 q = p + vec3(folds.x - 0.5, folds.y - 0.5, folds.x - folds.y) * 1.35;
    q += styleCurrent(p, morph) * 0.72;
    vec2 cloud = fbm2(q, WEATHER_OCTAVES);
    float density = smoothstep(0.22, 0.79, cloud.x);
    float litDensity = fbm2(q + vec3(-0.27, 0.42, 0.16), WEATHER_LIGHT_OCTAVES).x;
    float lighting = clamp(0.53 + (cloud.x - litDensity) * 2.3, 0.15, 0.95);
    float pearl = exp(-pow((cloud.x - 0.43) * 5.2, 2.0));
    vec3 quiet = mix(vec3(0.54, 0.61, 0.69), vec3(0.84, 0.88, 0.92), day);
    vec3 shade = mix(vec3(0.31, 0.385, 0.49), vec3(0.59, 0.665, 0.75), day);
    vec3 milk = mix(vec3(0.77, 0.825, 0.885), vec3(0.98, 0.985, 0.98), day);
    vec3 col = mix(shade, milk, 0.31 + lighting * 0.62);
    col = mix(col, milk, (1.0 - density) * 0.31);
    col += vec3(0.080, 0.072, 0.060) * pearl * (0.4 + folds.y * 0.6);

    // A sheer second blanket is visibly distinct from the darker rain scud: broad, translucent and softly lit.
    vec3 veilPoint = p * vec3(1.48, 0.68, 1.48) + vec3(-flow * 1.82, morph * 0.35, flow * 0.52);
    vec2 veil = fbm2(veilPoint + vec3(folds.y, folds.x, folds.x) * 0.82, QUALITY > 0 ? 3 : 2);
    float blanket = smoothstep(0.39, 0.74, veil.x);
    col = mix(col, mix(shade, milk, 0.43 + veil.y * 0.36), blanket * 0.35);
    vec3 lightDirection = normalize(vec3(-0.44, 0.73, -0.30));
    float diffuseGlow = pow(max(dot(d, lightDirection), 0.0), 4.0);
    col += vec3(0.067, 0.070, 0.077) * diffuseGlow * exp(-density * 1.7);
    col *= 1.0 + SkyAmount.w * 0.012 * sin(SkyTime.z + folds.x * 4.0);
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.87, 0.93, 1.0), SkyTint[10], SkyTint[11]);
}

// FIRE — a fairy-tale furnace above the world: golden hot updrafts inside immense orange-red flame clouds, rolling
// ember-lit smoke and cooler cinder shadows. Anisotropic, upward-advection makes tongues of fire rather than veins.
vec3 fireSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float morph = SkyTime.y * 0.14;
    float flow = SkyTime.x * 0.18;
    float height = smoothstep(-0.12, 1.0, d.y);
    vec3 p = weatherShell(d, 3.5) + vec3(14.2, 3.7, 21.4);
    p.y -= flow;
    vec2 billows = fbm2(p * vec3(0.72, 0.88, 0.72) + vec3(morph * 0.08, 0.0, -morph * 0.11), 3);
    vec3 q = p * vec3(1.35, 0.58, 1.35);
    q += vec3(billows.x - 0.5, billows.y - 0.5, billows.x - billows.y) * 2.1;
    q += styleCurrent(p * vec3(1.0, 0.5, 1.0), morph * 1.3);
    vec2 flame = fbm2(q, WEATHER_OCTAVES);
    float heat = clamp((flame.x - 0.26) * 2.15 + (billows.y - 0.5) * 0.43 - height * 0.19, 0.0, 1.0);
    float body = smoothstep(0.12, 0.68, heat);
    float gold = smoothstep(0.51, 0.87, heat);
    float core = smoothstep(0.74, 0.99, heat);
    vec3 quiet = mix(vec3(0.18, 0.029, 0.018), vec3(0.32, 0.065, 0.026), day);
    vec3 red = vec3(0.47, 0.046, 0.014);
    vec3 orange = vec3(1.02, 0.29, 0.026);
    vec3 golden = vec3(1.18, 0.75, 0.19);
    vec3 col = mix(quiet * 0.58, red, smoothstep(0.01, 0.31, heat));
    col = mix(col, orange, body);
    col = mix(col, golden, gold);
    col += vec3(0.20, 0.24, 0.18) * core;

    // The closest smoke rolls rise at their own speed. Offset density lights their rims from the fire underneath.
    vec3 smokePoint = p * vec3(0.85, 1.05, 0.85) + vec3(4.8 + morph * 0.24, flow * 0.48, 8.1);
    smokePoint += vec3(billows.y, billows.x, billows.x) * 1.4;
    vec2 smoke = fbm2(smokePoint, QUALITY > 0 ? 4 : 3);
    float soot = smoothstep(0.43, 0.73, smoke.x + height * 0.10);
    float belowDensity = fbm2(smokePoint + vec3(0.08, -0.34, 0.04), WEATHER_LIGHT_OCTAVES).x;
    float rim = clamp((smoke.x - belowDensity) * 4.0, 0.0, 1.0);
    vec3 sootColour = mix(vec3(0.055, 0.028, 0.038), vec3(0.23, 0.058, 0.034), smoke.y);
    sootColour += vec3(0.61, 0.15, 0.025) * rim * (0.32 + body * 0.68);
    col = mix(col, sootColour, soot * 0.90);
    float transmitted = exp(-soot * 4.6) * (0.45 + billows.y * 0.55);
    col += vec3(0.31, 0.077, 0.009) * transmitted * (0.3 + body * 0.7);
    // Low-amplitude, spatially varying combustion: steady glowing fire, never a screen-wide strobe.
    col *= 1.0 + SkyAmount.w * 0.10 * sin(SkyTime.z + billows.x * 6.0 + smoke.y * 3.0 - p.y * 1.3);
    return styleAtmosphere(d, horizon, quiet, col, vec3(1.0, 0.68, 0.32), SkyTint[12], SkyTint[13]);
}
