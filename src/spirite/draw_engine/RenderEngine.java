package spirite.draw_engine;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MImageObserver;
import spirite.image_data.ImageWorkspace;

public class RenderEngine 
	implements MImageObserver
{
	List<Cache> image_cache;
	MasterControl master;
	
	public RenderEngine( MasterControl master) {
		this.master = master;
		image_cache = new ArrayList<>();
		
		master.addImageObserver(this);
	}
	
	public BufferedImage renderImage(RenderSettings settings) {
		// First Attempt to draw an image from a pre-rendered cache
		BufferedImage image = getCachedImage( settings);
		
		if( image == null) {
			// Otherwise get the drawing queue and draw it
			ImageWorkspace workspace = settings.workspace;
			
			if( workspace == null) return null;
			
			List<BufferedImage> drawing_queue = workspace.getDrawingQueue();
			
			image = new BufferedImage( workspace.getWidth(), workspace.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();
	
			for( BufferedImage img : drawing_queue) {
				g.drawImage(img, 0, 0, null);
			}
			
			Cache c = new Cache();
			c.data = image;
			c.settings.workspace = settings.workspace;
			image_cache.add(c);
		}
		
		return image;
	}
	
	// Looks to see if the current rendering requested is already cached; if so, just give the cache
	private BufferedImage getCachedImage( RenderSettings settings) {
		for( Cache c : image_cache) {
			if( c.settings.equals(settings)) {
				return c.data;
			}
		}
		
		return null;
	}
	
	private class Cache {
		BufferedImage data;
		RenderSettings settings;
		long last_used;
		
		Cache() {
			last_used = System.currentTimeMillis();
			settings = new RenderSettings();
		}
		
	}
	
	public static class RenderSettings {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RenderSettings other = (RenderSettings) obj;
			if (workspace == null) {
				if (other.workspace != null)
					return false;
			} else if (!workspace.equals(other.workspace))
				return false;
			return true;
		}

		public ImageWorkspace workspace;
	}

	@Override
	public void imageChanged() {
		
		Iterator<Cache> i = image_cache.iterator();
		
		while( i.hasNext()) {
			Cache c = i.next();
			
			// TODO: make this only remove the cached image if it has changed
			i.remove();
		}
	}

	@Override
	public void newImage() {}
}
