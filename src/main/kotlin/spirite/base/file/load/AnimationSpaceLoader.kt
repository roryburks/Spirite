package spirite.base.file.load

import spirite.base.file.BadSifFileException
import spirite.base.file.LoadContext
import spirite.base.file.SaveLoadUtil
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.AnimationSpace
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace.SpacialLink
import spirite.base.util.linear.Vec2i


interface IAnimationSpaceLoader
{
    fun loadAnimationSpace(context : LoadContext, name: String) : AnimationSpace?
}

object AnimationSpaceLoaderFactory
{
    fun getAnimationSpaceLoader(version: Int, typeId: Int) : IAnimationSpaceLoader = when(typeId) {
        SaveLoadUtil.ANIMSPACE_FFA -> FFASpaceLoader
        else -> throw BadSifFileException("Unrecognized AnimationSpace Type ID: $typeId (version mismatch or corrupt file?)")
    }
}

object FFASpaceLoader : IAnimationSpaceLoader
{
    override fun loadAnimationSpace(context: LoadContext, name: String): AnimationSpace? {
        val ra = context.ra
        val anims = context.animations

        val space = FFAAnimationSpace(name, context.workspace)

        val numAnimations = ra.readUnsignedShort()
        repeat(numAnimations) {
            val animation = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation

            val endLinkAnimId = ra.readInt()
            val endLinkFrame = if( endLinkAnimId != -1) ra.readInt() else 0

            val logX = ra.readUnsignedShort()
            val logY = ra.readUnsignedShort()

            if( animation != null) {
                space.addAnimation(animation)

                val endLinkAnim = anims.getOrNull(endLinkAnimId) as? FixedFrameAnimation
                if( endLinkAnim != null)
                    space.setOnEndBehavior(animation, Pair(endLinkAnim, endLinkFrame))

                space.stateView.setLogicalSpace(animation, Vec2i(logX,logY))
            }
        }

        val numLinks = ra.readUnsignedShort()
        repeat(numLinks) {
            val origin = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation
            val originFrame = ra.readInt()
            val destination = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation
            val destinationFrame = ra.readInt()
            if( origin != null && destination != null)
                space.addLink(SpacialLink(origin, originFrame, destination, destinationFrame))
        }

        return space
    }

}