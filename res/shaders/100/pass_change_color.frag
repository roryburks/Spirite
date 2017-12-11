#version 100

#define thresh 0.005

precision highp float;

varying vec2 vUV;

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
	vec4 texCol = texture2D(myTexture, vUV);
	vec3 checkCol = vec3(texCol.rgb);

	bool premult = bool((optionMask >> 2) & 1);

	if( premult)
		checkCol /= texCol.a;

	int _mode = optionMask & 3;

	if( _mode == 2 ||
		(distance(cFrom.r , checkCol.r) < thresh &&
		distance(cFrom.g , checkCol.g) < thresh &&
		distance(cFrom.b , checkCol.b) < thresh &&
		(_mode == 1 || distance(cFrom.a , texCol.a) < thresh) )) {
		gl_FragColor.rgb = (premult)?cTo.rgb * texCol.a:cTo.rgb;
	}
	else
		gl_FragColor = texCol;
}