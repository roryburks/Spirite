package spirite.base.graphics.gl;

import java.util.ArrayList;
import java.util.List;

import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.compaction.ReverseFloatCompactor;
import spirite.base.util.glmath.GLC;
import spirite.base.util.glmath.Vec2;

/**
 * GLGeom contains methods which performs software alternative to the Geometry Shaders for
 * implementations which do not implement Geometry Shaders.
 *
 * @author Rory Burks
 */
public class GLGeom {

    public static class Primitive {
        float raw[];
        int attrLengths[];
        int primitiveType;
        int primitiveLengths[];

        public Primitive() {}
        public Primitive(float raw[], int attrLengths[], int primitiveType, int[] primitiveLengths){
            this.raw = raw;
            this.attrLengths = attrLengths;
            this.primitiveType = primitiveType;
            this.primitiveLengths = primitiveLengths;
        }
    }

    public static String printPrimative( Primitive prim) {
        String str = "";

        int tal = 0;
        for( int i=0; i < prim.attrLengths.length; ++i){
            tal += prim.attrLengths[i];
        }

        for( int o=0; o < prim.raw.length; ++o) {
            str += prim.raw[o] + ",";
            if( ((o+1) % tal)==0) str += " || ";
        }

        return str;
    }

    private static class PrimitiveBuilder {
        private final int primitiveType;
        private final int attrLengths[];
        private final int totalAttrLength;
        private final FloatCompactor output;
        private final List<Integer> primitiveLengths = new ArrayList<>();

        int plen = 0;

        private PrimitiveBuilder( int attrLengths[], int primitiveType) {
            this.primitiveType = primitiveType;
            this.attrLengths = attrLengths;

            output = new FloatCompactor();

            // Calculate Total Vertex Length
            int t = 0;
            for( int i=0; i < attrLengths.length; ++i)
                t += attrLengths[i];
            totalAttrLength = t;
        }

        private void emitVertex( float[] vertexData) {
            int vl = (vertexData == null) ? 0 : vertexData.length;

            for( int i=0; i < totalAttrLength; ++i) {
                output.add( i < vl ? vertexData[i] : 0.0f);
            }
            plen++;
        }

        private void emitPrimitive() {
        	if( plen > 0)
        		primitiveLengths.add(plen);
            plen = 0;
        }

        private Primitive build() {
            Primitive primitive = new Primitive();
            primitive.primitiveType = primitiveType;
            primitive.attrLengths = attrLengths;
            primitive.raw = output.toArray();
            primitive.primitiveLengths = new int[primitiveLengths.size()];
            for( int i=0; i < primitiveLengths.size(); ++i)
                primitive.primitiveLengths[i] = primitiveLengths.get(i);

            return primitive;
        }
    }
    private static class DoubleEndedSinglePrimitiveBuilder {
        private final int primitiveType;
        private final int attrLengths[];
        private final int totalAttrLength;
        private final FloatCompactor forward;
        private final ReverseFloatCompactor backward;

        private DoubleEndedSinglePrimitiveBuilder( int attrLengths[], int primitiveType) {
            this.primitiveType = primitiveType;
            this.attrLengths = attrLengths;

            forward = new FloatCompactor();
            backward = new ReverseFloatCompactor();

            // Calculate Total Vertex Length
            int t = 0;
            for( int i=0; i < attrLengths.length; ++i)
                t += attrLengths[i];
            totalAttrLength = t;
        }

        private void emitVertexFront( float[] vertexData) {
            int vl = (vertexData == null) ? 0 : vertexData.length;

            for( int i=0; i < totalAttrLength; ++i) {
            	forward.add( i < vl ? vertexData[i] : 0.0f);
            }
        }
        private void emitVertexBack( float[] vertexData) {
            int vl = (vertexData == null) ? 0 : vertexData.length;

            for( int i=0; i < totalAttrLength; ++i) {
            	backward.add( i < vl ? vertexData[totalAttrLength-i-1] : 0.0f);
            }
        }

        private Primitive build() {
            Primitive primitive = new Primitive();
            primitive.primitiveType = primitiveType;
            primitive.attrLengths = attrLengths;
            primitive.raw = new float[ forward.size() +backward.size()];
            forward.insertIntoArray(primitive.raw, 0);
            backward.insertIntoArray(primitive.raw, forward.size());
            
            int stride = 0;
            for( int len : attrLengths)
            	stride += len;
            primitive.primitiveLengths = new int[]{primitive.raw.length / stride};

            return primitive;
        }
    }

    /**
     *
     * @param raw raw float data [x, y, size], [x, y, size]
     * @return
     */
    public static Primitive strokeBasicGeom( float[] raw, float fH)
    {
        PrimitiveBuilder builder = new PrimitiveBuilder(
                // [x, y], fWeight, wX, fY, fM
                new int[]{2, 1, 1, 1, 1},
                GLC.GL_TRIANGLE_STRIP
        );

        int length = raw.length/SB_STRIDE - 3;

        float fX = 0;
        float fY = 0;
        float fM = 0;
        float fWeight = 0;

        for( int i=0; i < length; ++i) {
            float w1 = raw[(i+1)*SB_STRIDE+2]/2.0f;
            float w2 = raw[(i+2)*SB_STRIDE+2]/2.0f;

            Vec2 bc= new Vec2( raw[(i+1)*SB_STRIDE], raw[(i+1)*SB_STRIDE+1]);
            Vec2 tc = new Vec2( raw[(i+2)*SB_STRIDE], raw[(i+2)*SB_STRIDE+1]);

            Vec2 dif = tc.sub(bc).normalize();

            // Start Dot
            Vec2 bl = new Vec2( bc.x - dif.y * w1, bc.y + dif.x * w1);
            Vec2 br = new Vec2( bc.x + dif.y * w1, bc.y - dif.x * w1);
            fWeight = -1;
            fX = bc.x+0.5f;
            fY = fH - (bc.y+0.5f);
            fM = w1;

            builder.emitVertex( new float[]{bl.x-dif.x*w1, bl.y-dif.y*w1, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{bl.x+dif.x*w1, bl.y+dif.y*w1, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{br.x-dif.x*w1, br.y-dif.y*w1, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{br.x+dif.x*w1, br.y+dif.y*w1, fWeight, fX, fY, fM});
            builder.emitPrimitive();

            // End Dot
            Vec2 tl = new Vec2( tc.x - dif.y * w2, tc.y + dif.x * w2);
            Vec2 tr = new Vec2( tc.x + dif.y * w2, tc.y - dif.x * w2);
            fX = tc.x+0.5f;
            fY = fH-(tc.y+0.5f);
            fM = w2;
            builder.emitVertex( new float[]{tl.x-dif.x*w2, tl.y-dif.y*w2, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{tl.x+dif.x*w2, tl.y+dif.y*w2, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{tr.x-dif.x*w2, tr.y-dif.y*w2, fWeight, fX, fY, fM});
            builder.emitVertex( new float[]{tr.x+dif.x*w2, tr.y+dif.y*w2, fWeight, fX, fY, fM});
            builder.emitPrimitive();

            fX = 0;
            fY = 0;
            fM = w2;

            builder.emitVertex( new float[]{ bl.x, bl.y, 0, fX, fY, fM});
            builder.emitVertex( new float[]{ tl.x, tl.y, 0, fX, fY, fM});
            builder.emitVertex( new float[]{ bc.x, bc.y, 1, fX, fY, fM});
            builder.emitVertex( new float[]{ tc.x, tc.y, 1, fX, fY, fM});
            builder.emitVertex( new float[]{ br.x, br.y, 0, fX, fY, fM});
            builder.emitVertex( new float[]{ tr.x, tr.y, 0, fX, fY, fM});
            builder.emitPrimitive();
        }

        return builder.build();
    }
    private static final int SB_STRIDE = 3;

    /**
     * Does the job of line_render.geom in software rendering
     *
     * @param raw vertex data arranyed xyxyxy
     * @param uJoin Join Method: 0: none, 1: Miter, 2: Bevel
     * @param uWidth Line Width
     * @return
     */
    public static Primitive lineRenderGeom( float[] raw, int uJoin, float uWidth)
    {
        PrimitiveBuilder builder = new PrimitiveBuilder(
                // [x, y]
                new int[]{2},
                GLC.GL_TRIANGLE_STRIP
                );

        int length = raw.length/LRG_STRIDE - 3;

        switch( uJoin) {
        case 1:
            for (int i = 0; i < length; ++i)
                lrgDoMiter(raw, i, uWidth, builder);
            break;
        case 2:
            for (int i = 0; i < length; ++i)
                lrgDoBevel(raw, i, uWidth, builder);
            break;
        default:
            for (int i = 0; i < length; ++i)
                lrgDoFlat(raw, i, uWidth, builder);
        }

        return builder.build();
    }
    private final static int LRG_STRIDE = 2;
    private final static float MITER_MAX = 10.0f;
    private static void lrgDoFlat( float[] raw, int index, float uWidth, PrimitiveBuilder builder) {
        Vec2 p1 = new Vec2( raw[(index+1)*LRG_STRIDE ], raw[(index+1)*LRG_STRIDE +1]);
        Vec2 p2 = new Vec2( raw[(index+2)*LRG_STRIDE ], raw[(index+2)*LRG_STRIDE +1]);
        Vec2 normal = p2.sub(p1).normalize();
        Vec2 nl = new Vec2( -normal.y, normal.x);
        Vec2 nr = new Vec2( normal.y, -normal.x);

        builder.emitVertex( new float[] { nl.x * uWidth + p1.x, nl.y * uWidth + p1.y});
        builder.emitVertex( new float[] { nr.x * uWidth + p1.x, nr.y * uWidth + p1.y});
        builder.emitVertex( new float[] { nl.x * uWidth + p2.x, nl.y * uWidth + p2.y});
        builder.emitVertex( new float[] { nr.x * uWidth + p2.x, nr.y * uWidth + p2.y});
        builder.emitPrimitive();
    }
    private static void lrgDoMiter( float[] raw, int index, float uWidth, PrimitiveBuilder builder) {
        Vec2 p0 = new Vec2( raw[(index+0)*LRG_STRIDE ], raw[(index+0)*LRG_STRIDE +1]);
        Vec2 p1 = new Vec2( raw[(index+1)*LRG_STRIDE ], raw[(index+1)*LRG_STRIDE +1]);
        Vec2 p2 = new Vec2( raw[(index+2)*LRG_STRIDE ], raw[(index+2)*LRG_STRIDE +1]);
        Vec2 p3 = new Vec2( raw[(index+3)*LRG_STRIDE ], raw[(index+3)*LRG_STRIDE +1]);
        Vec2 normal = p2.sub( p1).normalize();

        if( p0.equals( p1)){
            builder.emitVertex( new float[]{ -normal.y*uWidth + p1.x, normal.x*uWidth + p1.y});
            builder.emitVertex( new float[]{ normal.y*uWidth + p1.x, -normal.x*uWidth + p1.y});
        }
        else {
            Vec2 tangent = p2.sub(p1).normalize().add( p1.sub(p0).normalize()).normalize();
            Vec2 miter = new Vec2( -tangent.y, tangent.x);
            Vec2 n1 = (new Vec2( -(p1.y - p0.y), p1.x - p0.x)).normalize();
            float length = Math.max( 0.5f, Math.min( MITER_MAX, uWidth / miter.dot(n1)));

            builder.emitVertex( new float[]{ miter.x*length + p1.x, miter.y*length + p1.y});
            builder.emitVertex( new float[]{ -miter.x*length + p1.x, -miter.y*length + p1.y});
        }
        if( p2.equals( p3)) {
            builder.emitVertex( new float[]{ -normal.y*uWidth + p2.x, normal.x*uWidth + p2.y});
            builder.emitVertex( new float[]{ normal.y*uWidth + p2.x, -normal.x*uWidth + p2.y});
        }
        else {
            Vec2 tangent = p3.sub(p2).normalize().add( p2.sub(p1).normalize()).normalize();
            Vec2 miter = new Vec2( -tangent.y, tangent.x);
            Vec2 n2 = (new Vec2( -(p2.y - p1.y), p2.x - p1.x)).normalize();
            float length = Math.max( 0.5f, Math.min( MITER_MAX, uWidth / miter.dot(n2)));

            builder.emitVertex( new float[]{ miter.x*length + p2.x, miter.y*length + p2.y});
            builder.emitVertex( new float[]{ -miter.x*length + p2.x, -miter.y*length + p2.y});

        }
        builder.emitPrimitive();
    }
    private static void lrgDoBevel( float[] raw, int index, float uWidth, PrimitiveBuilder builder) {
        Vec2 p1 = new Vec2( raw[(index+1)*LRG_STRIDE ], raw[(index+1)*LRG_STRIDE +1]);
        Vec2 p2 = new Vec2( raw[(index+2)*LRG_STRIDE ], raw[(index+2)*LRG_STRIDE +1]);
        Vec2 p3 = new Vec2( raw[(index+3)*LRG_STRIDE ], raw[(index+3)*LRG_STRIDE +1]);
        Vec2 normal = p2.sub( p1).normalize();
        Vec2 nl = new Vec2( -normal.y, normal.x);
        Vec2 nr = new Vec2( normal.y, -normal.x);

        builder.emitVertex( new float[] { nl.x * uWidth + p1.x, nl.y * uWidth + p1.y});
        builder.emitVertex( new float[] { nr.x * uWidth + p1.x, nr.y * uWidth + p1.y});
        builder.emitVertex( new float[] { nl.x * uWidth + p2.x, nl.y * uWidth + p2.y});
        builder.emitVertex( new float[] { nr.x * uWidth + p2.x, nr.y * uWidth + p2.y});
        builder.emitPrimitive();

        if( !p3.equals(p2)) {
            Vec2 normal2 = p3.sub(p2).normalize();
            builder.emitVertex( new float[] { nl.x * uWidth + p2.x, nl.y * uWidth + p2.y});
            builder.emitVertex( new float[] { nr.x * uWidth + p2.x, nr.y * uWidth + p2.y});
            builder.emitVertex( new float[] { -normal2.y * uWidth + p2.x, normal2.x * uWidth + p2.y});
            builder.emitVertex( new float[] { normal2.y * uWidth + p2.x, -normal2.x * uWidth + p2.y});
            builder.emitPrimitive();
        }
    }
    

    private final static int SV2_STRIDE = 3;
    private final static float MITER_MAX2 = 2.0f;
    public static Primitive[] strokeV2LinePassGeom(float[] raw) {
        DoubleEndedSinglePrimitiveBuilder builder1 = new DoubleEndedSinglePrimitiveBuilder(
                // [x, y]
                new int[]{2},
                GLC.GL_LINE_STRIP
                );
        PrimitiveBuilder builder2 = new PrimitiveBuilder(
                // [x, y]
                new int[]{2},
                GLC.GL_TRIANGLE_STRIP
                );
        for( int i=0; i < (raw.length / SV2_STRIDE) - 3; i++) {
        	Vec2 p0 = new Vec2( raw[(i+0)*SV2_STRIDE], raw[(i+0)*SV2_STRIDE+1]);
        	Vec2 p1 = new Vec2( raw[(i+1)*SV2_STRIDE], raw[(i+1)*SV2_STRIDE+1]);
        	Vec2 p2 = new Vec2( raw[(i+2)*SV2_STRIDE], raw[(i+2)*SV2_STRIDE+1]);
        	Vec2 p3 = new Vec2( raw[(i+3)*SV2_STRIDE], raw[(i+3)*SV2_STRIDE+1]);
        	float size1 = raw[(i+1)*SV2_STRIDE+2]/2;
        	float size2 = raw[(i+2)*SV2_STRIDE+2]/2;
        	Vec2 n10 = p1.sub(p0).normalize();
        	Vec2 normal = p2.sub(p1).normalize();
        	Vec2 n32 = p3.sub(p2).normalize();
        	
        	if( p0.equals(p1)) {
        		builder1.emitVertexFront(new float[] { p1.x - normal.x * size1/2, p1.y - normal.y * size1/2});
        		builder1.emitVertexBack(new float[] { p1.x - normal.x * size1/2, p1.y - normal.y * size1/2});
        		
        		//if( size1 > 0.5) {
        			float s = size1;//-0.5f;
        			builder2.emitVertex(new float[] { p1.x - normal.x * s/2, p1.y - normal.y * s/2});
        			builder2.emitVertex(new float[] { p1.x - normal.x * s/2, p1.y - normal.y * s/2});
        		//}
        		//else builder2.emitPrimitive();
        	}
        	else {
//                Vec2 tangent = p2.sub(p1).normalize().add( p1.sub(p0).normalize()).normalize();
//                Vec2 miter = new Vec2( -tangent.y, tangent.x);
//                Vec2 n1 = (new Vec2( -(p1.y - p0.y), p1.x - p0.x)).normalize();
                
                
                float length = Math.max(0,size1 - 0.5f);
//                float length = Math.max( 0.5f, Math.min( MITER_MAX2*size1, size1 / miter.dot(n1)));

                float[] left = new float[] {p1.x + (p2.y - p0.y)* length/2, p1.y - (p2.x - p0.x) * length/2};
                float[] right = new float[] {p1.x - (p2.y - p0.y)* length/2, p1.y + (p2.x - p0.x) * length/2};

                builder1.emitVertexFront(left);
                builder1.emitVertexBack(right);
        		//if( length > 0.5) {
        			float s = length;//-0.5f;
        			builder2.emitVertex(left);
        			builder2.emitVertex(right);
        		//}
        		//else builder2.emitPrimitive();
//
//                builder1.emitVertexFront(new float[] { miter.x*length + p1.x, miter.y*length + p1.y});
//                builder1.emitVertexBack(new float[] { -miter.x*length + p1.x, -miter.y*length + p1.y});
//        		if( length > 0.5) {
//        			float s = length;//-0.5f;
//        			builder2.emitVertex(new float[] { miter.x*s + p1.x, miter.y*s + p1.y});
//        			builder2.emitVertex(new float[] { -miter.x*s + p1.x, -miter.y*s + p1.y});
//        		}
//        		else builder2.emitPrimitive();
        	}
        	if( p2.equals(p3)) {
                float length = Math.max(0,size2 - 0.5f);
        		builder1.emitVertexFront(new float[] { p2.x + normal.x * length/2, p2.y + normal.y * length/2});
        		builder1.emitVertexBack(new float[] { p2.x + normal.x * length/2, p2.y + normal.y * length/2});
        		//if( size2 > 0.5) {
        			float s = size2;//-0.5f;
        			builder2.emitVertex(new float[] { p2.x + normal.x * s/2, p2.y + normal.y * s/2});
        			builder2.emitVertex(new float[] { p2.x + normal.x * s/2, p2.y + normal.y * s/2});
        		//}
        		builder2.emitPrimitive();
        	}
        	/*else {
                Vec2 tangent = p3.sub(p2).normalize().add( p2.sub(p1).normalize()).normalize();
                Vec2 miter = new Vec2( -tangent.y, tangent.x);
                Vec2 n2 = (new Vec2( -(p2.y - p1.y), p2.x - p1.x)).normalize();
                float length = Math.max( 0.5f, Math.min( MITER_MAX2, size2 / miter.dot(n2)));

                builder1.emitVertexFront( new float[]{ miter.x*length + p2.x, miter.y*length + p2.y});
                builder1.emitVertexBack( new float[]{ -miter.x*length + p2.x, -miter.y*length + p2.y});
        	}*/
        }
        
        // !!!! USED FOR DEBUGING !!!!
//        if( builder1.forward.size() > 800) {
//        	Primitive[] p =new Primitive[] {builder1.build(), builder2.build()};
//
//        	List<Float> angle = new ArrayList<>();
//        	List<Float> distance= new ArrayList<>();
//        	for( int i=0; i < raw.length/3-2; i+=2) {
//        		float x1 = raw[i*3];
//        		float x2 = raw[(i+1)*3];
//        		float y1 = raw[i*3+1];
//        		float y2 = raw[(i+1)*3+1];
//        		angle.add( (float)Math.atan2(y2-y1, x2-x1));
//        		distance.add( (float)MUtil.distance(x1, y1, x2, y2));
//        	}
//        	float[] anglesArray = new float[angle.size()];
//        	float[] distancesArray = new float[angle.size()];
//        	for( int i=0; i < angle.size(); ++i){
//        		anglesArray[i] = angle.get(i);
//        		distancesArray[i] = distance.get(i);
//        	}
//        	System.out.println("S");
//        }
        
        
        return new Primitive[] {builder1.build(), builder2.build()};
        
    	/*vec2 p0 = vec2(gl_in[0].gl_Position.x,gl_in[0].gl_Position.y);
    	vec2 p1 = vec2(gl_in[1].gl_Position.x,gl_in[1].gl_Position.y);
    	vec2 p2 = vec2(gl_in[2].gl_Position.x,gl_in[2].gl_Position.y);
    	vec2 p3 = vec2(gl_in[3].gl_Position.x,gl_in[3].gl_Position.y);
    	vec2 n01 = normalize( p1 - p1);
    	vec2 normal = normalize( p2 - p1);
    	vec2 n23 = normalize( p3 - p2);

    	vec4 out1a;
    	vec4 out1b;
    	vec4 out2a;
    	vec4 out2b;

    	if( p0 == p1) {
    		out1a = out2a = perspectiveMatrix*vec4(p1 - (normal * vSize[1]), 0, 1);
    	}
    	else {
    		vec2 tangent = normalize( normalize(p2-p1) + normalize(p1-p0));
    		vec2 miter = vec2( -tangent.y, tangent.x);
    		vec2 n1 = normalize( vec2(-(p1.y-p0.y), p1.x-p0.x));
    		float length = max(0.5,min(vSize[1] / dot(miter, n1), MITER_MAX));

    		out1a = perspectiveMatrix*vec4( miter*length + p1, 0, 1);
    		out2a = perspectiveMatrix*vec4( -miter*length + p1, 0, 1);
    	}

    	if( p2 == p3) {
    		out2b = out1b = perspectiveMatrix*vec4(p2 + (normal * vSize[2]), 0, 1);
    	}
    	else {
    		vec2 tangent = normalize( normalize(p3-p2) + normalize(p2-p1));
    		vec2 miter = vec2( -tangent.y, tangent.x);
    		vec2 n2 = normalize( vec2(-(p2.y-p1.y), p2.x-p1.x));
    		float length = max(0.5,min(vSize[2] / dot(miter, n2), MITER_MAX));

    	    out1b = perspectiveMatrix*vec4( miter*length + p2, 0, 1);
    	    out2b = perspectiveMatrix*vec4( -miter*length + p2, 0, 1);
    	}*/
    }
}
