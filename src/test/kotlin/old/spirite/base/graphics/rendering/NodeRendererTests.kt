package old.spirite.base.graphics.rendering

import old.TestConfig
import old.TestHelper
import org.junit.jupiter.api.Test
import rb.glow.Colors
import rb.glow.gle.RenderMethod
import rb.glow.gle.RenderMethodType.MULTIPLY
import rb.glow.img.RawImage
import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.awt.ImageBI
import sguiSwing.hybrid.Hybrid
import spirite.base.graphics.rendering.NodeRenderer
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.MediumType.FLAT
import java.io.File
import javax.imageio.ImageIO

class NodeRendererTests {
    val gle = Hybrid.gle
    val imageConverter = AwtImageConverter{gle}
    val workspace = TestHelper.makeShellWorkspace(100,100)

    @Test fun RendersGroup() {
        val group = workspace.groupTree.addGroupNode(null,"Group1")
        val simpleLayer1 = workspace.groupTree.addNewSimpleLayer(group, "Layer1", FLAT, 50, 50)
        val simpleLayer2 = workspace.groupTree.addNewSimpleLayer(group, "Layer2", FLAT, 50, 50)
        val simpleLayer3 = workspace.groupTree.addNewSimpleLayer(group, "Layer3", FLAT, 50, 50)

        simpleLayer1.x = 25
        simpleLayer1.y = 0
        simpleLayer2.x = 10
        simpleLayer2.y = 25
        simpleLayer3.x = 40
        simpleLayer3.y = 25

        var gc = (((simpleLayer1.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.RED
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayer2.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.BLUE
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayer3.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.GREEN
        gc.fillRect( 0, 0, 50, 50)

        val renderLayer = Hybrid.imageCreator.createImage(100,100)
        NodeRenderer(group, workspace).render(renderLayer.graphicsOld)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(renderLayer)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\nodeRendererThreeLayers.png"))
        }
    }

    @Test fun RendersSubGroup() {
        val group = workspace.groupTree.addGroupNode(null,"Group1")
        val simpleLayer1 = workspace.groupTree.addNewSimpleLayer(group, "Layer1", FLAT, 50, 50)
        val groupInner = workspace.groupTree.addGroupNode(group,"Group1")
        val simpleLayerS1 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS1", FLAT, 50, 50)
        val simpleLayerS2 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS2", FLAT, 50, 50)
        val simpleLayerS3 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS3", FLAT, 50, 50)
        val simpleLayer2 = workspace.groupTree.addNewSimpleLayer(group, "Layer2", FLAT, 50, 50)

        simpleLayerS1.x = 25
        simpleLayerS1.y = 0
        simpleLayerS2.x = 10
        simpleLayerS2.y = 25
        simpleLayerS3.x = 40
        simpleLayerS3.y = 25
        simpleLayer2.x = 40
        simpleLayer2.y = 40

        groupInner.method = RenderMethod(MULTIPLY)

        var gc = (((simpleLayer1.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.DARK_GRAY
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayer2.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.LIGHT_GRAY
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS1.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.RED
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS2.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.GREEN
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS3.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.BLUE
        gc.fillRect( 0, 0, 50, 50)

        val renderLayer = Hybrid.imageCreator.createImage(100,100)
        NodeRenderer(group, workspace).render(renderLayer.graphicsOld)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(renderLayer)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\nodeRendererSubGroup.png"))
        }
    }

    @Test fun RendersSubScaled() {
        val group = workspace.groupTree.addGroupNode(null,"Group1")
        val simpleLayer1 = workspace.groupTree.addNewSimpleLayer(group, "Layer1", FLAT, 50, 50)
        val groupInner = workspace.groupTree.addGroupNode(group,"Group1")
        val simpleLayerS1 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS1", FLAT, 50, 50)
        val simpleLayerS2 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS2", FLAT, 50, 50)
        val simpleLayerS3 = workspace.groupTree.addNewSimpleLayer(groupInner, "LayerS3", FLAT, 50, 50)
        val simpleLayer2 = workspace.groupTree.addNewSimpleLayer(group, "Layer2", FLAT, 50, 50)

        simpleLayerS1.x = 25
        simpleLayerS1.y = 0
        simpleLayerS2.x = 10
        simpleLayerS2.y = 25
        simpleLayerS3.x = 40
        simpleLayerS3.y = 25
        simpleLayer2.x = 40
        simpleLayer2.y = 40

        groupInner.method = RenderMethod(MULTIPLY)

        var gc = (((simpleLayer1.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.DARK_GRAY
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayer2.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.LIGHT_GRAY
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS1.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.RED
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS2.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.GREEN
        gc.fillRect( 0, 0, 50, 50)
        gc = (((simpleLayerS3.layer as SimpleLayer).medium.medium as FlatMedium).image as RawImage).graphicsOld
        gc.color = Colors.BLUE
        gc.fillRect( 0, 0, 50, 50)

        val renderLayer = Hybrid.imageCreator.createImage(25,25)
        NodeRenderer(group, workspace, RenderSettings(25, 25)).render(renderLayer.graphicsOld)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(renderLayer)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\nodeRendererSubGroupScaled.png"))
        }
    }

}