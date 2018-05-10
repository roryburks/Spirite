#version 330
#define PI0_5 1.5707963
#define PI 3.1415926
#define PI1_5 4.7123889
#define PI2 6.2831852

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 17) out;

uniform mat4 perspectiveMatrix;
uniform float uH;	// Passing this is probably horribly misguided

in float vSize[];

smooth out float fWeight;
flat out float fX, fY, fM;

float angle_difference( float a1, float a2) {
	return mod( a1 - a2 + PI1_5, PI2) - PI0_5;
}

void main()
{
	vec2 bl, bc, br, tl, tc, tr;
	
	float w1 = vSize[1]/2;
	float w2 = vSize[2]/2;
	
	bl = br = bc = vec2( gl_in[1].gl_Position.x, gl_in[1].gl_Position.y);
	tl = tr = tc = vec2( gl_in[2].gl_Position.x, gl_in[2].gl_Position.y);
	
	vec2 dif = normalize(tc - bc);
	
	
	// Start Dot
	// Starting Endpoint
	bl += vec2(-dif.y, dif.x) * w1;
	br += vec2(dif.y, -dif.x) * w1;
    fWeight = -1;
    fX = bc.x+0.5;
    fY = uH-(bc.y+0.5);
    fM = w1;
    
    gl_Position = perspectiveMatrix*vec4(bl-dif*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(bl+dif*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(br-dif*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(br+dif*w1,0,1);
    EmitVertex();
    EndPrimitive();
    
    
	// End Dot
	tl += vec2(-dif.y, dif.x) * w2;
	tr += vec2(dif.y, -dif.x) * w2;
    fX = tc.x+0.5;
    fY = uH-(tc.y+0.5);
    fM = vSize[2]/2;
    
    gl_Position = perspectiveMatrix*vec4(tl+dif*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tl-dif*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tr+dif*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tr-dif*w2,0,1);
    EmitVertex();
    EndPrimitive();
    
    fX = 0;
    fY = 0;
	
	// 1 : Bottomleft
    gl_Position = perspectiveMatrix*vec4(bl,0,1);
    fWeight = 0;
    EmitVertex();

	// 2 : Topleft
    gl_Position = perspectiveMatrix*vec4(tl,0,1);
    fWeight = 0;
    EmitVertex();
    
    // 3 :bottomcenter
    gl_Position = perspectiveMatrix*vec4(bc,0,1);
    fWeight = 1;
    EmitVertex();
    
    // 4 :topcenter
    gl_Position = perspectiveMatrix*vec4(tc,0,1);
    fWeight = 1;
    EmitVertex();
    
    // 5: bottomright
    gl_Position = perspectiveMatrix*vec4(br,0,1);
    fWeight = 0;
    EmitVertex();
    
    // 6: topright
    gl_Position = perspectiveMatrix*vec4(tr,0,1);
    fWeight = 0;
    EmitVertex();
    
    EndPrimitive();

}