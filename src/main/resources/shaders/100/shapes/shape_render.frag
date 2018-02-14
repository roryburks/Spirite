#version 100

precision highp float;

uniform vec3 uColor;
uniform float uAlpha;

void main()
{
   	gl_FragColor  = vec4(uColor*uAlpha,uAlpha);
//   	gl_FragColor  = vec4(1,1,1,1);
}