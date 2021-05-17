package spirite.gui.resources

import sgui.swing.SwIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.ImageIcon

object SpiriteIcons {
    private val bi_format = BufferedImage.TYPE_INT_ARGB
    val defaultIcon by lazy { ImageIcon(BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB_PRE)) }

    class IconSheetScheme( resourceName: String, val width: Int, val height: Int ) {
        val sharedRoot by lazy { loadIconSheet(resourceName)}

        fun build(ix: Int, iy: Int) : ImageIcon {
            val img = BufferedImage( width-1, height-1, bi_format)
            img.graphics.drawImage(sharedRoot, - width * ix, - height*iy, null)
            return ImageIcon(img)
        }
    }

    fun loadIconSheet( resource: String) : BufferedImage {
        val stream = SpiriteIcons::class.java.getClassLoader().getResourceAsStream(resource)
        val native = ImageIO.read(stream)
        val converted = BufferedImage(native.width, native.height, bi_format)
        converted.graphics.drawImage(native, 0, 0, null)

        // Turns all pixels the same jcolor as the top-left pixel into transparent
        //  pixels
        val base = converted.getRGB(0, 0)

        for (x in 0 until converted.width) {
            for (y in 0 until converted.height)
                if (base == converted.getRGB(x, y))
                    converted.setRGB(x, y, 0)
        }

        return converted
    }

    enum class BigIcons(val ix: Int, val iy: Int) : SwIcon {
        VisibleOn(0,0),
        VisibleOff(1,0),
        NewLayer(2,0),
        NewGroup(3,0),

        Anim_StepF( 0, 1),
        Anim_Play( 1, 1),
        Anim_StepB(2, 1),
        Anim_Export(3,1),

        Frame_Layers(0,2),
        Frame_UndoHistory( 1, 2),
        Frame_AnimationScheme(2,2),
        Frame_ToolSettings(3,2),
        Frame_ReferenceScheme(4,2),
        ;

        override val icon: ImageIcon by lazy { scheme.build(ix, iy) }

        companion object {
            val scheme = IconSheetScheme("icons2.png", 25,25 )
        }
    }

    enum class SmallIcons(val ix: Int, val iy: Int) : SwIcon {
        Palette_NewColor(0,0),
        Palette_Save(1,0),
        Palette_Load(2,0),
        Link(3,0),
        Unlink(4,0),

        Rig_New(0,1),
        Rig_Remove(1,1),
        Rig_VisibleOn(2,1),
        Rig_VisibleOff(3,1),

        Expanded( 0, 2),
        ExpandedHighlighted( 1,2),
        Unexpanded(2,2),
        UnexpandedHighlighted(3,2),

        ArrowS(0,3),
        ArrowW(1,3),
        ArrowN(2,3),
        ArrowE(3,3)

        ;
        override val icon: ImageIcon by lazy { scheme.build(ix, iy) }

        companion object {
            val scheme = IconSheetScheme("icons_12x12.png", 13,13 )
        }
    }

}