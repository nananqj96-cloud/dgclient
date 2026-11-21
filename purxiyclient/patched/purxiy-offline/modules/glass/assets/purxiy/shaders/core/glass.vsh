#version 330

#moj_import <purxiy:glass.glsl>

// A surface turned to glass, from the quads it submitted (see GlassGeometry). The same shader writes its depth and then
// the glass itself, so both passes land on exactly the same pixels.

invariant gl_Position;

#ifndef DEPTH
uniform sampler2D Sampler2;
#endif

in vec3 Position;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;
in vec4 Bounds;
in vec4 Color;

out vec3 position;
out vec2 texCoord0;
out vec3 normal;
out vec3 lightColor;
out float skyLight;
flat out vec4 bounds;
flat out int body;
flat out int flags;

void main() {
    vec4 p = GlassModel * vec4(Position, 1.0);
    position = p.xyz;
    gl_Position = GlassClip * p;
    texCoord0 = UV0;
    normal = mat3(GlassModel) * Normal;
    bounds = Bounds;
    // the body rides along as the red of the colour, the flags as its green
    body = int(Color.r * 255.0 + 0.5);
    flags = int(Color.g * 255.0 + 0.5);
#ifdef DEPTH
    lightColor = vec3(1.0);
    skyLight = 1.0;
#else
    // the light where it is, as vanilla lights it (block and sky light, the time of day); full light for the menu's preview
    bool full = UV2.x < 0;
    lightColor = full ? vec3(GlassHorizon.w) : texture(Sampler2, clamp(vec2(UV2) / 256.0 + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0))).rgb;
    skyLight = full ? 1.0 : clamp(float(UV2.y) / 240.0, 0.0, 1.0);
#endif
}
