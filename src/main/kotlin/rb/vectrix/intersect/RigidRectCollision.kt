package rb.vectrix.intersect


import rb.vectrix.shapes.Rect

infix fun Rect.intersectsPrecise(obj: CollisionObject) : Boolean = when(obj) {
    is CollisionPoint -> contains(obj.x, obj.y)
    is CollisionLineSegment -> lineSegments.any { it intersection obj.lineSegment != null }
    is CollisionRigidRect -> intersects(obj.rect)
    is CollisionRayRect -> RayRectCollision.withRigidRect(obj.rayRect, this) != null
    is CollisionCircle -> CircleCollision.withRigidRect(obj.circle, this) != null
    is CollisionArc -> ArcCollision.withRect(obj.arc, this) != null
    is CollisionParabola -> ParabolaCollision.withRect(obj.parabola, this)
    is CollisionPolygon -> PolygonCollision.withRigidRect(obj.polygon, this)
    is CollisionMultiObj -> obj.objs.asSequence().any { this intersectsPrecise it }
}