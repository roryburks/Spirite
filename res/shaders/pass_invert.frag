#version 330

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;

void main()
{
	vec4 intex = texture(myTexture, vUV);
	vec4 texCol = vec4(intex[3],intex[2],intex[1],intex[0]);
	
	outputColor = vec4( 1 - texCol.r, 1 - texCol.g, 1 - texCol.b, texCol.a);
}