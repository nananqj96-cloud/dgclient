#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <purxiy:afterglow.glsl>

// A dissolving entity (see dev.purxiy.afterglow.PurxiyAfterglow). Vanilla's entity shading, then:
//
//   charge   the body fills with the light of the palette, brightest along its silhouette
//   flash    a soft flash of that light at the moment it falls
//   front    texel by texel it goes, from the top down: every texel of its texture as one, pixel-perfect, like the
//            blocks of the world. Texels close to the front glow; the ones about to go burn white-hot.
//
// Depth is written as usual, so what is left of the body hides what is behind it and the holes show what is inside.

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColorFront;
in vec4 vertexColorBack;
in vec4 lightMapColor;
in vec2 texCoord0;
in vec3 bodyPos;
in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

// How far ahead of the front a texel starts to burn, and how far the heat of the front reaches (field units).
const float EDGE = 0.05;
const float HEAT = 0.14;

// Where the centre of this pixel's texel sits on the body, so the whole texel goes at once. A face is flat, so the
// body follows the texture coordinates linearly across it: the screen derivatives give the way from one to the other.
vec3 texelCentre(vec2 size, out ivec2 texel) {
    vec2 t = floor(texCoord0 * size);
    texel = ivec2(t);
    vec2 ux = dFdx(texCoord0), uy = dFdy(texCoord0);
    vec3 px = dFdx(bodyPos), py = dFdy(bodyPos);
    float det = ux.x * uy.y - ux.y * uy.x;
    if (abs(det) < 1.0e-14) {
        return bodyPos;
    }
    // the way to the texel's centre in pixels on screen, then on the body
    vec2 delta = (t + 0.5) / size - texCoord0;
    float a = (delta.x * uy.y - delta.y * uy.x) / det;
    float b = (ux.x * delta.y - ux.y * delta.x) / det;
    vec3 offset = px * a + py * b;
    // where a pixel straddles an edge of the model its derivatives mix two faces: keep its own position there
    float texelBlocks = max(length(px), length(py)) / max(max(length(ux * size), length(uy * size)), 1.0e-6);
    return length(offset) <= texelBlocks * 1.5 + 1.0e-4 ? bodyPos + offset : bodyPos;
}

void main() {
    // derivatives first: the discards below would spoil them
    ivec2 texel;
    vec3 centre = texelCentre(vec2(textureSize(Sampler0, 0)), texel);

    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= (gl_FrontFacing ? vertexColorFront : vertexColorBack) * ColorModulator;
    color *= lightMapColor;

    uint seed = afterglowInt(TextureMat[1].w);
    float field = afterglowField(centre, TextureMat[3].z, TextureMat[1].x, seed) * (1.0 - TEXEL_JITTER)
        + afterglowRandom(ivec3(texel, 0x5EED), seed) * TEXEL_JITTER;
    float ahead = field - TextureMat[0].w;
    if (ahead < 0.0) {
        discard;
    }

    vec3 first = afterglowColor(TextureMat[2].x);
    vec3 second = afterglowColor(TextureMat[2].y);
    vec3 accent = afterglowColor(TextureMat[2].z);
    float glow = TextureMat[2].w;
    float charge = TextureMat[3].x;
    float flash = TextureMat[3].y;

    // as it charges, the body takes on the light of the palette, brightest along its silhouette
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    color.rgb = mix(color.rgb, first * (0.3 + 0.95 * luma), charge * 0.22);
    float rim = 1.0 - abs(dot(normalize(viewNormal), normalize(-viewPos)));
    color.rgb += first * rim * rim * rim * charge * (0.2 + 0.3 * glow);

    // close to the front it glows; the texels about to go burn white-hot
    float heat = 1.0 - smoothstep(0.0, HEAT, ahead);
    float edge = 1.0 - smoothstep(0.0, EDGE, ahead);
    color.rgb = mix(color.rgb, second * (0.65 + 0.7 * luma) + first * 0.12, heat * heat * 0.5);
    color.rgb = mix(color.rgb, mix(accent, vec3(1.0), 0.2 + 0.55 * edge * edge), edge * (0.55 + 0.45 * glow));

    // and a soft flash of light at the moment it falls, from its silhouette in
    color.rgb = mix(color.rgb, mix(first, vec3(1.0), 0.45), flash * (0.25 + 0.45 * rim * rim));
    color.a = max(color.a, edge);

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
