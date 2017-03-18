#version 330

#define thresh 0.005

in vec2 vUV;

out vec4 outputColor; 

uniform sampler2D myTexture;
uniform vec4 cFrom;
uniform vec4 cTo;

void main()
{
	vec4 texCol = texture(myTexture, vUV).rgba;
	if( texCol.a != 0) {
		// AWTTextureIO.newTexture comes premultiplied, but 
		//	AWTGLReadBufferUtil.readPixelsToBufferedImage
		//	inteprets it as non-premultiplied.  Go figure.
		texCol.r /= texCol.a;
		texCol.g /= texCol.a;
		texCol.b /= texCol.a;
	}
	
	if( distance(cFrom.r , texCol.r) < thresh &&
		distance(cFrom.g , texCol.g) < thresh &&
		distance(cFrom.b , texCol.b) < thresh ) {
		outputColor.rgb = cTo.rgb;
		outputColor.a = texCol.a;
	}
	else 
		outputColor = texCol;
}