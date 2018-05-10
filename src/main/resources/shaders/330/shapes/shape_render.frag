#version 330

#GLOBAL

uniform vec3 u_color;
uniform float u_alpha;

out vec4 outputColor;


void main()
{
    vec3 color = targetPremultiplied() ? u_color * u_alpha : u_color;

   	outputColor = vec4( color, u_alpha);
}