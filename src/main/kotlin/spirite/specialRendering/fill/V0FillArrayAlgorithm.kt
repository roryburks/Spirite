package spirite.specialRendering.fill

import rb.vectrix.mathUtil.MathUtil
import java.util.*

object V0FillArrayAlgorithm : IFillArrayAlgorithm {
    override fun fill(data: IntArray, w: Int, h: Int, x: Int, y: Int, color: Int): IntArray? {
        if( x < 0 || x >= w || y < 0 || y >= h) return null

        val from = data[x + y*w]
        if( color == from) return null

        val queue = LinkedList<Int>()
        queue.offer(MathUtil.packInt(x,y))

        val faW = (w-1) / 8 + 1
        val faH = (h-1) / 4 + 1

        val array = IntArray(faW*faH)

        fun checked( cx: Int, cy: Int) : Boolean{
            val off = cx/8 + cy/4*faW
            val mask = (1 shl ((cx%8) + (cy%4)*8))
            val c =  array[off] and mask != 0
            if( !c)array[off] = array[off] or mask
            return c
        }

        checked(x,y)

        while( queue.any()) {
            val p = queue.poll()
            val px = MathUtil.high16(p)
            val py = MathUtil.low16(p)

            if( px != 0 && data[px-1 + py*w] == from && !checked(px-1,py))
                queue.offer(MathUtil.packInt(px-1,py))
            if( px != w-1 && data[px+1 + py*w] == from&& !checked(px+1,py))
                queue.offer(MathUtil.packInt(px+1,py))
            if( py != 0 && data[px + (py-1)*w] == from&& !checked(px,py-1))
                queue.offer(MathUtil.packInt(px,py-1))
            if( py != h-1 && data[px + (py+1)*w] == from&& !checked(px,py+1))
                queue.offer(MathUtil.packInt(px,py+1))
        }
        return array
    }

}