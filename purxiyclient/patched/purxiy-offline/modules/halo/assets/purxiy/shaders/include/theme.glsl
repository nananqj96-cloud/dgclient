// The accent colour for shaders: prx_theme carries a colour designed in Purxiy lilac over to the accent the player
// picked, as dev.purxiy.gui.Theme#tint does for the interface. Bind Theme.uniforms() as PurxiyTheme.

#moj_import <purxiy:tint.glsl>

layout(std140) uniform PurxiyTheme {
    vec4 ThemeTurn;  // x = hue turn (radians), y = chroma scale, z = lightness shift, w = spread of the gradient
    vec4 ThemeMore;  // x = how yellow the accent is, y = 1 while nothing moves (lilac), z = the design's hue (radians)
};

vec3 prx_theme(vec3 srgb) {
    return prx_tint(srgb, ThemeTurn, ThemeMore);
}
