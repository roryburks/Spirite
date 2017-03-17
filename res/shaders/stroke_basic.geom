#version 330
#define PI 3.1415926

layout(lines) in;
layout(triangle_strip, max_vertices = 4) out;
uniform mat4 perspectiveMatrix;

in float vSize[];

void main()
{
	for( int i=0; i<gl_in.length()-1; ++i) {
		float ssize = vSize[i]/2;
		float angle = atan( 
			gl_in[i+1].gl_Position.y - gl_in[i].gl_Position.y,
			gl_in[i+1].gl_Position.x - gl_in[i].gl_Position.x);
			
		float cang = cos(angle);
		float sang = sin(angle);
			
	    gl_Position = perspectiveMatrix*(gl_in[i].gl_Position + 
	    	vec4( ssize*(sang - cang), ssize*(-cang - sang), 0.0, 0.0));
	    EmitVertex();
	
	    gl_Position = perspectiveMatrix*(gl_in[i].gl_Position + 
	    	vec4( ssize*(-sang - cang), ssize*(cang-sang), 0.0, 0.0));
	    EmitVertex();
	    
	    gl_Position = perspectiveMatrix*(gl_in[i+1].gl_Position + 
	    	vec4( ssize*(cang + sang), ssize*(sang-cang), 0.0, 0.0));
	    EmitVertex();
	
	    gl_Position = perspectiveMatrix*(gl_in[i+1].gl_Position + 
	    	vec4( ssize*(cang-sang), ssize*(sang+cang), 0.0, 0.0));
	    EmitVertex();
	    EndPrimitive();
    }

}