#version 100

precision highp float;

varying vec2 vUV;

uniform sampler2D myTexture;
uniform int uCycle;
uniform float uTWidth;
uniform float uTHeight;

void main()
{
	if( texture2D(myTexture, vUV).a == 0.0) {
		float p_w = 1.0f / uTWidth;
		float p_h = 1.0f / uTHeight;

		if( (vUV[0] >= p_w && texture2D(myTexture, vUV + vec2(-p_w, 0)).a != 0.0) ||
		    (1.0-vUV[0] >= p_w && texture2D(myTexture, vUV + vec2( p_w, 0)).a != 0.0) ||
		    (vUV[1] >= p_h && texture2D(myTexture, vUV + vec2( 0,-p_h)).a != 0.0) ||
		    (1.0-vUV[1] >= p_h && texture2D(myTexture, vUV + vec2( 0, p_h)).a != 0.0))
		{
			if( ((((int(gl_FragCoord.x) + int(gl_FragCoord.y) + uCycle) / 4) & 1)
			    ^ ((int(gl_FragCoord.y) + uCycle)/10&1)) == 0)
				gl_FragColor = vec4(0.0,0.0,0.0,1.0);
			else
				gl_FragColor = vec4(1.0,1.0,1.0,1.0);
		}
	}
	else
		gl_FragColor = vec4(0.0,0.0,0.0,0.0);
}