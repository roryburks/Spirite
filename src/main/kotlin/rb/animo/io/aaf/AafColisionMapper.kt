package rb.animo.io.aaf

import rb.vectrix.intersect.*
import rb.vectrix.linear.Vec2d
import rb.vectrix.mathUtil.d
import rb.vectrix.shapes.*

object AafColisionMapper {
    fun mapToVectrix( col: AafFCollisionKind) = when( col) {
        is AafFColPoint -> CollisionPoint(col.x.d, col.y.d)
        is AafFColRigidRect -> CollisionRigidRect(RectD(col.x.d, col.y.d, col.w.d, col.h.d))
        is AafFColCircle -> CollisionCircle(CircleD.Make( col.x.d, col.y.d, col.r.d))
        is AafFColArc -> CollisionArc(ArcD.Make(col.x.d, col.y.d, col.r.d, col.thStart.d, col.thEnd.d))
        is AafFColLineSegment -> CollisionLineSegment(LineSegmentD(col.x1.d, col.y1.d, col.x2.d, col.y2.d))
        is AafFColRayRect -> CollisionRayRect(RayRectD.Make(col.x.d, col.y.d, col.h.d, col.len.d, col.theta.d))
        is AafFColPoly -> CollisionPolygon(PolygonD.Make(col.points.map { Vec2d(it.x.d, it.y.d) }))
    }
}