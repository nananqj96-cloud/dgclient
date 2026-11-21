#version 330

#moj_import <purxiy:hand_trail.glsl>

// Your hand again, at one moment back along its swing (an instance per moment), in the light it stands in. The depth is
// the moment's age: every pixel keeps the most recent moment that covered it.

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform sampler2D Sampler2;

out vec2 texCoord0;
out vec4 vertexColor;
out float age;
// 1 where the colour is the texture's own (a skin), 0 where the vertex carries the sprite's colour
out float textured;

void main() {
#ifdef MASK
    // your hand as it is now
    mat4 ghost = mat4(1.0);
#else
    mat4 ghost = TrailGhosts[gl_InstanceID];
#endif
    gl_Position = TrailProjection * TrailModelView * ghost * vec4(Position, 1.0);
    age = TrailAges[gl_InstanceID].x;
    gl_Position.z = (age * 2.0 - 1.0) * gl_Position.w;

    // lit evenly: the edges of an item face other ways than its sides, and would fleck the sweep
    textured = Color.a < 0.75 ? 1.0 : 0.0;
    vertexColor = vec4(Color.rgb * 0.9, 1.0);
    vertexColor.rgb *= texelFetch(Sampler2, ivec2(TrailLight.xy) / 16, 0).rgb;
    texCoord0 = UV0;
}
