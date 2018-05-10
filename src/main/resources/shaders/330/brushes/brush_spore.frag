#version 330

#GLOBAL

in float fWeight;

uniform vec3 uColor;

out vec4 outputColor;

void main()
{
    outputColor = vec4(uColor,fWeight);
}