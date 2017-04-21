#version 330


in vec2 vUV;

out vec4 outputColor;

uniform sampler2D myTexture;
//uniform sampler2D myTexture2;
uniform float uAlpha;
uniform unsigned int uValue;

// 00000000 00000000 00000000 0000BBBA
// A: how to combine the color and alpha
//		0: multiply colors by the input alpha
//		1: multiply colors by total alpha
// B: which subroutine to use
//		0: Straight Pass (usually for Porter-Duff SourceOver)
//		1: As Color (defined by uValue)
//		2: Disolve
//		3: 
uniform int uComp;	

vec4 changeColor(vec4 texCol) {
	float r = ((uValue>>16)&0xFF)/255.0f;
	float g = ((uValue>>8)&0xFF)/255.0f;
	float b = ((uValue)&0xFF)/255.0f;
	return vec4( r*texCol.a, g*texCol.a, b*texCol.a, texCol.a);
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