package rb.animo

import rb.animo.io.IAafScope
import rb.animo.io.ILoader
import rb.global.ILogger
import rb.glow.gle.IGLEngine
import rb.glow.img.IImage


object AnimoDependencySet {
    lateinit var Gle: Lazy<IGLEngine>
    lateinit var Logger : Lazy<ILogger>

    // Loaders
    lateinit var AafLoader: Lazy<ILoader<IAafScope>>
    lateinit var ImageLoader: Lazy<ILoader<IImage>>
}