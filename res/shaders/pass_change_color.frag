#version 330

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;
uniform vec4 cFrom;
uniform vec4 cTo;
uniform int optionMask;

void main()
{
	vec4 intex = texture(myTexture, vUV);
	vec4 texCol = vec4(intex[3],intex[2],intex[1],intex[0]);
	
	
	if( distance(cFrom.r , texCol.r) < thresh &&
		distance(cFrom.g , texCol.g) < thresh &&
		distance(cFrom.b , texCol.b) < thresh &&
		(optionMask == 0 || distance(cFrom.a , texCol.a) < thresh) ) {
		outputColor.rgb = cTo.rgb;
		outputColor.a = texCol.a;
	}
	else 
		outputColor = texCol;
}