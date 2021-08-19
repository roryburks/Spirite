package spirite.pc

import rb.glow.img.IImage
import rbJvm.glow.awt.ImageBI
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.Hybrid
import java.io.File
import javax.imageio.ImageIO

object TestConfig {
    val save = false
    val saveLocation = "C:\\Bucket\\sunit"

    fun trySave(image: IImage, name: String) {
        if(save) {
            val imageBI = DiSet_Hybrid.imageConverter.convert(image, ImageBI::class) as ImageBI
            ImageIO.write(imageBI.bi, "png", File("$saveLocation\\$name.png"))
        }
    }
}
