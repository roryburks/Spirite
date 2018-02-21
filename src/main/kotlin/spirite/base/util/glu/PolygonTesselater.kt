package spirite.base.util.glu

import com.jogamp.opengl.glu.GLU
import com.jogamp.opengl.glu.GLUtessellatorCallback
import spirite.base.graphics.gl.GLPrimitive
import spirite.base.util.compaction.FloatCompactor

object PolygonTesselater {
    fun tesselatePolygon( x: List<Float>, y: List<Float>, count: Int) : GLPrimitive {
        val tess = GLU.gluNewTess()
        val callback = GLUTCB()

        GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, callback)
        GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback)
        GLU.gluTessCallback(tess, GLU.GLU_TESS_END, callback)
        GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, callback)
        GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback)

        GLU.gluTessProperty(tess,
                GLU.GLU_TESS_WINDING_RULE,
                GLU.GLU_TESS_WINDING_ODD.toDouble())
        GLU.gluTessBeginPolygon(tess, null)
        GLU.gluTessBeginContour(tess)
        for (i in 0 until count) {
            val buffer = doubleArrayOf(x[i].toDouble(), y[i].toDouble(), 0.0)
            GLU.gluTessVertex(tess, buffer, 0, buffer)
        }
        GLU.gluTessEndContour(tess)
        GLU.gluTessEndPolygon(tess)
        GLU.gluDeleteTess(tess)

        return callback.buildPrimitive()
    }

    private class GLUTCB : GLUtessellatorCallback {
        private val data = FloatCompactor()
        private val types = mutableListOf<Int>()
        private val lengths = mutableListOf<Int>()
        private var currentLength = 0

        override fun begin(type: Int) {
            types.add(type)
        }

        override fun combine(coords: DoubleArray, data: Array<Any>, weight: FloatArray, out: Array<Any>) {
            out[0] = coords
        }

        override fun edgeFlag(arg0: Boolean) {}

        override fun end() {
            lengths.add(currentLength)
            currentLength = 0
        }

        override fun error(errnum: Int) {
            val estring: String

            estring = GLU().gluErrorString(errnum)
            System.err.println("Tessellation Error: " + estring)
            System.exit(0)
        }

        override fun vertex(arg0: Any) {
            val d = arg0 as DoubleArray
            data.add(d[0].toFloat())
            data.add(d[1].toFloat())
            ++currentLength
        }

        fun buildPrimitive(): GLPrimitive {
            val len = Math.min(types.size, lengths.size)
            val ptypes = IntArray(len)
            val plengths = IntArray(len)
            for (i in 0 until len) {
                ptypes[i] = types[i]
                plengths[i] = lengths[i]
            }

            return GLPrimitive(data.toArray(), intArrayOf(2), ptypes, plengths)
        }

        override fun edgeFlagData(arg0: Boolean, arg1: Any) {}
        override fun beginData(type: Int, polygonData: Any) {}
        override fun combineData(coords: DoubleArray, data: Array<Any>, weight: FloatArray, out: Array<Any>, arg4: Any) {}
        override fun endData(arg0: Any) {}
        override fun errorData(arg0: Int, arg1: Any) {}
        override fun vertexData(arg0: Any, arg1: Any) {}

    }
}