#version 100

precision highp float;

varying vec2 vUV;

uniform sampler2D myTexture;

void main()
{
	gl_FragColor = texture2D(myTexture, vUV);
}