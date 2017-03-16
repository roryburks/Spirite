#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec2 space;
layout (location = 2) in float pressure;

smooth out vec2 relSpace;
smooth out float relPressure;

void main()
{
    gl_Position = position;
    relSpace = space;
    relPressure = pressure
}