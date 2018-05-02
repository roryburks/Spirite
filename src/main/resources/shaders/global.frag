
uniform sampler2D u_texture;

// 000000BA
//  A: 0 - destination isn't isPremultiplied, 1 - destination is isPremultiplied
//  B: 0 - texture1 isn't isPremultiplied, 1 - texture1 is isPremultiplied
uniform int u_flags;
bool targetPremultiplied() {return (u_flags & 1) != 0;}
bool sourcePremultiplied() {return (u_flags & 2) != 0;}