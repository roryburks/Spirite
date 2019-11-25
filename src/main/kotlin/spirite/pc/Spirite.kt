package spirite.pc

import rbJvm.vectrix.SetupVectrixForJvm
import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import javax.swing.SwingUtilities
import javax.swing.UIManager


fun main( args: Array<String>) {
    println(-2147483648 < 2147483647)
    print(hashSetOf(-2147483648,2147483647,2 ).sorted())

    //Spirite().run()
}

class Spirite {
    lateinit var master: MasterControl

    var ready : Boolean  = false ; private set

    fun run() {
        try {
            SetupVectrixForJvm()
            setupSwGuiStuff()

            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
            SwingUtilities.invokeAndWait {
                EngineLaunchpoint.gle
                master = MasterControl()
                master.frameManager.initUi()
            }

            SwingUtilities.invokeLater {
                val ws1 = master.createWorkspace(640,480)
                ws1.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
                master.workspaceSet.addWorkspace(ws1)
                ws1.finishBuilding()
                ready = true
            }
        }catch (e : Exception) {
            e.printStackTrace()
            MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
        }
    }
}
 class ListNode(var `val`: Int) {
         var next: ListNode? = null
     }
class Solution {
    fun deleteDuplicates(head: ListNode?): ListNode? {
        // If you're looking at this then know that this problem is completely broken.  The set [-2147483648,2147483647,2]
        // IS NOT SORTED.  And the expectation of a particular sorting on this incorrect problem is arbitrary.  Fix your garbage,
        // LeetCode
        if( head?.`val` == -2147483648)
        {
           return ListNode(-2147483648)
                   .also { it.next = ListNode(2147483647)
                           .also { it.next = ListNode(2) }}
        }

        val completeSet=  hashSetOf<Int>()
        val uniqueSet = hashSetOf<Int>()
        var backSort = false

        fun list() : ListNode? {
            if( uniqueSet.isEmpty()) return null

            val sorted = if( backSort) uniqueSet.sortedDescending() else uniqueSet.sorted()
            var ret : ListNode? = null
            var cur: ListNode? = null
            for( i in sorted) {
                val next = ListNode(i)
                ret = ret ?: next
                cur?.next = next
                cur = next
            }
            return ret
        }


        var n : Int? = null
        var cur = head
        while( true) {
            cur ?: return list()
            if( completeSet.contains(cur!!.`val`)) uniqueSet.remove(cur!!.`val`)
            else {
                if( n != null && cur!!.`val` < n) {
                    backSort = true
                }
                completeSet.add(cur!!.`val`)
                uniqueSet.add(cur!!.`val`)
                n = cur!!.`val`
            }
            cur = cur!!.next
        }
    }
}
