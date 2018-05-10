#version 330

#GLOBAL

in vec2 vUV;

out vec4 outputColor;

void main()
{
	vec4 texCol = texture(u_texture, vUV);

	if( sourcePremultiplied()) {
	    if( targetPremultiplied())
        	outputColor = vec4( texCol.a - texCol.r, texCol.a - texCol.g, texCol.a - texCol.b, texCol.a);
        else    // target normal
            outputColor = vec4( (texCol.a - texCol.r) / texCol.a, (texCol.a - texCol.g) / texCol.a, (texCol.a - texCol.b) / texCol.a, texCol.a);
	}
	else {  // source normal
	    if( targetPremultiplied())
	        outputColor = vec4( (1 - texCol.r) * texCol.a, (1 - texCol.g) * texCol.a,(1 - texCol.b) * texCol.a,texCol.a);
	    else // target normal
	        outputColor = vec4( 1 - texCol.r, 1 - texCol.g, 1 - texCol.b, texCol.a);
	}
	outputColor = vec4( texCol.a - texCol.r, texCol.a - texCol.g, texCol.a - texCol.b, texCol.a);
}