package sjunit.file;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.junit.Test;

import sjunit.TestWrapper;
import spirite.base.brains.MasterControl;
import spirite.base.file.LoadEngine.BadSIFFFileException;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.images.DynamicInternalImage;
import spirite.base.image_data.images.IInternalImage;
import spirite.base.image_data.images.IInternalImage.InternalImageTypes;
import spirite.base.image_data.images.maglev.MaglevInternalImage;
import spirite.base.image_data.images.InternalImage;
import spirite.base.image_data.images.PrismaticInternalImage;
import spirite.base.image_data.images.PrismaticInternalImage.LImg;
import spirite.base.image_data.images.drawer.IImageDrawer.IStrokeModule;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.hybrid.HybridHelper;

public class BuildSaveLoadTest {

	@Test
	public void BasicBuildSaveLoadFileTest() throws Exception {
		final File temp;
		temp = File.createTempFile("BasicBuildSaveLoadFileTest.sif", Long.toString(System.nanoTime()));
		
		TestWrapper.performTest((MasterControl master) -> {
			ImageWorkspace ws = new ImageWorkspace(master);
			ws.finishBuilding();
			Node node1 = ws.addNewSimpleLayer( null, HybridHelper.createImage(10, 10), "simpleLayer", InternalImageTypes.NORMAL);
			
			IStrokeModule drawer = (IStrokeModule)ws.getDrawerFromNode(node1);
			drawer.startStroke(new StrokeParams(), new PenState( 0, 0, 1));
			drawer.stepStroke(new PenState( 7.5f, 7.5f, 1));
			drawer.stepStroke(new PenState( 10f, 10f, 1));
			drawer.endStroke();

			master.saveWorkspace(ws, temp);
			ImageWorkspace ws2 = master.getLoadEngine().loadWorkspace(temp);
			
			VerifyWorkspacesEquivalent( ws, ws2);
		});
		temp.delete();
	}

	@Test
	public void DynamicImgBuildSaveLoadFileTest() throws Exception {
		final File temp;
		temp = File.createTempFile("BasicBuildSaveLoadFileTest.sif", Long.toString(System.nanoTime()));
		
		TestWrapper.performTest((MasterControl master) -> {
			ImageWorkspace ws = new ImageWorkspace(master);
			ws.setWidth(20);
			ws.setHeight(20);
			ws.finishBuilding();
			Node node1 = ws.addNewSimpleLayer( null, HybridHelper.createImage(10, 10), "dynamicLayer", InternalImageTypes.DYNAMIC);
			
			IStrokeModule drawer = (IStrokeModule)ws.getDrawerFromNode(node1);
			drawer.startStroke(new StrokeParams(), new PenState( 0, 0, 1));
			drawer.stepStroke(new PenState( 75f, 75f, 1));
			drawer.stepStroke(new PenState( 100f, 100f, 1));
			drawer.endStroke();

			master.saveWorkspace(ws, temp);
			ImageWorkspace ws2 = master.getLoadEngine().loadWorkspace(temp);
			
			VerifyWorkspacesEquivalent( ws, ws2);
		});
		temp.delete();
	}
	

	@Test
	public void PrismaticImgBuildSaveLoadFileTest() throws Exception {
		final File temp;
		temp = File.createTempFile("BasicBuildSaveLoadFileTest.sif", Long.toString(System.nanoTime()));
		
		TestWrapper.performTest((MasterControl master) -> {
			ImageWorkspace ws = new ImageWorkspace(master);
			ws.setWidth(20);
			ws.setHeight(20);
			ws.finishBuilding();
			Node node1 = ws.addNewSimpleLayer( null, HybridHelper.createImage(10, 10), "prismaticLayer", InternalImageTypes.PRISMATIC);
			
			int[] colors = new int[] {0xffff0000,0xff00ff00,0xff0000ff};
			float[][][] strokes = new float[][][] {
				{{0, 0, 1}, {15, 15, 1}, {100,100,100,1}},
				{{0, 0, 1}, {5, 5, 1}, {100,100,100,1}},
				{{20, 0, 1}, {0, 20, 1}, {100,100,100,1}},
			};
			for( int i=0; i < colors.length; ++i) {
				master.getPaletteManager().setActiveColor(0, colors[i]);

				IStrokeModule drawer = (IStrokeModule)ws.getDrawerFromNode(node1);
				drawer.startStroke(new StrokeParams(), new PenState( strokes[i][0][0], strokes[i][0][1], strokes[i][0][2]));
				
				for( int j=1; j < strokes[i].length; ++j) 
					drawer.stepStroke(new PenState( strokes[i][j][0], strokes[i][j][1], strokes[i][j][2]));
				drawer.endStroke();
			}

			master.saveWorkspace(ws, temp);
			ImageWorkspace ws2 = master.getLoadEngine().loadWorkspace(temp);
			
			VerifyWorkspacesEquivalent( ws, ws2);
		});
		temp.delete();
	}
	
	private void VerifyWorkspacesEquivalent(ImageWorkspace ws1, ImageWorkspace ws2) {
		GroupNode groupRoot1 = ws1.getRootNode();
		GroupNode groupRoot2 = ws2.getRootNode();
		Stack<Node> nodeStack1 = new Stack<>();
		Stack<Node> nodeStack2 = new Stack<>();
		Map<Integer,Integer> checkedIImgMap = new HashMap<Integer,Integer>();

		nodeStack1.addAll( groupRoot1.getChildren());
		nodeStack2.addAll( groupRoot2.getChildren());
		
		while( !nodeStack1.isEmpty()) {
			Node toCheck1 = nodeStack1.pop();
			Node toCheck2 = nodeStack2.pop();

			assert( toCheck1.getName().equals(toCheck2.getName()));
			assert( toCheck1.getOffsetX() == toCheck2.getOffsetX());
			assert( toCheck1.getOffsetY() == toCheck2.getOffsetY());
			assert( toCheck1.getRender().alpha == toCheck2.getRender().alpha);
			assert( toCheck1.getRender().method == toCheck2.getRender().method);
			assert( toCheck1.getRender().renderValue == toCheck2.getRender().renderValue);
			assert( toCheck1.getRender().visible == toCheck2.getRender().visible);
			
			if( toCheck1 instanceof GroupNode) {
				assert( toCheck2 instanceof GroupNode);
				
				nodeStack1.addAll(toCheck1.getChildren());
				nodeStack2.addAll(toCheck2.getChildren());
			}else if( toCheck1 instanceof LayerNode) {
				assert( toCheck2 instanceof LayerNode);
				
				Layer layer1 = ((LayerNode)toCheck1).getLayer();
				Layer layer2 = ((LayerNode)toCheck2).getLayer();

				if( layer1 instanceof ReferenceLayer) {
					assert( layer2 instanceof ReferenceLayer);
					
					// TODO
					//((ReferenceLayer)layer1).getUnderlying();
				}else if( layer1 instanceof SimpleLayer) {
					assert( layer2 instanceof SimpleLayer);
					
					VerifyImageEquivalent( ((SimpleLayer)layer1).getData(), ((SimpleLayer)layer2).getData(), checkedIImgMap);
				}else if( layer1 instanceof SpriteLayer) {
					assert( layer2 instanceof SpriteLayer);
					
					// TODO
				}
			}
		}
		assert( nodeStack2.isEmpty());
	}
	
	private void VerifyImageEquivalent( ImageHandle img1, ImageHandle img2, Map<Integer,Integer> checkedIImgMap)
	{
		if( checkedIImgMap.containsKey(img1.getID())) {
			assert( checkedIImgMap.get(img1.getID()).equals(img2.getID()));
		}
		else {
			for( Entry<Integer,Integer> entry: checkedIImgMap.entrySet())
				assert( !entry.getValue().equals(img2.getID()));

			IInternalImage iimg1 = img1.getContext().getData(img1);
			IInternalImage iimg2 = img2.getContext().getData(img2);

			if( iimg1 instanceof InternalImage) {
				assert( iimg2 instanceof InternalImage);

				VerifyRawImages( iimg1.readOnlyAccess(), iimg2.readOnlyAccess());
			}else if( iimg1 instanceof DynamicInternalImage) {
				assert( iimg2 instanceof DynamicInternalImage);

				assert( iimg1.getDynamicX() == iimg2.getDynamicX());
				assert( iimg1.getDynamicY() == iimg2.getDynamicY());
				
				VerifyRawImages( iimg1.readOnlyAccess(), iimg2.readOnlyAccess());
			}else if( iimg1 instanceof PrismaticInternalImage) {
				assert( iimg2 instanceof PrismaticInternalImage);

				PrismaticInternalImage pii1 = (PrismaticInternalImage)iimg1;
				PrismaticInternalImage pii2 = (PrismaticInternalImage)iimg2;

				List<LImg> colors1 = pii1.getColorLayers();
				List<LImg> colors2 = pii2.getColorLayers();

				assert( colors1.size() == colors2.size());
				for( int i=0; i < colors1.size(); ++i) {
					System.out.println(colors1.get(i).color);
					assert(colors1.get(i).color == colors2.get(i).color);
					assert(colors1.get(i).ox == colors2.get(i).ox);
					assert(colors1.get(i).oy == colors2.get(i).oy);
					
					VerifyRawImages(colors1.get(i).img,colors2.get(i).img);
				}
			}else if( iimg1 instanceof MaglevInternalImage) {
				assert( iimg2 instanceof MaglevInternalImage);
				
				// TODO
			}
			
			checkedIImgMap.put( img1.getID(), img2.getID());
		}
	}
	private void VerifyRawImages( RawImage raw1, RawImage raw2) {
		assert( raw1.getWidth() == raw2.getWidth());
		assert( raw1.getHeight() == raw2.getHeight());
		
		for( int x=0; x<raw1.getWidth(); ++x) 
			for( int y=0; y<raw1.getHeight(); ++y)
				assert( raw1.getRGB(x, y) == raw2.getRGB(x, y));
	}
}
