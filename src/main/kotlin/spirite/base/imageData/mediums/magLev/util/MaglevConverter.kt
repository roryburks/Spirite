package spirite.base.imageData.mediums.magLev.util

import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.magLev.MaglevMedium

object MaglevConverter {
    fun convertToDynamic(maglev: MaglevMedium, ws: MImageWorkspace) = DynamicMedium(ws, maglev.builtImage.deepCopy())
}