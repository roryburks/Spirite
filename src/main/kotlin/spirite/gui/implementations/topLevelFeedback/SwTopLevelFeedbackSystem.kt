package spirite.gui.implementations.topLevelFeedback

import rb.extendo.extensions.removeToList
import spirite.sguiHybrid.Hybrid
import spirite.base.brains.ITopLevelFeedbackSystem
import spirite.base.brains.SwFrameManager

class SwTopLevelFeedbackSystem(
) : ITopLevelFeedbackSystem
{
    var frameManager: SwFrameManager? = null
    private val _expiry = mutableListOf<Pair<Long,String>>()

    override fun broadcastGeneralMessage(message: String) {
        frameManager?.root?.topLevelView?.run {
            pushMessage(message)
            _expiry.add(Pair(Hybrid.timing.currentMilli + 2000, message))

        }
    }

    private fun tick() {
        val now = Hybrid.timing.currentMilli
        _expiry.removeToList { now > it.first }
                .forEach { frameManager?.root?.topLevelView?.popMessage(it.second) }
    }

    init {
        Hybrid.timing.createTimer(100, true) {tick()}
    }


}