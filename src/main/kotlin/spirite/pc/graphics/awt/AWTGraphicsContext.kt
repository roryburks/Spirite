//package spirite.pc.graphics.awt
//
//import rb.glow.GraphicsContext
//import rb.glow.IImage
//import rb.glow.LineAttributes
//import rb.glow.gle.RenderRubric
//import rb.glow.color.Color
//import rb.vectrix.linear.ITransformF
//import java.awt.Graphics
//import java.awt.Graphics2D
//import java.awt.Shape
//import java.awt.image.BufferedImage
//
//class AWTGraphicsContext : GraphicsContext {
//    override var alpha: Float
//        get() = TODO("not implemented")
//        set(scroll) {}
//    override var composite: Composite
//        get() = TODO("not implemented")
//        set(scroll) {}
//    override var jcolor: Color
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//        set(scroll) {}
//
//    override val width: Int
//    override val height: Int
//    val g2 : Graphics2D
//
//    constructor( g : Graphics, width: Int, height: Int) {
//        this.width = width
//        this.height = height
//        this.g2 = g as Graphics2D
//    }
//    constructor( bi: BufferedImage) {
//        width = bi.width
//        height = bi.height
//        g2 = bi.graphics as Graphics2D
//    }
//
//    override var transform: ITransformF
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//        set(scroll) {}
//    override var lineAttributes: LineAttributes
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//        set(scroll) {}
//
//    override fun drawBounds(image: IImage, c: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun clear() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun preTranslate(offsetX: Float, offsetY: Float) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun translate(offsetX: Float, offsetY: Float) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun preTransform(trans: ITransformF) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun transform(trans: ITransformF) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun preScale(sx: Float, sy: Float) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun scale(sx: Float, sy: Float) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//    override fun drawRect(xi: Int, yi: Int, wf: Int, h: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun drawOval(xi: Int, yi: Int, wf: Int, h: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun drawPolyLine(xi: IntArray, yi: IntArray, count: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun draw(shape: Shape) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun fillRect(xi: Int, yi: Int, wf: Int, h: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun fillOval(xi: Int, yi: Int, wf: Int, h: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun fillPolygon(xi: List<Float>, yi: List<Float>, length: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun setClip(i: Int, j: Int, width: Int, height: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun dispose() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun renderImage(rawImage: IImage, xi: Int, yi: Int, render: RenderRubric) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//}