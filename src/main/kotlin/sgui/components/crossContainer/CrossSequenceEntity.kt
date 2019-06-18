package sgui.components.crossContainer

import sgui.components.IComponent

sealed class CrossSequenceEntity

class CSE_Gap(
        val minWidth: Int,
        val defaultWidth: Int,
        val maxWidth: Int) : CrossSequenceEntity()

class CSE_Component(
        val component: IComponent,
        val overrideGroup : Int?,
        val fixed: Int?,
        val flex: Float?) : CrossSequenceEntity()

class CSE_Group(
        val subComponents: List<CrossSequenceEntity>,
        val fixed: Int?,
        val flex: Float?,
        val flat: Boolean,
        val padding: Int?,
        val pregap : Int?) : CrossSequenceEntity()