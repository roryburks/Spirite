#version 330
#define PI0_5 1.5707963
#define PI 3.1415926
#define PI1_5 4.7123889
#define PI2 6.2831852
#define MITER_MAX 10.0f

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

	const int i = 1;
	
	vec2 bl, bc, br, tl, tc, tr;
	
	float w1 = vSize[1]/2 + 1;
	float w2 = vSize[2]/2 + 1;
	float weight1 = ( vSize[1]/2 < 1) ? vSize[1]/2 : 1;
	float weight2 = ( vSize[2]/2 < 1) ? vSize[2]/2 : 1;
	float weight_out1 = -1 / (vSize[1]/2);
	float weight_out2= -1 / (vSize[2]/2);
	
	bl = br = bc = vec2( gl_in[1].gl_Position.x, gl_in[1].gl_Position.y);
	tl = tr = tc = vec2( gl_in[2].gl_Position.x, gl_in[2].gl_Position.y);
	
	float a2 = atan( 
		gl_in[2].gl_Position.y - gl_in[1].gl_Position.y,
		gl_in[2].gl_Position.x - gl_in[1].gl_Position.x);
	
	float cang2 = cos(a2);
	float sang2 = sin(a2);
	
	
	// Start Dot
	// Starting Endpoint
	bl += vec2(-sang2, cang2) * w1;
	br += vec2(sang2, -cang2) * w1;
    fWeight = -1;
    fX = bc.x+0.5;
    fY = uH-(bc.y+0.5);
    fM = vSize[1]/2;
    
    gl_Position = perspectiveMatrix*vec4(bl-vec2(cang2,sang2)*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(bl+vec2(cang2,sang2)*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(br-vec2(cang2,sang2)*w1,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(br+vec2(cang2,sang2)*w1,0,1);
    EmitVertex();
    EndPrimitive();
    
    
	// End Dot
    fWeight = -1;
    fX = tc.x+0.5;
    fY = uH-(tc.y+0.5);
    fM = vSize[2]/2;
    
	tl += vec2(-sang2, cang2) * w2;
	tr += vec2(sang2, -cang2) * w2;
    gl_Position = perspectiveMatrix*vec4(tl+vec2(cang2,sang2)*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tl-vec2(cang2,sang2)*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tr+vec2(cang2,sang2)*w2,0,1);
    EmitVertex();
    gl_Position = perspectiveMatrix*vec4(tr-vec2(cang2,sang2)*w2,0,1);
    EmitVertex();
    EndPrimitive();
    
    fX = 0;
    fY = 0;
	
	// 1 : Bottomleft
    gl_Position = perspectiveMatrix*vec4(bl,0,1);
    fWeight = weight_out1;
    EmitVertex();

	// 2 : Topleft
    gl_Position = perspectiveMatrix*vec4(tl,0,1);
    fWeight = weight_out2;
    EmitVertex();
    
    // 3 :bottomcenter
    gl_Position = perspectiveMatrix*vec4(bc,0,1);
    fWeight = weight1;
    EmitVertex();
    
    // 4 :topcenter
    gl_Position = perspectiveMatrix*vec4(tc,0,1);
    fWeight = weight2;
    EmitVertex();
    
    // 5: bottomright
    gl_Position = perspectiveMatrix*vec4(br,0,1);
    fWeight = weight_out1;
    EmitVertex();
    
    // 6: topright
    gl_Position = perspectiveMatrix*vec4(tr,0,1);
    fWeight = weight_out2;
    EmitVertex();
    
    EndPrimitive();

}
