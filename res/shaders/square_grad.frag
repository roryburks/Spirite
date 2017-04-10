#version 330

smooth in vec2 vUV;

uniform float fixedCol;

// which component is fixed:
// 0: r, 1: g, 2: b
// 3: h, 2: s, 3: v
uniform int varCol;

out vec4 outputColor;

vec4 HSVtoRGB( vec4 hsv) {
	float hh, p, q, t, ff;
	int i;
	vec4 ret;
	ret.a = hsv[3];
	
	if( hsv[1] <= 0.0) {
		ret.r = hsv[2];
		ret.g = hsv[2];
		ret.b = hsv[2];
	}
	hh = hsv[0];
	if( hh >= 1.0) hh = 0.0;
	hh *= 6.0;
	i = int(hh);
	ff = hh - i;
	p = hsv[2] * (1.0 - hsv[1]);
	q = hsv[2] * (1.0 - (hsv[1] * ff));
	t = hsv[2] * (1.0 - (hsv[1] * (1.0 - ff)));
	
	if( i == 0) {
		ret.r = hsv[2];
		ret.g = t;
		ret.b = p;
	} else if( i ==1) {
		ret.r = q;
		ret.g = hsv[2];
		ret.b = p;
	} else if( i ==2) {
		ret.r = p;
		ret.g = hsv[2];
		ret.b = t;
	} else if( i ==3) {
		ret.r = p;
		ret.g = q;
		ret.b = hsv[2];
	} else if( i ==4) {
		ret.r = t;
		ret.g = p;
		ret.b = hsv[2];
	} else if( i ==5) {
		ret.r = hsv[2];
		ret.g = p;
		ret.b = q;
	}
	
	return ret;
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
		vec4 hsv;
		hsv[0] = fixedCol;
		hsv[1] = vUV.x;
		hsv[2] = vUV.y;
		hsv[3] = 1;
		
		outputColor = HSVtoRGB(hsv);
	}
	else if( varCol == 4) {
		vec4 hsv;
		hsv[0] = vUV.x;;
		hsv[1] = fixedCol;
		hsv[2] = vUV.y;
		hsv[3] = 1;
		
		outputColor = HSVtoRGB(hsv);
	}
	else if( varCol == 5) {
		vec4 hsv;
		hsv[0] = vUV.x;
		hsv[1] = vUV.y;
		hsv[2] = fixedCol;
		hsv[3] = 1;
		
		outputColor = HSVtoRGB(hsv);
	}
}
