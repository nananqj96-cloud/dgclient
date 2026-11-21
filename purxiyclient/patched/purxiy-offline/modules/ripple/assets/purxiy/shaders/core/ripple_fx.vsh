#version 330

#moj_import <purxiy:ripple_fx.glsl>

// Six vertices per piece. Stars, sparkles and flares face you and are sized in pixels, so their shapes stay crisp at
// any distance; a pillar stands upright and turns towards you around its axis; a ring lies on the ground.

out vec2 local;
out vec3 world;
flat out int kind;
flat out vec4 piece;
// stars, sparkles, flares: pixels a block covers where they are; a pillar: how far away it stands (blocks)
flat out float scale;

void main() {
    int item = gl_VertexID / 6;
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : -1.0, i == 2 || i == 4 || i == 5 ? 1.0 : -1.0);
    vec4 head = FxItems[item * 2];
    piece = FxItems[item * 2 + 1];
    kind = int(head.w + 0.5);
    scale = 0.0;
    if (kind == PILLAR) {
        vec3 toward = vec3(-head.x, 0.0, -head.z);
        vec3 side = dot(toward, toward) > 1.0e-6 ? normalize(vec3(toward.z, 0.0, -toward.x)) : vec3(1.0, 0.0, 0.0);
        float halfWidth = 0.55 + piece.y * 0.9;
        local = vec2(corner.x * halfWidth, (corner.y * 0.5 + 0.5) * piece.x);
        world = head.xyz + side * local.x + vec3(0.0, local.y, 0.0);
        gl_Position = FxViewProj * vec4(world, 1.0);
        // how far away it stands (blocks, across the ground)
        scale = length(head.xz);
    } else if (kind == RING) {
        local = corner * (piece.x + 0.6);
        world = head.xyz + vec3(local.x, 0.0, local.y);
        gl_Position = FxViewProj * vec4(world, 1.0);
    } else {
        world = head.xyz;
        vec4 clip = FxViewProj * vec4(head.xyz, 1.0);
        if (clip.w <= 0.05) {
            // behind you
            local = vec2(0.0);
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            return;
        }
        scale = FxLook.w / max(length(head.xyz), 0.05);
        float body = piece.x * scale;
        float extent = kind == FLARE ? body * 2.4 + 2.0 : max(body, 0.9) * 8.0 + 3.0;
        local = corner * extent;
        gl_Position = clip + vec4(corner * extent * 2.0 / FxScreen.xy * clip.w, 0.0, 0.0);
    }
}
