#version 100

precision highp float;

varying float fWeight;
varying float fX;
varying float fY;
varying float fM;

uniform vec3 uColor;
uniform int uMode;

void main()
{
	float w;
	if( fWeight < 0.0) {
		w =max(0.0,1.0 - length(  vec2(gl_FragCoord.x - fX, gl_FragCoord.y - fY))/fM);
	}
	else w = fWeight;

	float alpha;
	switch( uMode) {
	case 0:
		alpha = sqrt(sqrt(w));
    	break;
    case 1:
    	alpha = (w>0.0)?1.0:0.0;
	}
   	gl_FragColor = vec4(uColor*alpha,alpha);
}