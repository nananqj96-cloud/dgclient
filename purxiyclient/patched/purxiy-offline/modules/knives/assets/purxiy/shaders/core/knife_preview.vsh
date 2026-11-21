#version 330

layout(std140) uniform KnifePreview {
    vec4 Camera;
    vec4 Frame;
};
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
out vec3 vNormal;
out vec4 vColor;
out vec2 vUv;

vec3 turntable(vec3 p) {
    float x = Camera.x * p.x + Camera.y * p.z;
    float z = Camera.x * p.z - Camera.y * p.x;
    return vec3(x, Camera.z * p.y - Camera.w * z, Camera.w * p.y + Camera.z * z);
}
void main() {
    vec3 view = turntable(Position - vec3(0.0, Frame.z, 0.0));
    gl_Position = vec4(view.xy * Frame.xy, -view.z * Frame.w, 1.0);
    vNormal = turntable(Normal);
    vColor = Color;
    vUv = UV0;
}
