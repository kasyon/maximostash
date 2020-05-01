package org.kasyon.maximostash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.kasyon.maximostash.model.logfileMetadata;
import org.kasyon.maximostash.model.LoggerLastEvents;

class FileInDirSimple
{
	private String name;
	private Long size;
	private LocalDateTime created;
	private Long sizePerformed;

	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getSize()
	{
		return size;
	}
	public void setSize(Long size)
	{
		this.size = size;
	}
	public LocalDateTime getCreated()
	{
		return created;
	}
	public void setCreated(LocalDateTime created)
	{
		this.created = created;
	}
	public Long getSizePerformed() 
	{
		return sizePerformed;
	}
	public void setSizePerformed(Long size) 
	{
		this.sizePerformed = size;
	}
}

class FileFilter implements FilenameFilter 
{
	String inclusionPattern;

	FileFilter(String inclusionPattern)
	{
		this.inclusionPattern = inclusionPattern;
	}

	@Override
	public boolean accept(File directory, String fileName) 
	{
		if (fileName.matches(inclusionPattern))
		{
			return true;
		}
		return false;
	}
}

public class DirectoryWatcher
{
	private File dir = null;
	private File[] filesInDir = null;
	private AppStatus as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)
	{
		int ret = 1;
		List <logfileMetadata>  llfm = as.getLlfm();
		List <LoggerLastEvents> llle = as.getLle();

		if (System.getProperty("os.name").matches("Windows.*"))
		{
			ret = scanDirWindows(srcDirectory, llfm, llle);
		}
		else
		{
			ret = scanDirJava(srcDirectory, llfm, llle);
		}

		return ret;
	}

	public int scanDirJava(String srcDirectory, List <logfileMetadata> llfm, List <LoggerLastEvents> llle)
	{
		List <FileInDirSimple> lfids2 = new ArrayList<>();
		int ret = 1;

		try
		{
			dir = new File(srcDirectory);
			String srcPath = dir.getCanonicalPath() + File.separator;
			Path srcDir = Paths.get(srcPath);

			if (Files.exists(srcDir)) 
			{
				File directory = new File(srcPath);

				if (as.getLogIncPat() != null)
				{
					FileFilter ff = new FileFilter(as.getLogIncPat());
					filesInDir = directory.listFiles(ff);
				}
				else
				{
					filesInDir = directory.listFiles();
				}

				saveNasizper(lfids2, llfm);

				if (llfm.size() > 0) 
				{
					llfm.clear();
				}

				for (File file : filesInDir)
				{
					FileInDirSimple fids = fileToFIDS(file);
					
					fids = updateSizperForFids(fids, lfids2);
					addNewLFM(llfm, fids);
					addLle(llle, fids);
				}

				if (lfids2.size() > 0) 
				{
					lfids2.clear();
				}

				as.setParsingMode(true);
				ret = 0;
			}
		} 
		catch (IOException ioe) 
		{
			ioe.printStackTrace();
		}

		return ret;
	}

	public int scanDirWindows(String srcDirectory, List <logfileMetadata> llfm, List <LoggerLastEvents> llle)
	{
		List <FileInDirSimple> lfids = new ArrayList<>();
		List <FileInDirSimple> lfids2 = new ArrayList<>();
		int ret = 1;

		dir = new File(srcDirectory);

		saveNasizper(lfids2, llfm);

		if (llfm.size() > 0) 
		{
			llfm.clear();
		}

		dirExec(lfids, srcDirectory);

		int siz = lfids.size();
		int i = 0;

		while (i < siz)
		{
			FileInDirSimple fids = lfids.get(i);

			fids = updateSizperForFids(fids, lfids2);
			addNewLFM(llfm, fids);
			addLle(llle, fids);

			i++;
		}

		if (lfids.size() > 0) 
		{
			lfids.clear();
		}
		if (lfids2.size() > 0) 
		{
			lfids2.clear();
		}

		as.setParsingMode(true);
		ret = 0;

		return ret;
	}

	private void saveNasizper(List <FileInDirSimple> lfids, List <logfileMetadata> llfm)
	{
		int siz = llfm.size();
		int i = 0;

		while (i < siz)
		{
			logfileMetadata lfm = llfm.get(i);
			FileInDirSimple fids = new FileInDirSimple();
			
			fids.setName(lfm.getFilename());
			fids.setSizePerformed(lfm.getSizePerformed()); 
			lfids.add(fids);

			i++;
		}
	}

	private FileInDirSimple updateSizperForFids(FileInDirSimple fids, List <FileInDirSimple> lfids2)
	{
		int siz = lfids2.size();
		int i = 0;

		while (i < siz)
		{
			FileInDirSimple tmp = lfids2.get(i);

			if (fids.getName().equals(tmp.getName()))
			{
				fids.setSizePerformed(tmp.getSizePerformed());
				break;
			}

			i++;
		}
		return fids;
	}

	private void addNewLFM(List <logfileMetadata> llfm, FileInDirSimple fids)
	{
		String logger = fileNameToLoggerName(fids.getName());
		logfileMetadata lfm = new logfileMetadata();

		lfm.setFilename(fids.getName());
		lfm.setLoggername(logger);
		lfm.setCreated(fids.getCreated());
		lfm.setSizeScanned(fids.getSize());
		lfm.setSizePerformed(fids.getSizePerformed());
		lfm.setLastEventLDT(null);
		if (!as.getQuiet())
		{
			System.out.println("\t" + lfm.getFilename() + "\t logger = " + lfm.getLoggername());
		}
		llfm.add(lfm);
	}

	private void addLle(List <LoggerLastEvents> llle, FileInDirSimple fids)
	{
		String logger = fileNameToLoggerName(fids.getName());

		int siz = llle.size();
		int i = 0;

		while (i < siz)
		{
			LoggerLastEvents lle = llle.get(i);

			if (logger.equals(lle.getLoggername()))
			{
				break;
			}

			i++;
		}

		if (i == siz)
		{
			LoggerLastEvents lle = new LoggerLastEvents();
			lle.setLoggername(logger);
			lle.setLastEventLDT(null);
			llle.add(lle);
		}
	}

	private static String fileNameToLoggerName(String fname)
	{
		String ret = "anotherLogger";

		if (fname.matches("System.*\\.log.*"))
		{
			if (fname.matches("SystemErr.*\\.log.*"))
			{
				ret = "SystemErr";
			}
			if (fname.matches("SystemOut.*\\.log.*"))
			{
				ret = "SystemOut";
			}
		} 
		else
		{
			if (fname.matches(".*_.*_.*\\.log.*"))	
			{
				int i1 = fname.indexOf('_');
				int i2 = fname.indexOf('_', i1 + 1);
				int i3 = fname.indexOf('.');
				ret = fname.substring(i2 + 1, i3);
			}
		}
		return ret;
	}

	private FileInDirSimple fileToFIDS(File file)
	{
		FileInDirSimple ret = new FileInDirSimple();
		
		try 
		{
			BasicFileAttributes bfa = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			FileTime creatim = bfa.creationTime();
			LocalDateTime creatimLDT =
				LocalDateTime.ofInstant(Instant.ofEpochMilli(creatim.toMillis()), 
				TimeZone.getDefault().toZoneId());
			ret.setCreated(creatimLDT);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		ret.setName(file.getName());
		ret.setSize(file.length());
		
		return ret;
	}

	public void dirExec(List <FileInDirSimple> lfids, String ldir) 
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.uuuu  HH:mm");
		LocalDateTime ldt = null;
		String cmd = "cmd.exe /C dir " + ldir;

		try 
		{
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) 
			{
				if (line.length() > 22)
				{
					if (line.charAt(22) == ' ')
					{
						FileInDirSimple fids = new FileInDirSimple();

						String dt = line.substring(0, 17);
						try 
						{
							ldt = LocalDateTime.parse(dt, dtf);
						} 
						catch (java.time.format.DateTimeParseException dte) 
						{ }

						String s = line.substring(22);
						s = s.replaceAll("^\\s+", "");

						byte[] fsn = s.getBytes(StandardCharsets.ISO_8859_1);
						byte[] fs = new byte[fsn.length];

						int i = 0, j = 0;
						
						for (i = 0, j = 0; i < fsn.length; i++)
						{
							if ((fsn[i] > 0x2F) && (fsn[i] < 0x3A))
							{
								fs[j] = fsn[i];
								j++;
							}
							if (fsn[i] == 0x20)
								break;
						}

						s = s.substring(i + 1);

						Long fsize = 0L;
						int exp = 1;

						for (i = j - 1; i >= 0; i--)
						{
							fsize = fsize + (fs[i] - 48) * exp;
							exp = exp * 10;
						}

						if (as.getLogIncPat() != null)
						{
							if (s.matches(as.getLogIncPat()))
							{
								fids.setName(s);
								fids.setSize(fsize);
								fids.setCreated(ldt);
								lfids.add(fids);
							}
						}
						else
						{
							fids.setName(s);
							fids.setSize(fsize);
							fids.setCreated(ldt);
							lfids.add(fids);
						}
					}
				}
			}
			int result = p.waitFor();
		} 
		catch (IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
}
