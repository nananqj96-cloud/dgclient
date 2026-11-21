#version 330

#moj_import <purxiy:frame.glsl>
#moj_import <purxiy:sdf.glsl>
#moj_import <purxiy:tint.glsl>

uniform sampler2D Sampler0; // content: glyph atlas page or image
uniform sampler2D Sampler1; // blurred backdrop

#ifdef SHEET
// A sheet standing in the world (see PurxiyRenderer#beginSheet): seen in perspective, and tested against the scene.
layout(std140) uniform PurxiySheet {
    // x, y = m22 and m32 of the camera's projection (depth to distance), z = opacity of what lies behind something in
    // the world, w = distance (blocks) over which it fades to that
    vec4 SheetInfo;
    // only where the sky shows: x = 1 for that, y = the camera's height, z = the clouds' underside, w = where the fog starts
    vec4 SheetSky;
    // x = where the fog ends (blocks); standing on the ground: y = 1 for that, z = the top of low growth around it (grass,
    // flowers), relative to the camera's height
    vec4 SheetFog;
    // sheet units to the world, relative to the camera
    mat4 SheetWorld;
};
uniform sampler2D DepthSampler; // the scene's depth: sampled, not attached

float sheetDistance(float depth) {
    return SheetInfo.y / (depth * 2.0 - 1.0 + SheetInfo.x);
}
#endif

in vec2 vLocal;
in vec2 vUv;
in vec2 vScreen;
in vec4 vColor;
in vec4 vColor2;
flat in vec2 vHalf;
flat in vec4 vRadii;
flat in vec4 vParams;
flat in vec4 vClip;
flat in vec4 vExtra;

out vec4 fragColor;

// Keep in sync with dev.purxiy.render.gpu.ShapeMode / ShapeFlags
const int MODE_RECT = 0;
const int MODE_SHADOW = 1;
const int MODE_ARC = 2;
const int MODE_IMAGE = 3;
const int MODE_TEXT = 4;
const int MODE_GLASS = 5;

const int FLAG_STRAIGHT_ALPHA = 1;
const int FLAG_ROUND_CAPS = 2;
const int FLAG_CONIC = 4;
const int FLAG_RADIAL = 8;
const int FLAG_ADDITIVE = 16;
const int FLAG_GRADIENT_BORDER = 32;
const int FLAG_SHEEN = 64;
const int FLAG_LOGO = 128;

float coverage(float d, float px) {
    return clamp(0.5 - d / px, 0.0, 1.0);
}

void main() {
    int mode = int(vParams.x + 0.5);
    int flags = int(vParams.w + 0.5);
#ifdef SHEET
    // the canvas sized edges and text for ScreenInfo.z device pixels per unit; perspective makes a unit larger or
    // smaller than that here
    vec2 alongX = dFdx(vScreen);
    vec2 alongY = dFdy(vScreen);
    float zoom = inversesqrt(max(abs(alongX.x * alongY.y - alongX.y * alongY.x), 1.0e-12)) / ScreenInfo.z;
#else
    const float zoom = 1.0;
#endif
    float px = vExtra.y / zoom;
    vec4 color;

    if (mode == MODE_RECT) {
        float d = prx_roundBox(vLocal, vHalf, vRadii);
        float outer = coverage(d, px);
        vec4 fill = vColor;
        if ((flags & FLAG_RADIAL) != 0) {
            fill = mix(vColor, vColor2, clamp(length(vLocal / vHalf), 0.0, 1.0));
        }
        float border = vParams.y;
        if (border > 0.0) {
            float inner = coverage(d + border, px);
            if ((flags & FLAG_GRADIENT_BORDER) != 0) {
                color = vColor * (outer - inner);
            } else {
                color = fill * inner + vColor2 * (outer - inner);
            }
        } else {
            color = fill * outer;
        }
    } else if (mode == MODE_SHADOW) {
        color = vColor * clamp(prx_roundBoxShadow(vLocal, vHalf, max(vParams.z, 0.001), vRadii.x), 0.0, 1.0);
    } else if (mode == MODE_ARC) {
        float d = prx_arc(vLocal, vHalf.x, vRadii.x, vRadii.y, vRadii.z, (flags & FLAG_ROUND_CAPS) != 0);
        vec4 fill = vColor;
        if ((flags & FLAG_CONIC) != 0) {
            float angle = atan(vLocal.y, vLocal.x) - vRadii.x;
            angle = mod(angle + 3.14159265, 6.28318531) - 3.14159265; // (-pi, pi] relative to the start
            if (angle < -0.35) {
                angle += 6.28318531; // wrap everything that is not the start cap to the far side
            }
            fill = mix(vColor, vColor2, clamp(angle / max(vRadii.y, 0.0001), 0.0, 1.0));
        }
        color = fill * coverage(d, px);
    } else if (mode == MODE_IMAGE) {
        vec4 tex = texture(Sampler0, vUv);
        if ((flags & FLAG_STRAIGHT_ALPHA) != 0) {
            tex.rgb *= tex.a;
        }
        if ((flags & FLAG_LOGO) != 0 && tex.a > 0.002) {
            // the logo in the logo's colour: its purple turns as the interface's lilac does
            tex.rgb = prx_tint(min(tex.rgb / tex.a, 1.0), LogoTurn, LogoMore) * tex.a;
        }
        color = tex * vColor * coverage(prx_roundBox(vLocal, vHalf, vRadii), px);
        if ((flags & FLAG_SHEEN) != 0) {
            // a soft band of light crossing the picture on a slant: Params.y the direction it moves in (radians),
            // Extra.z where its middle is and Extra.w its half width (both in half sizes of the picture)
            float along = dot(vLocal / vHalf, vec2(cos(vParams.y), sin(vParams.y)));
            float k = (along - vExtra.z) / max(vExtra.w, 1.0e-4);
            color *= exp(-k * k);
        }
    } else if (mode == MODE_TEXT) {
        float sd = texture(Sampler0, vUv).r;
        float dist = ((sd - 0.5) * vParams.z + vParams.y) * zoom;
        float soft = vExtra.z * zoom;
        float fillA = soft > 0.0 ? smoothstep(-soft, soft, dist) : clamp(dist + 0.5, 0.0, 1.0);
#ifdef SHEET
        if (soft <= 0.0) {
            // seen at a slant a glyph is squeezed more one way than the other (words lying on the ground): measure the
            // edge on screen along the distance field's own gradient, so it stays crisp across and along
            vec2 grad = vec2(dFdx(sd), dFdy(sd));
            float edge = (sd - 0.5 + vParams.y / max(vParams.z, 1.0e-4)) / max(length(grad), 1.0e-5);
            fillA = clamp(edge + 0.5, 0.0, 1.0);
        }
#endif
        if (soft > 0.0) {
            // a glow wider than the distance field (small text) would show the glyph's quad: fade it out before its edge
            fillA *= smoothstep(0.0, 0.18, sd);
        }
        float outline = vExtra.w * zoom;
        if (outline > 0.0) {
            float outerA = clamp(dist + outline + 0.5, 0.0, 1.0);
            color = vColor * fillA + vColor2 * max(outerA - fillA, 0.0);
        } else {
            color = vColor * fillA;
        }
    } else if (mode == MODE_GLASS) {
        vec3 backdrop = texture(Sampler1, gl_FragCoord.xy / ScreenInfo.xy).rgb;
        float luma = dot(backdrop, vec3(0.2126, 0.7152, 0.0722));
        backdrop = max(mix(vec3(luma), backdrop, vExtra.z), 0.0);
        vec3 glass = backdrop * (1.0 - vColor.a) + vColor.rgb;
        glass += (prx_hash12(gl_FragCoord.xy * 0.71 + 13.0) - 0.5) * vExtra.w;
        color = vec4(glass, 1.0) * (vParams.z * coverage(prx_roundBox(vLocal, vHalf, vRadii), px));
    } else {
        color = vec4(1.0, 0.0, 1.0, 1.0);
    }

    // Soft clip: rounded clip rectangle in UI space, anti-aliased on device pixels.
    vec2 clipHalf = (vClip.zw - vClip.xy) * 0.5;
    float clipDist = prx_roundBox(vScreen - (vClip.xy + clipHalf), clipHalf, vec4(vExtra.x));
    color *= clamp(0.5 - clipDist * ScreenInfo.z * zoom, 0.0, 1.0);

#ifdef SHEET
    // the scene may be a picture of its own, larger than the screen (the main menu's world is supersampled)
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy * vec2(textureSize(DepthSampler, 0)) / ScreenInfo.xy), 0).r;
    float scene = sheetDistance(depth);
    float here = sheetDistance(gl_FragCoord.z);
    if (SheetSky.x > 0.5) {
        // only where the sky shows: whatever the scene drew here hides it (a hill, a tree, a roof), however far away,
        // except clouds and what has faded into the fog
        if (depth < 1.0) {
            vec3 point = (SheetWorld * vec4(vScreen, 0.0, 1.0)).xyz * (scene / max(here, 1.0e-4));
            float cloud = step(SheetSky.z, SheetSky.y + point.y);
            float fog = smoothstep(SheetSky.w, SheetFog.x, length(point));
            color *= max(cloud, fog);
        }
    } else if (SheetFog.y > 0.5) {
        // standing on the ground: whatever stands in front hides it (a wall, a hill, a tree); only low growth around it
        // (grass, flowers) lets it show through, faint
        float behind = smoothstep(0.0, SheetInfo.w, here - scene);
        if (behind > 0.0) {
            vec3 point = (SheetWorld * vec4(vScreen, 0.0, 1.0)).xyz * (scene / max(here, 1.0e-4));
            float low = 1.0 - smoothstep(SheetFog.z - 0.1, SheetFog.z + 0.1, point.y);
            color *= 1.0 - behind * (1.0 - SheetInfo.z * low);
        }
    } else {
        // behind something in the world (a body walking past): still readable, but faint
        color *= mix(1.0, SheetInfo.z, smoothstep(0.0, SheetInfo.w, here - scene));
    }
#endif

    if (color.a <= 0.0 && (flags & FLAG_ADDITIVE) == 0) {
        discard;
    }

    // Dither away 8-bit banding in the dark gradients this UI is full of.
    color.rgb += (prx_hash12(gl_FragCoord.xy) - 0.5) * (1.5 / 255.0) * color.a;

    if ((flags & FLAG_ADDITIVE) != 0) {
        color.a = 0.0;
    }
    fragColor = color;
}
