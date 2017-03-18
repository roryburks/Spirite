#version 330

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;
uniform vec4 cFrom;
uniform vec4 cTo;

void main()
{
	vec4 texCol = texture(myTexture, vUV).rgba;
	
	if( distance(cFrom.r , texCol.r) < 0.00390625 &&
		distance(cFrom.g , texCol.g) < 0.00390625 &&
		distance(cFrom.b , texCol.b) < 0.00390625 ) {
		outputColor.rgb = cTo.rgb;
		outputColor.a = texCol.a;
	}
	else 
		outputColor = texCol;
}