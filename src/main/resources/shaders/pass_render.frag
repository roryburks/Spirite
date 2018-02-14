#version 330


in vec2 vUV;

out vec4 outputColor;

uniform sampler2D myTexture;
//uniform sampler2D myTexture2;
uniform float u_alpha;
uniform int u_value;

// 00000000 00000000 00000000 0000BBBA
// A: how to combine the color and alpha
//		0: multiply colors by the input alpha
//		1: multiply colors by total alpha
// B: which subroutine to use
//		0: Straight Pass (usually for Porter-Duff SourceOver)
//		1: As Color (defined by uValue)
//		2: As Color (all)
//		3: Disolve
uniform int u_composite;

vec4 disolve( vec4 tex)
{
	if( (u_value &
			(1 << (   (int(gl_FragCoord.x) % 4) + 4*( int(gl_FragCoord.y) %  4)))) != 0)
	{

		return tex;
	}
	else return vec4(0,0,0,0);
}

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

vec4 changeColorHue(vec4 texCol) {
	bool premult = false;
	vec3 cTo = vec3(((u_value>>16)&0xFF)/255.0f, ((u_value>>8)&0xFF)/255.0f, ((u_value)&0xFF)/255.0f);
	vec3 hsvFrom = premult ? rgb2hsv( texCol.rgb) / texCol.a : rgb2hsv( texCol.rgb);
	vec3 hsvTo = rgb2hsv( cTo.rgb);

	return vec4(
			(premult) ? hsv2rgb( vec3(hsvTo[0], hsvFrom[1], hsvFrom[2])) * texCol.a
				:hsv2rgb( vec3(hsvTo[0], hsvFrom[1], hsvFrom[2]))
			 ,  texCol.a);
}

vec4 changeColor(vec4 texCol) {
	float r = ((u_value>>16)&0xFF)/255.0f;
	float g = ((u_value>>8)&0xFF)/255.0f;
	float b = ((u_value)&0xFF)/255.0f;
	return vec4( r*texCol.a, g*texCol.a, b*texCol.a, texCol.a);
}

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	vec4 oCol;
	
	if( u_composite%2 != 0) {
		// Premultiply incoming data
		texCol.r *= texCol.a;
		texCol.g *= texCol.a;
		texCol.b *= texCol.a;
	}
	
	switch( (u_composite >> 1) & 0x7) {
	case 0: 
		oCol = texCol;
		break;
	case 1:
		oCol = changeColorHue( texCol);
		break;
	case 2:
		oCol = changeColor( texCol);
		break;
	case 3:
		oCol = disolve( texCol);
		break;
	}
	
	
	outputColor = oCol*u_alpha;
}
