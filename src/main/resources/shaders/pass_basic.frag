#version 330

#GLOBAL

in vec2 vUV;

out vec4 outputColor;

void main()
{
	outputColor = texture(u_texture, vUV);
}