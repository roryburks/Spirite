#version 100

precision highp float;

varying vec2 vUV;

uniform sampler2D myTexture;

void main() {
    vec4 texCol = texture2D( myTexture, vUV);
	gl_FragColor.r = 1.0-texCol.r;
	gl_FragColor.g = 1.0-texCol.g;
	gl_FragColor.b = 1.0-texCol.b;
	gl_FragColor.a = texCol.a;
}
