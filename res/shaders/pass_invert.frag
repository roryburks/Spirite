#version 330

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	
	outputColor = vec4( 1 - texCol.r, 1 - texCol.g, 1 - texCol.b, texCol.a);
}