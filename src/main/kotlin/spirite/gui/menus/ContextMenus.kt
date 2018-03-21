package spirite.gui.menus

import spirite.base.brains.commands.ICentralCommandExecutor
import spirite.base.brains.commands.ICommand
import spirite.base.brains.commands.ICommandExecuter
import spirite.base.brains.commands.NodeContextCommand.NodeCommand
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
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
           val customAction :(()->Unit)? = null)


    abstract fun LaunchContextMenu( point: UIPoint, scheme: List<MenuItem>, obj: Any? = null)

    fun schemeForNode( workspace: IImageWorkspace, node: Node?) : List<MenuItem>{

        val scheme = mutableListOf(
                MenuItem("&New..."),
                MenuItem(".New Simple &Layer", NodeCommand.NEW_SIMPLE_LAYER),
                MenuItem(".New Layer &Group", NodeCommand.NEW_GROUP),
                MenuItem(".New &Rig Layer", customAction = {TODO()}),
                MenuItem(".New &Puppet Layer", customAction = {TODO()})
        )

        if( node != null) {
            val descriptor = when( node) {
                is GroupNode -> "Layer Group"
                is LayerNode -> "Layer"
                else -> "..."
            }

            scheme.add(MenuItem("-"))
            scheme.add(MenuItem("D&uplicate $descriptor", customAction = {TODO()}))
            scheme.add(MenuItem("&Delete $descriptor", NodeCommand.DELETE))

            when( node) {
                is GroupNode -> {
                    scheme.add(MenuItem("-"))
                    scheme.add(MenuItem("&Construct Simple Animation From Group", customAction = {TODO()}))
                    scheme.add(MenuItem("&Add Group To Animation As New Layer", customAction = {TODO()}))
                    scheme.add(MenuItem("Write Group To GIF Animation", customAction = {TODO()}))
                }
                is LayerNode -> {
                    scheme.add(MenuItem("-"))
                    scheme.add(MenuItem("&Merge Layer Down", customAction = {TODO()}))
                    val layer = node.layer
                    when( layer) {
                        is SpriteLayer -> scheme.add(MenuItem("Construct &Rig Animation From Sprite", customAction = {TODO()}))
                        //is PuppetLayer -> scheme.add(MenuItem("Add &Derived Puppet Layer", customAction = {TODO()}))
                    }

                }
            }
        }

        return scheme
    }



}