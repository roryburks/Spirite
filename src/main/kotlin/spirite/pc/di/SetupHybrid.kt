package spirite.pc.di

import spirite.core.hybrid.DiSet_Hybrid

fun SetupHybrid() {
    DiSet_Hybrid.beep = {java.awt.Toolkit.getDefaultToolkit().beep()}


}