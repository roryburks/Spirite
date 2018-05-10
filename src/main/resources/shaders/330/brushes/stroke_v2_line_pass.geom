#version 330
#define PI0_5 1.5707963
#define PI 3.1415926
#define PI1_5 4.7123889
#define PI2 6.2831852
#define MITER_MAX 10.0f

layout(lines_adjacency) in;
layout(line_strip, max_vertices = 4) out;

uniform mat4 perspectiveMatrix;
uniform float uH;	// Passing this is probably horribly misguided

in float vSize[];

void main()
{
	vec2 p0 = vec2(gl_in[0].gl_Position.x,gl_in[0].gl_Position.y);
	vec2 p1 = vec2(gl_in[1].gl_Position.x,gl_in[1].gl_Position.y);
	vec2 p2 = vec2(gl_in[2].gl_Position.x,gl_in[2].gl_Position.y);
	vec2 p3 = vec2(gl_in[3].gl_Position.x,gl_in[3].gl_Position.y);
	vec2 n01 = normalize( p1 - p1);
	vec2 normal = normalize( p2 - p1);
	vec2 n23 = normalize( p3 - p2);

	vec4 out1a;
	vec4 out1b;
	vec4 out2a;
	vec4 out2b;

	if( p0 == p1) {
		out1a = out2a = perspectiveMatrix*vec4(p1 - (normal * vSize[1]), 0, 1);
	}
	else {
		vec2 tangent = normalize( normalize(p2-p1) + normalize(p1-p0));
		vec2 miter = vec2( -tangent.y, tangent.x);
		vec2 n1 = normalize( vec2(-(p1.y-p0.y), p1.x-p0.x));
		float length = max(0.5,min(vSize[1] / dot(miter, n1), MITER_MAX));

		out1a = perspectiveMatrix*vec4( miter*length + p1, 0, 1);
		out2a = perspectiveMatrix*vec4( -miter*length + p1, 0, 1);
	}

	if( p2 == p3) {
		out2b = out1b = perspectiveMatrix*vec4(p2 + (normal * vSize[2]), 0, 1);
	}
	else {
		vec2 tangent = normalize( normalize(p3-p2) + normalize(p2-p1));
		vec2 miter = vec2( -tangent.y, tangent.x);
		vec2 n2 = normalize( vec2(-(p2.y-p1.y), p2.x-p1.x));
		float length = max(0.5,min(vSize[2] / dot(miter, n2), MITER_MAX));

	    out1b = perspectiveMatrix*vec4( miter*length + p2, 0, 1);
	    out2b = perspectiveMatrix*vec4( -miter*length + p2, 0, 1);
	}

	gl_Position = out1a;
	gl_PointSize = 0.5;
    EmitVertex();
	gl_Position = out1b;
	gl_PointSize = 0.5;
    EmitVertex();
	EndPrimitive();
	gl_Position = out2a;
	gl_PointSize = 0.5;
    EmitVertex();
	gl_Position = out2b;
	gl_PointSize = 0.5;
    EmitVertex();
	EndPrimitive();
}
