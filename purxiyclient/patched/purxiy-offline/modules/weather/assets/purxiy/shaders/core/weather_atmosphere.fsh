#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:weather_atmosphere.glsl>
uniform sampler2D DepthSampler;
out vec4 fragColor;
float hash(vec3 p) { return fract(sin(dot(p,vec3(127.1,311.7,74.7))) * 43758.5453); }
float noise(vec3 p) {
    vec3 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
        mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
vec3 position(vec2 uv,float depth) {
    vec4 p=WeatherInverse * vec4(uv*2.0-1.0,depth*2.0-1.0,1);
    return p.xyz/p.w;
}
void main() {
    vec2 uv=gl_FragCoord.xy/WeatherScreen.xy;
    float depth=texture(DepthSampler,uv).r;
    bool sky=depth>.999999;
    // the Purxiy sky stays exactly the one you chose: no clouds, haze or flashes over it
    float own=sky?WeatherLight.z:0.0;
    vec3 relative=position(uv,min(depth,.99999));
    vec3 ray=normalize(relative);
    float distance=length(relative);
    float rain=WeatherModes.x,snow=WeatherModes.y,fire=WeatherModes.z,storm=WeatherModes.w;
    float weight=rain+snow+fire;
    vec3 tint=(vec3(.39,.49,.61)*rain+vec3(.68,.77,.86)*snow+vec3(.34,.15,.075)*fire)/max(weight,.001);
    // under the Purxiy sky the land's haze takes its horizon colour (the fog's), so land and sky still meet seamlessly
    tint=mix(tint*WeatherLight.x,FogColor.rgb,WeatherLight.z);
    float haze=(1.0-exp(-distance*.007))*(rain*.065+snow*.17+fire*.095)*(1.0-own);
    vec4 result=vec4(tint*haze,haze);
    if (sky && storm*(1.0-own)>.001) {
        vec3 cloud=vec3(.14,.19,.28)*WeatherLight.x;
        float cover=.76;
        if (ray.y>.018) {
            float travel=(WeatherCamera.w-WeatherCamera.y)/ray.y;
            vec3 p=WeatherCamera.xyz+ray*travel;
            p.xz+=vec2(WeatherScreen.z*.68,WeatherScreen.z*.25);
            float mass=0.0;
            // Multiple layers through the cloud volume give the canopy a moving, sculpted underside.
            for(int i=0;i<5;i++) {
                vec3 q=(p+ray*float(i)*7.0)*.018;
                float n=noise(q)*.65+noise(q*2.13)*.25+noise(q*4.3)*.1;
                mass+=(1.0-mass)*smoothstep(.32,.73,n)*.44;
            }
            float underside=noise(p*.031+vec3(0,WeatherScreen.z*.013,0));
            cloud=mix(vec3(.07,.095,.15),vec3(.32,.39,.48),underside*.7+mass*.3)*WeatherLight.x;
            cover=.66+mass*.31;
        }
        cloud+=vec3(.55,.65,.83)*WeatherScreen.w*.7;
        float a=cover*storm*(1.0-own);
        result=vec4(cloud*a,a)+result*(1.0-a);
    }
    float flash=WeatherScreen.w*(sky?.20:.095)*(1.0-own)*smoothstep(0.0,12.0,distance);
    result.rgb+=vec3(.58,.71,1.0)*flash;
    fragColor=result;
}
