//package rb.jvm
//
//import rb.owl.Contract
//import rb.owl.IContractor
//import rb.owl.bindableMList.IMutableListObserver
//import rb.owl.bindableMList.IListTriggers
//import rb.owl.bindableMList.ListChange
//import rb.owl.bindableMList.ListPermuation
//import java.lang.ref.WeakReference
//
//
//class WeakMutableListObserver<T>(trigger: IListTriggers<T>) :
//    IMutableListObserver<T>
//{
//
//    private val weakTrigger = WeakReference(trigger)
//    override val trigger = weakTrigger.get() ?: (NilTrigger<T>().also{ clearContracts() })
//
//
//    private fun clearContracts() {
//        contracts.forEach { it.void() }
//        contracts.clear()
//    }
//
//    private var contracts = mutableListOf<Contractor>()
//    private inner class Contractor(val contract: Contract) : IContractor {
//        init { contracts.add(this)}
//
//        override fun void() {
//            contract.void()
//            contracts.remove(this)
//        }
//    }
//}
//
//private class NilTrigger<T>: IListTriggers<T> {
//    override fun elementsChanged(changes: Set<ListChange<T>>) {}
//    override fun elementsPermuted(permutation: ListPermuation) {}
//    override fun elementsAdded(index: Int, elements: Collection<T>) {}
//    override fun elementsRemoved(elements: Collection<T>) {}
//
//}