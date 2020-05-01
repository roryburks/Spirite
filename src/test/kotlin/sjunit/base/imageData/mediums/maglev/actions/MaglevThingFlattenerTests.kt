package sjunit.base.imageData.mediums.maglev.actions

import org.junit.jupiter.api.Test
import rb.extendo.dataStructures.SinglyCollection
import rb.extendo.dataStructures.SinglyList
import rb.glow.color.Colors
import sjunit.base.imageData.mediums.maglev.MaglevTestHelper
import sjunit.testHelpers.makeWorkspaceWithMockedExternals
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.actions.MaglevThingFlattener
import kotlin.test.assertEquals

class MaglevThingFlattenerTests {
    @Test fun flatensBasic() {
        // Maglev made up of three strokes, flattens into correct three strokes
        val workspace = makeWorkspaceWithMockedExternals(100,100)
        val lines= listOf(
                MaglevTestHelper.makeMaglevLine(25f,0f,25f,100f),
                MaglevTestHelper.makeMaglevLine(75f, 0f, 75f, 100f),
                MaglevTestHelper.makeMaglevLine(0f, 50f, 100f, 50f))
        val maglev = MaglevMedium(workspace, lines)

        // Act
        val flattened = MaglevThingFlattener.flattenMaglevMedium(maglev)

        // Assert
        assertEquals(lines[0], flattened[0])
        assertEquals(lines[1], flattened[1])
        assertEquals(lines[2], flattened[2])
    }

    @Test fun flatensAfterRemoval(){
        // Scenario: Line 1, then Line 2, then Fill 1 on Line 1, then Line 3 then Fill 2 on line 3
        // Line 2 is removed
        // Should now have T1: Line1, T1: Fill1 with StrokeId = 0, T2: Line3, T3: Fill2 with StrokeId = 2
        val workspace = makeWorkspaceWithMockedExternals(100,100)
        val line1 =MaglevTestHelper.makeMaglevLine(25f,0f,25f,100f)
        val line2 = MaglevTestHelper.makeMaglevLine(75f, 0f, 75f, 100f)
        val line3 = MaglevTestHelper.makeMaglevLine(0f, 50f, 100f, 50f)
        val fill1 = MaglevFill(SinglyList(MaglevFill.StrokeSegment(0, 10, 30)),MagneticFillMode.BEHIND, Colors.WHITE)
        val fill2 = MaglevFill(SinglyList(MaglevFill.StrokeSegment(3, 7, 27)),MagneticFillMode.BEHIND, Colors.WHITE)
        val things = listOf<IMaglevThing>(line1, line2, fill1, line3, fill2)

        val maglev = MaglevMedium(workspace, things)
        val handle = workspace.mediumRepository.addMedium(maglev)
        val arranged = ArrangedMediumData(handle,0f,0f)
        maglev.removeThings(SinglyCollection(line2), arranged, "Unit Test")

        // Act
        val flattened = MaglevThingFlattener.flattenMaglevMedium(maglev)

        // Assert
        assertEquals(4, flattened.count())
        assertEquals(line1, flattened[0])
        assertEquals(line3, flattened[2])
        val fill1Readback = flattened[1] as MaglevFill
        assertEquals(0, fill1Readback.segments.single().strokeId)
        assertEquals(10, fill1Readback.segments.single().start)
        assertEquals(30, fill1Readback.segments.single().end)

        val fill2Readback = flattened[3] as MaglevFill
        assertEquals(2, fill2Readback.segments.single().strokeId)
    }
}