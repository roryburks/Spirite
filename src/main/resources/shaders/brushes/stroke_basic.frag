#version 330

#GLOBAL

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
	
	float alpha;
	switch( uMode) {
	case 0:
		alpha = sqrt(sqrt(w));
    	break;
    case 1:
    	alpha = (w>0)?1:0;
	}
   	outputColor = vec4(uColor*alpha,alpha);
	
}