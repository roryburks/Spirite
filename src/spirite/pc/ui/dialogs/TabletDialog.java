package spirite.pc.ui.dialogs;

import spirite.base.brains.MasterControl;
import spirite.base.brains.SettingsManager;
import spirite.base.util.MUtil;
import spirite.base.util.interpolation.CubicSplineInterpolator;
import spirite.base.util.linear.Vec2;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SPanel;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TabletDialog extends JDialog
{
	private static final String RAW_LABEL = "Raw Pressure";
	private static final String EFFECTIVE_LABEL = "Effective Pressure";
	private static final String RESET_LABEL = "Reset Curve";
	
	private final SettingsManager settings;
	
//	private final SPanel curvePanel = new SPanel();
	private final StrokeCurvePanel curvePanel = new StrokeCurvePanel();
	private final SButton btnResetCurve = new SButton(RESET_LABEL);



	TabletDialog( MasterControl master) {
		this.settings = master.getSettingsManager();
		
		CubicSplineInterpolator csi = settings.getTabletInterpolator();
		
		for( int i=0; i < csi.getNumPoints(); ++i) {
			weights.add(new Vec2(csi.getX(i), csi.getY(i)));
		}
		curvePanel.setBackground(bg);
		curvePanel.setOpaque(true);
		initLayout();
		initBindings();
	}
	
	private void initLayout() {
		setBounds(100, 100, 290, 500);
		
		JLabel lblDynamics = new JLabel("Tablet Pen Pressure Dynamics");
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
							.addComponent(btnResetCurve, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(curvePanel, Alignment.LEADING, 250, 250, Short.MAX_VALUE))
						.addComponent(lblDynamics))
					.addContainerGap(14, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblDynamics)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(curvePanel, 250, 250, 250)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(btnResetCurve)
					.addContainerGap(162, Short.MAX_VALUE))
		);
		getContentPane().setLayout(groupLayout);
	}
	
	private void initBindings() {
		btnResetCurve.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				weights.clear();
				weights.add(new Vec2(0,0));
				weights.add(new Vec2(1,1));
				saveWeights();
				curvePanel.repaint();
			}
		});
	}
	
	private void saveWeights() {
		settings.setTabletInterpolationPoints(weights);
	}
	

	private final List<Vec2> weights = new ArrayList<>();
	
	// ===============
	// ==== Custom Components
	private final Color bg = Color.WHITE;
	private class StrokeCurvePanel extends SPanel 
	{
		private Vec2 movingPoint = null;
		
		private final SCPAdapter adapter = new SCPAdapter();
		
		StrokeCurvePanel() {
			this.addMouseListener(adapter);
			this.addMouseMotionListener(adapter);
		}

		public final int DWIDTH = 4;
		public final double THRESH = 0.03;
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D)g;
			
			int width = this.getWidth();
			int height = this.getHeight();
			int x1 = DWIDTH;
			int y1 = DWIDTH;
			int x2 = width-DWIDTH;
			int y2 = height-DWIDTH;
			int w = width-(DWIDTH*2);
			int h = height-(DWIDTH*2);
			
			// Draw the grid border
			g.setColor( Color.GRAY);
			int x_[] = new int[]{ x1, x2, x2, x1, x1};
			int y_[] = new int[]{ y1, y1, y2, y2, y1};
			g.drawPolyline(x_, y_, 5);
			
			// Draw the grid
			for( int i=1; i < 5; ++i) {
				g.drawLine(x1 + ((x2-x1)*i)/5, y1, x1 + ((x2-x1)*i)/5, y2);
				g.drawLine(x1, y1 + ((y2-y1)*i)/5, x2, y1 +((y2-y1)*i)/5);
			}
			
			// Draw the linear guide
			g.setColor(Color.BLACK);
			g.drawLine( x1, y2, x2, y1);
			
			// Draw the axis labels
			FontMetrics fontMetrics = g2.getFontMetrics();
			g.drawString(RAW_LABEL, x2 - fontMetrics.stringWidth(RAW_LABEL)-4, y2 - 4);
			
			AffineTransform trans = g2.getTransform();
			g2.translate(x1+4, y1+4);
			g2.rotate(Math.PI/2);
			g.drawString(EFFECTIVE_LABEL, 0, 0);
			
			g2.setTransform(trans);
			
			
			// Construct a list of Interpolation Curve points, possibly excluding the 
			//	point you're dragging (if you're dragging it off)
			List<Vec2> points = new ArrayList<>(weights.size());
			for( int i=0; i < weights.size(); ++i) {
				Vec2 p2 = weights.get(i);
				
				// Comparison basically says "If the point is the point you're
				//	moving and it's been draged left of the previous point or
				//	right of the next point, don't display it."
				if( p2 != movingPoint || 
					!((i > 0 && movingPoint.getX() <= weights.get(i - 1).getX()) ||
					 ( i < weights.size()-1 && movingPoint.getX() >= weights.get(i + 1).getX())))
				{
					points.add(p2);	
				}
			}
			

			// :::: Draw a Visual Representation of the Interpolation Curve
			g.setColor(Color.red);
			CubicSplineInterpolator csi = new CubicSplineInterpolator(points, true, true);

			g2.setStroke( new BasicStroke(1.0f));
			int ox = -999;
			int oy = -999;
			int nx, ny;
			for( int i=0; i<=100; ++i) {
				nx = (int)Math.round(w*(0.01*i))+x1;
				ny = (int)Math.round(h*(1-MUtil.clip(0, csi.eval(0.01f*i), 1)))+y1;
				if( ox != -999) {
					g.drawLine(ox, oy, nx, ny);
				}
				ox = nx;
				oy = ny;
			}
			
/*			// Draws a 2D, non-function representation of the interpolation for comparison sake
 * 			CubicSplineInterpolator2D csi2 = new CubicSplineInterpolator2D(points, true);

			g.setColor(Color.blue);
			g2.setStroke( new BasicStroke(1.0f));
			ox = -999;
			oy = -999;
			for( double t = 0; t < csi2.getCurveLength(); t += 0.01) {
				Vec2 p = csi2.eval(t);
				
				nx = (int)Math.round(w*(p.getX()))+x1;
				ny = (int)Math.round(h*(1-p.getY()))+y1;
				if( ox != -999) {
					g.drawLine(ox, oy, nx, ny);
				}
				ox = nx;
				oy = ny;
			}*/
			
			// Draw Marker Circles
			g.setColor(Color.BLACK);
			for( Vec2 p : weights) {
				int dx = x1+(int)Math.round(w*(p.getX()));
				int dy = y1+(int)Math.round(h*(1- p.getY()));
				g.drawOval(dx-DWIDTH, dy-DWIDTH, DWIDTH*2, DWIDTH*2);
			}
		}
		
		public Vec2 screenToLogic(Vec2 p) {
			int width = getWidth();
			int height = getHeight();
			int x1 = DWIDTH;
			int y1 = DWIDTH;
			int w = width-(DWIDTH*2);
			int h = height-(DWIDTH*2);
			
			return new Vec2(MUtil.clip(0, (p.getX() -x1)/w, 1),
					MUtil.clip(0, 1-(p.getY() -y1)/h, 1));
		}
		public Vec2 logicToScreen(Vec2 p){
			int width = getWidth();
			int height = getHeight();
			int x1 = DWIDTH;
			int y1 = DWIDTH;
			int w = width-(DWIDTH*2);
			int h = height-(DWIDTH*2);
			
			return new Vec2( p.getX() *w+x1, 1- p.getY() *h+y1);
		}

		class SCPAdapter extends MouseAdapter {
			@Override
			public void mousePressed(MouseEvent evt){
				super.mousePressed(evt);
				
				
				Vec2 p = new Vec2(evt.getX(), evt.getY());
				screenToLogic(p);
				determineMovingMethod(p);
				repaint();
			}
			
			private void determineMovingMethod( Vec2 p) {
				for( Vec2 weight : weights) {
					if( MUtil.distance(p.getX(), p.getY(), weight.getX(), weight.getY()) < THRESH) {
						movingPoint = weight;
						return;
					}
				}
				movingPoint = p;
				weights.add(p);
				weights.sort(new Comparator<Vec2>() {
					@Override
					public int compare(Vec2 o1, Vec2 o2) {
						double d = o1.getX() - o2.getX();
						return (int) Math.signum(d);
					}
				});
			}
			
			@Override
			public void mouseDragged(MouseEvent evt) {
				super.mouseDragged(evt);
				
				if( movingPoint != null) {
					movingPoint = screenToLogic( new Vec2(evt.getX(), evt.getY()));
					repaint();
				}
			}
			@Override
			public void mouseReleased(MouseEvent evt) {
				super.mouseReleased(evt);
				
				if( movingPoint != null) {
					int i = weights.indexOf(movingPoint);
					
					// Remove the point you're moving if you've dragged it left
					//	of the previous or right of the next point.
					if( i > 0 && movingPoint.getX() <= weights.get(i - 1).getX())
						weights.remove(movingPoint);
					else if( i < weights.size()-1 && movingPoint.getX() >= weights.get(i + 1).getX())
						weights.remove(movingPoint);
					
					movingPoint = null;
					saveWeights();
					repaint();
				}
			}
		}
	}
}
