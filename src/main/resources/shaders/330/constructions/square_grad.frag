#version 330

#GLOBAL

smooth in vec2 vUV;

uniform float u_fixedAmmount;

// which resizeComponent is fixed:
// 0: r, 1: g, 2: b
// 3: h, 2: s, 3: v
uniform int u_typeCode;

out vec4 outputColor;


vec3 hsv2rgb(vec3 _color)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(_color.xxx + K.xyz) * 6.0 - K.www);
    return _color.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), _color.y);
}

void main()
{
	if( u_typeCode == 0) {
		outputColor.r = u_fixedAmmount;
		outputColor.g = vUV.x;
		outputColor.b = vUV.y;
	    outputColor.a = 1;
	}
	else if( u_typeCode == 1) {
		outputColor.r = vUV.x;
		outputColor.g = u_fixedAmmount;
		outputColor.b = vUV.y;
	    outputColor.a = 1;
	}
	else if( u_typeCode == 2) {
		outputColor.r = vUV.x;
		outputColor.g = vUV.y;
		outputColor.b = u_fixedAmmount;
	    outputColor.a = 1;
	}
	else if( u_typeCode == 3) {
		vec3 hsv;
		hsv[0] = u_fixedAmmount;
		hsv[1] = vUV.x;
		hsv[2] = vUV.y;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}
	else if( u_typeCode == 4) {
		vec3 hsv;
		hsv[0] = vUV.x;;
		hsv[1] = u_fixedAmmount;
		hsv[2] = vUV.y;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}
	else if( u_typeCode == 5) {
		vec3 hsv;
		hsv[0] = vUV.x;
		hsv[1] = vUV.y;
		hsv[2] = u_fixedAmmount;
		
		outputColor = vec4(hsv2rgb(hsv), 1);
	}

	if( targetPremultiplied())
	    outputColor *= vec4(outputColor.a, outputColor.a, outputColor.a, 1);
}
