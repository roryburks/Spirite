#version 450

smooth in vec4 theColor;

in float fWeight;
flat in float fX, fY, fM;

uniform vec3 uColor;

out vec4 outputColor;

void main()
{
	float w;
	if( fWeight < 0) {
		w =1 - length(  vec2(gl_FragCoord.x - fX, gl_FragCoord.y - fY))/fM;
	}
	else w = fWeight;

	// Porter-Duff Source Over Destination
    outputColor = vec4(uColor,sqrt(sqrt(w)));
}