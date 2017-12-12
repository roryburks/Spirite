package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.linear.Rect;
import spirite.pc.ui.panel_work.WorkPanel.View;

public class CroppingBehavior extends DrawnStateBehavior {
		public CroppingBehavior(Penner penner) {
			super(penner);
		}

		boolean building = false;
		boolean modifying = false;
		Rect cropSection = null;
		Rect middle;
		Rect topRight;
		Rect topLeft;
		Rect bottomRight;
		Rect bottomLeft;
		int startx, starty;
		//	0x1 : Top
		//	0x2 : Bottom
		//	0x4 : Left
		//	0x8 : Right
		byte cardinalMap = 0x00;

		static final byte TOPMASK = 0x01;
		static final byte BOTTOMMASK = 0x02;
		static final byte LEFTMASK = 0x04;
		static final byte RIGHTMASK = 0x08;
//		Rectangle 
		
		private void buildCrop( ) {
			middle = MUtil.scaleRect( cropSection, 0.6f);
			topLeft = MUtil.scaleRect( cropSection, 0.2f);
			topLeft.x = cropSection.x;
			topLeft.y = cropSection.y;
			topRight = new Rect(topLeft);
			topRight.x = cropSection.x + cropSection.width - topRight.width;
			topRight.y = cropSection.y;
			bottomLeft = new Rect(topLeft);
			bottomLeft.x = cropSection.x;
			bottomLeft.y = cropSection.y + cropSection.height - bottomLeft.height;
			bottomRight = new Rect(topLeft);
			bottomRight.x = cropSection.x + cropSection.width - bottomRight.width;
			bottomRight.y = cropSection.y + cropSection.height - bottomRight.height;
		}

		@Override
		public void start() {
			building = true;
			startx = this.penner.x;
			starty = this.penner.y;
			cropSection = new Rect( this.penner.x, this.penner.y, 0, 0);
		}

		@Override
		public void onPenUp() {
	
			ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.CROP);
	
			cardinalMap = 0;


			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, this.penner.x, this.penner.y);
				if( (Boolean)settings.getValue("quickCrop")) {
					this.penner.workspace.cropNode(
						this.penner.workspace.getSelectedNode(), 
						cropSection,
						(Boolean)settings.getValue("shrinkOnly"));
					end();
				}
				else
					buildCrop();
				
				building = false;
			}
		}
		
		@Override public void onTock() {}

		@Override
		public void onMove() {

			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, this.penner.x, this.penner.y);
			}
			else if( modifying) {
				if( (cardinalMap & TOPMASK) != 0 ) {
					cropSection.setY(cropSection.getY() + this.penner.y - this.penner.oldY);
					cropSection.setHeight(cropSection.getHeight() - (this.penner.y - this.penner.oldY));
				}
				if( (cardinalMap & BOTTOMMASK) != 0) {
					cropSection.setHeight(cropSection.getHeight() + (this.penner.y - this.penner.oldY));
				}
				if( (cardinalMap & LEFTMASK) != 0) {
					cropSection.setX(cropSection.getX() + (this.penner.x - this.penner.oldX));
					cropSection.setWidth(cropSection.getWidth() - (this.penner.x - this.penner.oldX));
				}
				if( (cardinalMap & RIGHTMASK)!= 0) {
					cropSection.setWidth(cropSection.getWidth() + (this.penner.x - this.penner.oldX));
				}
				buildCrop();
			}
		}
		
		@Override
		public void onPenDown() {
			if( this.penner.toolsetManager.getSelectedTool() != Tool.CROP) { end(); return;}
			
			if( cropSection == null || !cropSection.contains(this.penner.x, this.penner.y)) {
				building = true;
				startx = this.penner.x;
				starty = this.penner.y;
			}
			else {
				cardinalMap = 0;
				
				if( middle.contains( this.penner.x, this.penner.y)) {
					this.penner.workspace.cropNode(
							this.penner.workspace.getSelectedNode(), 
							cropSection,
							(Boolean)this.penner.toolsetManager.getToolSettings(Tool.CROP).getValue("shrinkOnly"));
					
					end();
				}
				else if( topRight.contains(this.penner.x, this.penner.y)) 
					cardinalMap = TOPMASK | RIGHTMASK;
				else if( topLeft.contains(this.penner.x,this.penner.y))
					cardinalMap = TOPMASK | LEFTMASK;
				else if( bottomLeft.contains(this.penner.x,this.penner.y))
					cardinalMap = BOTTOMMASK| LEFTMASK;
				else if( bottomRight.contains(this.penner.x,this.penner.y))
					cardinalMap = BOTTOMMASK | RIGHTMASK;
				
				if( cardinalMap != 0)
					modifying = true;
			}
		}

		@Override
		public void paintOverlay(GraphicsContext gc) {
			
			// Outline
/*            Stroke new_stroke = new BasicStroke(
            		1, 
            		BasicStroke.CAP_BUTT, 
            		BasicStroke.JOIN_BEVEL, 
            		0, 
            		new float[]{8,4}, 0);
            g2.setStroke(new_stroke);*/
            
            View view = this.penner.context.getCurrentView();
            Rect r = view.itsRm(cropSection);
			gc.setColor(Colors.BLACK);
            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
			

            // Grey area outside
//			Composite c = gc.getComposite();
			int x1 = view.itsXm(0);
			int y1 = view.itsYm(0);
			int x2 = view.itsXm(this.penner.workspace.getWidth());
			int y2 = view.itsYm(this.penner.workspace.getHeight());

			if( r.getX() < x1) { r.setWidth(r.getWidth() - (x1 - r.getX())); r.setX(x1);}
			if( r.getX() + r.getWidth() > x2) { r.setWidth(x2 - r.getX());}

			gc.setColor(Colors.YELLOW);
			gc.setComposite(Composite.SRC_OVER, 0.4f);
			gc.fillRect( x1, y1, r.getX() - x1 - 1, y2-y1);
			gc.fillRect( r.getX() -1, y1, r.getWidth() +2, r.getY() - y1 - 1);
			gc.fillRect( r.getX() -1, r.getY() + r.getHeight() +1, r.getWidth() +2, y2 - (r.getHeight() + r.getY()) + 1);
			gc.fillRect( r.getX() + r.getWidth() +1,  y1, x2 - (r.getWidth() + r.getX())+1, y2-y1);
			
			// The various inner rectangles represenging the modification points
			if( !building) {
//				gc.setStroke(new BasicStroke(2.0f));
				
				if( middle.contains(this.penner.x,this.penner.y)) {
					r = view.itsRm( middle);
					gc.setColor(Colors.YELLOW);
		            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
				}

				if( topRight.contains(this.penner.x,this.penner.y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(topRight);
	            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());

				if( topLeft.contains(this.penner.x,this.penner.y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(topLeft);
	            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());

				if( bottomLeft.contains(this.penner.x,this.penner.y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(bottomLeft);
	            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());

				if( bottomRight.contains(this.penner.x,this.penner.y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(bottomRight);
	            gc.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
			}

			gc.setComposite(Composite.SRC_OVER, 1.0f);
//    		gc.setComposite(c);
//    		gc.setStroke(s);
		}
	}