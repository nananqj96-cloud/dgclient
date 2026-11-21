#version 330

#moj_import <purxiy:cape.glsl>

out vec2 texel;
flat out int design;

// The live cape: one quad over the part of the 64 by 32 layout that the cape and the elytra use, or only the cape's
// part while nobody wears the cape on an elytra (the target is the whole texture, row 0 at the top). Swatches
// (CapeLayout.x of them, CapeLayout.y to a row): the outside of one design each, design n in cell n.
void main() {
    int quad = gl_VertexID / 6;
    int i = gl_VertexID - quad * 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    if (CapeLayout.x < 0.5) {
        design = int(CapeFrame.y + 0.5);
        texel = corner * (CapeLayout.z > 0.5 ? vec2(22.0, 17.0) : vec2(46.0, 22.0));
        gl_Position = vec4(texel / vec2(64.0, 32.0) * 2.0 - 1.0, 0.0, 1.0);
    } else {
        design = quad;
        float columns = CapeLayout.y;
        float rows = ceil(CapeLayout.x / columns);
        vec2 cell = vec2(mod(float(quad), columns), floor(float(quad) / columns));
        texel = vec2(1.0) + corner * vec2(10.0, 16.0);
        gl_Position = vec4((cell + corner) / vec2(columns, rows) * 2.0 - 1.0, 0.0, 1.0);
    }
}
