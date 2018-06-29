#version 330

#define A 1664525u
#define C 1013904223u
#define IMAX2 65535u

layout(points) in;
layout(points, max_vertices = 10) out;

uniform mat4 perspectiveMatrix;


out float fWeight;

uint prng( uint seed) {
	uint rand = (seed*A + C);	// % 2^32 is inherent in uint
	return rand;
}

void main()
{

	uint rand;
	uint seed;
	
	
	seed = (uint(gl_in[0].gl_Position.x * 9999)) & IMAX2
		|((uint(gl_in[0].gl_Position.y * 9999)) & IMAX2 << 16);
		
	    
	for( uint i=0u; i < 10u; ++i) {
		rand = prng(seed + 666u*i);
	    gl_Position = perspectiveMatrix*
	    	(gl_in[0].gl_Position + 
	    	vec4(-5.0+10.0*float(rand & 255u)/255.0f,-5.0+10.0*float((rand>>8) & 255u)/255.0f,0,0));
	    gl_PointSize = 1.0f;
	    
		fWeight = 0.1 +  0.1*float((rand>>16) & 255u)/255.0f;
	    EmitVertex();
	    EndPrimitive();
    }
}