#version 330

smooth in vec2 vUV;

uniform float fixedCol;

// which component is fixed:
// 0: r, 1: g, 2: b
// 3: h, 2: s, 3: v
uniform int varCol;

out vec4 outputColor;

vec3 hsv2rgb(vec3 _color)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(_color.xxx + K.xyz) * 6.0 - K.www);
    return _color.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), _color.y);
}

void main()
{
	if( varCol == 0) {
		outputColor.r = fixedCol;
		outputColor.g = vUV.x;
		outputColor.b = vUV.y;
	    outputColor.a = 1;
	}
	else if( varCol == 1) {
		outputColor.r = vUV.x;
		outputColor.g = fixedCol;
		outputColor.b = vUV.y;
	    outputColor.a = 1;
	}
	else if( varCol == 2) {
		outputColor.r = vUV.x;
		outputColor.g = vUV.y;
		outputColor.b = fixedCol;
	    outputColor.a = 1;
	}
	else if( varCol == 3) {
		vec3 hsv;
		hsv[0] = fixedCol;
		hsv[1] = vUV.x;
		hsv[2] = vUV.y;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}
	else if( varCol == 4) {
		vec3 hsv;
		hsv[0] = vUV.x;;
		hsv[1] = fixedCol;
		hsv[2] = vUV.y;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}
	else if( varCol == 5) {
		vec3 hsv;
		hsv[0] = vUV.x;
		hsv[1] = vUV.y;
		hsv[2] = fixedCol;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}
}
