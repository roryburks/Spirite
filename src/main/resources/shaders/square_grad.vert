#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec2 space;

smooth out vec2 relSpace;

void main()
{
    gl_Position = position;
    relSpace = space;
}