package spirite.pc.graphics.awt

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.graphics.LineAttributes
import spirite.base.graphics.RenderProperties
import spirite.base.imageData.MediumHandle
import spirite.base.util.Color
import spirite.base.util.linear.Transform
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.image.BufferedImage

class AWTGraphicsContext : GraphicsContext {
    override var color: Color
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override val width: Int
    override val height: Int
    val g2 : Graphics2D

    constructor( g : Graphics, width: Int, height: Int) {
        this.width = width
        this.height = height
        this.g2 = g as Graphics2D
    }
    constructor( bi: BufferedImage) {
        width = bi.width
        height = bi.height
        g2 = bi.graphics as Graphics2D
    }

    override var transform: Transform
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override val alpha: Float
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val composite: Composite
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override var lineAttributes: LineAttributes
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun drawBounds(image: IImage, c: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun preTranslate(offsetX: Double, offsetY: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun translate(offsetX: Double, offsetY: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun preTransform(trans: Transform) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transform(trans: Transform) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun preScale(sx: Double, sy: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun scale(sx: Double, sy: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setComposite(composite: Composite, alpha: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawPolyLine(x: IntArray, y: IntArray, count: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(shape: Shape) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fillRect(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fillOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawImage(img: IImage, x: Int, y: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawHandle(handle: MediumHandle, x: Int, y: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fillPolygon(x: List<Float>, y: List<Float>, length: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setClip(i: Int, j: Int, width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderProperties) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun renderHandle(handle: MediumHandle, x: Int, y: Int, render: RenderProperties) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}