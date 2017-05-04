#version 100

precision highp float;

attribute vec2 position;
attribute vec2 vertexUV;

varying vec2 vUV;
uniform mat4 perspectiveMatrix;

void main()
{
	gl_Position = perspectiveMatrix*vec4(position,0.0,1.0);
	vUV = vertexUV;
}
