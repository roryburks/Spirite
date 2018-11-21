package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.file.defaultAafExporter
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.dialogs.IDialog
import spirite.gui.components.dialogs.IDialog.FilePickType.AAF

class AnimationCommandExecutor (val master: IMasterControl)
    : ICommandExecuter
{
    override val domain: String get() = "anim"
    override val validCommands: List<String> get() = executers.map { "anim.${it.name}" }

    override fun executeCommand(string: String, extra: Any?): Boolean {
        val workspace = master.workspaceSet.currentMWorkspace ?: return false
        val animation = workspace.animationManager.currentAnimation
        return executers.asSequence()
                .filter { it.name == string }
                .any { it.execute(master, workspace, animation) }
    }

}

internal val executers = mutableListOf<IAnimationCommand>()

interface IAnimationCommand : ICommand
{
    val name : String
    fun execute( master: IMasterControl, workspace: IImageWorkspace, animation: Animation?) : Boolean

    override val commandString: String get() = "anim.$name"
    override val keyCommand: KeyCommand
        get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.animationManager?.currentAnimation}
}

object ExportAafCommand : IAnimationCommand
{
    init {executers.add(this)}
    override val name: String get() = "exportAsAaf"
    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        animation as? FixedFrameAnimation ?: return false
        val file = master.dialog.pickFile(AAF) ?: return false

        defaultAafExporter.export(animation, file.absolutePath)
        return true
    }
}