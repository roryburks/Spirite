package spirite.dialogs;

import javax.swing.JDialog;

import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.SettingsManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;

import javax.swing.border.BevelBorder;

import mutil.Interpolation.CubicSplineInterpolator;
import mutil.Interpolation.CubicSplineInterpolator2D;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

public class TabletDialog extends JDialog
{
	private final String RAW_LABEL = "Raw Pressure";
	private final String EFFECTIVE_LABEL = "Effective Pressure";
	
	private final MasterControl master;
	private final SettingsManager settings;
	
//	private final JPanel curvePanel = new JPanel();
	private final StrokeCurvePanel curvePanel = new StrokeCurvePanel();
	private final JButton btnResetCurve = new JButton("Reset Curve");



	TabletDialog( MasterControl master) {
		this.master = master;
		this.settings = master.getSettingsManager();
		
		CubicSplineInterpolator csi = settings.getTabletInterpolator();
		
		for( int i=0; i < csi.getNumPoints(); ++i) {
			weights.add(new Point2D.Double(csi.getX(i), csi.getY(i)));
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
				weights.add(new Point2D.Double(0,0));
				weights.add(new Point2D.Double(1,1));
				saveWeights();
				curvePanel.repaint();
			}
		});
	}
	
	private void saveWeights() {
		settings.setTabletInterpolationPoints(weights);
	}
	

	private final List<Point2D> weights = new ArrayList<>();
	
	// ===============
	// ==== Custom Components
	private final Color bg = Color.WHITE;
	private class StrokeCurvePanel extends JPanel 
	{
		private Point2D movingPoint = null;
		
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
			List<Point2D> points = new ArrayList<>(weights.size());
			for( int i=0; i < weights.size(); ++i) {
				Point2D p2 = weights.get(i);
				
				// Comparison basically says "If the point is the point you're
				//	moving and it's been draged left of the previous point or
				//	right of the next point, don't display it."
				if( p2 != movingPoint || 
					!((i > 0 && movingPoint.getX() < weights.get(i-1).getX()) ||
					 ( i < weights.size()-1 && movingPoint.getX() > weights.get(i+1).getX())))
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
				ny = (int)Math.round(h*(1-MUtil.clip(0, csi.eval(0.01*i), 1)))+y1;
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
				Point2D p = csi2.eval(t);
				
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
			for( Point2D p : weights) {
				int dx = x1+(int)Math.round(w*(p.getX()));
				int dy = y1+(int)Math.round(h*(1-p.getY()));
				g.drawOval(dx-DWIDTH, dy-DWIDTH, DWIDTH*2, DWIDTH*2);
			}
		}
		
		public void screenToLogic(Point2D p) {
			int width = getWidth();
			int height = getHeight();
			int x1 = DWIDTH;
			int y1 = DWIDTH;
			int w = width-(DWIDTH*2);
			int h = height-(DWIDTH*2);
			
			p.setLocation( MUtil.clip(0, (p.getX()-x1)/w, 1), 
				MUtil.clip(0, 1-(p.getY()-y1)/h, 1));
		}
		public void logicToScreen(Point2D p){
			int width = getWidth();
			int height = getHeight();
			int x1 = DWIDTH;
			int y1 = DWIDTH;
			int w = width-(DWIDTH*2);
			int h = height-(DWIDTH*2);
			
			p.setLocation( p.getX()*w+x1,  1-p.getY()*h+y1);
		}

		class SCPAdapter extends MouseAdapter {
			@Override
			public void mousePressed(MouseEvent evt){
				super.mousePressed(evt);
				
				
				Point2D p = new Point2D.Double(evt.getX(), evt.getY());
				screenToLogic(p);
				determineMovingMethod(p);
				repaint();
			}
			
			private void determineMovingMethod( Point2D p) {
				for( Point2D weight : weights) {
					if( MUtil.distance(p.getX(), p.getY(), weight.getX(), weight.getY()) < THRESH) {
						movingPoint = weight;
						return;
					}
				}
				movingPoint = p;
				weights.add(p);
				weights.sort(new Comparator<Point2D>() {
					@Override
					public int compare(Point2D o1, Point2D o2) {
						double d = o1.getX() - o2.getX();
						return (int) Math.signum(d);
					}
				});
			}
			
			@Override
			public void mouseDragged(MouseEvent evt) {
				super.mouseDragged(evt);
				
				if( movingPoint != null) {
					movingPoint.setLocation(evt.getX(), evt.getY());
					screenToLogic(movingPoint);
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
					if( i > 0 && movingPoint.getX() < weights.get(i-1).getX())
						weights.remove(movingPoint);
					else if( i < weights.size()-1 && movingPoint.getX() > weights.get(i+1).getX())
						weights.remove(movingPoint);
					
					movingPoint = null;
					saveWeights();
					repaint();
				}
			}
		}
	}
}
