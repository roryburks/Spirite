#version 330

smooth in vec2 vUV;

uniform vec3 u_Color1;
uniform vec3 u_Color2;
uniform int uSize;

out vec4 outputColor;

void main() {
	if(((int(gl_FragCoord.x) / uSize) + (int(gl_FragCoord.y) / uSize)) % 2 != 0)
		outputColor = vec4(u_Color1, 1);
	else
		outputColor = vec4(u_Color2, 1);
}