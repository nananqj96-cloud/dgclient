// Carries a colour designed in Purxiy lilac over to another colour, as dev.purxiy.gui.Theme does on the CPU (OKLCH:
// the hue turns, colourfulness scales, lightness hardly moves; greys stay grey). turn and more are a Theme.Turn
// (Turn#write): x = hue turn (radians), y = chroma scale, z = lightness shift, w = spread of the gradient; and
// x = how yellow the colour is, y = 1 while nothing moves (lilac), z = the design's hue (radians).

vec3 prx_oklab(vec3 srgb) {
    vec3 c = mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), srgb));
    vec3 lms = vec3(
        0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b,
        0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b,
        0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b);
    lms = sign(lms) * pow(abs(lms), vec3(1.0 / 3.0));
    return vec3(
        0.2104542553 * lms.x + 0.7936177850 * lms.y - 0.0040720468 * lms.z,
        1.9779984951 * lms.x - 2.4285922050 * lms.y + 0.4505937099 * lms.z,
        0.0259040371 * lms.x + 0.7827717662 * lms.y - 0.8086757660 * lms.z);
}

vec3 prx_linear_of(vec3 lab) {
    vec3 lms = vec3(
        lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z,
        lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z,
        lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z);
    lms = lms * lms * lms;
    return vec3(
        4.0767416621 * lms.x - 3.3077115913 * lms.y + 0.2309699292 * lms.z,
        -1.2684380046 * lms.x + 2.6097574011 * lms.y - 0.3413193965 * lms.z,
        -0.0041960863 * lms.x - 0.7034186147 * lms.y + 1.7076147010 * lms.z);
}

float prx_turn(float a) {
    return a - 6.2831853 * floor((a + 3.1415927) / 6.2831853);
}

float prx_yellow(float hue) {
    float d = degrees(mod(hue, 6.2831853));
    return smoothstep(50.0, 80.0, d) * (1.0 - smoothstep(105.0, 125.0, d));
}

vec3 prx_tint(vec3 srgb, vec4 turn, vec4 more) {
    if (more.y > 0.5) {
        return srgb;
    }
    vec3 lab = prx_oklab(srgb);
    float chroma = length(lab.yz);
    if (chroma < 0.002) {
        return srgb;
    }
    float colourful = smoothstep(0.06, 0.12, chroma);
    float scale = mix(min(turn.y, 1.15), turn.y, colourful);
    float offset = prx_turn(atan(lab.z, lab.y) - more.z) * turn.w;
    if (offset > 0.0) {
        offset *= 1.0 - 0.75 * more.x;
    }
    float hue = more.z + turn.x + offset;
    float light = lab.x + turn.z * smoothstep(0.30, 0.45, lab.x) * (1.0 - smoothstep(0.80, 0.93, lab.x)) * colourful;
    hue -= radians(30.0) * prx_yellow(hue) * (1.0 - smoothstep(0.55, 0.85, light));
    vec2 dir = vec2(cos(hue), sin(hue));
    // lower the chroma until it fits in sRGB, keeping hue and lightness
    float low = 0.0, high = chroma * scale;
    vec3 rgb = prx_linear_of(vec3(light, dir * high));
    if (any(lessThan(rgb, vec3(-1.0e-4))) || any(greaterThan(rgb, vec3(1.0001)))) {
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
    return mix(12.92 * rgb, 1.055 * pow(rgb, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), rgb));
}
