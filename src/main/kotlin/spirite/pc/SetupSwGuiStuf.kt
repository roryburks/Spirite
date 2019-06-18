package spirite.pc

import rbJvm.glow.awt.AwtIImageConverter
import rbJvm.glow.jogl.JOGLProvider
import sguiSwing.SwProvider

fun setupSwGuiStuff() {
    SwProvider.converter = AwtIImageConverter
}