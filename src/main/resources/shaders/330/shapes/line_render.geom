#version 330

#define MITER_MAX 10.0f

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 17) out;

uniform mat4 perspectiveMatrix;
uniform mat4 worldMatrix;
uniform int u_join;	// 0 : none, 1: miter, 2: bevel
uniform float u_width;

in float vSize[];

void doFlat() {
	vec2 p1 = (worldMatrix*vec4(gl_in[1].gl_Position.x,gl_in[1].gl_Position.y,0,1)).xy;
	vec2 p2 = (worldMatrix*vec4(gl_in[2].gl_Position.x,gl_in[2].gl_Position.y,0,1)).xy;
	vec2 normal = normalize( p2 - p1);
	vec2 nl = vec2( -normal.y, normal.x);
	vec2 nr = vec2( normal.y, -normal.x);
	
    gl_Position = perspectiveMatrix*vec4( nl*u_width + p1, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nr*u_width + p1, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nl*u_width + p2, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nr*u_width + p2, 0, 1);
    EmitVertex();
    EndPrimitive();
}

void doMiter() {
	vec2 p0 = (worldMatrix*vec4(gl_in[0].gl_Position.x,gl_in[0].gl_Position.y, 0, 1)).xy;
	vec2 p1 = (worldMatrix*vec4(gl_in[1].gl_Position.x,gl_in[1].gl_Position.y,0,1)).xy;
	vec2 p2 = (worldMatrix*vec4(gl_in[2].gl_Position.x,gl_in[2].gl_Position.y,0,1)).xy;
	vec2 p3 = (worldMatrix*vec4(gl_in[3].gl_Position.x,gl_in[3].gl_Position.y,0,1)).xy;
	vec2 normal = normalize( p2 - p1);
	
	if( p0 == p1) {
		vec2 nl = vec2( -normal.y, normal.x);
		vec2 nr = vec2( normal.y, -normal.x);
	    gl_Position = perspectiveMatrix*vec4( nl*u_width + p1, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( nr*u_width + p1, 0, 1);
	    EmitVertex();
	}
	else {
		vec2 tangent = normalize( normalize(p2-p1) + normalize(p1-p0));
		vec2 miter = vec2( -tangent.y, tangent.x);
		vec2 n1 = normalize( vec2(-(p1.y-p0.y), p1.x-p0.x));
		float length = max(0.5,min( u_width / dot(miter, n1), MITER_MAX));
		
	    gl_Position = perspectiveMatrix*vec4( miter*length + p1, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( -miter*length + p1, 0, 1);
	    EmitVertex();
	}
	
	if( p2 == p3) {
		vec2 nl = vec2( -normal.y, normal.x);
		vec2 nr = vec2( normal.y, -normal.x);
	    gl_Position = perspectiveMatrix*vec4( nl*u_width + p2, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( nr*u_width + p2, 0, 1);
	    EmitVertex();
	}
	else {
		vec2 tangent = normalize( normalize(p3-p2) + normalize(p2-p1));
		vec2 miter = vec2( -tangent.y, tangent.x);
		vec2 n2 = normalize( vec2(-(p2.y-p1.y), p2.x-p1.x));
		float length = max(0.5,min(u_width / dot(miter, n2), MITER_MAX));
		
	    gl_Position = perspectiveMatrix*vec4( miter*length + p2, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( -miter*length + p2, 0, 1);
	    EmitVertex();
	}
    EndPrimitive();
}


void doBevel() {
	vec2 p1 = (worldMatrix*vec4(gl_in[1].gl_Position.x,gl_in[1].gl_Position.y,0,1)).xy;
	vec2 p2 = (worldMatrix*vec4(gl_in[2].gl_Position.x,gl_in[2].gl_Position.y,0,1)).xy;
	vec2 p3 = (worldMatrix*vec4(gl_in[3].gl_Position.x,gl_in[3].gl_Position.y,0,1)).xy;
	vec2 normal = normalize( p2 - p1);
	vec2 nl = vec2( -normal.y, normal.x);
	vec2 nr = vec2( normal.y, -normal.x);
	
    gl_Position = perspectiveMatrix*vec4( nl*u_width + p1, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nr*u_width + p1, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nl*u_width + p2, 0, 1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4( nr*u_width + p2, 0, 1);
    EmitVertex();
    EndPrimitive();
    
    if( p3 != p2) {
    	vec2 normal2 = normalize( p3 - p2);
		vec2 nl2 = vec2( -normal2.y, normal2.x);
		vec2 nr2 = vec2( normal2.y, -normal2.x);
	    gl_Position = perspectiveMatrix*vec4( nl*u_width + p2, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( nr*u_width + p2, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( nl2*u_width + p2, 0, 1);
	    EmitVertex();
	    gl_Position = perspectiveMatrix*vec4( nr2*u_width + p2, 0, 1);
	    EmitVertex();
	    EndPrimitive();
    }
}

void main()
{
	switch( u_join) {
	case 0:
		doFlat();
		break;
	case 1:
		doMiter();
		break;
	case 2:
		doBevel();
		break;
	}
}