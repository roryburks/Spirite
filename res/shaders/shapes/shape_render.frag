#version 330

uniform vec3 uColor;
uniform float uAlpha;

out vec4 outputColor;

void main()
{
   	outputColor = vec4(uColor*uAlpha,uAlpha);
}