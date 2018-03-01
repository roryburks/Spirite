#version 330

#GLOBAL

/*	brush_intensify.frag
 */
in vec2 vUV;
uniform int uMode;

out vec4 outputColor;

void main()
{
	vec4 texCol = texture(u_texture, vUV);
	float alpha = texCol.a;

	float newAlpha;

	switch( uMode) {
	case 0:
		newAlpha = (alpha > 0.5)?pow(alpha,0.3):pow(alpha,1.5);
	}
   	outputColor = vec4(texCol.rgb*newAlpha,newAlpha);
}
