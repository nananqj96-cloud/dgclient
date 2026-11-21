#version 330

#moj_import <purxiy:cape.glsl>

in vec2 texel;
flat in int design;

out vec4 fragColor;

// One texel of a cape texture (see cape.glsl). On the small mip levels, where a texel covers a cape pixel or more,
// several samples inside it keep the finer details from flickering. (A loop the compiler cannot unroll: the designs
// are compiled once, not once per sample.)
void main() {
    float aa = CapeFrame.w;
    int samples = aa < 0.3 ? 1 : 4;
    float step_ = samples == 1 ? 0.0 : aa * 0.25;
    float fine = samples == 1 ? aa : aa * 0.5;
    vec4 sum = vec4(0.0);
    for (int i = 0; i < samples; i++) {
        vec2 o = vec2((i & 1) == 0 ? -step_ : step_, (i & 2) == 0 ? -step_ : step_);
        sum += vc_cape(design, texel + o, CapeFrame.x, fine, CapeFrame.z > 0.5);
    }
    fragColor = sum / float(samples);
}
