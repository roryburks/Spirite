package spirite.base.file.aaf

import rb.glow.img.IImage
import rb.glow.img.RawImage
import rb.vectrix.shapes.RectI
import sguiSwing.hybrid.Hybrid
import spirite.base.file.readUTF8NT
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.PrimaryGroupTree.InsertBehavior.InsertTop
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.DynamicMedium

class AafV1Anim(
        val name: String,
        val rigs: List<AafV1Rig>)

class AafV1Rig(
        val frames: List<AafV1Frame>)

class AafV1Frame(
        val frameId: Int,
        val offsetX: Int,
        val offsetY: Int)

object AafV1Importer : IAafImporter {
    override fun importIntoWorkspace(context: AafLoadContext) {
        val ra = context.ra
        val ws = context.workspace
        val rootImportNode = ws.groupTree.addGroupNode(ws.groupTree.root, context.fileName)

        // Animations
        val anims = List(ra.readUnsignedShort()) {
            val name = ra.readUTF8NT()
            val rigs = List(ra.readUnsignedShort()) {
                val frames = List(ra.readUnsignedByte()) {
                    AafV1Frame(
                            ra.readUnsignedShort(), // FrameId
                            ra.readUnsignedShort(), // ox
                            ra.readUnsignedShort()) // oy
                }

                AafV1Rig(frames)
            }
            AafV1Anim(name,rigs)
        }

        // Frames
        val frames = List(ra.readUnsignedShort()) {
            RectI(
                    ra.readInt(),
                    ra.readInt(),
                    ra.readInt(),
                    ra.readInt())
        }

        anims.forEach { importAnim(it, frames, context, rootImportNode) }
    }

    fun importAnim( anim: AafV1Anim, frames: List<RectI>, context: AafLoadContext, importRoot: GroupNode) {
        // Todo: Basic level de-dupe
        val ws = context.workspace
        val groupTree = context.workspace.groupTree
        val root = groupTree.addGroupNode(importRoot, anim.name)

        anim.rigs.mapIndexed{index, rig ->
                    val spriteData = rig.frames.mapIndexed { rigIndex, frame ->
                        val img = imageFromFrame(frames[frame.frameId],context.img)
                        val dynamicImg = DynamicImage(img, frame.offsetX, frame.offsetY)
                        val handle = ws.mediumRepository.addMedium(DynamicMedium(ws, dynamicImg))
                        Pair(handle, SpritePartStructure(depth = index, partName = "${index}_$rigIndex"))
                    }

                    SpriteLayer(ws, spriteData)
                }
                .forEachIndexed{index, spriteLayer ->
                    groupTree.importLayer(root, "$index", spriteLayer, false, InsertTop)
                }
    }

    fun imageFromFrame( frame: RectI, image: IImage) : RawImage {
        val output = Hybrid.imageCreator.createImage(frame.wi,frame.hi)
        output.graphics.renderImage(image, -frame.x1, -frame.y1)
        return output
    }
}