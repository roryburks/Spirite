package spirite.gui.menus

import sgui.swing.SwIcon
import spirite.base.brains.commands.ICommand

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
    val icon: SwIcon? = null,
    val customAction :(()->Unit)? = null,
    val enabled: Boolean = true)