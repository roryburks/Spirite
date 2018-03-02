#version 330

#GLOBAL

uniform vec3 u_color;

out vec4 outputColor;

void main()
{
   	outputColor = vec4(u_color,1);
}