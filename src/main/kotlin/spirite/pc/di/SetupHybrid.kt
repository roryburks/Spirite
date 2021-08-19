package spirite.pc.di

import sgui.swing.systems.SwImageCreator
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.EngineLaunchpoint

fun SetupHybrid() {
    DiSet_Hybrid.beep = {java.awt.Toolkit.getDefaultToolkit().beep()}

    DiSet_Hybrid.imageCreatorLazy = lazy { SwImageCreator(EngineLaunchpoint.gle)}

}