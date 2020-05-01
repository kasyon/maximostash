package org.kasyon.maximostash;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;


public class WatcherService 
{
	private static AppStatus as = AppStatus.getInstance();

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private boolean trace;

	WatcherService(Path dir) throws IOException 
	{
		watcher = FileSystems.getDefault().newWatchService();
		keys = new HashMap<WatchKey, Path>();

		register(dir);
		trace = true;
	}

	private void register(Path dir) throws IOException 
	{
		WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, 
			StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

		if (trace) 
		{
			Path prev = keys.get(key);
			if (null == prev) 
			{
				System.out.println("Register path: [{}]: " + dir);
			} 
			else 
			{
				if (!dir.equals(prev)) 
				{
					System.out.println("Updated path: [{}] -> [{}]: " + prev + " " + dir);
				}
			}
		}

		keys.put(key, dir);
	}

	boolean watch() 
	{
		WatchKey key;

		try 
		{
			key = watcher.take();
		} 
		catch (InterruptedException | ClosedWatchServiceException exc) 
		{
			return false;
		}

		Path dir = keys.get(key);

		if (null == dir) 
		{
			System.out.println("WatchKey is not recognized!");
			return false;
		}

		for (WatchEvent<?> event: key.pollEvents()) 
		{
			Kind<?> kind = event.kind();

			if (StandardWatchEventKinds.OVERFLOW == kind) 
			{
				continue;
			}

			as.incFsPollingCounter();
		}

		boolean valid = key.reset();

		if (!valid) 
		{
			keys.remove(key);

			if (keys.isEmpty()) 
			{
				return false;
			}
		}

		return true;
	}

	void stop() throws IOException
	{
		watcher.close();
	}
}