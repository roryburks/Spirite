#version 330

in vec2 position;


uniform mat4 perspectiveMatrix;


void main()
{
    gl_Position = perspectiveMatrix*vec4(position,0,1);
}