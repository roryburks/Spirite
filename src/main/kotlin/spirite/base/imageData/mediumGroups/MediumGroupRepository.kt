package spirite.base.imageData.mediumGroups

import spirite.base.imageData.IImageWorkspace


interface IMediumGroupRepository
{
    val mediumGroups: List<MediumGroup>
}

class MediumGroupRepository(val ws: IImageWorkspace) : IMediumGroupRepository
{
    override val mediumGroups: MutableList<MediumGroup> = mutableListOf()
}