#version 330

uniform vec3 uColor;

out vec4 outputColor;

void main()
{
   	outputColor = vec4(uColor,1);
}