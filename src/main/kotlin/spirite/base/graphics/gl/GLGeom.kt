package spirite.base.graphics.gl

import spirite.base.util.compaction.FloatCompactor
import spirite.base.util.compaction.ReverseFloatCompactor
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Vec2

object GLGeom {

    private class PrimitiveBuilder(
            val attrLengths: IntArray,
            val primitiveType: Int
    ) {
        val output = FloatCompactor()
        val totalAttrLength = attrLengths.sum()
        var primitiveLengths = mutableListOf<Int>()
        var plen = 0

        fun emitVertex( vertexData: FloatArray) {
            (0 until totalAttrLength)
                    .forEach { output.add( if( it < vertexData.size) vertexData[it] else 0.0f ) }
            plen++
        }

        fun emitPrimitive() {
            if( plen > 0)
                primitiveLengths.add(plen)
            plen = 0
        }

        fun build() = GLPrimitive( output.toArray(), attrLengths, intArrayOf(primitiveType), primitiveLengths.toIntArray())
    }
    private class DoubleEndedSinglePrimitiveBuilder(
            val attrLengths: IntArray,
            val primitiveType: Int
    ) {

        val forward = FloatCompactor()
        val backward = ReverseFloatCompactor()
        val totalAttrLength = attrLengths.sum()

        fun emitVertexFront( vertexData: FloatArray) {
            (0 until totalAttrLength)
                    .forEach { forward.add( if( it < vertexData.size) vertexData[it] else 0.0f ) }
        }

        fun emitVertexBack( vertexData: FloatArray) {
            (0 until totalAttrLength)
                    .forEach { backward.add( if( it < vertexData.size) vertexData[totalAttrLength - it - 1] else 0.0f ) }
        }

        fun build() : GLPrimitive {
            val raw = FloatArray( forward.size + backward.size)
            forward.insertIntoArray(raw, 0)
            backward.insertIntoArray(raw, forward.size)

            val primitiveTypes = intArrayOf(primitiveType)
            val primitiveLengths = intArrayOf(raw.size / attrLengths.sum())

            return GLPrimitive(raw, attrLengths, primitiveTypes, primitiveLengths)
        }
    }

    private val SV2_STRIDE = 3
    fun strokeV2LinePassGeom( raw: FloatArray) : Pair<GLPrimitive,GLPrimitive> {
        val lineBuilder = DoubleEndedSinglePrimitiveBuilder(intArrayOf(2), GLC.LINE_STRIP)
        val polyBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_STRIP)

        for( i in 0 until (raw.size / SV2_STRIDE) - 3) {
            val p0 = Vec2(raw[(i + 0) * SV2_STRIDE], raw[(i + 0) * SV2_STRIDE + 1])
            val p1 = Vec2(raw[(i + 1) * SV2_STRIDE], raw[(i + 1) * SV2_STRIDE + 1])
            val p2 = Vec2(raw[(i + 2) * SV2_STRIDE], raw[(i + 2) * SV2_STRIDE + 1])
            val p3 = Vec2(raw[(i + 3) * SV2_STRIDE], raw[(i + 3) * SV2_STRIDE + 1])
            val size1 = raw[(i + 1) * SV2_STRIDE + 2] / 2
            val size2 = raw[(i + 2) * SV2_STRIDE + 2] / 2
            //Vec2 n10 = p1.minus(p0).normalize();
            val (x, y) = p2.minus(p1).normalize()
            //Vec2 n32 = p3.minus(p2).normalize();

            if (p0 == p1) {
                lineBuilder.emitVertexFront(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                lineBuilder.emitVertexBack(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))

                //if( size1 > 0.5) {
                polyBuilder.emitVertex(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                polyBuilder.emitVertex(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                //}
                //else polyBuilder.emitPrimitive();
            } else {
                //                Vec2 tangent = p2.minus(p1).normalize().plus( p1.minus(p0).normalize()).normalize();
                //                Vec2 miter = new Vec2( -tangent.y, tangent.x);
                //                Vec2 n1 = (new Vec2( -(p1.y - p0.y), p1.x - p0.x)).normalize();


                val length = Math.max(0f, size1 - 0.5f)
                //                float length = Math.max( 0.5f, Math.min( MITER_MAX2*size1, size1 / miter.dot(n1)));

                val left = floatArrayOf(p1.x + (p2.y - p0.y) * length / 2, p1.y - (p2.x - p0.x) * length / 2)
                val right = floatArrayOf(p1.x - (p2.y - p0.y) * length / 2, p1.y + (p2.x - p0.x) * length / 2)

                lineBuilder.emitVertexFront(left)
                lineBuilder.emitVertexBack(right)
                //if( length > 0.5) {
                //float s = length;//-0.5f;
                polyBuilder.emitVertex(left)
                polyBuilder.emitVertex(right)
                //}
                //else polyBuilder.emitPrimitive();
                //
                //                lineBuilder.emitVertexFront(new float[] { miter.x*length + p1.x, miter.y*length + p1.y});
                //                lineBuilder.emitVertexBack(new float[] { -miter.x*length + p1.x, -miter.y*length + p1.y});
                //        		if( length > 0.5) {
                //        			float s = length;//-0.5f;
                //        			polyBuilder.emitVertex(new float[] { miter.x*s + p1.x, miter.y*s + p1.y});
                //        			polyBuilder.emitVertex(new float[] { -miter.x*s + p1.x, -miter.y*s + p1.y});
                //        		}
                //        		else polyBuilder.emitPrimitive();
            }
            if (p2 == p3) {
                val length = Math.max(0f, size2 - 0.5f)
                lineBuilder.emitVertexFront(floatArrayOf(p2.x + x * length / 2, p2.y + y * length / 2))
                lineBuilder.emitVertexBack(floatArrayOf(p2.x + x * length / 2, p2.y + y * length / 2))
                //if( size2 > 0.5) {
                polyBuilder.emitVertex(floatArrayOf(p2.x + x * size2 / 2, p2.y + y * size2 / 2))
                polyBuilder.emitVertex(floatArrayOf(p2.x + x * size2 / 2, p2.y + y * size2 / 2))
                //}
                polyBuilder.emitPrimitive()
            }
            /*else {
                Vec2 tangent = p3.minus(p2).normalize().plus( p2.minus(p1).normalize()).normalize();
                Vec2 miter = new Vec2( -tangent.y, tangent.x);
                Vec2 n2 = (new Vec2( -(p2.y - p1.y), p2.x - p1.x)).normalize();
                float length = Math.max( 0.5f, Math.min( MITER_MAX2, size2 / miter.dot(n2)));

                lineBuilder.emitVertexFront( new float[]{ miter.x*length + p2.x, miter.y*length + p2.y});
                lineBuilder.emitVertexBack( new float[]{ -miter.x*length + p2.x, -miter.y*length + p2.y});
        	}*/
        }

        return Pair( lineBuilder.build(), polyBuilder.build())
    }
}