package sjunit.base.imageData.mediums.magLev.selecting

import org.junit.jupiter.api.Test
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import sjunit.testHelpers.makeWorkspaceWithMockedExternals
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams


class SimpleMaglevStrokeSelectionTests {
    class FromArranged {
        fun makeMaglevLine(x1: Float, y1: Float, x2: Float, y2: Float): MaglevStroke
        {
            val params = StrokeParams()
            val len = MathUtil.distance(x1,y1,x2,y2)
            val n = len.floor + 1
            val points=  DrawPoints(
                    (0..n).map { MathUtil.lerp(x1, x2, it.f / n.f) }.toFloatArray(),
                    (0..n).map { MathUtil.lerp(y1, y2, it.f / n.f) }.toFloatArray(),
                    (0..n).map { 1f }.toFloatArray())
            return MaglevStroke(params, points)
        }

        @Test fun selectsRectangle(){
            // Here's the case.  Workspace is 100x100
            // Selection is a 50x100 square: the right half of the workspace
            // image has three strokes:
            //   one going from (25,0) to (25,100), well outside of the selection
            //   one going from (75,0) to (75,100), inside the selection
            //   one goes from (0,50) to (100,50), half inside the selection
            val workspace = makeWorkspaceWithMockedExternals(100,100)
            val lines= makeMaglevLine()
            val maglev = MaglevMedium(workspace,  )
        }
    }
}