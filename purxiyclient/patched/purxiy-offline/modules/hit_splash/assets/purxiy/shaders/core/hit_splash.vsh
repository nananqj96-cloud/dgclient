#version 330

#moj_import <purxiy:hit_splash.glsl>

flat out int cluster;

// Six vertices per cluster: the rectangle its box covers on screen.
void main() {
    cluster = gl_VertexID / 6;
    int i = gl_VertexID % 6;
    vec4 rect = SplashClusters[cluster * 3 + 2];
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? rect.z : rect.x, i == 2 || i == 4 || i == 5 ? rect.w : rect.y);
    gl_Position = vec4(corner, 0.0, 1.0);
}
