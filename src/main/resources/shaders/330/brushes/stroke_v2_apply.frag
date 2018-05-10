#version 330

#define MAX_CALLS 10

#GLOBAL

in vec2 vUV;

out vec4 outputColor;

uniform vec3 u_color;
uniform float u_alpha;
uniform int u_intensifyMode;

float alphaIntensify( float alpha) {
    switch(u_intensifyMode) {
    case 0: return (alpha > 0.5)?pow(alpha,0.3):pow(alpha,1.5);
    case 1: return (alpha > 0.5)?1:0;
    }

    return alpha;
}

void main() {
	vec4 texCol = texture(u_texture, vUV);

    // Intensify Alpha
    float alpha = alphaIntensify(texCol.a * u_alpha);

	vec4 oColor = vec4(u_color, alpha);

	outputColor = (targetPremultiplied())
	    ? vec4(oColor.a*oColor.r, oColor.a*oColor.g, oColor.a*oColor.b, oColor.a )
	    : oColor;
}
