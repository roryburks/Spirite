package sjunit.spirite.base.graphics.rendering

import org.junit.Test
import sjunit.TestConfig
import sjunit.TestHelper
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.rendering.NodeRenderer
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.hybrid.Hybrid
import spirite.hybrid.ImageConverter
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.graphics.ImageBI
import spirite.pc.resources.JClassScriptService
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class CompositingTests {

    val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())
    val imageConverter = ImageConverter(gle)
    val workspace = TestHelper.makeShellWorkspace(100,100)

    @Test
    fun compositesFlatImagesCorrectly() {
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "Layer1", FLAT,20,20)
        layer1.x = 10
        layer1.y = 10

        val mediumHandle = (layer1.layer as SimpleLayer).medium
        val flatImage = (mediumHandle.medium as FlatMedium).image

        val gc = flatImage.graphics
        gc.color = Colors.GREEN
        gc.fillRect(0,0,20,20)

        val tMediumToWS = MutableTransform.TranslationMatrix(10f,10f)
        workspace.compositor.compositeSource = CompositeSource(
                ArrangedMediumData(mediumHandle, tMediumToWS),
                {it.color = Colors.RED
                    it.fillRect( 10,10, 20, 20)})


        val wsImage = Hybrid.imageCreator.createImage(100,100)

        NodeRenderer(workspace.groupTree.root, workspace).render(wsImage.graphics)


        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(wsImage)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\compositing_flat.png"))
        }

        for( x in 0 until 100) {
            for( y in 0 until 100) {
                val expected = when {
                    x in (0 until 10) || y in (0 until 10) || x in (30 until 100) || y in (30 until 100) -> 0
                    x in (20 until 30) && y in (20 until 30) -> Colors.RED.argb
                    else -> Colors.GREEN.argb
                }
                assertEquals(expected, wsImage.getARGB(x,y))
            }
        }
    }

    @Test
    fun compositesDynamicImagesCorrectly() {
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "Layer1", DYNAMIC)
        layer1.x = 10
        layer1.y = 10

        val mediumHandle = (layer1.layer as SimpleLayer).medium
        val dynamicImage = (mediumHandle.medium as DynamicMedium).image

        dynamicImage.drawToImage({
            it.graphics.fillRect(0,0,20,20)
        }, 20, 20)


        val tMediumToWS = MutableTransform.TranslationMatrix(10f,10f)
        workspace.compositor.compositeSource = CompositeSource(
                ArrangedMediumData(mediumHandle, tMediumToWS),
                {it.color = Colors.RED
                    it.fillRect( 40,40, 10, 10)})


        val wsImage = Hybrid.imageCreator.createImage(100,100)

        NodeRenderer(workspace.groupTree.root, workspace).render(wsImage.graphics)


        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(wsImage)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\compositing_dynamic.png"))
        }

        for( x in 10 until 30)
            for( y in 10 until 30)
                assertEquals(Colors.BLACK.argb, wsImage.getARGB(x, y))
        for( x in 40 until 50)
            for( y in 40 until 50)
                assertEquals(Colors.RED.argb,wsImage.getARGB(x,y))
    }
}