package sgui.components

interface IResizeContainerPanel : IComponent
{

    var minStretch : Int
    var orientation : sgui.Orientation
    var barSize : Int
    var stretchComponent : IComponent

    fun getPanel(index: Int) : IResizeBar?
    fun addPanel(component : IComponent, minSize: Int, defaultSize: Int, position: Int = 0, visible: Boolean = true) : Int
    fun removePanel( index: Int)

    interface IResizeBar {
        var minSize: Int
        var resizeComponent: IComponent
    }
}