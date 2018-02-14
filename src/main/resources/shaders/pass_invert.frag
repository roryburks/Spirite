#version 330

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	
	outputColor = vec4( texCol.a - texCol.r, texCol.a - texCol.g, texCol.a - texCol.b, texCol.a);
}