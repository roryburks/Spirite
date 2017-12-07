package spirite.base.image_data.mediums.maglev.parts;

import spirite.base.brains.tools.ToolSchemes.PenDrawMode;
import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.maglev.AMagLevThing;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.IndexedDrawPoints;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.base.util.interpolation.Interpolator2D;

import java.util.Arrays;

public class MagLevStroke extends AMagLevThing {
	public final PenState[] states;
	public final StrokeParams params;
	IndexedDrawPoints direct;

	public MagLevStroke( PenState[] states, StrokeParams params, int id) {
		super(id);
		
		this.states = states;
		this.params = params;
		
		Interpolator2D interpolator;
		switch( params.getInterpolationMethod()){
		case CUBIC_SPLINE:
			interpolator = new CubicSplineInterpolator2D(null, true);
			break;
		default:	
			interpolator = null;
			break;
		}
		if( interpolator != null) {
			for( PenState ps : states)
				interpolator.addPoint(ps.x, ps.y);
		}
		direct = StrokeEngine.Companion.buildIndexedPoints(interpolator, Arrays.asList(states), params);
	}
	public MagLevStroke( PenState[] states, StrokeParams params) {
		this(states,params,-1);
	}
	
	// ===========
	// ==== Access of Custom Data
	public IndexedDrawPoints getDirect() {return direct;}
	public PenState[] getPenstates() {return states;}

	// =============
	// ==== Abstract Implementation
	@Override
	protected MagLevStroke _clone() {
		PenState[] newStates = new PenState[states.length];
		for( int i=0; i < states.length; ++i) {
			newStates[i] = new PenState( states[i]);
		}
		
		return new MagLevStroke(newStates, params);
	}

	@Override
	public void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context) {
		_draw(built,mask,gc,context,false);
	}
	public void _draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context, boolean behind) {
		ImageWorkspace workspace = context.getContext();
		StrokeEngine _engine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
		if( behind)
			params.setMode(PenDrawMode.BEHIND);
		_engine.batchDraw(params, states, built, mask);
		if( behind)
			params.setMode(PenDrawMode.NORMAL);
	}

	@Override
	public float[] getPoints() {
		float[] data = new float[states.length*2];
		for( int i=0; i<states.length;++i) {
			data[i*2] = states[i].x;
			data[i*2+1] = states[i].y;
		}
		return data;
	}

	@Override
	public void setPoints(float[] xy) {
		for( int i=0; i<states.length; ++i) {
			states[i] = new PenState(xy[i*2], xy[i*2+1], states[i].pressure);
		}
		Interpolator2D interpolator;
		switch( params.getInterpolationMethod()){
		case CUBIC_SPLINE:
			interpolator = new CubicSplineInterpolator2D(null, true);
			break;
		default:
			interpolator = null;
			break;
		}
		if( interpolator != null) {
			for( PenState ps : states)
				interpolator.addPoint(ps.x, ps.y);
		}
		direct = StrokeEngine.Companion.buildIndexedPoints(interpolator, Arrays.asList(states), params);
	}
}