#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 arcaneVertexColor;
out vec2 arcaneUV;
out float arcaneDistance;
out float arcaneShade;
void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    arcaneUV = UV0;
    arcaneVertexColor = Color;
    arcaneDistance = length(view.xyz);
    arcaneShade = 0.78 + 0.22 * abs(normalize(Normal).y);
}
