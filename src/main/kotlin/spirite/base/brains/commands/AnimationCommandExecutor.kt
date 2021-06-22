package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.file.ExportToGif
import spirite.base.file.aaf.defaultAafExporter
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.menus.dialogs.IDialog.FilePickType.AAF
import spirite.gui.menus.dialogs.IDialog.FilePickType.GIF
import java.io.File

class AnimationCommandExecutor (val master: IMasterControl)
    : ICommandExecutor
{
    override val domain: String get() = "anim"
    override val validCommands: List<String> get() = executors.map { "anim.${it.name}" }

    override fun executeCommand(string: String, extra: Any?): Boolean {
        val workspace = master.workspaceSet.currentMWorkspace ?: return false
        val animation = workspace.animationManager.currentAnimation
        return executors.asSequence()
                .filter { it.name == string }
                .any { it.execute(master, workspace, animation) }
    }

}

private val executors = mutableListOf<AnimationCommand>()

abstract class AnimationCommand : ICommand
{
    // Note: this is somewhat of a hacky way to make sure each AnimationCommand added gets added to the list
    init {executors.add(this)}

    abstract val name: String
    abstract fun execute( master: IMasterControl, workspace: IImageWorkspace, animation: Animation?) : Boolean

    override val commandString: String get() = "anim.$name"
    override val keyCommand: KeyCommand
        get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.animationManager?.currentAnimation}
}

object ExportAafCommand : AnimationCommand()
{
    override val name: String get() = "exportAsAaf"
    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        if (animation !is FixedFrameAnimation) return false
        val file = master.dialog.pickFile(AAF) ?: return false

        defaultAafExporter.export(animation, file.absolutePath)
        return true
    }
}
object ExportGifCommand : AnimationCommand() {
    override val name: String get() = "exportAsGif"

    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        val anim = animation as? FixedFrameAnimation ?: return false
        var file = master.dialog.pickFile(GIF) ?: return false
        if( file.extension == "") {
            file = File(file.absolutePath + ".gif")
        }

        ExportToGif.exportAnim(animation,file, animation.stateBind.speed)
        return true
    }

}
object RenameAnimationCommand : AnimationCommand() {
    override val name: String get() = "rename"
    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        animation ?: return false
        val newName = master.dialog.promptForString("Enter New Animation Name:", animation.name) ?: return true
        animation.name = newName
        return true
    }
}
object DeleteAnimationCommand : AnimationCommand() {
    override val name: String get() = "delete"
    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        workspace.animationManager.removeAnimation(animation ?: return false)
        return true
    }
}
object DuplicateAnimationCommand : AnimationCommand() {
    override val name: String get() = "dupe"
    override fun execute(master: IMasterControl, workspace: IImageWorkspace, animation: Animation?): Boolean {
        workspace.animationManager.addAnimation( animation?.dupe() ?: return false)
        return true
    }
}