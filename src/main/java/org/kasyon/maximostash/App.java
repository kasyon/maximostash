package org.kasyon.maximostash;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.kasyon.maximostash.model.LoggerLastEvents;
import org.kasyon.maximostash.model.logfileMetadata;

class LlfmInFile implements Serializable 
{
	private static final long serialVersionUID = -211570163426727072L;
	public List <logfileMetadata> llfm;
	public List <LoggerLastEvents> lle;

	public void setLlfm(List<logfileMetadata> llfm)
	{
		this.llfm = llfm;
	}

	public List<logfileMetadata> getLlfm()
	{
		return this.llfm;
	}

	public void setLle(List<LoggerLastEvents> lle)
	{
		this.lle = lle;
	}

	public List<LoggerLastEvents> getLle()
	{
		return this.lle;
	}	
}


public class App 
{
	private static AppStatus as = AppStatus.getInstance();
	private static Timer paceMaker = new Timer();
	
	public static Object readObjectFromFile(String filePath)
	{
		Object obj = null;
    
		try 
		{
			FileInputStream f_in = new FileInputStream(filePath);
			ObjectInputStream obj_in = new ObjectInputStream (f_in);
			obj = obj_in.readObject();
			obj_in.close();
		} 
		catch (Exception ex) 
		{ }
		return obj;
	}
   
	public static void writeObjectToFile(Object obj, String filePath)
	{   
		try 
		{
			FileOutputStream f_out = new FileOutputStream(filePath);
			ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject(obj); 
			obj_out.flush();
			obj_out.close();
		} 
		catch (Exception ex) 
		{ }
	}

	public static int init()
	{
		String prefsPath = "./maximostash.prefs";
		String propPath = "./config.properties";
		Properties prop = new Properties();
		int kom = 0, pos = 0, ret = 0;
		List <logfileMetadata> llfm = as.getLlfm();
		List <LoggerLastEvents> lle = as.getLle();
		LlfmInFile lif = null;

		lif = (LlfmInFile)readObjectFromFile(prefsPath);

		if (lif != null)
		{
			as.setFirstRun(false);

			if (lif.getLlfm() != null)
			{
				Iterator <logfileMetadata> llfmito = lif.getLlfm().iterator();

				while (llfmito.hasNext()) 
				{
					llfm.add((logfileMetadata)llfmito.next());
				}

				lif.getLlfm().clear();

				Iterator <LoggerLastEvents> lleito = lif.getLle().iterator();

				while (lleito.hasNext()) 
				{
					lle.add((LoggerLastEvents)lleito.next());
				}

				lif.getLle().clear();
			}
		}
		else
		{
			as.setFirstRun(true);
		}
		
		try (InputStream inputStream = new FileInputStream(propPath)) 
		{
			prop.load(inputStream);

			as.setLdir(prop.getProperty("logs.directory"));
			if (as.getLdir() == null)
			{
				as.setLdir(".");
				kom += 1;
			}
			as.setElasticurl(prop.getProperty("elasticsearch.url"));
			if (as.getElasticurl() == null)
				kom += 10;
			as.setLogIncPat(prop.getProperty("loginclusion.pattern"));
			if (as.getLogIncPat() == null)
				kom += 100;
			as.setLogExcPat(prop.getProperty("logexclusion.pattern"));
			if (as.getLogExcPat() == null)
				kom += 1000;
			as.setElasticIndex(prop.getProperty("elasticsearch.index"));
			if (as.getElasticIndex() == null)
				kom += 10000;
			as.setFilterLevel(prop.getProperty("filter.level"));
			if (as.getFilterLevel() == null)
				kom += 100000;
		} 
		catch (IOException ex) 
		{ 
			System.out.println("There is no 'config.properties' file!\n");
			ret = 1;
		}

		if (kom > 0)
		{
			System.out.println("Check 'config.properties' file:\n");
		}

		while (kom > 0)
		{
			int r = kom % 10;
			if (r == 1)
			{
				switch (pos)
				{
					case 0:
						System.out.println("[INFO]: No logs directory defined!");
						break;
					case 1:
						System.out.println("[ERROR]: No URL to elasticsearch server defined!");
						ret = 1;
						break;
					case 2:
						System.out.println("[WARN]:  No inclusion pattern for logfile names.");
						break;
					case 3:
						System.out.println("[WARN]:  No exclusion pattern for logfile names.");
						break;
					case 4:
						System.out.println("[WARN]:  No elasticsearch index name.");
						break;
					case 5:
						System.out.println("[INFO]:  No filter level.");
						break;
				}
			}
			kom = kom / 10;
			pos++;
		}
		return ret;
	}

	public static void printLFM(List <logfileMetadata> llfm)
	{
		int siz = llfm.size();
		int i = 0;

		System.out.println("=================================================");
		System.out.println("Maximo logs stashing application (build 20191113)");
		System.out.println("=================================================");
		System.out.println("Filename \t\t\t Created \t SizeScanned \t SizePerformed \t LastEvent");
		System.out.println("---------\t\t\t---------\t-------------\t---------------\t----------");

		while (i < siz)
		{
			logfileMetadata lfm = llfm.get(i);
			System.out.println(lfm.getFilename() + "\t" + lfm.getCreated() + "\t" + lfm.getSizeScanned() + "\t\t" + lfm.getSizePerformed() + "\t" 
				+ lfm.getLastEventLDT());
			i++;
		}
		System.out.println("===================================================");
	}

	private static String Menu()
	{
		System.out.println("\n--- MENU ---");
		System.out.println("");
		System.out.println("[i] Info");
		System.out.println("[q] Quit");
	
		return ConsoleRead();
	}

	private static String ConsoleRead()
	{
		BufferedReader in = new BufferedReader(
			new InputStreamReader(System.in));
		
		System.out.print(" : "); 
		System.out.flush();
		String ret = "";
		
		try 
		{
			ret = in.readLine();
		} 
		catch (IOException ioe)
		{
			System.out.println("I/O Error.");
		}
		
		return ret;
	}

	public static void main(String[] args)
	{
		String prefsPath = "./maximostash.prefs";
		boolean finito = false;
		String choice;
		as.llfmCreate();
		as.lleCreate();
		List <logfileMetadata> llfm = as.getLlfm();
		List <LoggerLastEvents> lle = as.getLle();
		int i = init();
		
		if (args.length == 1)
		{
			if ("quiet".equals(args[0]))
			{
				as.setQuiet(true);
			}
			else
			{
				as.setQuiet(false);
			}
		}
		else
		{
			as.setQuiet(false);
		}

		if (i == 0)
		{
			WatcherThread wath = null;
			
			try 
			{
				wath = new WatcherThread(as.getLdir());
				wath.start();
			} 
			catch (IOException e) 
			{
				System.out.println(as.getLdir() + " does not exist!");
				finito = true;
			}

			TimerTask scanparseTrigger = new TimerTask()
			{
				@Override
				public void run() 
				{
					ParserService ps = new ParserService();
					int first = as.getFsPollingCounter();
					DirectoryWatcher dw = new DirectoryWatcher();
					
					try
					{
						Thread.sleep(1000);
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}

					int second = as.getFsPollingCounter();

					if (as.getFsPollingCounter() > 0)
					{
						if ((first == second))
						{
							as.setFsPollingCounter(0);
							dw.scanDir(as.getLdir());
						}
					}

					if (as.getParsingMode())
					{
						LlfmInFile lif = new LlfmInFile();
						lif.setLlfm(llfm);
						lif.setLle(lle);
						ps.scanParse();
						writeObjectToFile(lif, prefsPath);
					}
				}
			};

			paceMaker.schedule(scanparseTrigger, 100, 15000);

			while (!finito)
			{
				choice = Menu();
			
				if (choice != null)
				{
					if (choice.equals("i")) 
					{
						printLFM(llfm);
					}

					if (choice.equals("q")) 
					{
						finito = true;
					}
				}
				else
				{
					finito = true;
				}	
			}

			if (wath != null)
			{
				wath.stopThread();
			}
		}

		LlfmInFile lif = new LlfmInFile();
		lif.setLlfm(llfm);
		lif.setLle(lle);
		writeObjectToFile(lif, prefsPath);
		
		as.llfmDestroy();
		as.lleDestroy();

		paceMaker.cancel();
		paceMaker.purge();
	}
}
