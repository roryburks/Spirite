#version 330

// PassFill is takes a 1-bit texture packed into 8x4 chunks through ints and translate them into a straight
//  pass of a given jcolor.  It is designed to transcribe the result of a sequentially-calculated fill algorithm
//  onto a texture quickly and efficiently.
//
//  Note: Because the int array is interpretted as a UnsignedInt texture (filled entirely into red), usampler2D is needed,
//  meaning the global pragma can't be used, so versioning care has to be taken.

uniform usampler2D u_texture;

uniform int u_flags;
bool targetPremultiplied() {return (u_flags & 1) != 0;}

in vec2 vUV;

out vec4 outputColor;

uniform vec4 u_color;
uniform float u_wratio;
uniform float u_hratio;
uniform int u_width;
uniform int u_height;


void main() {
    uint x = uint(floor( vUV.x * u_width));
    uint y = uint(floor( vUV.y * u_height));
    vec2 uv = vec2( vUV.x * u_wratio, vUV.y * u_hratio);
    uint sector = texture(u_texture, uv).r;

    uint mask = 1u << ((x % 8u) + (y%4u)*8u);
    if( (sector & mask) != 0u)
        outputColor = (targetPremultiplied())
            ? vec4(u_color.r*u_color.a,u_color.g*u_color.a,u_color.b*u_color.a,u_color.a)
            : u_color;
    else
        outputColor = vec4(0,0,0,0);
}