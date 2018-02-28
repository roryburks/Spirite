package spirite.base.graphics.rendering.sources

import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node


/**
 * A RenderSource corresponds to an object which can be rendered and it implements
 * everything needed to perform a Render using certain RenderSettings.
 *
 * Note: It is important that subclasses overload the equals and hashCode methods
 * of each RenderSource since the RenderEngine uses them to determine if you
 * are rendering the same thing as something that has already been rendered.
 * If you just go on the built-in uniqueness test and pass them through renderImage
 * then unless you are storing the RenderSource locally yourself (which is possible
 * and not harmful but defeats the purpose of RenderEngine), then RenderEngine
 * will get clogged remembering different renders of the same image.
 */
interface RenderSource {
    val workspace: IImageWorkspace
    val defaultWidth: Int
    val defaultHeight: Int
    val imageDependencies: Collection<MediumHandle>
    val nodeDependencies: Collection<Node>

    fun render( settings: RenderSettings) : RawImage
}