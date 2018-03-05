package sjunit

import spirite.base.graphics.IImage
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import java.io.File
import javax.imageio.ImageIO

object TestConfig {
    val save = true
    val saveLocation = "C:\\Bucket\\sunit"

    fun trySave( image: IImage, name: String) {
        if( TestConfig.save) {
            val imageBI = Hybrid.imageConverter.convert<ImageBI>(image)
            ImageIO.write(imageBI.bi, "png", File("${saveLocation}\\$name.png"))
        }
    }
}