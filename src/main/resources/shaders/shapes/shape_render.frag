#version 330

uniform vec3 u_color;
uniform float u_alpha;


uniform int u_flags;
// 000000BA
//  A: 0 - destination isn't isPremultiplied, 1 - destination is isPremultiplied
//  B: 0 - texture1 isn't isPremultiplied, 1 - texture1 is isPremultiplied

out vec4 outputColor;

bool targetPremultiplied() {return (u_flags & 1) == 1;}
bool sourcePremultiplied() {return (u_flags & 2) == 1;}

void main()
{
    vec3 color = targetPremultiplied() ? u_color * u_alpha : u_color;

   	outputColor = vec4( color, u_alpha);
}