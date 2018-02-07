#version 330

uniform vec3 u_color;
uniform float u_alpha;

out vec4 outputColor;

void main()
{
   	outputColor = vec4(u_color*u_alpha,u_alpha);
}