#version 330
#define PI0_5 1.5707963
#define PI 3.1415926
#define PI1_5 4.7123889
#define PI2 6.2831852

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 9) out;
uniform mat4 perspectiveMatrix;

in float vSize[];

out float fWeight;

// Could probably be optimized to only use 1 mod with knowledge
//	of the range atan can be in
float angle_difference( float a1, float a2) {
	return mod( a1 - a2 + PI1_5, PI2) - PI0_5;
}

void main()
{

	const int i = 1;
	
	vec2 bl, bc, br, tl, tc, tr;
	
	bl = br = bc = vec2( gl_in[1].gl_Position.x, gl_in[1].gl_Position.y);
	tl = tr = tc = vec2( gl_in[2].gl_Position.x, gl_in[2].gl_Position.y);
	
	float a2 = atan( 
		gl_in[2].gl_Position.y - gl_in[1].gl_Position.y,
		gl_in[2].gl_Position.x - gl_in[1].gl_Position.x);
	
	float cang2 = cos(a2);
	float sang2 = sin(a2);
	
	
	if( vSize[0] < 0) {
		// Starting Endpoint
		bl += vec2(-sang2, cang2) * vSize[1];
		br += vec2(sang2, -cang2) * vSize[1];
	}
	else {
		float a1 = atan( 
			gl_in[1].gl_Position.y - gl_in[0].gl_Position.y,
			gl_in[1].gl_Position.x - gl_in[0].gl_Position.x);
		float cang1 = cos(a1);
		float sang1 = sin(a1);
			
		if( angle_difference( a1, a2) > 0){
			bl += vec2(-sang2, cang2) * vSize[1];
			br += vec2(sang1 + sang2, -cang1 - cang2) * vSize[1]/2;
		}
		else {
			bl += vec2(-sang1 - sang2, cang1 + cang2) * vSize[1]/2;
			br += vec2(sang2, -cang2) * vSize[1];
		}
	}
	
	if( vSize[3] < 0) {
		tl += vec2(-sang2, cang2) * vSize[2];
		tr += vec2(sang2, -cang2) * vSize[2];
	}
	else {
		vec2 te = tc;
		float a3 = atan( 
			gl_in[3].gl_Position.y - gl_in[2].gl_Position.y,
			gl_in[3].gl_Position.x - gl_in[2].gl_Position.x);
		float cang3 = cos(a3);
		float sang3 = sin(a3);
		
		if( angle_difference( a2, a3) > 0){
			tl += vec2(-sang2, cang2) * vSize[2];
			tr += vec2(sang3 + sang2, -cang3 - cang2) * vSize[2]/2;
			te += vec2( -sang3, cang3) * vSize[2];
			
			
		    gl_Position = perspectiveMatrix*vec4(tl,0,1);
		    fWeight = 0;
		    gl_FrontColor = gl_FrontColorIn[1];
		    EmitVertex();
		}
		else {
			tl += vec2(-sang3 - sang2, cang3 + cang2) * vSize[2]/2;
			tr += vec2(sang2, -cang2) * vSize[2];
			te += vec2( sang3, -cang3) * vSize[2];
			
		    gl_Position = perspectiveMatrix*vec4(tr,0,1);
		    fWeight = 0;
		    gl_FrontColor = gl_FrontColorIn[1];
		    EmitVertex();
		}
			
		// Elbo joint
	    gl_Position = perspectiveMatrix*vec4(tc,0,1);
	    fWeight = 1;
	    gl_FrontColor = gl_FrontColorIn[1];
	    EmitVertex();
	    
	    gl_Position = perspectiveMatrix*vec4(te,0,1);
	    fWeight = 0;
	    gl_FrontColor = gl_FrontColorIn[1];
	    EmitVertex();
	    EndPrimitive();
	}
	
	
	
	
	float ssize_1 = vSize[i]/2;
	float ssize_2 = vSize[i+1]/2;
	float angle = atan( 
		gl_in[i+1].gl_Position.y - gl_in[i].gl_Position.y,
		gl_in[i+1].gl_Position.x - gl_in[i].gl_Position.x);
		
	float cang = cos(angle);
	float sang = sin(angle);
	
	// 1 : Bottomleft
    gl_Position = perspectiveMatrix*vec4(bl,0,1);
    fWeight = 0;
    gl_FrontColor = gl_FrontColorIn[1];
    EmitVertex();

	// 2 : Topleft
    gl_Position = perspectiveMatrix*vec4(tl,0,1);
    fWeight = 0;
    gl_FrontColor = gl_FrontColorIn[2];
    EmitVertex();
    
    // 3 :bottomcenter
    gl_Position = perspectiveMatrix*vec4(bc,0,1);
    fWeight = 1;
    gl_FrontColor = gl_FrontColorIn[1];
    EmitVertex();
    
    // 4 :topcenter
    gl_Position = perspectiveMatrix*vec4(tc,0,1);
    fWeight = 1;
    gl_FrontColor = gl_FrontColorIn[2];
    EmitVertex();
    
    // 5: bottomright
    gl_Position = perspectiveMatrix*vec4(br,0,1);
    fWeight = 0;
    gl_FrontColor = gl_FrontColorIn[1];
    EmitVertex();
    
    // 6: topright
    gl_Position = perspectiveMatrix*vec4(tr,0,1);
    fWeight = 0;
    gl_FrontColor = gl_FrontColorIn[2];
    EmitVertex();
    
    EndPrimitive();

}