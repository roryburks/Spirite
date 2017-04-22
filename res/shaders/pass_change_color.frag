#version 130

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;
uniform vec4 cFrom;
uniform vec4 cTo;

// 0BAA : 
//	AA : 0 - Exact Match
//		 1 - Ignore Alpha
//		 2 - Change All
//  B : whether or not data is premultiplied
uniform int optionMask;

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	vec3 checkCol = vec3(texCol.rgb);
	
	bool premult = bool((optionMask >> 2) & 1);
	
	if( premult)
		checkCol /= texCol.a;
	
	int mode = optionMask & 3;
	
	if( mode == 2 || 
		(distance(cFrom.r , checkCol.r) < thresh &&
		distance(cFrom.g , checkCol.g) < thresh &&
		distance(cFrom.b , checkCol.b) < thresh &&
		(mode == 1 || distance(cFrom.a , texCol.a) < thresh) )) {
		outputColor.rgb = (premult)?cTo.rgb * texCol.a:cTo.rgb;
		outputColor.a = texCol.a;
	}
	else 
		outputColor = texCol;
}