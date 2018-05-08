#version 330

// PassFill is takes a 1-bit texture packed into 8x4 chunks through ints and translate them into a straight
//  pass of a given color.  It is designed to transcribe the result of a sequentially-calculated fill algorithm
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
uniform int u_width;
uniform int u_height;


void main() {
    int x = int(floor( vUV.x * u_width));
    int y = int(floor( vUV.y * u_height));
    uint sector = texture(u_texture, vUV).r;

    uint mask = 1 << ((x % 8) + (y%4)*8);
    if( (sector & mask) != 0)
        outputColor = (targetPremultiplied())
            ? vec4(u_color.r*u_color.a,u_color.g*u_color.a,u_color.b*u_color.a,u_color.a)
            : u_color;
    else
        outputColor = vec4(0,0,0,0);
}