package sjunit.rbJvm.owl

import rb.owl.Observable
import rbJvm.owl.WeakObserver
import rbJvm.owl.addWeakObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeakObserverTests {
    data class DataBlob(val int:Int = 0, val string: String = "DSFADSFASDFA")

    @Test fun weakObserverFallsOut() {
        val weakObs = WeakObserver(DataBlob())

        // Note: I'm not 100% of this behavior's consistency
        System.gc()
        assertNull(weakObs.triggers)
    }

    @Test fun weakObsStaysWithExternalRef() {
        val obs = Observable<Int>()
        val weakContract = obs.addWeakObserver(55)
        System.gc()
        var readback : Int? = 0
        obs.trigger { readback = it }
        assertEquals(55, readback)
    }

    @Test fun weakObsNonreferenceDies() {
        val obs = Observable<DataBlob>()
        obs.addWeakObserver(DataBlob())
        System.gc()
        var readback : DataBlob? = null
        obs.trigger { readback = it }
        println(readback)
        assertNull(readback)
    }
}