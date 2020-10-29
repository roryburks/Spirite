package spirite.base.pen.behaviors

import rb.glow.GraphicsContext_old
import rb.glow.ColorARGB32Normal
import rb.glow.IGraphicsContext
import rb.vectrix.mathUtil.d
import rbJvm.glow.SColor
import rb.vectrix.mathUtil.f
import spirite.base.imageData.drawer.IImageDrawer.IMagneticFillModule
import spirite.base.pen.Penner
import spirite.gui.views.work.WorkSectionView
import kotlin.math.min

class MagneticFillBehavior(penner: Penner, val drawer: IMagneticFillModule, val color: SColor)
    : DrawnPennerBehavior(penner)
{
    override fun onStart() = drawer.startMagneticFill()
    override fun onTock() {}
    override fun onMove()  = drawer.anchorPoints(penner.x.f, penner.y.f, 10f, penner.holdingShift, penner.holdingCtrl)

    override fun onPenUp() {
        val mode = penner.toolsetManager.toolset.MagneticFill.mode
        try {
            drawer.endMagneticFill(color, mode)
        }finally {
            super.onPenUp()
        }
    }

    override fun paintOverlay(gc: IGraphicsContext, view: WorkSectionView) {
        gc.transform  = view.tWorkspaceToScreen
        gc.color = ColorARGB32Normal(0xFFFFFF xor color.argb32)
        val fx = drawer.magFillXs.map { it.d }
        val fy = drawer.magFillYs.map { it.d }
        gc.drawPolyLine(fx, fy, min(fx.size, fy.size))
    }
}