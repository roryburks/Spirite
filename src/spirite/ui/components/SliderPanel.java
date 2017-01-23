package spirite.ui.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import spirite.ui.UIUtil;

public class SliderPanel extends JPanel {
	private float value = 0.0f;
	private float min = 0.0f;
	private float max = 1.0f;
	private String label = "";
	protected boolean hardCapped = true;
	
	public SliderPanel() {
		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setValue( widthToValue( e.getX() / (float)getWidth()));
				super.mousePressed(e);
			}
			@Override
			public void mouseDragged(MouseEvent e) {
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
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		Paint oldP = g2.getPaint();
		Paint newP = new GradientPaint( 0,0, new Color(64,64,64), getWidth(), 0, new Color( 128,128,128));
		g2.setPaint(newP);
		g2.fillRect(0, 0, getWidth(), getHeight());

		newP = new GradientPaint( 0,0, new Color(120,120,190), 0, getHeight(), new Color( 90,90,160));
		g2.setPaint(newP);
		g2.fillRect( 0, 0, Math.round(getWidth()*valueToWidth(value)), getHeight());
		
		g2.setColor( new Color( 222,222,222));
		
		UIUtil.drawStringCenter(g2, label + valueAsString(value), getBounds());

		g2.setPaint(oldP);
		g2.setColor( Color.BLACK);
		g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
	}
	
}