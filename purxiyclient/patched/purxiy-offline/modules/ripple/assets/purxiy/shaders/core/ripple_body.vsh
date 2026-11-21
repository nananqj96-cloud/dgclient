#version 330

#moj_import <purxiy:ripple.glsl>

// A body that was hit, again, from the quads it submitted (positions relative to the camera). The same shader writes
// its depth and then its light, so both passes land on exactly the same pixels.

invariant gl_Position;

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
out vec3 world;
out vec3 normal;
flat out int body;

void main() {
    world = Position;
    gl_Position = RippleViewProj * vec4(Position, 1.0);
    texCoord0 = UV0;
    normal = Normal;
    // the body rides along as the red of the colour
    body = int(Color.r * 255.0 + 0.5);
}
