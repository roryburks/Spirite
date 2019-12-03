package sjunit.pc

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import spirite.base.brains.MasterControl
import spirite.pc.Spirite
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class InitializationTests {
    fun doInitTest(lambda : (MasterControl)->Unit) {
        var ran = false
        //runBlocking {
            val root = Spirite()
            root.run()

            while(!root.ready){
                Thread.sleep(100)
                //delay(100)
            }

            ran = true
            lambda(root.master)
        //}
        assertTrue(ran)
    }

    @Test fun basicInitializeTest() {
        doInitTest {
            assertNotNull(it)
        }
    }

    @Test fun initializeToolsets(){
        doInitTest {
            val tools = it.frameManager.root.toolSection.imp.data.entries

            assertTrue { tools.count() > 10 }
        }
    }
}
