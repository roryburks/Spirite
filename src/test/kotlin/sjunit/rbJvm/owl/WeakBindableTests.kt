package sjunit.rbJvm.owl

import org.junit.jupiter.api.Test
import rb.owl.bindable.Bindable
import rbJvm.owl.bindWeaklyTo
import sjunit.rb.owl.BindableTests
import kotlin.test.assertEquals

class WeakBindableTests {
    data class DummyInfoClass(val a: Int = 999)

    @Test fun basicBinding(){
        val bindable1 = Bindable(BindableTests.DummyInfoClass(1))
        val bindable2 = Bindable(BindableTests.DummyInfoClass(2))

        // Act 1: Bind 2 to 1 and make sure both bindables recognize as data field 1
        val contract = bindable2.bindWeaklyTo(bindable1)

        assertEquals(1, bindable1.field.a)
        assertEquals(1, bindable2.field.a)

        // Act 2: Void the Binding contract and make sure they update separately
        contract.void()
        bindable1.field = BindableTests.DummyInfoClass(3)

        assertEquals(3, bindable1.field.a)
        assertEquals(1, bindable2.field.a)
    }
}