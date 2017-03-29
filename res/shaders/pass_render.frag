#version 330


in vec2 vUV;

out vec4 outputColor;

uniform sampler2D myTexture;
uniform float uAlpha;

// 00000000 00000000 00000000 0000BBBA
// A: how to combine the color and alpha
//		0: multiply colors by the input alpha
//		1: multiply colors by total alpha
// B: which subroutine to use
uniform int uComp;	

vec4 changeColor(vec4 texCol) {
	return vec4( 1*texCol.a, 0, 0, texCol.a);
}

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	vec4 oCol;
	
	if( uComp%2) {
		// Premultiply incoming data
		texCol.r *= texCol.a;
		texCol.g *= texCol.a;
		texCol.b *= texCol.a;
	}
	
	switch( (uComp >> 1) & 0x7) {
	case 0: 
		oCol = texCol;
		break;
	case 1:
		oCol = changeColor( texCol);
		break;
	}
	
	
	outputColor = oCol*uAlpha;
}