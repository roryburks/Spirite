#version 100

precision highp float;

attribute vec2 position;

uniform mat4 perspectiveMatrix;

void main()
{
    gl_Position = perspectiveMatrix*vec4(position,0.0,1.0);
}