package rb.vectrix.intersect

import rb.vectrix.mathUtil.MathUtil


infix fun CollisionPoint.intersectsPrecise(obj: CollisionObject) : Boolean = when(obj) {
    is CollisionPoint -> obj.x == x && obj.y == y
    is CollisionLineSegment -> LineSegmentCollision.withPoint(obj.lineSegment, x,y) != null
    is CollisionRigidRect -> obj.rect.contains(x,y)
    is CollisionRayRect -> RayRectCollision.withPoint(obj.rayRect, x, y) != null
    is CollisionCircle -> MathUtil.distance(x,y,obj.circle.x, obj.circle.y) <= obj.circle.r
    is CollisionArc -> ArcCollision.withPoint(obj.arc, x,y) != null
    is CollisionParabola -> ParabolaCollision.withPoint(obj.parabola, x, y)
    is CollisionPolygon -> PolygonCollision.withPoint(obj.polygon, x, y)
    is CollisionMultiObj -> obj.objs.asSequence().any { this intersectsPrecise it }
}