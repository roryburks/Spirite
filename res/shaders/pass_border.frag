#version 330

in vec2 vUV;

out vec4 outputColor; 
uniform sampler2D myTexture;


void main()
{
	
	if( texture(myTexture, vUV)[0] == 0) {
		vec2 size = textureSize( myTexture, 0);
		float p_w = 1.0f / float(size.x);
		float p_h = 1.0f / float(size.y);
		
		if( texture(myTexture, vUV + vec2(-p_w, 0))[0] != 0 ||
		    texture(myTexture, vUV + vec2( p_w, 0))[0] != 0 ||
		    texture(myTexture, vUV + vec2( 0,-p_h))[0] != 0 ||
		    texture(myTexture, vUV + vec2( 0, p_h))[0] != 0)
		{
			if( ((((int(gl_FragCoord.x) + int(gl_FragCoord.y)) / 4) % 2)
			    ^ (int(gl_FragCoord.y)/10%2)) == 0)
				outputColor = vec4(0,0,0,1);
			else
				outputColor = vec4(1,1,1,1);
		}
	}
	else
		outputColor = vec4(0,0,0,0);
}