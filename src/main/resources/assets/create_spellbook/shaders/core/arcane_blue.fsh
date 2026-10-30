#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec4 arcaneVertexColor;
in vec2 arcaneUV;
in float arcaneDistance;
in float arcaneShade;
out vec4 fragColor;
void main() {
    vec4 sampled = texture(Sampler0, arcaneUV);
    float alpha = sampled.a * arcaneVertexColor.a * ColorModulator.a;
    if (alpha < 0.01) discard;
    float detail = dot(sampled.rgb, vec3(0.299, 0.587, 0.114));
    vec3 blue = mix(vec3(0.16, 0.42, 0.78), vec3(0.64, 0.91, 1.0), detail);
    blue *= arcaneShade * arcaneVertexColor.rgb * ColorModulator.rgb;
    float fog = smoothstep(FogStart, max(FogEnd, FogStart + 0.001), arcaneDistance);
    fragColor = vec4(mix(blue, FogColor.rgb, fog), alpha);
}
