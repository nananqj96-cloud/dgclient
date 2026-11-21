#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh: one quad per light cell (see bokeh_spots), as big as the light's blur; cells without a light of their own make
// no quad. The light's energy is spread over its disc: a bigger disc of the same light is dimmer. Lights are brighter
// than white in the world, so they are boosted (by how much the Highlights setting says).

uniform sampler2D SpotSampler;
uniform sampler2D SceneSampler;

out vec2 local;
out vec3 light;
flat out float spriteCoc;
flat out vec2 spriteScreen;

void main() {
    int cell = gl_VertexID / 6;
    int corner = gl_VertexID - cell * 6;
    int columns = int(BkGrid.z);
    ivec2 at = ivec2(cell - (cell / columns) * columns, cell / columns);
    vec4 spot = texelFetch(SpotSampler, at, 0);
    local = vec2(0.0);
    light = vec3(0.0);
    spriteCoc = 0.0;
    spriteScreen = vec2(0.5);
    if (spot.r + spot.g + spot.b <= 0.0) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }
    float coc = bokehDecode(spot.a);
    int span = 2 * int(BkScreen.z);
    vec3 energy = spot.rgb * spot.rgb * float(span * span);
    float boost = 64.0 * BkLook.x * BkLook.x + 10.0 * BkLook.x;
    vec3 shine = min(energy * boost / (BOKEH_PI * coc * coc * bokehLensArea()), vec3(1.2));
    // a disc too faint to see would only show its rim
    if (max(shine.r, max(shine.g, shine.b)) < 0.02) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }
    // where in the cell the light is: the centre of its brightest pixels, so discs glide instead of jumping cell to cell
    ivec2 base = at * span;
    ivec2 last = ivec2(BkScreen.xy) - 1;
    vec2 sum = vec2(0.0);
    float weight = 0.0;
    for (int y = 0; y < span; y++) {
        for (int x = 0; x < span; x++) {
            vec3 pixel = texelFetch(SceneSampler, min(base + ivec2(x, y), last), 0).rgb;
            float v = max(pixel.r, max(pixel.g, pixel.b));
            v *= v * v * v;
            sum += (vec2(x, y) + 0.5) * v;
            weight += v;
        }
    }
    vec2 centre = vec2(base) + (weight > 0.0 ? sum / weight : vec2(float(span) * 0.5));
    // room for the fringe and a soft edge
    float radius = abs(coc) * (1.0 + 0.2 * BkLook.y) + 2.0 * BkScreen.z;
    const vec2 CORNERS[6] = vec2[](vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0));
    vec2 c = CORNERS[corner];
    gl_Position = vec4((centre + c * radius) / BkScreen.xy * 2.0 - 1.0, 0.0, 1.0);
    local = c * radius / abs(coc);
    light = shine;
    spriteCoc = coc;
    spriteScreen = centre / BkScreen.xy;
}
