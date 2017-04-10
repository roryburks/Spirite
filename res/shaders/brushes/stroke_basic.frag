#version 330

in float fWeight;
flat in float fX, fY, fM;

uniform vec3 uColor;
uniform int uMode;

out vec4 outputColor;

void main()
{
	float w;
	if( fWeight < 0) {
		w =max(0,1 - length(  vec2(gl_FragCoord.x - fX, gl_FragCoord.y - fY))/fM);
	}
	else w = fWeight;
	
	switch( uMode) {
	case 0:
    	outputColor = vec4(uColor,sqrt(sqrt(w)));
    	break;
    case 1:
    	outputColor = vec4(uColor,(w>0)?1:0);
	}
	
}