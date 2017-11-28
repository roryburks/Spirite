package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.pen.Penner;
import spirite.base.util.linear.Rect;

public class RigManipulationBehavior extends TransformBehavior {
	Part selectedPart;
	final SpriteLayer sprite;
	final Node node;
	
	public RigManipulationBehavior( Penner penner, Part selectedPart, Node node) {
		super(penner);
		this.node = node;
		this.selectedPart = selectedPart;
		sprite = selectedPart.getContext();
	}
	
	@Override
	public void start() {
		this.setState(TransormStates.INACTIVE);
		
	}
	@Override
	public void paintOverlay(GraphicsContext gc) {
		if( !(getState() == TransormStates.READY && this.penner.holdingCtrl))
			super.paintOverlay(gc);
	}
	
	@Override
	public void onTock() {
		Node node = this.penner.workspace.getSelectedNode();
		
		if( this.penner.workspace.getSelectedNode() != node) 
			end();
		else {
			region = new Rect( 
					node.getOffsetX() + selectedPart.getImageHandle().getDynamicX(),
					node.getOffsetY() + selectedPart.getImageHandle().getDynamicY(),
					selectedPart.getImageHandle().getWidth(),
					selectedPart.getImageHandle().getHeight());
			translateX = selectedPart.getTranslationX();
			translateY = selectedPart.getTranslationY();
			scaleX = selectedPart.getScaleX();
			scaleY = selectedPart.getScaleY();
			rotation = selectedPart.getRotation();
		}
		
		//selectedPart.
	}

	@Override public void onPenUp() {
		setState(TransormStates.READY);
	}
	@Override
	public void onPenDown() {
		if( this.penner.holdingCtrl) {
			Part p = sprite.grabPart(this.penner.x - node.getOffsetX(), this.penner.y - node.getOffsetY(), true);
			if( p != null)
				selectedPart = p;
		}
		else {
			if( overlap >= 0 && overlap <= 7)
				this.setState(TransormStates.RESIZE);
			else if( overlap >= 8 && overlap <= 0xB)
				this.setState(TransormStates.ROTATE);
			else if( overlap == 0xC)
				this.setState(TransormStates.MOVING);
		}
	}

	@Override protected void onScaleChanged() {_perform();}
	@Override protected void onTrnalsationChanged() {_perform();}
	@Override protected void onRotationChanged() { _perform();}
	private void _perform() {
		SpriteLayer.PartStructure structure = selectedPart.getStructure();
		structure.transX = translateX;
		structure.transY = translateY;
		structure.scaleX = scaleX;
		structure.scaleY = scaleY;
		structure.rot = rotation;

		sprite.modifyPart( selectedPart, structure);
	}
}