package spirite.pc

import sgui.swing.PrimaryIcon.*
import sgui.swing.SwPrimaryIconSet
import sgui.swing.SwProvider
import sgui.hybrid.EngineLaunchpoint
import spirite.gui.resources.SpiriteIcons

fun setupSwGuiStuff() {
    SwProvider.converter = EngineLaunchpoint.converter

    SwPrimaryIconSet.setIcon(SmallExpanded, SpiriteIcons.SmallIcons.Expanded)
    SwPrimaryIconSet.setIcon(SmallExpandedHighlighted, SpiriteIcons.SmallIcons.ExpandedHighlighted)
    SwPrimaryIconSet.setIcon(SmallUnexpanded, SpiriteIcons.SmallIcons.Unexpanded)
    SwPrimaryIconSet.setIcon(SmallUnexpandedHighlighted, SpiriteIcons.SmallIcons.UnexpandedHighlighted)

    SwPrimaryIconSet.setIcon(SmallArrowN, SpiriteIcons.SmallIcons.ArrowN)
    SwPrimaryIconSet.setIcon(SmallArrowS, SpiriteIcons.SmallIcons.ArrowS)
    SwPrimaryIconSet.setIcon(SmallArrowE, SpiriteIcons.SmallIcons.ArrowE)
    SwPrimaryIconSet.setIcon(SmallArrowW, SpiriteIcons.SmallIcons.ArrowW)
}