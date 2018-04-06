package spirite.base.pen.behaviors

import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.pen.Penner
import spirite.base.util.f


class MovingNodeBehavior(
        penner: Penner,
        val node: Node)
    : PennerBehavior(penner)
{
    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        if( penner.oldX != penner.x || penner.oldY != penner.y) {
            node.x += penner.x - penner.oldX
            node.y += penner.y - penner.oldY
        }
    }
}