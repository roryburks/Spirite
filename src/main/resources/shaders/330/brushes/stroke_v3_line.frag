#version 330
#GLOBAL

smooth in float fAlpha;
out vec4 outputColor;

void main() {
	outputColor = vec4(1.0, 0.0, 0.0, fAlpha);
}
