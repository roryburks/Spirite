#version 330

#GLOBAL

smooth in vec4 theColor;

out vec4 outputColor;

void main()
{
    outputColor = theColor;
}