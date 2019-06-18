package spirite.specialRendering.fill

import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.MathUtil
import java.util.concurrent.ConcurrentLinkedQueue

private val DELAY = 10L

/***
 * The V1 Fill Algoritm separates the image into 9x9 chunks, consisting of a 1-pixel outer layers (32 pixels, tracked
 * using an Int bitmask) and a 7x7 inner layers (49 pixels, tracked using a Long bitmask).  Can add bitmasks to be run
 * only through the outer layers (though the inner chunk can ignore them if it's been done before).
 */
object V1FillArrayAlgorithm : IFillArrayAlgorithm {
    override fun fill(data: IntArray, w: Int, h: Int, x: Int, y: Int, color: Int): IntArray? {
        if( x < 0 || x >= w || y < 0 || y >= h) return null

        val from = data[x + y*h]
        if( color == from) return null

        val context = V1FillContext(data, w, h, from)

        val cx = x / 9
        val cy = y / 9

        val workers = Array( (w-1)/9 + 1) {ox ->
            Array( (h-1)/9 + 1) {oy ->
                when {
                    ox == cx && oy == cy -> V1FillWorker(context, ox, oy, start = Vec2i(x % 9, y % 9))
                    else -> V1FillWorker(context, ox, oy)
                }
            }
        }
        context.workers = workers
        workers.forEach { it.forEach { it.loop() } }

//        runBlocking {
            while (!context.done) {
//                delay(20)
            }
//        }

        return context.fillArray
    }

    class V1FillContext(
            val data: IntArray,
            val w: Int,
            val h: Int,
            val color: Int) {
        val faW = (w - 1) / 8 + 1
        val faH = (h - 1) / 4 + 1


        fun mark(x: Int, y: Int) {
            val off = x / 8 + y / 4 * faW
            val mask = (1 shl ((x % 8) + (y % 4)*8))
            synchronized(fillArray) {
                fillArray[off] = fillArray[off] or mask
            }
        }

        val fillArray = IntArray(faW * faH, { 0 })

        lateinit var workers: Array<Array<V1FillWorker>>
        val done: Boolean get() = !workers.any {it.any { it.working }}
    }


    class V1FillWorker(
            val ctx: V1FillContext,
            val offsetX: Int,
            val offsetY: Int,
            start : Vec2i? = null
    )
    {
        val working get() = _working || checkQueue.any() || toCheck != 0

        private var _working = true
        private var toCheck : Int
        private var notChecked = 0xffffffff.toInt()
        private var actualChecked = 0
        private var checkQueue = ConcurrentLinkedQueue<Int>()
        private var internal = 0L

        init {
            if( start != null) {
                toCheck =  posToMask(start.xi, start.yi)

                if( toCheck == 0) {
                    checkQueue.offer(MathUtil.packInt(start.xi, start.yi))
                }
            }
            else{
                toCheck = 0
                notChecked
            }
        }

        fun markCheck( checkMask: Int) {
            synchronized(toCheck) {
                toCheck = toCheck or checkMask
            }
        }

        fun loop() {
//            launch {
                while(!ctx.done) {
                    work()
//                    delay(10, MILLISECONDS)
                }
//            }
        }


        var runUnchecked = 0
        fun work() {
            _working = true
            var checkMask = 0
            runUnchecked++
            synchronized(checkMask) {
                checkMask = toCheck and notChecked
                notChecked = notChecked xor checkMask
                toCheck = 0
            }

            var offset = 0
            while( checkMask != 0 && offset < 32){
                val mask = (1 shl offset)
                if( checkMask and mask != 0){
                    checkQueue.offer( when {
                        offset < 8 -> MathUtil.packInt(offset, 0)
                        offset < 16 -> MathUtil.packInt(8, offset-8)  // xi = 9 : 8,1
                        offset < 24 -> MathUtil.packInt(24-offset, 8)  // xi = 17: 7,8
                        else -> MathUtil.packInt(0, 32-offset)   // xi = 25: 0, 7
                    })
                }

                offset++
            }

            loop@ while ( checkQueue.any()) {
                runUnchecked = 0
                val p = checkQueue.poll()

                val lx = MathUtil.high16(p)
                val ly = MathUtil.low16(p)

                val gx = lx + offsetX * 9
                val gy = ly + offsetY * 9

                if( checked(lx, ly, true)) continue@loop

                ctx.mark(gx, gy)

                if (gx != 0 && ctx.data[gx - 1 + gy * ctx.w] == ctx.color) {
                    if (lx == 0)
                        ctx.workers[offsetX - 1][offsetY].markCheck(posToMask(8, ly))
                    else if (!checked(lx - 1, ly))
                        checkQueue.add(MathUtil.packInt(lx - 1, ly))
                }
                if (gx != ctx.w - 1 && ctx.data[gx + 1 + gy * ctx.w] == ctx.color) {
                    if (lx == 8)
                        ctx.workers[offsetX + 1][offsetY].markCheck(posToMask(0, ly))
                    else if (!checked(lx + 1, ly))
                        checkQueue.add(MathUtil.packInt(lx + 1, ly))
                }
                if (gy != 0 && ctx.data[gx + (gy - 1) * ctx.w] == ctx.color) {
                    if (ly == 0)
                        ctx.workers[offsetX][offsetY - 1].markCheck(posToMask(lx, 8))
                    else if (!checked(lx, ly - 1))
                        checkQueue.add(MathUtil.packInt(lx, ly - 1))
                }
                if (gy != ctx.h - 1 && ctx.data[gx + (gy + 1) * ctx.w] == ctx.color) {
                    if (ly == 8)
                        ctx.workers[offsetX][offsetY + 1].markCheck(posToMask(lx, 0))
                    else if (!checked(lx, ly + 1))
                        checkQueue.add(MathUtil.packInt(lx, ly + 1))
                }
            }
            _working = false
        }

        fun posToMask( x: Int, y: Int)=  when {
            y == 0 -> 1 shl x
            x == 8 -> 1 shl (8 + y)
            y == 8 -> 1 shl (24 - x)
            x == 0 -> 1 shl (32 - y)    // Note: xi == 0, yi == 0 will be handled by case 1
            else -> 0
        }

        fun checked( x: Int, y: Int, change : Boolean = false) : Boolean{
            when {
                y == 0 -> {
                    val mask = (1 shl x)
                    if( actualChecked and mask != 0 ) return true
                    if(change) actualChecked = actualChecked or mask
                }
                x == 8 -> {
                    val mask = 1 shl (8+y)
                    if( actualChecked and mask != 0 ) return true
                    if(change) actualChecked = actualChecked or mask
                }
                y == 8 -> {
                    val mask = 1 shl (24-x)
                    if( actualChecked and mask != 0 ) return true
                    if(change) actualChecked = actualChecked or mask
                }
                x == 0 ->  {    // Note: xi == 0, yi == 0 will be handled by case 1
                    val mask = 1 shl (32-y)
                    if( actualChecked and mask != 0 ) return true
                    if(change) actualChecked = actualChecked or mask
                }
                else -> {
                    val mask = 1L shl (x + y*7 - 8)
                    if( internal and mask != 0L) return true
                    if(change) internal = internal or mask
                }
            }
            return false
        }
    }
}


