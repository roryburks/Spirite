package spirite.gui.menus

import spirite.base.brains.commands.NodeCommands
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer

object NodeMenus {
    fun schemeForNode(workspace: IImageWorkspace, node: Node?) : List<MenuItem>{

        val scheme = mutableListOf(
                MenuItem("&New..."),
                MenuItem(".New Simple &Layer", NodeCommands.NewSimpleLayer),
                MenuItem(".New Layer &Group", NodeCommands.NewGroup),
                MenuItem(".New &Sprite Layer", NodeCommands.NewSpriteLayer),
                MenuItem(".New &Puppet Layer", NodeCommands.NewPuppetLayer)
        )

        if( node != null) {
            val descriptor = when( node) {
                is GroupNode -> "Layer Group"
                is LayerNode -> "Layer"
                else -> "..."
            }

            scheme.add(MenuItem("-"))
            scheme.add(MenuItem("D&uplicate $descriptor", NodeCommands.Duplicate))
            scheme.add(MenuItem("&Copy $descriptor", NodeCommands.Copy))
            scheme.add(MenuItem("&Delete $descriptor", NodeCommands.Delete))

            when( node) {
                is GroupNode -> {
                    val ffaEnabled = workspace.animationManager.currentAnimation is FixedFrameAnimation

                    scheme.add(MenuItem("-"))
                    scheme.add(MenuItem("&Make Simple Animation From Group", NodeCommands.AnimFromGroup))
                    scheme.add(MenuItem("&Add Group To Animation As New Layer", NodeCommands.InsertGroupInAnim, enabled = ffaEnabled))
                    if( ffaEnabled) {
                        scheme.add(MenuItem("Add Group To Animation As New &Lexical Layer", NodeCommands.InsertLexicalLayer))
                        scheme.add(MenuItem("Add Group To Animation As New &Cascading Layer", NodeCommands.InsertCascadingLayer))
                    }
                    scheme.add(MenuItem("Write Group To GIF Animation", NodeCommands.GifFromGroup))
                }
                is LayerNode -> {
                    scheme.add(MenuItem("-"))
                    scheme.add(MenuItem("&Merge Layer Down", NodeCommands.MergeDown))
                    val layer = node.layer
                    when( layer) {
                        is SimpleLayer -> scheme.add(MenuItem(("Con&vert Simple Layer To Sprite Layer"), NodeCommands.ConvertLayerToSprite))
                        is SpriteLayer -> {
                            scheme.add(MenuItem("&Sprite Layer Commands"))
                            scheme.add(MenuItem(".Diffuse Sprite Layer", NodeCommands.DiffuseSpriteLayer))
                            scheme.add(MenuItem(".Shift Sprite Layer Depth", NodeCommands.ShiftSpriteLayerDepth))
                            scheme.add(MenuItem("Construct &Rig Animation From Sprite", NodeCommands.NewRigAnimation))
                        }
                        //is PuppetLayer -> scheme.add(MenuItem("Add &Derived Puppet Layer", customAction = {TODO()}))
                    }

                }
            }
            scheme.add(MenuItem("-"))
            scheme.add(MenuItem("Clear All &View Settings", NodeCommands.ClearViewSettings))
        }


        return scheme
    }
}