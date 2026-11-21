#version 330

layout(std140) uniform SkinStage {
    mat4 ViewProj;
    mat4 View;
    mat4 Parts[7];  // head, body, right arm, left arm, right leg, left leg, cape: each part's space to the stage
    vec4 Depth[2];  // how far each part's outer layer stands out (model pixels): parts 0-3, then 4-5
    vec4 Rim;       // rgb = rim light colour, a = strength
    vec4 Params;    // x = time (s), y = hover (0..1)
};

in vec3 Position; // model pixels in the part's own space
in vec2 UV0;
in vec4 Color;    // r = corner shade, g = part (+8 on the outer layer), b = the direction the corner grows in (0..26)
in vec3 Normal;

out vec2 vUv;
out vec3 vNormal;
out float vShade;

void main() {
    int code = int(Color.g * 255.0 + 0.5);
    int part = code & 7;
    int dir = int(Color.b * 255.0 + 0.5);
    vec3 grow = vec3(float(dir % 3), float((dir / 3) % 3), float(dir / 9)) - 1.0;
    float thickness = part < 4 ? Depth[0][part] : Depth[1][part - 4];
    vec3 local = Position + grow * thickness;
    mat4 toStage = Parts[part];
    gl_Position = ViewProj * (toStage * vec4(local, 1.0));
    vUv = UV0;
    vNormal = mat3(View) * (mat3(toStage) * Normal);
    vShade = Color.r;
}
