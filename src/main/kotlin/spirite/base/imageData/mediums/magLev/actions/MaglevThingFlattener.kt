package spirite.base.imageData.mediums.magLev.actions

import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevMedium

object MaglevThingFlattener {
    fun flattenMaglevMedium(maglev: MaglevMedium): List<IMaglevThing> {
        val oldIdToNewMap = mutableMapOf<Int, Int>()
        val thingsPass1 = maglev.thingsMap.entries
                .sortedBy { it.key }
                .mapIndexed { index, (id, thing) ->
                    oldIdToNewMap[id] = index
                    thing
                }

        // do as second pass to guarentee oldIdToNewMap is filled (support forward-facing references)
        val thingsPass2 = thingsPass1
                .map { thing ->
                    if (thing is MaglevFill) {
                        val remappedSegments = thing.segments.mapNotNull {
                            val remap = oldIdToNewMap[it.strokeId]
                            if (remap == null) {
                                println("1: This should really be an ILogger.  2: Your re-mapping failed")
                                null
                            } else MaglevFill.StrokeSegment(remap, it.start, it.end)
                        }
                        MaglevFill(remappedSegments, thing.mode, thing.color)
                    } else thing
                }

        return thingsPass2
    }
}
