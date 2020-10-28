package sjunit.base.imageData.mediums.maglev.selecting

import org.junit.jupiter.api.Test
import rb.glow.Colors
import sjunit.base.imageData.mediums.maglev.MaglevTestHelper
import sjunit.testHelpers.images.TRectIImage
import sjunit.testHelpers.makeWorkspaceWithMockedExternals
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.selecting.SimpleMaglevStrokeSelectionExtra
import spirite.base.imageData.selection.Selection
import spirite.base.util.linear.Rect
import kotlin.test.assertEquals


class SimpleMaglevStrokeSelectionTests {
    class FromArranged {

        @Test fun selectsRectangle(){
            // Here's the case.  Workspace is 100x100
            // Selection is a 50x100 square: the right half of the workspace
            // image has three strokes:
            //   one going from (25,0) to (25,100), well outside of the selection, not counted
            //   one going from (75,0) to (75,100), inside the selection, counted
            //   one goes from (0,50) to (100,50), half inside the selection, not counted (below 70% threshold)
            val workspace = makeWorkspaceWithMockedExternals(100,100)
            val lines= listOf(
                    MaglevTestHelper.makeMaglevLine(25f,0f,25f,100f),
                    MaglevTestHelper.makeMaglevLine(75f, 0f, 75f, 100f),
                    MaglevTestHelper.makeMaglevLine(0f, 50f, 100f, 50f))
            val maglev = MaglevMedium(workspace, lines )
            val handle = workspace.mediumRepository.addMedium(maglev)
            val selectionImage = TRectIImage(Rect(50,0,50,100), Colors.WHITE)
            val selection = Selection(selectionImage)
            val arranged = ArrangedMediumData(handle, selection = selection)

            // Act
            val magSel = SimpleMaglevStrokeSelectionExtra.FromArranged(arranged)

            // Assert
            assertEquals(lines[1], magSel.lines.single())
        }

    }
}