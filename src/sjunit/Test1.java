/**
 * 
 */
package sjunit;

import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import spirite.brains.MasterControl;
import spirite.file.LoadEngine;
import spirite.file.LoadEngine.BadSIFFFileException;
import spirite.file.SaveEngine;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.PenState;
import spirite.image_data.DrawEngine.StrokeAction;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.UndoEngine;
import spirite.image_data.layers.Layer;

/**
 * @author Guy
 *
 */
public class Test1 {
	MasterControl master = new MasterControl();
	
	public Test1() {
		LoadEngine.setMaster(master);	// still bad
	}
	
	/** Creates an image, saves it to a temp file, then loads it, comparing
	 * image structures to verify that either Saving and Loading are working 
	 * as intended or they're both screwed up symmetrically. 
	 * */
	@Test
	public void testSaveLoadIntegrity() {
		ImageWorkspace workspace = new ImageWorkspace(master.getCacheManager());

		// Verify that nothing funny happens when you try to render a null workspace
		try {
			RenderSettings settings = new RenderSettings();
			settings.workspace = workspace;
			BufferedImage img = master.getRenderEngine().renderImage(settings);
			assert( img == null);
		}catch( Exception e) {
			e.printStackTrace();
			fail( "Null workspace does not draw correctly.");
		}
		
		// Verify Image dimension adjustment
		workspace.addNewSimpleLayer(null, 100, 100, "Name", new Color(50,50,50));
		workspace.addNewSimpleLayer(null, 50, 900, "Namex", new Color(50,50,50));
		assert( workspace.getWidth() == 100 && workspace.getHeight() == 900);
	
		GroupNode beta = workspace.addGroupNode(null, "Beta");
		workspace.addNewSimpleLayer(beta, 50,  50, "Beta_1", new Color( 0,100,0));
		workspace.addNewSimpleLayer(beta, 50,  50, "Beta_2", new Color( 0,0,100));
		workspace.addNewSimpleLayer(beta, 50,  50, "Beta_3", new Color( 100,0,0));
		
		// Save file then load it to verify it's working
		File temp = null;
		try {
			temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
		} catch (IOException e) {
			e.printStackTrace();
			fail( "Couldn't create TempFile.");
		}
		SaveEngine.saveWorkspace(workspace, temp);
		
		ImageWorkspace ws2 = null;
		try {
			ws2 = LoadEngine.loadWorkspace(temp);
		} catch (BadSIFFFileException e) {
			e.printStackTrace();
			fail( "Couldn't loadback file.");
		}
		
		// Compare the two workspaces
		Node g1 = workspace.getRootNode();
		Node g2 = ws2.getRootNode();
		
		List<Node> l1 = g1.getAllNodes();
		List<Node> l2 = g2.getAllNodes();
		
		if( l1.size() != l2.size()) 
			fail("Node desync." + l1.size() + "v s " + l2.size());
		
		for( int i = 0; i < l1.size(); ++i) {
			g1 = l1.get(i);
			g2 = l2.get(i);
			
			assert( g1.getName().equals(g2.getName()));
			assert( g1.getClass().equals(g2.getClass()));
			
			if( g1 instanceof LayerNode) {
				Iterator<ImageHandle> it1 = ((LayerNode)g1).getLayer().getUsedImageData().iterator();
				Iterator<ImageHandle> it2 = ((LayerNode)g2).getLayer().getUsedImageData().iterator();

				while( it1.hasNext()) {
					assert(
						compareImages(
								it1.next().deepAccess(),
								it2.next().deepAccess()
								
						));
				}
				assert( !it2.hasNext());
			}
		}
		
		for( ImageHandle data : workspace.getImageData()) {
			BufferedImage b1 = data.deepAccess();
			BufferedImage b2 = data.deepAccess();
			
			assert( compareImages(b1,b2));
		}
	}
	
	/** Deep compare the two images. */
	public static boolean compareImages(BufferedImage img1, BufferedImage img2) {
		if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) 
			return false;
		
		int width = img1.getWidth();
		int height = img1.getHeight();

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
			if (img1.getRGB(x, y) != img2.getRGB(x, y) || img1.getTransparency() != img2.getTransparency())
				return false;
			}
		}

		return true;
	}
	public static BufferedImage deepCopy( BufferedImage toCopy) {
		return new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null);
	}
	
	/**
	 * This test performs a series of undo actions verifying at certain steps that everything
	 * is working as expected.
	 * 
	 * Should probably be more thorough
	 */
	@Test
	public void testUndoEngineIntegrity() {
		ImageWorkspace workspace = new ImageWorkspace(master.getCacheManager());
		master.addWorkpace(workspace, false);
		Layer layer1 = ((LayerNode)workspace.addNewSimpleLayer(null, 150, 150, "base", new Color(0,0,0,0))).getLayer();
		workspace.finishBuilding();
		
		UndoEngine engine = workspace.getUndoEngine();
		
		assert engine.getQueuePosition() == 0;
		
		Layer layer2 = ((LayerNode)workspace.addNewSimpleLayer(null, 160, 160, "two", new Color(0,0,0,0))).getLayer();
		
		
		
		RenderSettings settings = new RenderSettings();
		settings.workspace = workspace;
		BufferedImage img = master.getRenderEngine().renderImage(settings);
		BufferedImage img2 = deepCopy(img);
		

		Layer layer3 = ((LayerNode)workspace.addNewSimpleLayer(null, 900, 900, "three", new Color(0,0,0,0))).getLayer();
		
		engine.undo();
		
		assert compareImages( img2, master.getRenderEngine().renderImage(settings));
	}

	Random rn = new Random(System.nanoTime());
	public void performRandomUndoableAction(ImageWorkspace workspace) {
		int random = rn.nextInt(4);
		switch(rn.nextInt(4)) {
		case 0:
			// Add layer
			workspace.addNewSimpleLayer(
					getRandomNode(workspace), 
					100 + rn.nextInt(100), 100 + rn.nextInt(100)
					, Float.toHexString(rn.nextFloat()), randomColor());
			break;
		case 1:
			// Add Group
//			workspace.addGroupNode(getRandomNode(workspace), Float.toHexString(rn.nextFloat()));
			break;
		case 2:
		case 3:
			// Add Random Stroke
			workspace.setSelectedNode(randomLayerNode(workspace));
			
			PenState p[] = new PenState[50];
			for( int i=0; i<50; ++i) {
				p[i] = new PenState(rn.nextInt(200),
						 rn.nextInt(200),
						 rn.nextFloat());
			}
			
			StrokeParams params = new StrokeParams();
			
			params.setAlpha( rn.nextFloat());
			params.setMethod((random == 3)?Method.BASIC:Method.ERASE);
			params.setColor( randomColor() );
			params.setWidth( rn.nextInt(5));
			
			StrokeAction action =
			workspace.getDrawEngine().new StrokeAction(params, p, workspace.getSelectionEngine().getBuiltSelection(), workspace.getActiveData());
			
			action.performImageAction(workspace.getActiveData());
			workspace.getUndoEngine().storeAction(action);
		}
	}
	
	public Color randomColor() {
		return new Color(rn.nextInt(255),rn.nextInt(255),rn.nextInt(255));
	}
	
	public Node getRandomNode( ImageWorkspace workspace) {
		List<Node> l = workspace.getRootNode().getAllNodes();
		if( l.size() <= 0) return null;
		return l.get( rn.nextInt(l.size()));
	}
	public LayerNode randomLayerNode( ImageWorkspace workspace) {
		List<Node> l = workspace.getRootNode().getAllNodesST( new GroupTree.NodeValidatorLayer());
		
		if( l.size() == 0) return null;
		
		return (LayerNode) l.get( rn.nextInt(l.size()));
	}
	
	/*** Performs a series of randomly-constructed semi-memory-intensive actions
	 * , closing the workspace regularly and creating new ones to make sure cache
	 * data is getting cleared.
	 * 
	 * Use in conjunction with VisualVM or such if you suspect untracked leaks.
	 */
	public static final int CACHE_ROUNDS = 100;
	@Test
	public void testCacheClearing() {
		for( int round=0; round < CACHE_ROUNDS; ++round) {
			ImageWorkspace workspace = new ImageWorkspace(master.getCacheManager());
			workspace.finishBuilding();
			master.addWorkpace(workspace, false);
			
			workspace.addNewSimpleLayer(null, 50, 50, "BASE", new Color(0,0,0,0));
			for( int i=0; i<50; ++i) {
				performRandomUndoableAction( workspace);
			}
			RenderSettings settings = new RenderSettings();
			settings.workspace = workspace;
			master.getRenderEngine().renderImage(settings);
			
			master.closeWorkspace(workspace, false);

			if( master.getCacheManager().getCacheSize() > 30000) {
				fail( "Cache uncleared." +  master.getCacheManager().getCacheSize() + ":" + round);
			}
		}
	}
}
