#version 330
layout(std140) uniform PorscheSurface { mat4 SurfaceModel; vec4 SurfaceMaterial; };
uniform sampler2D SurfaceSampler;
in vec2 texCoord;
in vec3 surfacePosition;
in vec3 surfaceNormal;
out vec4 fragColor;
void main() {
    // Sample before any discard: mip and anisotropy selection need derivatives from the whole pixel quad.
    vec4 content=texture(SurfaceSampler,texCoord);
    float radius=SurfaceMaterial.z;
    if(radius>0.0) {
        vec2 p=(texCoord-0.5)*vec2(SurfaceMaterial.w,1.0);
        vec2 q=abs(p)-(vec2(SurfaceMaterial.w,1.0)*0.5-radius);
        if(length(max(q,0.0))+min(max(q.x,q.y),0.0)>radius) discard;
    }
    if(content.a<0.1) discard;
    vec3 color=content.rgb*SurfaceMaterial.y;
    float glass=SurfaceMaterial.x;
    if(glass>0.0) {
        vec3 n=normalize(surfaceNormal), v=normalize(-surfacePosition);
        if(dot(n,v)<0.0) n=-n;
        float fresnel=pow(1.0-max(dot(n,v),0.0),5.0);
        vec3 reflection=reflect(-v,n);
        float sky=smoothstep(-0.08,0.8,reflection.y);
        float edge=pow(1.0-min(min(texCoord.x,1.0-texCoord.x),min(texCoord.y,1.0-texCoord.y))*2.0,32.0);
        float highlight=pow(max(dot(reflection,normalize(vec3(-0.4,0.8,0.5))),0.0),90.0);
        color=mix(color,vec3(0.62,0.73,0.86),glass*(0.018+0.07*fresnel)*sky);
        color+=glass*(edge*0.045+highlight*0.10);
    }
    fragColor=vec4(color,1.0);
}
