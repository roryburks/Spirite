#version 130

in vec2 position; 
in vec2 vertexUV;

out vec2 vUV;
uniform mat4 perspectiveMatrix;

void main() 
{
	gl_Position = perspectiveMatrix*vec4(position,0,1);
	vUV = vertexUV;
	
}