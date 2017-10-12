#version 330

/*	brush_intensify.frag
 */
in vec2 vUV;
uniform sampler2D myTexture;
uniform int uMode;

out vec4 outputColor;

void main()
{
	vec4 texCol = texture(myTexture, vUV);
	float alpha = texCol.a;

	float newAlpha;

	switch( uMode) {
	case 0:
		newAlpha = (alpha > 0.5)?sqrt(alpha):pow(alpha,1.5);
	}
   	outputColor = vec4(texCol.rgb*newAlpha/alpha,newAlpha);
}
