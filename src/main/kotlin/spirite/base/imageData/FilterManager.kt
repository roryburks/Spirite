package spirite.base.imageData

import spirite.base.graphics.filter.IFilter

interface IFilterManager
{
    fun setFilterForMedium(filter: IFilter?, mediumHandle: MediumHandle)
    fun getFilterForMedium(mediumHandle: MediumHandle) : IFilter?
}

class FilterManager : IFilterManager
{
    private val map = mutableMapOf<MediumHandle, IFilter>()

    override fun setFilterForMedium(filter: IFilter?, mediumHandle: MediumHandle) {
        if( filter == null)
            map.remove(mediumHandle)
        else
            map[mediumHandle] = filter
    }

    override fun getFilterForMedium(mediumHandle: MediumHandle): IFilter? = map[mediumHandle]
}