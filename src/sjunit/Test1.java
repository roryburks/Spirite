/**
 * 
 */
package sjunit;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import spirite.brains.MasterControl;
import spirite.file.LoadEngine;
import spirite.file.LoadEngine.BadSIFFFileException;
import spirite.file.SaveEngine;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine.RenderSettings;

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
	 * as intended or they're both screwed up symmetrically. */
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
		workspace.addNewLayer(null, 100, 100, "Name", new Color(50,50,50));
		workspace.addNewLayer(null, 50, 900, "Namex", new Color(50,50,50));
		assert( workspace.getWidth() == 100 && workspace.getHeight() == 900);
	
		GroupNode beta = workspace.addGroupNode(null, "Beta");
		workspace.addNewLayer(beta, 50,  50, "Beta_1", new Color( 0,100,0));
		workspace.addNewLayer(beta, 50,  50, "Beta_2", new Color( 0,0,100));
		workspace.addNewLayer(beta, 50,  50, "Beta_3", new Color( 100,0,0));
		
		// Save file then load it to verify it's working
		File temp = null;
		try {
			temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail( "Couldn't create TempFile.");
		}
		SaveEngine.saveWorkspace(workspace, temp);
		
		try {
			ImageWorkspace ws2 = LoadEngine.loadWorkspace(temp);
		} catch (BadSIFFFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail( "Couldn't loadback file.");
		}
		
		// Compare the two workspaces
		Node g1 = workspace.getRootNode();
		Node g2 = workspace.getRootNode();
		
		List<Node> l1 = g1.getAllNodes();
		List<Node> l2 = g2.getAllNodes();
		
		if( l1.size() != l2.size()) 
			fail("Node desync.");
		
		for( int i = 0; i < l1.size(); ++i) {
			g1 = l1.get(i);
			g2 = l2.get(i);
			
			assert( g1.getName().equals(g2.getName()));
			assert( g1.getClass().equals(g2.getClass()));
			
			if( g1 instanceof LayerNode) {
				BufferedImage img1 = ((LayerNode)g1).getImageData().readImage().image;
				BufferedImage img2 = ((LayerNode)g2).getImageData().readImage().image;
				
				assert( compareImages(img1,img2));
			}
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

}
