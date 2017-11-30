package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.base.brains.ToolsetManager.MToolsetObserver
import spirite.base.brains.ToolsetManager
import spirite.base.graphics.GraphicsContext
import spirite.base.util.compaction.FloatCompactor
import spirite.base.image_data.mediums.drawer.IImageDrawer.IBoneDrawer
import spirite.base.util.Colors
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.graphics.GraphicsContext.CapMethod
import spirite.base.graphics.GraphicsContext.JoinMethod
import spirite.base.graphics.GraphicsContext.LineAttributes
import spirite.base.util.interpolation.CubicSplineInterpolator2D
import spirite.base.util.linear.Vec2
import spirite.hybrid.HybridHelper
import spirite.base.brains.ToolsetManager.Tool
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone
import spirite.base.brains.tools.ToolSchemes.BoneStretchMode
import spirite.base.util.MUtil
import spirite.base.util.linear.MatTrans

class BoneContortionBehavior(penner: Penner, val drawer : IBoneDrawer) : DrawnStateBehavior(penner), MToolsetObserver
{
	init {
		penner.toolsetManager.addToolsetObserver(this)
	}
	
	enum class State {
		MAKING,
		BETWEEN,
		DEFORMING,
		PROPOSING,
		MOVING_PROP,
	}
	
	var state = State.MAKING
	
	var x1 = 0f
	var y1 = 0f
	var x2 = 0f
	var y2 = 0f
	var startX = 0
	var startY = 0
	
	lateinit var fx : FloatCompactor
	lateinit var fy : FloatCompactor
	var proposing : CubicSplineInterpolator2D? = null
	lateinit var proposedPoints : List<Vec2>
	var baseProposing : CubicSplineInterpolator2D? = null
	lateinit var baseProposedPoints : List<Vec2>
	
	override fun paintOverlay(gc: GraphicsContext) {
		gc.transform(penner.view.viewTransform)
		gc.setColor(Colors.GRAY)
		
		gc.setComposite(Composite.SRC_OVER, 1.0f)
		gc.setColor(Colors.WHITE)
		gc.lineAttributes = LineAttributes(3f, CapMethod.SQUARE, JoinMethod.MITER)
		gc.drawLine(x1, y1, x2, y2)
		
		
		if( state == State.DEFORMING) {
			gc.setColor( Colors.BLACK);
			gc.drawPolyLine(fx.toArray(), fy.toArray(), fx.size())
		}
		if( proposing != null) {
			gc.setColor( Colors.BLACK)
			
			val dx = FloatCompactor()
			val dy = FloatCompactor()
			var t = 0f
			while( t < proposing!!.getCurveLength()) {
				var dp = proposing!!.eval(t)
				dx.add(dp.x)
				dy.add(dp.y)
				t+= 5f
			}
			
			gc.drawPolyLine(dx.toArray(), dy.toArray(), dx.size())
			
			gc.setColor(Colors.RED)
			gc.lineAttributes = LineAttributes(1f, CapMethod.SQUARE, JoinMethod.MITER)
			for( point in proposedPoints)
				gc.drawOval(point.x.toInt()-2, point.y.toInt()-2, 4, 4)
		}
	}
	override fun start() {
		x1 = penner.x.toFloat()
		y1 = penner.y.toFloat()
	}
	
	override fun end() {
		HybridHelper.queueToRun { penner.toolsetManager.removeToolsetObserver(this) }
		super.end()
	}
	override fun onTock() {}
	
	override fun onPenUp() {
		when( state) {
			State.MAKING -> {
				state = State.BETWEEN
				fx = FloatCompactor()
				fy = FloatCompactor()
			}
			State.DEFORMING -> {
				val prel = CubicSplineInterpolator2D( fx.toArray(), fy.toArray(), false)
				
				var clen = prel.curveLength
				
				baseProposedPoints = (0..SUBDIVISIONS).map { prel.eval(it*clen/SUBDIVISIONS) }
				baseProposing = CubicSplineInterpolator2D(baseProposedPoints, true)
				baseProposing?.setExtrapolating(true)
				
				state = State.PROPOSING
				_doScale()
			}
			State.MOVING_PROP -> {
				if( baseProposedPoints != proposedPoints) {
					baseProposedPoints = baseProposedPoints.map{ Vec2(it.x + penner.x - startX, it.y + penner.y - startY)}
					baseProposing = CubicSplineInterpolator2D(baseProposedPoints, false)
					baseProposing?.setExtrapolating(true)
				}
			}
			else ->{}
		}
		penner.repaint()
	}
	
	override fun onPenDown() {
		when {
			state == State.BETWEEN 	-> state = State.DEFORMING
			state == State.PROPOSING && penner.holdingShift -> {
				startX = penner.x
				startY = penner.y
				state = State.MOVING_PROP
			}
			state == State.PROPOSING && !penner.holdingShift -> {
				state = State.DEFORMING
				fx = FloatCompactor()
				fy = FloatCompactor()
				proposing = null
				baseProposing = null
			}
		}
	}
	
	override fun onMove() {
		when( state) {
			State.MAKING -> {
				x2 = penner.x.toFloat()
				y2 = penner.y.toFloat()
			}
			State.DEFORMING -> {
				fx.add(penner.x.toFloat())
				fy.add(penner.y.toFloat())
			}
			State.MOVING_PROP -> {
				proposedPoints = proposedPoints.map { Vec2(it.x + penner.x - penner.oldX, it.y + penner.y - penner.oldY)}
				proposing = CubicSplineInterpolator2D( proposedPoints, false)
				proposing?.setExtrapolating(true)
			}
			else -> {}
		}
	}
	
	// :::: MToolset Observer
	override fun toolsetChanged(newTool: ToolsetManager.Tool?) {}
	override fun toolsetPropertyChanged(tool: ToolsetManager.Tool?, property: ToolsetManager.Property?) {
		if( tool != Tool.BONE)
			return;
		
		_doScale()
		
		if( property?.id == "do") {
			if( proposing != null) 
				drawer.contort(BaseBone(x1,y1,x2,y2,1f), proposing)
			end()
		}
	}

	private fun _doScale() {
		if( baseProposing == null)
			return
		
		val settings = penner.toolsetManager.getToolSettings(Tool.BONE)
		val resize = settings.getValue("resize") as Boolean
		val mode = settings.getValue("mode") as BoneStretchMode
		var leniency = settings.getValue("leniency") as Float
		
		if(!resize) {
			proposedPoints = baseProposedPoints
			proposing = baseProposing
			return
		}
		
		var boneLen = MUtil.distance(x1, y1, x2, y2)
		var curveLen = baseProposing!!.curveLength
		when {
			boneLen * (1-leniency) > curveLen 	-> boneLen *= 1 - leniency
			boneLen* (1+leniency) > curveLen	-> {
				proposing = baseProposing
				penner.repaint()
				return
			}
			else -> boneLen *= 1 + leniency
		}
		
		proposedPoints = when( mode) {
			BoneStretchMode.CLIP_EVEN -> run {
				// Note: 80% code duplicated from CLIP_HEAD
				val start = (curveLen - boneLen)/2f
				val end = curveLen - start
				(0..SUBDIVISIONS).map { baseProposing!!.eval( start + it * (end-start) / SUBDIVISIONS) }
			}
			BoneStretchMode.SCALE -> run {
				val scale = boneLen/curveLen
				
				val rect = MUtil.rectFromPoints(baseProposedPoints)
				val center = Vec2( rect.x + rect.width/2f, rect.y + rect.height/2f)
				
				val trans = MatTrans()
				trans.preTranslate(-center.x, -center.y)
				trans.preScale(scale, scale)
				trans.preTranslate(center.x, center.y)
				
				proposedPoints.map{ trans.transform(it)}
				
			}
			BoneStretchMode.SCALE_TO_BONE-> {proposedPoints}
			BoneStretchMode.CLIP_HEAD -> {
				(0..SUBDIVISIONS).map { baseProposing!!.eval( it * boneLen / SUBDIVISIONS) }
			}
			BoneStretchMode.INTELI -> {proposedPoints}
		}
		proposing = CubicSplineInterpolator2D(proposedPoints,false)
		proposing?.setExtrapolating(true)
		
		penner.repaint()
	}
}

val SUBDIVISIONS = 4