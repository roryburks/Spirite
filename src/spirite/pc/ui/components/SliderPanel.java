package spirite.pc.ui.components;

import spirite.gui.hybrid.SPanel;
import spirite.pc.ui.UIUtil;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

public class SliderPanel extends SPanel {
	private float value = 0.0f;
	private float min = 0.0f;
	private float max = 1.0f;
	private String label = "";
	protected boolean hardCapped = true;
	

	public SliderPanel() {
		this(0,1);
	}
	public SliderPanel( float min, float max) {
		this.min = min;
		this.max = max;
		
		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( isEnabled())
					setValue( widthToValue( e.getX() / (float)getWidth()));
				super.mousePressed(e);
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				if( isEnabled())
					setValue( widthToValue( e.getX() / (float)getWidth()));
				super.mouseDragged(e);
			}
		};

		addMouseListener( adapter);
		addMouseMotionListener( adapter);
	}
	
	public void onValueChanged( float newValue) {
		repaint();
	}
	
	// :::: Getters/Setters
	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		if( hardCapped)
			value = Math.min( max, Math.max(min, value));
		if( this.value != value) {
			this.value = value;
			onValueChanged( value);
		}
	}

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		this.min = min;
		if( hardCapped)
			value = Math.max(min, value);
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		this.max = max;
		if( hardCapped)
			value = Math.max(min, value);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		if( label == null) label = "";
		this.label = label;
	}
	
	// :::: Determine how it's drawn
	protected String valueAsString(float value) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		return df.format(value);
	}
	
	protected float valueToWidth(float value) {
		return Math.max(0.0f, Math.min(1.0f, (value - min) / (max - min)));
	}
	protected float widthToValue( float portion) {
		return portion * (max-min)  + min;
	}

	Color bgGradLeft = new Color(64,64,64);
	Color bgGradRight = new Color( 128,128,128);
	Color fgGradLeft =  new Color(120,120,190);
	Color fgGradRight =  new Color( 90,90,160);
	Color disabledGradLeft = new Color(120,120,120);
	Color disabledGradRight = new Color(160,160,160);
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		
		Paint oldP = g2.getPaint();
		Paint newP = new GradientPaint( 0,0,bgGradLeft, getWidth(), 0, bgGradRight);
		g2.setPaint(newP);
		g2.fillRect(0, 0, getWidth(), getHeight());

		if( isEnabled())
			newP = new GradientPaint( 0,0, fgGradLeft, 0, getHeight(), fgGradRight);
		else
			newP = new GradientPaint( 0,0, disabledGradLeft, 0, getHeight(), disabledGradRight);
			
		g2.setPaint(newP);
		g2.fillRect( 0, 0, Math.round(getWidth()*valueToWidth(value)), getHeight());
		
		g2.setColor( new Color( 222,222,222));
		
		UIUtil.drawStringCenter(g2, label + valueAsString(value), getBounds());

		g2.setPaint(oldP);
		g2.setColor( Color.BLACK);
		g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
	}
	
}