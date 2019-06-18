package spirite.pc

import sguiSwing.PrimaryIcon.*
import sguiSwing.SwPrimaryIconSet
import sguiSwing.SwProvider
import spirite.gui.resources.SpiriteIcons
import spirite.hybrid.EngineLaunchpoint

fun setupSwGuiStuff() {
    SwProvider.converter = EngineLaunchpoint.converter

    SwPrimaryIconSet.setIcon(SmallExpanded, SpiriteIcons.SmallIcons.Expanded)
    SwPrimaryIconSet.setIcon(SmallExpandedHighlighted, SpiriteIcons.SmallIcons.ExpandedHighlighted)
    SwPrimaryIconSet.setIcon(SmallUnexpanded, SpiriteIcons.SmallIcons.Unexpanded)
    SwPrimaryIconSet.setIcon(SmallUnexpandedHighlighted, SpiriteIcons.SmallIcons.UnexpandedHighlighted)
}