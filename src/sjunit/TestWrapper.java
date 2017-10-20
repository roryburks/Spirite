package sjunit;

import java.util.concurrent.ExecutorCompletionService;

import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

public class TestWrapper {
	private static class CarryInfo {
		boolean completed = false;
		Thread swingThread = null;
		Exception e = null;
		AssertionError assertFail = null;
	}
	public interface DoOnMaster {
		public void Do(MasterControl master);
	}
	public static void performTest( DoOnMaster runner) throws Exception {
		final CarryInfo info = new CarryInfo();
		MasterControl master;
        master = new MasterControl();

        SwingUtilities.invokeLater( new Runnable() {
            public void run() { 
            	info.swingThread = Thread.currentThread();
            	synchronized (info.swingThread) {
            		try {
            			runner.Do(master);
            		}
            		catch( Exception e) { info.e = e;}
            		catch( AssertionError e) { info.assertFail = e;}
            		finally {
	            		info.completed = true;
	            		info.swingThread.notify();
            		}
				}
            }
        });
        
        
        while( info.swingThread == null)
        	Thread.currentThread().sleep(100);
        while( !info.completed)
        	synchronized (info.swingThread) 
        		{info.swingThread.wait(100);}
        
        if( info.e != null)
        	throw info.e;
        if( info.assertFail != null)
        	throw info.assertFail;
	}
}
