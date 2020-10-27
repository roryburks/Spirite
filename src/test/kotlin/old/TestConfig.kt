package old

import rb.glow.IImage
import rbJvm.glow.awt.ImageBI
import sguiSwing.hybrid.Hybrid
import java.io.File
import javax.imageio.ImageIO

object TestConfig {
    val save = false
    val saveLocation = "C:\\Bucket\\sunit"

    fun trySave(image: IImage, name: String) {
        if(save) {
            val imageBI = Hybrid.imageConverter.convert(image,ImageBI::class) as ImageBI
            ImageIO.write(imageBI.bi, "png", File("$saveLocation\\$name.png"))
        }
    }
}