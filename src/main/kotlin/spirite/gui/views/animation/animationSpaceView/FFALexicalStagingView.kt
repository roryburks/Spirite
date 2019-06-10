package spirite.gui.views.animation.animationSpaceView

import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.FFASpace.FFALexicalPlayback
import sgui.generic.components.IComponent
import sgui.generic.components.ICrossPanel
import spirite.gui.components.dialogs.IDialog
import spirite.hybrid.Hybrid

class FFALexicalStagingView(
        val dialog: IDialog,
        val space: FFAAnimationSpace,
        val imp: ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    val tfLexicon = Hybrid.ui.TextArea()
    val btnSetLexicon = Hybrid.ui.Button( "Validate And Set Lexicon")

    init {
        imp.setLayout {
            rows.add(tfLexicon)
            rows.add(btnSetLexicon, height = 24)
        }

        btnSetLexicon.enabled = true

        btnSetLexicon.action = {
            val lexicon = FFALexicalPlayback(tfLexicon.text, space)


            val validationErrors = lexicon.validate()
            if( validationErrors != null)
                dialog.promptMessage(validationErrors)
            else
                space.stateView.playback = lexicon
        }
    }
}