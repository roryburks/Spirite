#version 330

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;

void main()
{
	vec4 intex = texture(myTexture, vUV);
	
	outputColor = vec4( texCol.r, texCol.g, texCol.b, texCol.a);
}