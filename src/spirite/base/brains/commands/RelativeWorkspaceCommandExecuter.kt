package spirite.base.brains.commands

import spirite.base.brains.MasterControl
import spirite.base.brains.ToolsetManager.Tool
import spirite.base.brains.ToolsetManager.ToolSettings
import spirite.base.image_data.Animation
import spirite.base.image_data.GroupTree.LayerNode
import spirite.base.image_data.GroupTree.Node
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ReferenceManager
import spirite.base.image_data.animations.ffa.FFAAnimationState
import spirite.base.image_data.animations.ffa.FixedFrameAnimation
import spirite.base.image_data.layers.Layer
import spirite.base.image_data.mediums.IMedium.InternalImageTypes
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.image_data.mediums.drawer.IImageDrawer.IClearModule
import spirite.base.image_data.mediums.drawer.IImageDrawer.IInvertModule
import spirite.base.image_data.mediums.drawer.IImageDrawer.ITransformModule
import spirite.base.image_data.selection.SelectionEngine
import spirite.base.image_data.selection.SelectionMask
import spirite.base.pen.Penner
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Vec2
import spirite.hybrid.HybridHelper
import spirite.hybrid.HybridUtil
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException

import java.util.ArrayList
import java.util.HashMap

/**
 * draw.* Command Executer
 *
 * These are commands that make direct and immediate changes to the current
 * ImageWorkspace's image data (usually the active data)
 */
class RelativeWorkspaceCommandExecuter(private val master: MasterControl) : CommandExecuter {
    private val commandMap = HashMap<String, Runnable>()
    private lateinit var workspace: ImageWorkspace

    override fun getCommandDomain(): String {
        return "draw"
    }

    init {

        commandMap.put("undo", Runnable{ workspace.undoEngine.undo() })
        commandMap.put("redo", Runnable{ workspace.undoEngine.redo() })
        commandMap.put("shiftRight",Runnable {
            val drawer = workspace.activeDrawer
            if (drawer is ITransformModule)
                (drawer as ITransformModule).transform(MatTrans.TranslationMatrix(1f, 0f))
        })
        commandMap.put("shiftLeft",Runnable {
            val drawer = workspace.activeDrawer
            if (drawer is ITransformModule)
                (drawer as ITransformModule).transform(MatTrans.TranslationMatrix(-1f, 0f))
        })
        commandMap.put("shiftDown",Runnable {
            val drawer = workspace.activeDrawer
            if (drawer is ITransformModule)
                (drawer as ITransformModule).transform(MatTrans.TranslationMatrix(0f, 1f))
        })
        commandMap.put("shiftUp",Runnable {
            val drawer = workspace.activeDrawer
            if (drawer is ITransformModule)
                (drawer as ITransformModule).transform(MatTrans.TranslationMatrix(0f, -1f))
        })
        commandMap.put("newLayerQuick",Runnable {
            workspace.addNewSimpleLayer(workspace.selectedNode,
                    workspace.width, workspace.height,
                    "New Layer", 0x00000000, InternalImageTypes.DYNAMIC)
        })
        commandMap.put("clearLayer",Runnable {

            if (workspace.selectionEngine.isLifted) {
                workspace.selectionEngine.clearLifted()
            } else {
                val drawer = workspace.activeDrawer
                if (drawer is IClearModule)
                    (drawer as IClearModule).clear()
                else
                    HybridHelper.beep()
            }
        })
        commandMap.put("cropSelection",Runnable {
            val node = workspace.selectedNode
            val selectionEngine = workspace.selectionEngine

            val selection = selectionEngine.selection
            if (selection == null) {
                HybridHelper.beep()
                return@Runnable
            }

            val rect = Rect(selection.dimension)
            rect.x = selection.ox
            rect.y = selection.oy

            workspace.cropNode(node, rect, false)
        })
        commandMap.put("autocroplayer", Runnable{
            val node = workspace.selectedNode

            if (node is LayerNode) {
                val layer = node.layer

                try {
                    val rect: Rect
                    rect = HybridUtil.findContentBounds(
                            layer.activeData.handle.deepAccess(),
                            1,
                            false)
                    rect.x += node.getOffsetX()
                    rect.y += node.getOffsetY()
                    workspace.cropNode(node, rect, true)
                } catch (e: UnsupportedImageTypeException) {
                    e.printStackTrace()
                }

            }
        })
        commandMap.put("layerToImageSize",Runnable {
            val node = workspace.selectedNode

            if (node != null)
                workspace.cropNode(node, Rect(0, 0, workspace.width, workspace.height), false)
        })
        commandMap.put("invert", Runnable{
            val drawer = workspace.activeDrawer

            if (drawer is IInvertModule)
                (drawer as IInvertModule).invert()
            else
                HybridHelper.beep()
        })
        commandMap.put("applyTransform", Runnable{
            val settings = master.toolsetManager.getToolSettings(Tool.RESHAPER)
            if (workspace.selectionEngine.isProposingTransform)
                workspace.selectionEngine.applyProposedTransform()
            else {
                val scale = settings.getValue("scale") as Vec2
                val translation = settings.getValue("translation") as Vec2
                val rotation = settings.getValue("rotation") as Float

                val trans = MatTrans()
                trans.preScale(scale.x, scale.y)
                trans.preRotate((rotation * 180.0f / Math.PI).toFloat())
                trans.preTranslate(translation.x, translation.y)
                workspace.selectionEngine.transformSelection(trans)

            }

            settings.setValue("scale", Vec2(1f, 1f))
            settings.setValue("translation", Vec2(0f, 0f))
            settings.setValue("rotation", 0f)

            val p = master.frameManager.penner
            p?.cleanseState()
        })
        commandMap.put("toggle_reference",Runnable {
            val rm = workspace.referenceManager
            rm.isEditingReference = !rm.isEditingReference
        })
        commandMap.put("reset_reference",Runnable {
            val rm = workspace.referenceManager
            rm.resetTransform()
        })
        commandMap.put("lift_to_reference",Runnable {
            val se = workspace.selectionEngine
            val rm = workspace.referenceManager

            if (se.isLifted) {
                rm.addReference(se.liftedData.readonlyAccess(), rm.center, se.liftedDrawTrans)
                se.clearLifted()
            }
        })

        commandMap.put("resizeWorkspace",Runnable { master.dialogs.callResizeLayer(workspace) })


        commandMap.put("addGapQuick",Runnable {
            val animation = workspace.animationManager.pseudoSelectedAnimation
            if( animation is FixedFrameAnimation) {
                val state = workspace.animationManager.getAnimationState(animation) as FFAAnimationState
                val frame = state.selectedFrame ?: return@Runnable
                frame.gapAfter = 1 + frame.gapAfter
            }
        })
    }

    override fun getValidCommands(): List<String> {
        return ArrayList(commandMap.keys)
    }

    override fun executeCommand(command: String, extra: Any?): Boolean {
        val runnable = commandMap[command]

        if (runnable != null) {
            workspace = master.currentWorkspace ?: return true
            runnable.run()
            return true
        } else
            return false
    }
}