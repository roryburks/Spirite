#version 330

in vec2 vUV;

out vec4 outputColor;

uniform sampler2D myTexture;

void main()
{
	outputColor = texture(myTexture, vUV);
}