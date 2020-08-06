package rb.clicker.telemetry

import rb.extendo.dataStructures.Deque
import sguiSwing.hybrid.Hybrid


class TelemetryEvent {
    private class TelemetryActionInner(
            val description: String,
            val startTime: Long,
            val statistics: MutableMap<String, Double> = mutableMapOf())

    class TelemetryAction(
            val description: String,
            val durationMs: Long,
            val statistics: Map<String, Double> = mutableMapOf())

    val actions: List<TelemetryAction> get() = _actions

    private val actionStack = Deque<TelemetryActionInner>()
    private val _actions = mutableListOf<TelemetryAction>()

    fun runAction(description: String, lambda: () -> Unit) {
        try {
            actionStack.addFront(TelemetryActionInner(description, Hybrid.timing.currentMilli))
            lambda()
        }
        finally {
            val inner = actionStack.popBack() ?: return
            _actions.add(TelemetryAction(
                    inner.description,
                    Hybrid.timing.currentMilli - inner.startTime,
                    inner.statistics))
        }
    }

    fun mark(statistic: String, value: Double) {
        actionStack.peekFront()?.statistics?.set(statistic, value)
    }
}