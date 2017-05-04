#version 330

layout (location = 0) in vec2 position;
layout (location = 1) in float size;
//layout (location = 2) in float pressure;

out float vSize;

uniform mat4 perspectiveMatrix;


void main()
{
    gl_Position = vec4(position,0,1);
    vSize = size;
    
}