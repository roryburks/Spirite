//package spirite.base.brains.tools
//
//// TODO: Finish implementing properties and schemas
//
//import spirite.base.image_data.mediums.drawer.BaseSkeletonDrawer
//import spirite.base.image_data.mediums.drawer.DefaultImageDrawer
//import spirite.base.image_data.mediums.drawer.GroupNodeDrawer
//import spirite.base.image_data.mediums.drawer.IImageDrawer
//import spirite.base.image_data.mediums.drawer.IImageDrawer.*
//import spirite.base.image_data.mediums.maglev.MaglevImageDrawer
//import spirite.base.pen.stroke.StrokeBuilder.Method.*
//import spirite.base.brains.IObservable
//import spirite.base.brains.Observable
//import spirite.base.brains.tools.Cursor.ERASER
//import spirite.base.brains.tools.Cursor.MOUSE
//import spirite.base.brains.tools.ToolsetManager.MToolsetObserver
//import spirite.base.brains.tools.ToolsetManager.ToolSettings
//import spirite.hybrid.MDebug
//import spirite.hybrid.MDebug.WarningType.REFERENCE
//
//enum class Tool(
//        val description: String,
//        val iconX : Int,
//        val iconY : Int
//) {
//    PEN("Pen", 0, 0),
//    ERASER("Eraser", 1, 0),
//    FILL("Fill", 2, 0),
//    BOX_SELECTION("Box Selection", 3, 0),
//    FREEFORM_SELECTION("Free Selection", 0, 1),
//
//    MOVE("Move", 1, 1),
//    PIXEL("Pixel", 2, 1),
//    CROP("Cropper", 3, 1),
//    COMPOSER("Rig Composer", 0, 2),
//    FLIPPER("Horizontal/Vertical Flipping", 1, 2),
//
//    RESHAPER("Reshaping Tool", 2, 2),
//    COLOR_CHANGE("Color Change Tool", 3, 2),
//    COLOR_PICKER("Color Picker", 0, 3),
//    MAGLEV_FILL("Magnetic Fill", 1, 3),
//    EXCISE_ERASER("Stroke Erasor", 2, 3),
//
//    BONE("Bone Constructor", 4, 0),
//    FLOPPYBONE("Bone Deformer", 4, 1),
//    PUPPET_BONE("Puppet Bone Composer", 4, 0)
//}
//
//enum class Cursor { MOUSE, STYLUS, ERASER}
//
//abstract class ToolPropertySchema<T> (
//
//) {
//
//}
//
//private val ToolsForDefaultDrawer = arrayOf(
//        Tool.PEN,
//        Tool.ERASER,
//        Tool.FILL,
//        Tool.BOX_SELECTION,
//        Tool.FREEFORM_SELECTION,
//        Tool.MOVE,
//        Tool.PIXEL,
//        Tool.CROP,
//        Tool.COMPOSER,
//        Tool.FLIPPER,
//        Tool.RESHAPER,
//        Tool.COLOR_CHANGE,
//        Tool.COLOR_PICKER,
//        Tool.MAGLEV_FILL)
//
//private val ToolsForMaglevDrawer = arrayOf(
//        Tool.PEN,
//        Tool.ERASER,
//        Tool.PIXEL,
//        Tool.BOX_SELECTION,
//        Tool.FREEFORM_SELECTION,
//        Tool.MOVE,
//        Tool.MAGLEV_FILL,
//        Tool.EXCISE_ERASER,
//        Tool.COLOR_CHANGE,
//        Tool.BONE,
//        Tool.FLOPPYBONE)
//
//interface IToolsetManager {
//    var selectedTool: Tool
//    var cursor: Cursor
//
//    fun getToolSettings( tool: Tool) : ToolSettings
//    fun getToolsForDrawer( drawer: IImageDrawer) : List<Tool>
//
//    val toolsetObservable : IObservable<MToolsetObserver>
//}
//
//class ToolsetManager : IToolsetManager {
//    override var selectedTool : Tool
//        get() = _selected[cursor.ordinal]
//        set(value) {
//            _selected[cursor.ordinal] = value
//            triggerToolsetChanged(value)
//        }
//
//    private val _selected = Cursor.values().map {
//        if( it == ERASER) Tool.ERASER
//        else Tool.PEN
//    }.toTypedArray()
//
//    override var cursor: Cursor = MOUSE
//        get() = field
//        set(value) {
//            field = value
//            triggerToolsetChanged(_selected[cursor.ordinal])
//        }
//
//    private val toolSettings = Tool.values().map {
//        ToolSettings(it, listOf())
//    }.toTypedArray()
//
//    override fun getToolSettings( tool: Tool) = toolSettings[tool.ordinal]
//
//    // region Tool Groups
//    override fun getToolsForDrawer( drawer: IImageDrawer) : List<Tool> = when (drawer) {
//        is DefaultImageDrawer -> ToolsForDefaultDrawer.asList()
//        is GroupNodeDrawer -> ToolsForDefaultDrawer.asList()
//        is MaglevImageDrawer -> ToolsForMaglevDrawer.asList()
//        is BaseSkeletonDrawer -> listOf( Tool.PUPPET_BONE)
//        else -> {
//            val list = mutableListOf<Tool>()
//
//            if( drawer is IStrokeModule) {
//                if( drawer.canDoStroke(BASIC))
//                    list.add(Tool.PEN)
//                if( drawer.canDoStroke(ERASE))
//                    list.add(Tool.ERASER)
//                if( drawer.canDoStroke(PIXEL))
//                    list.add(Tool.PIXEL)
//            }
//            if( drawer is IFillModule)
//                list.add(Tool.FILL)
//            if (drawer is IFlipModule)
//                list.add(Tool.FLIPPER)
//            if (drawer is IColorChangeModule)
//                list.add(Tool.COLOR_CHANGE)
//            if (drawer is IMagneticFillModule)
//                list.add(Tool.MAGLEV_FILL)
//            if (drawer is IWeightEraserModule)
//                list.add(Tool.EXCISE_ERASER)
//
//            list.add(Tool.BOX_SELECTION)
//            list.add(Tool.FREEFORM_SELECTION)
//            list.add(Tool.MOVE)
//            list.add(Tool.CROP)
//            list.add(Tool.RESHAPER)
//
//            list
//        }
//    }
//
//
//    // endregion
//
//
//    /**
//     * ToolSettins is an abstract to describe all the settings a particular tool
//     * has.  For quick development and modularity purposes, these settings are
//     * not inherently hard-coded, but are constructed from an object array scheme
//     * (see constructFromScheme for the format) in which strings are used to
//     * get a particular property.
//     */
//    inner class ToolSettings(
//            val tool: Tool,
//            val properties: List<ToolProperty>
//    ) {
//        fun getProperty( id: String) = properties.find { it.id == id }
//
//        fun getValue( id: String) = properties.find{ it.id == id}?.value
//        fun setValue(id: String, value: Any?) {
//            val property = properties.find{it.id == id}
//            if( property == null)
//                MDebug.handleWarning(REFERENCE, "Could not find property $id for tool $tool")
//            else {
//                property.value = value
//                triggerToolsetPropertyChanged(tool, property)
//            }
//        }
//    }
//
//
//    abstract class ToolProperty(
//            val id: String,
//            val hrName: String,
//            val mask: Int = 0
//    )
//    {
//        abstract var value: Any?
//    }
//    // region Toolset Observer
//    interface MToolsetObserver {
//        fun toolsetChanged( newTool: Tool)
//        fun toolsetPropertyChanged(newTool: Tool, property: ToolProperty)
//    }
//    override val toolsetObservable : IObservable<MToolsetObserver> get() = _toolsetObs
//    val _toolsetObs = Observable<MToolsetObserver>()
//
//    fun triggerToolsetChanged( newTool: Tool)
//            = _toolsetObs.trigger { it.toolsetChanged(newTool) }
//    fun triggerToolsetPropertyChanged(tool: Tool, property: ToolProperty)
//            = _toolsetObs.trigger { it.toolsetPropertyChanged(tool, property) }
//    // endregion
//}