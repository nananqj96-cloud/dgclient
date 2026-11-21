#version 330

#moj_import <purxiy:hit_splash.glsl>

flat out vec2 head;
flat out vec2 tail;
flat out float radius;
flat out float reach;
flat out float bright;
flat out float away;
flat out int flash;

// Six vertices per droplet: a rectangle on screen around its streak (from where it was a moment ago to where it is),
// wide enough for its glow. Sizes are in pixels, from its size in blocks at its distance.
void main() {
    int drop = gl_VertexID / 6;
    int i = gl_VertexID % 6;
    vec4 a = SplashDrops[drop * 2];
    vec4 b = SplashDrops[drop * 2 + 1];
    vec4 h = SplashViewProj * vec4(a.xyz, 1.0);
    vec4 t = SplashViewProj * vec4(b.xyz, 1.0);
    flash = b.w < 0.0 ? 1 : 0;
    bright = abs(b.w);
    away = length(a.xyz);
    head = vec2(0.0);
    tail = vec2(0.0);
    radius = 0.0;
    reach = 1.0;
    if (h.w < 0.05 || t.w < 0.05 || bright <= 0.0) {
        // behind the camera, or dark: nothing
        gl_Position = vec4(0.0, 0.0, 2.0, 1.0);
        return;
    }
    head = (h.xy / h.w * 0.5 + 0.5) * SplashScreen.xy;
    tail = (t.xy / t.w * 0.5 + 0.5) * SplashScreen.xy;
    float perBlock = SplashScreen.y * 0.5 * SplashScreen.z / h.w;
    radius = max(a.w * perBlock, 0.6);
    reach = flash == 1 ? radius * 1.5 : max(radius * 2.6, 2.0);
    float margin = radius + reach * 2.6;
    vec2 axis = head - tail;
    float span = length(axis);
    axis = span > 1.0e-3 ? axis / span : vec2(1.0, 0.0);
    vec2 side = vec2(-axis.y, axis.x);
    float along = i == 1 || i == 2 || i == 4 ? 1.0 : 0.0;
    float across = i == 2 || i == 4 || i == 5 ? 1.0 : -1.0;
    vec2 at = mix(tail - axis * margin, head + axis * margin, along) + side * across * margin;
    gl_Position = vec4(at / SplashScreen.xy * 2.0 - 1.0, 0.0, 1.0);
}
