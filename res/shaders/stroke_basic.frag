#version 450

smooth in vec4 theColor;

in float fWeight;

uniform vec3 uColor;

out vec4 outputColor;

void main()
{
	// Porter-Duff Source Over Destination
    outputColor = vec4(uColor,sqrt(sqrt(fWeight)));
}