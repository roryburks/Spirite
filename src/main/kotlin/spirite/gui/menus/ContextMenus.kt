package spirite.gui.menus

import spirite.base.brains.commands.ICentralCommandExecutor
import spirite.base.brains.commands.ICommand
import spirite.base.brains.commands.NodeCommands
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.gui.UIPoint
import spirite.gui.resources.IIcon

abstract class ContextMenus( val commandExecuter: ICentralCommandExecutor) {

    /***
     * Sample scheme:
     * MenuItem("&Root")
     * MenuItem(".Sub1")
     * MenuItem("-")
     * MenuItem(".Sub2")
     * MenuItem("..SubSub1")
     * MenuItem("Root2")
     *
     * Each dot before the name indicates the level it should be in.  For example one dot
     *   means it goes inside the last zero-dot item, two dots means it should go in the last
     *   one-dot item, etc.  Note: if you skip a certain level of dot's (eg: going from
     *   two dots to four dots), then the extra dots will be ignored, possibly resulting
     *   in unexpected menu form.
     * The & character before a letter represents the Mnemonic key that should be associated
     *   with it.
     * If the title is simply - (perhaps after some .'s representing its depth), then it is
     *   will simply construct a separator and will ignore the last two elements in the
     *   array (in fact they don't need to exist).
     */
    data class MenuItem(
           val lexicon : String,
           val command: ICommand? = null,
           val icon: IIcon? = null,
           val customAction :(()->Unit)? = null,
           val enabled: Boolean = true)


    abstract fun LaunchContextMenu( point: UIPoint, scheme: List<MenuItem>, obj: Any? = null)

    fun schemeForNode( workspace: IImageWorkspace, node: Node?) : List<MenuItem>{

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
                        is SpriteLayer -> scheme.add(MenuItem("Construct &Rig Animation From Sprite", NodeCommands.NewRigAnimation))
                        //is PuppetLayer -> scheme.add(MenuItem("Add &Derived Puppet Layer", customAction = {TODO()}))
                    }

                }
            }
        }

        return scheme
    }



}