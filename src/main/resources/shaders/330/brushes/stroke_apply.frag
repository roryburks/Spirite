#version 330

#GLOBAL

in vec2 vUV;

out vec4 outputColor;

uniform vec3 u_color;
uniform float u_alpha;

void main() {
	vec4 texCol = texture(u_texture, vUV);

    float alpha = texCol.a * u_alpha;

	vec4 oColor = vec4(u_color, alpha);

	outputColor = (targetPremultiplied())
	    ? vec4(oColor.a*oColor.r, oColor.a*oColor.g, oColor.a*oColor.b, oColor.a )
	    : oColor;
}
