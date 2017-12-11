#version 130

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;
uniform vec4 cFrom;
uniform vec4 cTo;

// CBAA : 
//	AA : 0 - Exact Match
//		 1 - Ignore Alpha
//		 2 - Change All
//  B (premult) : whether or not data is premultiplied
//  C (hueOnly) : whether to change all color data or just the hue
uniform int optionMask;

vec3 rgb2hsv(vec3 _color)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(_color.bg, K.wz), vec4(_color.gb, K.xy), step(_color.b, _color.g));
    vec4 q = mix(vec4(p.xyw, _color.r), vec4(_color.r, p.yzx), step(p.x, _color.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 _color)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(_color.xxx + K.xyz) * 6.0 - K.www);
    return _color.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), _color.y);
}

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	vec3 checkCol = vec3(texCol.rgb);
	
	bool premult = bool((optionMask >> 2) & 1);
	bool hueOnly = bool((optionMask >> 3) & 1);
	
	if( premult)
		checkCol /= texCol.a;
	
	int _mode = optionMask & 3;
	
	if( _mode == 2 ||
		(distance(cFrom.r , checkCol.r) < thresh &&
		distance(cFrom.g , checkCol.g) < thresh &&
		distance(cFrom.b , checkCol.b) < thresh &&
		(_mode == 1 || distance(cFrom.a , texCol.a) < thresh) ))
	{
		if( !hueOnly) {
			outputColor.rgb = (premult)?cTo.rgb * texCol.a:cTo.rgb;
			outputColor.a = texCol.a;
		}
		else {
			vec3 hsvFrom = premult ? rgb2hsv( texCol.rgb) / texCol.a : rgb2hsv( texCol.rgb);
			vec3 hsvTo = rgb2hsv( cTo.rgb);
			outputColor.rgb = (premult) ? hsv2rgb( vec3(hsvTo[0], hsvFrom[1], hsvFrom[2])) * texCol.a
					:hsv2rgb( vec3(hsvTo[0], hsvFrom[1], hsvFrom[2]));
			outputColor.a = texCol.a;
		}
	}
	else 
		outputColor = texCol;
}
