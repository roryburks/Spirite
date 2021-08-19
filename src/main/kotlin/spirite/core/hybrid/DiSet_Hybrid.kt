package spirite.core.hybrid

import rb.glow.IImageConverter
import sgui.core.systems.IImageCreator

object DiSet_Hybrid {
    var beep : ()->Unit = {}


    // These are defined in sgui.core, so might not belong here or maybe this could reference a DiSet in sguiCore
    val imageCreator : IImageCreator get() = imageCreatorLazy.value
    lateinit var imageCreatorLazy: Lazy<IImageCreator>

    // These are defined in rbGlow, so might not belong here or maybe this could reference a DiSet in sguiCore
    val imageConverter : IImageConverter get() = imageConverterLazy.value
    lateinit var imageConverterLazy : Lazy<IImageConverter>
}