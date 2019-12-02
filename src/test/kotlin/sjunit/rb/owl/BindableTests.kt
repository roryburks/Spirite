package sjunit.rb.owl

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BindableTests{
    data class DummyInfoClass(val a: Int = 999)

    @Test fun basicBinding(){
        val bindable1 = Bindable<DummyInfoClass>(DummyInfoClass(1))
        val bindable2 = Bindable<DummyInfoClass>(DummyInfoClass(2))

        // Act 1: Bind 2 to 1 and make sure both bindables recognize as data field 1
        val contract = bindable2.bindTo(bindable1)

        assertEquals(1, bindable1.field.a)
        assertEquals(1, bindable2.field.a)

        // Act 2: Void the Binding contract and make sure they update separately
        contract.void()
        bindable1.field = DummyInfoClass(3)

        assertEquals(3, bindable1.field.a)
        assertEquals(1, bindable2.field.a)
    }

    @Test fun bindableObserverTests(){
        val bindable1 = Bindable(1)
        val bindable2 = Bindable(2)
        val observeredChangesList = mutableListOf<Int>()

        // Act 1: Add an observer to bindable 2: observer triggers and adds 2 to the list
        val contract1 = bindable2.addObserver(true) { new, _ -> observeredChangesList.add(new)  }

        // Act 2: Bind 2 to 1, observer triggers and adds 1 to the list
        val contract2 = bindable2.bindTo(bindable1)

        // Act 3: Change bindable1's value to 3, observer triggers and adds 3 to the list
        bindable1.field = 3

        // Act 4: Unbind bindable 2 and re-change bindable 1 to 4, will not be triggered as they are no longer bound
        contract2.void()
        bindable1.field = 4

        // Act 5: Change bindable2 to 5, adds to list
        bindable2.field = 5

        // Act 6: Void the obserbver and set bindable2 to 6, not added to list
        contract1.void()
        bindable2.field = 6

        // Assert: list should be 2,1,3,5
        assertEquals(4, observeredChangesList.size)
        assertEquals(2, observeredChangesList[0])
        assertEquals(1, observeredChangesList[1])
        assertEquals(3, observeredChangesList[2])
        assertEquals(5, observeredChangesList[3])


    }
}