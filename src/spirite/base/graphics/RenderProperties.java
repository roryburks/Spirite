package spirite.base.graphics;

import spirite.base.brains.renderer.RenderEngine.RenderMethod;

public class RenderProperties {
	private final Trigger trigger;
	
	public boolean visible = true;
	public float alpha = 1.0f;
	public RenderMethod method = RenderMethod.DEFAULT;
	public int renderValue = 0;

	public RenderProperties() {trigger = null;}
	public RenderProperties( Trigger trigger) {this.trigger = trigger;}
	public RenderProperties(RenderProperties other) {
		this.trigger = null;
		this.visible = other.visible;
		this.alpha = other.alpha;
		this.method = other.method;
		this.renderValue = other.renderValue;
	}
	public RenderProperties(RenderProperties other, Trigger trigger) {
		this.trigger = trigger;
		this.visible = other.visible;
		this.alpha = other.alpha;
		this.method = other.method;
		this.renderValue = other.renderValue;
	}
	
	public void directCopy( RenderProperties other) {
		this.visible = other.visible;
		this.alpha = other.alpha;
		this.method = other.method;
		this.renderValue = other.renderValue;
	}

	public boolean isVisible() {return this.visible && alpha > 0;}
	public void setVisible( boolean visible) {
		if( this.visible != visible && (trigger == null || trigger.visibilityChanged(visible)))
			this.visible = visible;
	}

	public float getAlpha() {return this.alpha;}
	public void setAlpha( float alpha) {
		if( this.alpha != alpha && (trigger == null || trigger.alphaChanged(alpha)))
			this.alpha = alpha;
	}

	public RenderMethod getMethod() {return this.method;}
	public int getRenderValue() {return this.renderValue;}
	public void setMethod( RenderMethod method, int value) {
		if( (this.method != method || this.renderValue != value) && (trigger == null || trigger.methodChanged(method,value))) {
			this.method = method;
			this.renderValue = value;
		}
	}
	
	public static interface Trigger {
		public boolean visibilityChanged( boolean newVisible);
		public boolean alphaChanged ( float newAlpha);
		public boolean methodChanged( RenderMethod newMethod, int newValue);
	}
}
