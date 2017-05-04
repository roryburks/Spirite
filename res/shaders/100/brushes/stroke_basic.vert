#version 100

precision highp float;

attribute vec2 _1position;
attribute float _2Weight;
attribute float _3X;
attribute float _4Y;
attribute float _5M;

uniform mat4 perspectiveMatrix;

varying float fWeight;
varying float fX;
varying float fY;
varying float fM;

void main() {
	gl_Position = perspectiveMatrix*vec4(_1position, 0.0, 1.0);
	fWeight = _2Weight;

	vec4 t = perspectiveMatrix*vec4(_3X, _4Y, 0.0, 1.0);

	fX = _3X;
	fY = _4Y;
	fM = _5M;
}
