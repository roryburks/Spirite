package rb.clicker.telemetry

import sguiSwing.hybrid.Hybrid
import spirite.base.imageData.selection.drawLiftedTransform

class TelemetryStopwatch {
    private val _telemetryMap = mutableMapOf<String, Long>()
    private val _stopwatchSet = mutableMapOf<String,Long>()


    fun start(description: String) {
        _stopwatchSet[description] = Hybrid.timing.currentMilli
    }

    fun end( description: String) {
        val start = _stopwatchSet[description] ?: Hybrid.timing.currentMilli
        val end=  Hybrid.timing.currentMilli
        _telemetryMap[description] = _telemetryMap.getOrDefault(description, 0) + (end - start)

    }
}