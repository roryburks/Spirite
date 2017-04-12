#version 330

smooth in vec2 vUV;

uniform vec3 uColor1;
uniform vec3 uColor2;
uniform int uSize;

out vec4 outputColor;

void main() {
	if(((int(gl_FragCoord.x) / uSize) + (int(gl_FragCoord.y) / uSize)) % 2)
		outputColor = vec4(uColor1, 1);
	else
		outputColor = vec4(uColor2, 1);
}