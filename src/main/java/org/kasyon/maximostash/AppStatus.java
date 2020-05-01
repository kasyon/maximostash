package org.kasyon.maximostash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.kasyon.maximostash.model.logfileMetadata;
import org.kasyon.maximostash.model.LoggerLastEvents;

public class AppStatus implements Serializable
{
	private static final long serialVersionUID = 4434270418410877472L;
	private List <logfileMetadata> llfm;
	private List <LoggerLastEvents> lle;
	private boolean firstRun;
	private boolean parsingMode;
	private boolean quiet;
	private String ldir;
	private int fsPollingCounter = 0;
	private String elasticurl;
	private String logIncPat;
	private String logExcPat;
	private String elasticIndex;
	private String filterLevel;

	private AppStatus()
	{}

	private static class AppStatusHelper
	{
		private static final AppStatus instance = new AppStatus();
	}
	
	protected Object readResolve() 
	{
		return getInstance();
	}

	public static AppStatus getInstance()
	{
		return AppStatusHelper.instance;
	}
	
	public void llfmCreate()
	{
		llfm = new ArrayList<>();
	}
	
	public List <logfileMetadata> getLlfm()
	{
		return this.llfm;
	}
	
	public void llfmDestroy()
	{
		llfm.clear();
	}

	public void lleCreate()
	{
		lle = new ArrayList<>();
	}

	public List <LoggerLastEvents> getLle()
	{
		return this.lle;
	}
	
	public void lleDestroy()
	{
		lle.clear();
	}

	public String getLdir()
	{
		return this.ldir;
	}

	public void setLdir(String ldir)
	{
		this.ldir = ldir;
	}

	public boolean getFirstRun()
	{
		return this.firstRun;
	}

	public void setFirstRun(boolean firstRun)
	{
		this.firstRun = firstRun;
	}

	public boolean getParsingMode()
	{
		return this.parsingMode;
	}

	public void setParsingMode(boolean parsingMode)
	{
		this.parsingMode = parsingMode;
	}
	
	public boolean getQuiet()
	{
		return this.quiet;
	}

	public void setQuiet(boolean quiet)
	{
		this.quiet = quiet;
	}

	public int getFsPollingCounter()
	{
		return this.fsPollingCounter;
	}

	public void setFsPollingCounter(int fsPollingCounter)
	{
		this.fsPollingCounter = fsPollingCounter;
	}

	public void incFsPollingCounter()
	{
		this.fsPollingCounter++;
	}

	public String getElasticurl()
	{
		return this.elasticurl;
	}

	public void setElasticurl(String elasticurl)
	{
		this.elasticurl = elasticurl;
	}

	public String getLogIncPat()
	{
		return this.logIncPat;
	}

	public void setLogIncPat(String logIncPat)
	{
		this.logIncPat = logIncPat;
	}

	public String getLogExcPat()
	{
		return this.logExcPat;
	}

	public void setLogExcPat(String logExcPat)
	{
		this.logExcPat = logExcPat;
	}

	public String getElasticIndex()
	{
		return this.elasticIndex;
	}

	public void setElasticIndex(String elasticIndex)
	{
		this.elasticIndex = elasticIndex;
	}

	public String getFilterLevel()
	{
		return this.filterLevel;
	}

	public void setFilterLevel(String filterLevel)
	{
		this.filterLevel = filterLevel;
	}
}