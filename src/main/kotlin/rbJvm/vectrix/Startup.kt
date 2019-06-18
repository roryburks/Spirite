package rbJvm.vectrix

import rb.vectrix.VectrixMathLayer

fun SetupVectrixForJvm() {
    VectrixMathLayer.mathLayer = JvmMathLayer
}