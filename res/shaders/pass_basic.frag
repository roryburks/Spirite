#version 330


in vec2 vUV;

out vec4 outputColor;

uniform sampler2D myTexture;
uniform float uAlpha;

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	
	outputColor = vec4( texCol.r, texCol.g, texCol.b, texCol.a*uAlpha);
}