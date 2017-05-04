#version 100

precision highp float;

varying vec2 vUV;

uniform vec3 uColor1;
uniform vec3 uColor2;
uniform int uSize;


void main() {
	if((((int(gl_FragCoord.x) / uSize) + (int(gl_FragCoord.y) / uSize)) & 1)!=0)
		gl_FragColor = vec4(uColor1, 1.0);
	else
		gl_FragColor = vec4(uColor2, 1.0);
}