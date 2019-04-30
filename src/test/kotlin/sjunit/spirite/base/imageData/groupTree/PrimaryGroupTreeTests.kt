package sjunit.spirite.base.imageData.groupTree


import sjunit.TestHelper
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.duplicateInto
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.MediumType.FLAT
import kotlin.test.assertEquals
import org.junit.Test as test

open class PrimaryGroupTreeTests {
    val workspace = TestHelper.makeShellWorkspace(100,100)
    val tree = workspace.groupTree

    @test fun CreatesSimpleLayer() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        val ancestors = tree.root.getAllAncestors()
        val layerNode = ancestors.first() as LayerNode

        assertEquals(30, layerNode.layer.width)
        assertEquals(45, layerNode.layer.height)
        assert( (layerNode.layer as SimpleLayer).medium.medium is FlatMedium)
    }

    @test fun UndoesAndRedoes_AddNewSimpleLayer() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        assertEquals(1, tree.root.getAllAncestors().count())

        workspace.undoEngine.undo()

        assertEquals(0, tree.root.getAllAncestors().count())
        workspace.undoEngine.redo()

        assertEquals(1, tree.root.getAllAncestors().count())

        val ancestors = tree.root.getAllAncestors()
        val layerNode = ancestors.first() as LayerNode
        assertEquals(30, layerNode.layer.width)
        assertEquals(45, layerNode.layer.height)
        assert( (layerNode.layer as SimpleLayer).medium.medium is FlatMedium)
    }

    @test fun UndoesAndRedoes_PropertyEdit() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        val layerNode = tree.root.getLayerNodes().first()

        layerNode.x = 10
        layerNode.x = 20
        assertEquals(20, layerNode.x)

        workspace.undoEngine.undo()
        assertEquals(10, layerNode.x)
        workspace.undoEngine.undo()
        assertEquals(0, layerNode.x)

        workspace.undoEngine.redo()
        assertEquals(10, layerNode.x)
        workspace.undoEngine.redo()
        assertEquals(20, layerNode.x)
    }

    @test fun CreatesNestedGroups() {
        // Group 1
        // ---- Group 2
        // -------- Group 4
        // Group 3
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(group1, "Group2")
        val group3 = tree.addGroupNode(null, "Group3")
        val group4 = tree.addGroupNode(group2, "Group4")

        var ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)
        assert( group4.parent == group2)

        workspace.undoEngine.undo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)

        workspace.undoEngine.undo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( !ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)

        workspace.undoEngine.redo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)

        workspace.undoEngine.redo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)
        assert( group4.parent == group2)
    }

    @test fun DeletesInNestedGroups() {
        // Group 1
        // ---- Group 2
        // -------- Group 4
        // Group 3
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(group1, "Group2")
        val group3 = tree.addGroupNode(null, "Group3")
        val group4 = tree.addGroupNode(group2, "Group4")

        group2.delete()

        var ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( !ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group3.parent == tree.root)

        workspace.undoEngine.undo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)
        assert( group4.parent == group2)

        workspace.undoEngine.redo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( !ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group3.parent == tree.root)
    }

    @test fun MovesAbove() {
        // Group 1
        // ---- Layer1
        // ---- Layer2
        // ---- Layer3
        // Group 2
        // ---- Layer4
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(null, "Group2")
        val layer1 = tree.addNewSimpleLayer(group1, "Layer1",FLAT,30,30)
        val layer2 = tree.addNewSimpleLayer(group1, "Layer2",FLAT,30,30)
        val layer3 = tree.addNewSimpleLayer(group1, "Layer3",FLAT,30,30)
        val layer4 = tree.addNewSimpleLayer(group2, "Layer4",FLAT,30,30)

        assertEquals(3, group1.getLayerNodes().count())

        // Group 1
        // ---- Layer1
        // ---- Layer4
        // ---- Layer2
        // ---- Layer3
        // Group 2
        tree.moveAbove(layer4, layer2)
        assertEquals(layer4.previousNode, layer1)
        assertEquals(layer4.nextNode, layer2)
        assertEquals(4, group1.getLayerNodes().count())

        // Layer 2
        // Group 1
        // ---- Layer1
        // ---- Layer4
        // ---- Layer3
        // Group 2
        tree.moveAbove(layer2, group1)
        assertEquals(group1.previousNode, layer2)
        assertEquals(3, group1.getLayerNodes().count())

        workspace.undoEngine.undo()
        assertEquals(layer4.previousNode, layer1)
        assertEquals(layer4.nextNode, layer2)
        assertEquals(4, group1.getLayerNodes().count())

        workspace.undoEngine.redo()
        assertEquals(group1.previousNode, layer2)
        assertEquals(3, group1.getLayerNodes().count())
    }

    @test fun MoveBelow() {
        // Group 1
        // ---- Layer1
        // ---- Layer2
        // ---- Layer3
        // Group 2
        // ---- Layer4
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(null, "Group2")
        val layer1 = tree.addNewSimpleLayer(group1, "Layer1",FLAT,30,30)
        val layer2 = tree.addNewSimpleLayer(group1, "Layer2",FLAT,30,30)
        val layer3 = tree.addNewSimpleLayer(group1, "Layer3",FLAT,30,30)
        val layer4 = tree.addNewSimpleLayer(group2, "Layer4",FLAT,30,30)

        // Group 1
        // ---- Layer1
        // ---- Layer2
        // ---- Layer4
        // ---- Layer3
        // Group 2
        tree.moveBelow(layer4, layer2)
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(layer2, layer4.previousNode)
        assertEquals(layer3, layer4.nextNode)
    }

    @test fun MoveInto() {
        // Group 1
        // ---- Layer1
        // ---- Layer2
        // ---- Layer3
        // Group 2
        // ---- Layer4
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(null, "Group2")
        val layer1 = tree.addNewSimpleLayer(group1, "Layer1",FLAT,30,30)
        val layer2 = tree.addNewSimpleLayer(group1, "Layer2",FLAT,30,30)
        val layer3 = tree.addNewSimpleLayer(group1, "Layer3",FLAT,30,30)
        val layer4 = tree.addNewSimpleLayer(group2, "Layer4",FLAT,30,30)

        // Group 1
        // ---- Layer1
        // ---- Layer2
        // ---- Layer3
        // ---- Layer4
        // Group 2
        tree.moveInto(layer4, group1, false)
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(layer3, layer4.previousNode)
        assertEquals(null, layer4.nextNode)

        // Group 1
        // ---- Layer3
        // ---- Layer1
        // ---- Layer2
        // ---- Layer4
        // Group 2
        tree.moveInto(layer3, group1, true)
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(null, layer3.previousNode)
        assertEquals(layer1, layer3.nextNode)

        // Group 1
        // ---- Layer3
        // ---- Layer1
        // ---- Layer4
        // Group 2
        // ---- Layer2
        tree.moveInto(layer2, group2, true)
        assertEquals(3, group1.children.size)
        assertEquals(1, group2.children.size)
        assertEquals(null, layer2.previousNode)
        assertEquals(null, layer2.nextNode)

        // region Undo and Redo
        workspace.undoEngine.undo()
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(null, layer3.previousNode)
        assertEquals(layer1, layer3.nextNode)

        workspace.undoEngine.undo()
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(layer3, layer4.previousNode)
        assertEquals(null, layer4.nextNode)

        workspace.undoEngine.redo()
        assertEquals(4, group1.children.size)
        assertEquals(0, group2.children.size)
        assertEquals(null, layer3.previousNode)
        assertEquals(layer1, layer3.nextNode)

        workspace.undoEngine.redo()
        assertEquals(3, group1.children.size)
        assertEquals(1, group2.children.size)
        assertEquals(null, layer2.previousNode)
        assertEquals(null, layer2.nextNode)
        // endregion
    }

    @test fun DuplicateLayerNode() {
        val layer1 = tree.addNewSimpleLayer(null, "Layer1",FLAT,30,30)

        tree.duplicateInto(layer1)

        assertEquals(2, tree.root.children.size)
        assertEquals(layer1, tree.root.children.last())
        workspace.undoEngine.undo()
        assertEquals(1, tree.root.children.size)
        assertEquals(layer1, tree.root.children.last())
        workspace.undoEngine.redo()
        assertEquals(2, tree.root.children.size)
        assertEquals(layer1, tree.root.children.last())
    }

    @test fun DuplicateGroupNode() {
        // Layer0
        // Group
        // ---- Layer1
        // ---- SubGroup
        // -------- Layer2
        // ---- Layer3
        val layer0 = tree.addNewSimpleLayer(null, "Layer0",FLAT,30,30)
        val group = tree.addGroupNode(null, "Group")
        val layer1 = tree.addNewSimpleLayer(group, "Layer1",FLAT,30,30)
        val subGroup = tree.addGroupNode(group, "Subgroup")
        val layer2 = tree.addNewSimpleLayer(subGroup, "Layer2",FLAT,30,30)
        val layer3 = tree.addNewSimpleLayer(group, "Layer3",FLAT,30,30)

        tree.duplicateInto(group)

        assertEquals(3, tree.root.children.size)
        assertEquals(7, tree.root.getLayerNodes().count())
        assertEquals( 6, tree.root.getAllAncestors().filter { it.depth == 2 }.count())
        assertEquals( 2, tree.root.getAllAncestors().filter { it.depth == 3 }.count())
        workspace.undoEngine.undo()
        assertEquals(2, tree.root.children.size)
        assertEquals(4, tree.root.getLayerNodes().count())
        assertEquals( 3, tree.root.getAllAncestors().filter { it.depth == 2 }.count())
        assertEquals( 1, tree.root.getAllAncestors().filter { it.depth == 3 }.count())
        workspace.undoEngine.redo()
        assertEquals(3, tree.root.children.size)
        assertEquals(7, tree.root.getLayerNodes().count())
        assertEquals( 6, tree.root.getAllAncestors().filter { it.depth == 2 }.count())
        assertEquals( 2, tree.root.getAllAncestors().filter { it.depth == 3 }.count())
        val layerMediums = tree.root.getLayerNodes().fold(mutableListOf<MediumHandle>(),
                {agg, it -> agg.apply { this.addAll(it.imageDependencies)}})
        assertEquals( layerMediums.count(), layerMediums.distinct().count())
    }
}