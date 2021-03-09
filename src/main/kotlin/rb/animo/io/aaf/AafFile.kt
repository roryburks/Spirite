package rb.animo.io.aaf

data class AafFile(
    val version: Int,
    val animations : List<AafFAnimation>,
    val cels : List<AafFCel>
)

data class AafFAnimation(
    val name: String,
    val ox: Int,
    val oy: Int,
    val frames: List<AafFFrame> )

data class AafFFrame(
    val chunks: List<AafFChunk>,
    val hitboxes: List<AafFHitbox> )

data class AafFChunk(
    val group : Char,
    val celId : Int,
    val offsetX: Int,
    val offsetY: Int,
    val drawDepth: Int )

data class AafFHitbox(
    val typeId: Int,
    val col: AafFCollisionKind)

data class AafFCel(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int)

/*=== CollisionsKinds ===*/
sealed class AafFCollisionKind {}

data class AafFColPoint(
    val x: Float,
    val y: Float) : AafFCollisionKind()

data class AafFColRigidRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float) : AafFCollisionKind()

data class AafFColCircle(
    val x: Float,
    val y: Float,
    val r: Float) : AafFCollisionKind()

data class AafFColArc(
    val x: Float,
    val y: Float,
    val r: Float,
    val thStart: Float,
    val thEnd: Float) : AafFCollisionKind()

data class AafFColLineSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float) : AafFCollisionKind()

data class AafFColRayRect(
    val x: Float,
    val y: Float,
    val h: Float,
    val len: Float,
    val theta: Float) : AafFCollisionKind()

data class AafFColPoly(
    val points: List<AafFColPoint>) : AafFCollisionKind()

