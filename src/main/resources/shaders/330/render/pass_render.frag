#version 330

#define MAX_CALLS 10

#GLOBAL

in vec2 vUV;

out vec4 outputColor;

//uniform sampler2D myTexture2;
uniform float u_alpha;


// Subvariable assosciated with Composite
uniform int u_values[MAX_CALLS];

// 0: StraightPass
// 1: As Hue
// 2: As Color (all
// 3: Disolve
uniform int u_composites[MAX_CALLS];


// ==== As Hue ====
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

vec4 changeColorHue(vec4 texCol, int subValue) {
	vec3 cTo = vec3(((subValue>>16)&0xFF)/255.0f, ((subValue>>8)&0xFF)/255.0f, ((subValue)&0xFF)/255.0f);
	vec3 hsvFrom = rgb2hsv( texCol.rgb);
	vec3 hsvTo = rgb2hsv( cTo.rgb);

	return vec4(hsv2rgb( vec3(hsvTo[0], hsvFrom[1], hsvFrom[2])) , texCol.a);
}

// ==== Method 2: As Color ====
vec4 changeColor(vec4 texCol, int subValue) {
	float r = ((subValue>>16)&0xFF)/255.0f;
	float g = ((subValue>>8)&0xFF)/255.0f;
	float b = ((subValue)&0xFF)/255.0f;

	return vec4( r, g, b, texCol.a);
}

// ==== Method 3: Disolbe ====
vec4 disolve( vec4 tex, int subValue)
{
	if( (subValue &
			(1 << (   (int(gl_FragCoord.x) % 4) + 4*( int(gl_FragCoord.y) %  4)))) != 0)
	{
		return tex;
	}
	else return vec4(0,0,0,0);
}



void main()
{
	vec4 texCol = texture(u_texture, vUV);
	vec4 oCol;

	if( sourcePremultiplied()) {
	    // De-multiply source so that we can work with standardized RGB values
	    texCol.r /= texCol.a;
	    texCol.g /= texCol.a;
	    texCol.b /= texCol.a;
	}

	oCol = texCol;  // Default as straight pass

	for( int i = 0; i < MAX_CALLS; ++i) {
	    switch( u_composites[i]) {
	    case 0: break;
	    case 1:
	        oCol = changeColorHue( oCol, u_values[i]);
	        break;
	    case 2:
	        oCol = changeColor( oCol, u_values[i]);
	        break;
	    case 3:
	        oCol = disolve(oCol, u_values[i]);
	        break;
	    }
	}

	outputColor = (true)
	    ? vec4(oCol.r * oCol.a, oCol.g * oCol.a, oCol.b * oCol.a, oCol.a) * u_alpha
	    : vec4(oCol.r, oCol.g, oCol.b, oCol.a * u_alpha);
}
