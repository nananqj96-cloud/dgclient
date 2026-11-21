#version 330

#moj_import <purxiy:tint.glsl>

// Four complete, direction-space skies. Everything is evaluated on a sphere in world space: there are no texture
// edges, cube faces or poles to reveal, and changing the field of view never changes the scale of the material.
// Each style owns its palette and composition. The host blends complete skies, including their atmosphere.

const int STYLE_OCTAVES = QUALITY > 1 ? 5 : (QUALITY > 0 ? 4 : 3);

// A style in other colours (Sky, "Colours"; dev.purxiy.sky.SkyTint does the same on the CPU for the world's fog). In
// OKLCH, as prx_tint does for the accent: the style's hues turn from its key colour to the colour picked and keep part
// of their spread, colourfulness follows the colour picked, lightness hardly moves, and greys take on its hue as well,
// so silver light becomes light of your colour. turn: x = hue turn (radians), y = chroma scale, z = lightness shift,
// w = spread of the hues around the key colour; more: x = chroma the greys take on, y = how far the colours have turned
// (0 = as designed), z = the key colour's hue (radians), w = how yellow the colour picked is.
vec3 skyTint(vec3 srgb, vec4 turn, vec4 more) {
    if (more.y <= 0.0) {
        return srgb;
    }
    vec3 lab = prx_oklab(max(srgb, vec3(0.0)));
    float chroma = length(lab.yz);
    float hue = chroma > 1.0e-5 ? atan(lab.z, lab.y) : more.z;
    // real colours scale with the colour picked, near-greys only a little (they take on its hue below instead)
    float colourful = smoothstep(0.02, 0.08, chroma);
    float scale = mix(min(turn.y, 1.15), turn.y, colourful);
    float offset = prx_turn(hue - more.z) * turn.w;
    if (offset > 0.0) {
        offset *= 1.0 - 0.75 * more.w;
    }
    float picked = more.z + turn.x;
    // the middle and light tones take on the hue; the darkest stay close to black, the brightest to white
    float tone = smoothstep(0.12, 0.6, lab.x) * (1.0 - 0.55 * smoothstep(0.80, 1.0, lab.x));
    vec2 ab = vec2(cos(picked + offset), sin(picked + offset)) * chroma * scale + vec2(cos(picked), sin(picked)) * more.x * tone;
    float light = clamp(lab.x + turn.z * smoothstep(0.25, 0.5, lab.x), 0.0, 1.0);
    float c = length(ab);
    float h = c > 1.0e-5 ? atan(ab.y, ab.x) : picked;
    // dark yellows read as olive: they turn towards amber
    h -= radians(30.0) * prx_yellow(h) * (1.0 - smoothstep(0.55, 0.85, light));
    vec2 dir = vec2(cos(h), sin(h));
    // lower the chroma until it fits in sRGB, keeping hue and lightness
    vec3 rgb = prx_linear_of(vec3(light, dir * c));
    if (any(lessThan(rgb, vec3(-1.0e-4))) || any(greaterThan(rgb, vec3(1.0001)))) {
        float low = 0.0, high = c;
        for (int i = 0; i < 10; i++) {
            float mid = (low + high) * 0.5;
            vec3 test = prx_linear_of(vec3(light, dir * mid));
            if (any(lessThan(test, vec3(-1.0e-4))) || any(greaterThan(test, vec3(1.0001)))) {
                high = mid;
            } else {
                low = mid;
            }
        }
        rgb = prx_linear_of(vec3(light, dir * low));
    }
    rgb = clamp(rgb, 0.0, 1.0);
    vec3 tinted = mix(12.92 * rgb, 1.055 * pow(rgb, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), rgb));
    return mix(srgb, tinted, more.y);
}

// Flow and morph have separate continuously integrated clocks, so changing speed or liveliness cannot jump the sky.
vec3 styleDirection(vec3 d, float speed) {
    d.xz = rotate2(d.xz, SkyTime.x * speed);
    return d;
}

// Broad, travelling eddies deform the material locally, independently of its overall drift. Use the integrated
// morph clock (never time multiplied by a live setting), so speed changes, pause and reduced motion stay smooth.
vec3 styleCurrent(vec3 p, float t) {
    vec3 wave = sin(p.yzx * 1.3 + p.zxy * 0.6 + vec3(t, -t * 0.83, t * 0.67));
    return wave * (0.06 + 0.22 * SkyAmount.w);
}

// A narrow luminous contour with a pixel-integrated Gaussian profile. Conservation of energy keeps very fine veins
// from turning into sparkling dots when the camera moves or the render resolution changes.
float styleLine(float distance, float width) {
    float pixel = max(fwidth(distance), 0.00001);
    float filteredWidth = sqrt(width * width + pixel * pixel * 0.18);
    float x = distance / filteredWidth;
    return exp(-x * x) * width / filteredWidth;
}

// Linear up to 0.8, then eases highlights into white instead of clipping.
vec3 softClip(vec3 x) {
    vec3 excess = max(x - 0.8, 0.0);
    return min(x, vec3(0.8)) + 0.2 * (1.0 - exp(-excess / 0.2));
}

// Exact horizon/fog agreement, a gentle lower hemisphere, and a common response to intensity, brightness and weather.
// turn and more are the style's colours (see skyTint).
vec3 styleAtmosphere(vec3 d, vec3 horizon, vec3 quiet, vec3 painted, vec3 flashTint, vec4 turn, vec4 more) {
    float intensity = clamp(SkyStyleParams.x, 0.0, 1.0);
    vec3 col = mix(quiet, painted, intensity);
    col *= mix(0.58, 1.0, smoothstep(-0.45, 0.02, d.y));
    col = mix(col, vec3(dot(col, vec3(0.2126, 0.7152, 0.0722))), SkyState.z * 0.40);
    col *= SkyStyleParams.y;
    col += flashTint * SkyState.w * smoothstep(-0.04, 0.25, d.y) * 0.20;
    // Compress highlights before adding fog: a bright snow horizon must keep the exact world's fog colour.
    // The horizon colour is already faded between styles, in their colours and scaled by brightness on the CPU.
    return mix(skyTint(softClip(col), turn, more), horizon, exp(-abs(d.y) * 9.0));
}

// AETHER — silver caustics drawn through a deep charcoal volume. Two differently moving sheets meet in a soft web;
// light pools at their intersections while broad shadowed spaces give the fine filaments room to breathe.
vec3 aetherSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float life = SkyAmount.w;
    float t = SkyTime.y * 0.095;
    vec3 p = styleDirection(d, 0.013) * 3.6 + vec3(13.4, 7.1, 2.6);
    vec2 drift = fbm2(p * 0.66 + vec3(t * 0.23, -t * 0.16, t * 0.12), 3);
    vec3 q = p + vec3(drift.x, drift.y, drift.x - drift.y) * 2.1;
    q += vec3(t * 0.35, -t * 0.25, t * 0.18);
    q += styleCurrent(p, t);
    // The caustic's silhouette stays smooth: extra high-frequency octaves would turn flowing light into lightning.
    vec2 sheets = fbm2(q, 3);
    float a = sheets.x - 0.50;
    float b = sheets.y - 0.51;
    float pearl = smoothstep(0.28, 0.75, noise3(q * 0.62 + vec3(4.0, t * 0.3, 9.0)));
    float threadA = styleLine(a, mix(0.009, 0.025, pearl * pearl));
    float threadB = styleLine(b, mix(0.005, 0.014, pearl));
    float haloA = exp(-abs(a) * 18.0);
    float haloB = exp(-abs(b) * 24.0);
    float glint = smoothstep(0.22, 0.78, sheets.y + (drift.x - 0.5) * 0.65);
    float travelling = 0.5 + 0.5 * sin(dot(p, vec3(1.1, -0.7, 0.8)) - t * 2.2 + drift.y * 7.0);
    float breathing = 1.0 + life * (0.14 * sin(SkyTime.z + drift.x * 5.0) + 0.30 * travelling);
    vec3 quiet = mix(vec3(0.098, 0.103, 0.112), vec3(0.19, 0.20, 0.22), day);
    vec3 col = quiet * (0.72 + 0.45 * sheets.x);
    col += vec3(0.16, 0.17, 0.18) * (haloA * 0.72 + haloB * 0.35);
    vec3 silver = mix(vec3(0.73, 0.77, 0.82), vec3(0.90, 0.91, 0.94), pearl);
    float filaments = threadA * (0.08 + 0.45 * glint * pearl) + threadB * (0.025 + 0.23 * pearl * pearl);
    col += silver * (filaments + threadA * haloB * 0.22) * breathing;
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.75, 0.84, 1.0), SkyTint[0], SkyTint[1]);
}

// NOCTURNE — large, velvet cloud banks with lavender light held behind their edges. Two depth scales and a coherent
// light direction produce volume; restrained small-scale detail keeps the surface soft even at native resolution.
vec3 nocturneSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float t = SkyTime.y * 0.085;
    vec3 p = styleDirection(d, 0.010) * vec3(2.8, 4.0, 2.8) + vec3(7.6, 18.3, 3.8);
    p += vec3(t * 0.35, -t * 0.12, t * 0.19);
    vec2 folds = fbm2(p * 0.58 + vec3(0.0, t * 0.15, 0.0), 3);
    vec3 q = p + vec3(folds.x, folds.y, folds.x - folds.y) * 1.65;
    q += styleCurrent(p, t * 0.9);
    vec2 clouds = fbm2(q, STYLE_OCTAVES);
    float density = smoothstep(0.31, 0.70, clouds.x);
    float backlight = exp(-pow((clouds.x - 0.435) * 7.0, 2.0));
    // Compare neighbouring density along the same light axis, rather than lighting by screen-space derivatives.
    vec3 towardLight = vec3(-0.24, 0.30, 0.16);
    vec2 litClouds = fbm2(q + towardLight, QUALITY > 0 ? 3 : 2);
    float silverLining = clamp((clouds.x - litClouds.x) * 4.5 + 0.16, 0.0, 1.0);
    float window = smoothstep(0.24, 0.75, clouds.y);
    float deepVeil = smoothstep(0.37, 0.72, noise3(p * 0.74 + vec3(-t * 0.62, t * 0.21, 8.4)));
    vec3 quiet = mix(vec3(0.063, 0.043, 0.083), vec3(0.13, 0.091, 0.18), day);
    vec3 glow = mix(vec3(0.30, 0.205, 0.37), vec3(0.41, 0.295, 0.48), day);
    vec3 col = quiet * (0.65 + 0.30 * window);
    col += glow * backlight * (0.20 + 0.45 * window);
    col = mix(col, quiet * 0.48, density * 0.72);
    col += vec3(0.50, 0.36, 0.61) * silverLining * backlight * (0.12 + 0.27 * window);
    col += vec3(0.16, 0.085, 0.24) * deepVeil * (1.0 - density) * 0.23;
    // Very slow local illumination, never a full-screen pulse or a sudden flash.
    col *= 1.0 + SkyAmount.w * 0.10 * sin(SkyTime.z + folds.y * 4.0 + p.x * 0.7);
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.77, 0.61, 1.0), SkyTint[2], SkyTint[3]);
}

// AMETHYST — luminous violet curtains suspended in an open indigo sky. Tilted, overlapping arcs carry fine rays
// upward from a lilac edge, while rose light travels along their folds and distant crystal dust slowly glimmers.
// Everything is sampled in 3D direction space, including the dust: no azimuth seam or polar convergence.
vec3 amethystSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float life = SkyAmount.w;
    float t = SkyTime.y * 0.12;
    vec3 p = styleDirection(d, 0.008);
    vec3 quiet = mix(vec3(0.026, 0.017, 0.075), vec3(0.088, 0.060, 0.17), day);
    vec2 mist = fbm2(p * 2.6 + vec3(8.3, t * 0.23, 17.4 - t * 0.16), 3);
    vec3 col = quiet * (0.65 + 0.40 * mist.y);
    col += vec3(0.17, 0.052, 0.26) * smoothstep(0.30, 0.76, mist.x) * 0.32;

    const int curtains = QUALITY > 0 ? 3 : 2;
    for (int i = 0; i < curtains; i++) {
        float layer = float(i);
        vec3 axis = normalize(vec3(0.23 - layer * 0.19, 1.0, 0.20 + layer * 0.12));
        vec3 along = normalize(cross(axis, vec3(0.0, 0.0, 1.0)));
        vec3 across = cross(axis, along);
        vec2 uv = vec2(dot(p, along), dot(p, across));
        vec3 field = vec3(uv * (2.1 + layer * 0.3), layer * 5.7 + t * 0.30);
        vec2 folds = fbm2(field + vec3(0.0, -t * 0.23, 0.0), 3);
        float wave = sin(uv.x * 5.0 + uv.y * 3.0 + t * 0.8 + layer * 2.2);
        float edge = 0.16 + layer * 0.20 + (folds.x - 0.5) * 0.46 + wave * 0.065;
        float height = dot(p, axis) - edge;
        float rays = noise3(vec3(uv * (38.0 + layer * 7.0), 12.0 + layer * 9.0 - t * 1.8));
        float rayDetail = mix(0.55, 0.32 + rays * rays * 1.5, 1.0 / (1.0 + length(fwidth(uv)) * 45.0));
        float curtain = smoothstep(-0.015, 0.035, height) * exp(-max(height, 0.0) * (8.0 + layer));
        float crest = styleLine(height, 0.010) * 0.32;
        float presence = smoothstep(0.22, 0.65, folds.y);
        float current = 0.5 + 0.5 * sin(uv.x * 7.0 - uv.y * 4.0 - t * 2.0 + layer * 2.7);
        float pulse = 1.0 + life * (0.24 * current + 0.12 * sin(SkyTime.z + uv.x * 4.0 + layer * 2.0));
        vec3 violet = mix(vec3(0.40, 0.12, 0.88), vec3(0.73, 0.23, 0.62), folds.x * 0.65);
        vec3 lilac = mix(vec3(0.77, 0.62, 1.0), vec3(0.98, 0.62, 0.87), current * 0.55);
        vec3 light = mix(lilac, violet, smoothstep(0.01, 0.18, max(height, 0.0)));
        col += (light * curtain * rayDetail + lilac * crest) * presence * pulse * (0.90 - layer * 0.15);
        col += violet * exp(-abs(height) * 12.0) * presence * 0.10;
    }

    // Dust centres stay inside their cells; the compact profile vanishes before a cell boundary. Pixel filtering
    // preserves energy at low resolution instead of letting tiny grains blink as the camera moves.
    vec3 dustPoint = styleDirection(d, -0.003) * 95.0;
    vec3 cell = floor(dustPoint);
    vec3 seed = hash3(cell + 19.7);
    vec3 offset = fract(dustPoint) - (0.24 + seed * 0.52);
    float radius = 0.065 + 0.035 * seed.y;
    float pixel = max(length(dFdx(dustPoint)), length(dFdy(dustPoint)));
    float width = sqrt(radius * radius + pixel * pixel * 0.16);
    float grain = exp(-dot(offset, offset) / (width * width)) * radius * radius / (width * width);
    grain *= 1.0 - smoothstep(0.14, 0.23, length(offset));
    float glimmer = 0.72 + life * 0.28 * sin(SkyTime.x * (0.8 + seed.x) + seed.z * TAU);
    col += vec3(0.80, 0.72, 1.0) * grain * smoothstep(0.52, 0.80, seed.z) * glimmer;
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.77, 0.60, 1.0), SkyTint[4], SkyTint[5]);
}

// OPALINE — translucent satin folds, with cyan and lilac on opposite faces and a restrained pearl highlight where
// the sheets turn into the light. Long anisotropic forms and broad shadows distinguish it from clouds and caustics.
vec3 opalineSky(vec3 d, vec3 horizon) {
    float day = SkyState.x;
    float t = SkyTime.y * 0.080;
    vec3 p = styleDirection(d, 0.011) * 2.50 + vec3(5.8, 2.4, 16.3);
    vec2 bends = fbm2(p * 0.72 + vec3(t * 0.15, t * 0.12, -t * 0.12), 3);
    vec3 q = p + vec3(bends.x, bends.y, bends.x - bends.y) * 1.65;
    q += styleCurrent(p, t);
    vec2 silk = fbm2(q * vec3(0.78, 1.3, 0.78) + vec3(-t * 0.17, t * 0.07, t * 0.10), STYLE_OCTAVES);
    float phase = dot(p, vec3(1.35, 2.35, -0.50)) + (bends.x - 0.5) * 8.0 + (silk.x - 0.5) * 3.1 - t * 0.45;
    float wave = sin(phase);
    float fold = 0.5 + 0.5 * wave;
    float face = smoothstep(0.27, 0.72, silk.y + 0.16 * sin(phase * 0.33 + bends.y * 5.0));
    float ribbon = pow(max(fold, 0.0), 3.0);
    float pearl = styleLine(1.0 - fold, 0.038);
    float shadow = smoothstep(0.25, 0.78, silk.y);
    vec3 quiet = mix(vec3(0.070, 0.11, 0.15), vec3(0.17, 0.23, 0.28), day);
    vec3 cyan = mix(vec3(0.23, 0.46, 0.49), vec3(0.36, 0.59, 0.61), day);
    vec3 lilac = mix(vec3(0.46, 0.29, 0.58), vec3(0.57, 0.43, 0.66), day);
    vec3 opal = mix(cyan, lilac, face);
    vec3 col = quiet * (0.64 + 0.38 * shadow);
    col += opal * ribbon * (0.36 + 0.52 * shadow);
    col += mix(vec3(0.66, 0.84, 0.90), vec3(0.90, 0.75, 0.94), face) * pearl * (0.07 + shadow * 0.23);
    // A second sheer layer slides across the main fold, with no hard contour or fine glitter.
    float veil = pow(max(0.5 + 0.5 * sin(phase * 0.61 - silk.y * 5.0 + t * 0.22), 0.0), 5.0);
    col += mix(cyan, lilac, 1.0 - face) * veil * 0.10;
    col *= 1.0 + SkyAmount.w * 0.09 * sin(SkyTime.z + bends.y * 4.0 + phase * 0.4);
    return styleAtmosphere(d, horizon, quiet, col, vec3(0.70, 0.88, 1.0), SkyTint[6], SkyTint[7]);
}
