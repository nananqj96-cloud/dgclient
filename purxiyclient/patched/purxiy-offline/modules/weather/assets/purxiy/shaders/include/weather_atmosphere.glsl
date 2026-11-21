layout(std140) uniform WeatherAtmosphere {
    mat4 WeatherInverse;
    vec4 WeatherScreen;
    vec4 WeatherModes;
    vec4 WeatherCamera;
    vec4 WeatherLight; // x = light of the sky, y = intensity, z = how much of the sky is the Purxiy sky (left alone)
};
