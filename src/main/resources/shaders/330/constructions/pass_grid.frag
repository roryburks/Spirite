#version 330

#GLOBAL

smooth in vec2 vUV;

uniform vec3 u_Color1;
uniform vec3 u_Color2;
uniform int u_Size;

out vec4 outputColor;

void main() {
	if(((int(gl_FragCoord.x) / u_Size) + (int(gl_FragCoord.y) / u_Size)) % 2 != 0)
		outputColor = vec4(u_Color1, 1);
	else
		outputColor = vec4(u_Color2, 1);
}