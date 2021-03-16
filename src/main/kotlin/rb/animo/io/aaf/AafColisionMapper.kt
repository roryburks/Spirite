package rb.animo.io.aaf

import rb.vectrix.intersect.*
import rb.vectrix.linear.Vec2d
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
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

    fun mapFromVectrix( col : CollisionObject) : AafFCollisionKind = when(col) {
        is CollisionPoint -> AafFColPoint(col.x.f, col.y.f)
        is CollisionRigidRect -> AafFColRigidRect(col.rect.x1.f, col.rect.y1.f, col.rect.w.f, col.rect.h.f)
        is CollisionCircle -> AafFColCircle(col.circle.x.f, col.circle.y.f, col.circle.r.f)
        is CollisionArc -> AafFColArc(col.arc.x.f, col.arc.y.f, col.arc.r.f, col.arc.angleStart.f, col.arc.angleEnd.f)
        is CollisionLineSegment -> AafFColLineSegment(col.lineSegment.x1.f, col.lineSegment.y1.f, col.lineSegment.x2.f, col.lineSegment.y2.f)
        is CollisionRayRect -> AafFColRayRect(col.rayRect.x.f, col.rayRect.y.f, col.rayRect.h.f, col.rayRect.len.f, col.rayRect.theta.f)
        is CollisionPolygon -> AafFColPoly(col.polygon.vertices.map { AafFColPoint(it.x.f, it.y.f) })
        else -> throw Exception("Could Not Convert $col")
    }
}