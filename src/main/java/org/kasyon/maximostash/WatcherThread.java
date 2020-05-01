package org.kasyon.maximostash;

import java.io.IOException;
import java.nio.file.Paths;

public class WatcherThread extends Thread 
{
	private boolean watch = true;
	private WatcherService watcherService;

	public WatcherThread(String searchingPath) throws IOException 
	{
		watcherService = new WatcherService(Paths.get(searchingPath));
	}

	@Override
	public void run() 
	{
		System.out.println("Artifact watching thread started.");
		while (watch) 
		{
			if (!watcherService.watch()) 
			{
				break;
			}
		}
		System.out.println("Artifact watching thread stopped.");
	}

	public void stopThread() 
	{
		watch = false;
		
		try 
		{
			watcherService.stop();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}