#version 330

uniform vec3 u_color;
uniform float u_alpha;


uniform int u_flags;
// 000000BA
//  A: 0 - destination isn't isPremultiplied, 1 - destination is isPremultiplied
//  B: 0 - texture1 isn't isPremultiplied, 1 - texture1 is isPremultiplied

out vec4 outputColor;

void main()
{
    vec3 color = ( (u_flags & 1) == 0) ? u_color : u_color * u_alpha;

   	outputColor = vec4( color, u_alpha);
}