package org.kasyon.maximostash;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.json.simple.JSONObject;

import org.kasyon.maximostash.model.LogLines;
import org.kasyon.maximostash.model.logfileMetadata;
import org.kasyon.maximostash.model.LoggerLastEvents;

public class ParserService 
{
	private AppStatus as = AppStatus.getInstance();
	private List<LogLines> evlines = new ArrayList<>();
	private byte[] buffer = null;

	public void scanParse()
	{
		List <logfileMetadata> llfm = as.getLlfm();
		boolean doesNotMatchExcPat = true;
		int bytesRead = 0;
		int siz = llfm.size();
		int i = 0;

		File dir = new File(as.getLdir());
		String srcPath = null;

		try 
		{
			srcPath = dir.getCanonicalPath() + File.separator;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		if (!as.getQuiet())
		{
			if (as.getFirstRun())
			{
				System.out.println("First Run");
			}
			else
			{
				System.out.println("Subsequent Run");
			}
		}

		while (i < siz)
		{
			logfileMetadata lfm = llfm.get(i);

			if (as.getLogExcPat() != null)
			{
				if (lfm.getFilename().matches(as.getLogExcPat()))
				{
					doesNotMatchExcPat = false;
				}
				else
				{
					doesNotMatchExcPat = true;
				}
			}
			else
			{
				doesNotMatchExcPat = true;
			}

			if (doesNotMatchExcPat)
			{
				String fName = srcPath + lfm.getFilename();
				if (!as.getQuiet())
				{
					System.out.println("\nFile : " + fName);
				}

				bytesRead = readFileB(fName, lfm.getSizeScanned());

				if (bytesRead > 0) 
				{
					if (!as.getQuiet())
					{
						System.out.println(bytesRead + " bytes read.");
					}
					if (lfm.getSizePerformed() == null)
					{
						lfm.setSizePerformed(0L);
					}

					bufferToElastic(buffer, lfm.getSizePerformed(), 
						lfm.getFilename(), lfm.getLoggername(), evlines);

					int elsi = evlines.size();
					if (elsi > 0)
					{
						LogLines lg = evlines.get(elsi - 1);
						if (lg.getEventLDT() != null)
						{
							lfm.setLastEventLDT(lg.getEventLDT());
							updateLoggerLastEvent(lfm.getLoggername(), lg.getEventLDT());
						}
						evlines.clear();
					}

					lfm.setSizePerformed((long)bytesRead);
				}
				else
				{
					if (!as.getQuiet())
					{
						System.out.println("0 bytes read.");
					}
				}
			}
			i++;
		}

		if (as.getFirstRun())
		{
			as.setFirstRun(false);
		}

		as.setParsingMode(false);
	}

	private int readFileB(String fileName, long fileSizeScanned)
	{
		int totalBytesRead = 0;
		int tbl = 0;

		try (InputStream input = new BufferedInputStream(new FileInputStream(fileName)))
		{
			long realFileSize = new File(fileName).length();
			this.buffer = new byte[(int)realFileSize + 2];

			if (fileSizeScanned != realFileSize)
			{
				if (!as.getQuiet())
				{
					System.out.println("fileSizeScanned = " + fileSizeScanned + "\trealFileSize = " + realFileSize);
				}
			}

			tbl = this.buffer.length - 2;

			while (totalBytesRead < tbl)
			{
				int bytesRemaining = tbl - totalBytesRead;
				int bytesRead = input.read(this.buffer, totalBytesRead, bytesRemaining); 
				if (bytesRead > 0)
				{
					totalBytesRead = totalBytesRead + bytesRead;
				}
			}

			this.buffer[tbl] = 0x0d;
			this.buffer[tbl + 1] = 0x0a;

		}
		catch (FileNotFoundException ex) 
		{
			if (!as.getQuiet())
			{
				System.out.println("File not found.");
			}
		}
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}

		return totalBytesRead;
	}

	private void bufferToElastic(byte[] buf, long alreadyDone, String fname, String logger, List<LogLines> evlines)
	{
		boolean later = true;
		int eventsCount = 0;

		if ("MboCount".equals(logger))
		{
			eventsCount = fetchMboCountEventsFromBuffer(buf, alreadyDone, evlines);	
		}
		else
		{
			eventsCount = fetchEventsFromBuffer(buf, alreadyDone, evlines);	
		}

		if (evlines.size() > 0)
		{
			LogLines lg1 = evlines.get(0);
			LogLines lg2 = evlines.get(evlines.size() - 1);
			if (!as.getQuiet())
			{
				System.out.println("Events from: " + lg1.getEventLDT() + "\t till: " + lg2.getEventLDT());
			}
		}

		if (!as.getQuiet())
		{
			System.out.println("Event lines: " + eventsCount + "\t( filesize / performed ): ( " + (buf.length - 2) + " / " + alreadyDone + " )");
		}

		if (!as.getFirstRun())
		{
			later = laterEvents(evlines, logger);
		}

		if (later)
		{
			int eventsSent = sendEvents(evlines, fname, logger);
			if (!as.getQuiet())
			{
				System.out.println("Events sent: " + eventsSent);
			}
		}
		else
		{
			if (!as.getQuiet())
			{
				System.out.println("Events have not been sent.");
			}
		}
	}

	private boolean laterEvents(List<LogLines> evlines, String logger)
	{
		LocalDateTime thisFileLastEventLDT = null;
		List <LoggerLastEvents> llle = as.getLle();
		int siz = llle.size();
		boolean ret = true;
		int i = 0;

		if (evlines.size() > 0) 
		{
			LogLines lg = evlines.get(evlines.size() - 1);
			if (lg != null)
			{
				thisFileLastEventLDT = lg.getEventLDT();
			}
		}

		if (thisFileLastEventLDT != null) 
		{
			if (!as.getQuiet())
			{
				System.out.println("thisFileLastEventLDT = " + thisFileLastEventLDT + "\t compared with: ");
			}

			while (i < siz)
			{
				LoggerLastEvents lle = llle.get(i);

				if (logger.equals(lle.getLoggername()))
				{
					if (lle.getLastEventLDT() != null)
					{
						if (!as.getQuiet())
						{
							System.out.print("logger = " + lle.getLoggername() + "\t LastEventLDT = " + lle.getLastEventLDT());
						}

						if (lle.getLastEventLDT().isAfter(thisFileLastEventLDT)) 
						{
							if (!as.getQuiet())
							{
								System.out.println(" is after this file");
							}
							ret = false;
						}
						else
						{
							if (!as.getQuiet())
							{
								System.out.println(" is not after this file");
							}
						}
					}
					else
					{
						if (!as.getQuiet())
						{
							System.out.println("logger = " + lle.getLoggername() + "\t LastEventLDT = NULL");
						}
					}
					break;
				}
				i++;
			}
		} 
		else 
		{
			if (!as.getQuiet())
			{
				System.out.println("thisFileLastEventLDT = null");
			}
			ret = false;
		}
		return ret;
	}

	private String logLinesToJSON(LogLines lg, String fname, String logger)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		List<NameValuePair> lnvp = new ArrayList<>();
		JSONObject json = new JSONObject();
		
		lnvp.add(new BasicNameValuePair("timestamp", lg.getEventLDT().format(dtf)));
		lnvp.add(new BasicNameValuePair("event", lg.getEventType()));
		lnvp.add(new BasicNameValuePair("bmx", lg.getBMXcode()));
		lnvp.add(new BasicNameValuePair("logger", logger));

		if ("MboCount".equals(logger))
		{
			lnvp.add(new BasicNameValuePair("serverName", lg.getServerName()));
			lnvp.add(new BasicNameValuePair("serverIP", lg.getServerIP()));
			lnvp.add(new BasicNameValuePair("totalMemory", lg.getServerTotalMem()));
			lnvp.add(new BasicNameValuePair("availMemory", lg.getServerAvailMem()));
		}

		lnvp.add(new BasicNameValuePair("file", fname));
		lnvp.add(new BasicNameValuePair("content", lg.getLine()));

		for (NameValuePair nvp : lnvp)
		{
			json.put(nvp.getName(), nvp.getValue());
		}

		lnvp.clear();

		return json.toString();
	}

	private int sendEvents(List<LogLines> evlines, String fname, String logger)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		Pattern replace = Pattern.compile("\\s+|:|\\.|-");
 		RESTclient rc = new RESTclient();
 		String URLfirstPart = as.getElasticurl();
 		String elasticidx;
 		LocalDateTime prevEventLDT = null;
		int siz = evlines.size();
		int sameDTcounter = 0;
		int i = 0, j = 0, ret = 0;

		if (as.getElasticIndex() != null)
		{
			elasticidx = as.getElasticIndex();
		} 
		else
		{
			if (fname.matches("System.*\\.log.*"))
			{
				elasticidx = "maximo_logs";
			} 
			else
			{
				if (fname.matches(".*_.*_.*\\.log.*"))
				{
					int i1 = fname.indexOf('_');
					int i2 = fname.indexOf('_', i1 + 1);
					elasticidx = fname.substring(i1 + 1, i2);
				}
				else
				{
					elasticidx = "maximo_logs";
				}
			}
		}

		char lastChar = URLfirstPart.charAt(URLfirstPart.length() - 1);

		if (lastChar == '/')
		{
			URLfirstPart = URLfirstPart + elasticidx + "/event/";
		}
		else
		{
			URLfirstPart = URLfirstPart + "/" + elasticidx + "/event/";
		}

		String[] allowedEvents;
		boolean lineAllowedToBeSent = false;

		if (as.getFilterLevel() == null)
		{
			allowedEvents = new String[] {"DEBUG", "ERROR", "INFO", "WARN", "SystemErr"};
		} 
		else
		{
			allowedEvents = as.getFilterLevel().split("\\s*,\\s*");
		}

		long t1 = System.currentTimeMillis();

		while (i < siz)
		{
			LogLines lg = evlines.get(i);

			String et = lg.getEventType();
			int k = 0;

			for (k = 0; k < allowedEvents.length; k++)
			{
				if (et.matches(allowedEvents[k]))
				{
					lineAllowedToBeSent = true;
					break;
				}
			}

			if (k == allowedEvents.length)
			{
				lineAllowedToBeSent = false;
			}

			if (lg.getEventLDT() == null)
			{
				lineAllowedToBeSent = false;
			}

			if (lineAllowedToBeSent)
			{
				if (prevEventLDT != null)
				{
					if (prevEventLDT.isEqual(lg.getEventLDT()))
					{
						sameDTcounter++;
					}
					else
					{
						prevEventLDT = lg.getEventLDT();
						sameDTcounter = 0;
					}
				} 
				else
				{
					prevEventLDT = lg.getEventLDT();
				}

				if (lg.getEventLDT() != null)
				{
					String json = logLinesToJSON(lg, fname, logger);
					Matcher matcher = replace.matcher(lg.getEventLDT().format(dtf));

					StringBuilder sb = new StringBuilder();
					sb.append(URLfirstPart);
					sb.append(matcher.replaceAll("_"));
					sb.append(String.format("_%03d", sameDTcounter));
					sb.append(logger);
					sb.append("?op_type=create");
					String url = sb.toString();

					int scode = 0;

					while (true)
					{
						scode = rc.putPage(url, json);

						if ((scode == 201) || (scode == 409))
						{
							break;
						}
					}
				}

				if ((ret % 500) == 0)
				{
					if ((j % 10) == 0)
					{
						if (ret != 0)
						{
							if (!as.getQuiet())
							{
								System.out.println(" ");
							}
						}
						j = 0;
					}

					if (!as.getQuiet())
					{
						System.out.print(String.format("%05d", ret) + " ");
					}
					j++;
				}
				ret++;
			}
			i++;
		}

		long t2 = System.currentTimeMillis();
		long reqTime = t2 - t1;
		if (!as.getQuiet())
		{
			System.out.println("\nIt took : " + reqTime + " miliseconds.");
		}

		rc.clientClose();
		return ret;
	}

	private int fetchEventsFromBuffer(byte[] buf, long offset, List<LogLines> evlines)
	{
		String[] whichEventsReg = new String[] {".*\\[DEBUG\\].*", ".*\\[ERROR\\].*", ".*\\[INFO\\].*", ".*\\[WARN\\].*", ".* SystemErr .*"};
		String[] whichEvents = new String[] {"[DEBUG]", "[ERROR]", "[INFO]", "[WARN]", " SystemErr "};
		LocalDateTime ds = null;
		String firstLine = "";
		int begin = 0, end = 0, endOfString = 0;
		boolean EOLcounted = false;
		int ret = 0;

		if (offset < buf.length)
		{
			begin = (int) offset;
			end = (int) offset;
			endOfString = (int) offset;
		}

		while (end < buf.length)
		{
			if (buf[end] == 0x0d)
			{
				EOLcounted = true;
			}

			if (buf[end] == 0x0a)
			{
				if (EOLcounted)
				{
					EOLcounted = false;
					endOfString = end - 1;
				}
				else
				{
					endOfString = end;
				}

				int rowsiz = endOfString - begin;
				if (rowsiz > 0)
				{
					String s = pickFirstLine(buf, whichEventsReg, begin, rowsiz);

					if (!"".equals(s))
					{
						if ("".equals(firstLine))
						{
							firstLine = s;
						} 
						else
						{
							ds = fetchDateStamp(firstLine);
							if (ds != null)
							{
								LogLines el = new LogLines();
								el.setLine(firstLine);
								el.setEventLDT(ds);
								el.setEventType(fetchEventType(firstLine, whichEvents));
								el.setBMXcode(fetchBMXcode(firstLine));
								evlines.add(el);
								ret++;
							}
							firstLine = s;
						}
					}
					else
					{
						String nextLine = pickNextLine(buf, begin, rowsiz);
						firstLine = firstLine + "\n" + nextLine;
					}
				}
				begin = end + 1;
			}
			end++;
		}

		if (!"".equals(firstLine))
		{
			ds = fetchDateStamp(firstLine);
			if (ds != null)
			{
				LogLines el = new LogLines();
				el.setLine(firstLine);
				el.setEventLDT(ds);
				el.setEventType(fetchEventType(firstLine, whichEvents));
				el.setBMXcode(fetchBMXcode(firstLine));
				evlines.add(el);
				ret++;
			}
		}
		return ret;
	}

	private int fetchMboCountEventsFromBuffer(byte[] buf, long offset, List<LogLines> evlines)
	{
		String[] whichEventsReg = new String[] {".*\\[DEBUG\\].*", ".*\\[ERROR\\].*", ".*\\[INFO\\].*", ".*\\[WARN\\].*", ".* SystemErr .*"};
		String[] whichEvents = new String[] {"[DEBUG]", "[ERROR]", "[INFO]", "[WARN]", " SystemErr "};
		LogLines el = new LogLines();
		LocalDateTime ds = null;
		String firstLine = "";
		int begin = 0, end = 0, endOfString = 0;
		boolean EOLcounted = false;
		int ret = 0;

		if (offset < buf.length)
		{
			begin = (int) offset;
			end = (int) offset;
			endOfString = (int) offset;
		}

		while (end < buf.length)
		{
			if (buf[end] == 0x0d)
			{
				EOLcounted = true;
			}

			if (buf[end] == 0x0a)
			{
				if (EOLcounted)
				{
					EOLcounted = false;
					endOfString = end - 1;
				}
				else
				{
					endOfString = end;
				}

				int rowsiz = endOfString - begin;
				if (rowsiz > 0)
				{
					String s = pickFirstLine(buf, whichEventsReg, begin, rowsiz);

					if (!"".equals(s))
					{
						ds = fetchDateStamp(s);

						if (ds != null)
						{
							String bmxCode = fetchBMXcode(s);

							if (!"".equals(bmxCode))
							{
								String serverMem = fetchField(" memory is ", s);
								if (!"".equals(serverMem))
								{
									el.setServerTotalMem(serverMem);
								}

								String serverAvail = fetchField(" available is ", s);
								if (!"".equals(serverAvail))
								{
									el.setServerAvailMem(serverAvail);
								}

								String serverIP = fetchServerIP(s);
								if (!"".equals(serverIP))
								{
									el.setServerIP(serverIP);
								}

								String serverName = fetchServerName(s);
								if (!"".equals(serverName))
								{
									if (el.getEventLDT() == null)
									{
										el.setEventLDT(ds);
									}
									el.setBMXcode(bmxCode);
									el.setLine(firstLine);
									el.setEventType(fetchEventType(s, whichEvents));
									el.setServerName(serverName);
									evlines.add(el);
									el = new LogLines();
									firstLine = "";
									ret++;
								}
							}
							else
							{
								if (!"".equals(firstLine))
								{
									if ((el.getEventLDT() != null) && (el.getEventType() != null))
									{
										el.setLine(firstLine);
										evlines.add(el);
										ret++;
									}
								}
								el = new LogLines();
								el.setEventLDT(ds);
								el.setEventType(fetchEventType(s, whichEvents));
								firstLine = s;
							}
						}
					}
					else
					{
						String nextLine = pickNextLine(buf, begin, rowsiz);
						firstLine = firstLine + "\n" + nextLine;
					}
				}
				begin = end + 1;
			}
			end++;
		}

		if (!"".equals(firstLine))
		{
			if ((el.getEventLDT() != null) && (el.getEventType() != null))
			{
				el.setLine(firstLine);
				evlines.add(el);
				ret++;
			}
		}
		return ret;
	}

	private LocalDateTime fetchDateStamp(String s)
	{
		String[] monthNames = new String[] {" Jan "," Feb "," Mar "," Apr "," May "," Jun "," Jul "," Aug "," Sep "," Oct "," Nov "," Dec "};
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MM uuuu HH:mm:ss:SSS");
		StringBuilder sb = new StringBuilder();
		LocalDateTime ret = null;
		int i = 0, mn = 0;

		if (s.length() < 24)
		{
			return ret;
		}

		String ss = s.substring(0, 24);
		if (ss.matches("^\\d\\d\\s\\w\\w.\\s(\\d){4}\\s(\\d\\d:){3}(\\d){3}.*"))
		{
			for (mn = 0; mn < monthNames.length; mn++)
			{
				i = s.indexOf(monthNames[mn]);
				if (i > -1)
				{
					break;
				}
			}

			if (mn < monthNames.length)
			{
				if ((i - 2) >= 0)
				{
					sb.append(s.substring(i - 2, i));
					sb.append(String.format(" %02d ", mn + 1));
					sb.append(s.substring(i + 5, i + 22));
					String dt = sb.toString();

					try 
					{
						ret = LocalDateTime.parse(dt, dtf);  
					} 
					catch (java.time.format.DateTimeParseException dte) 
					{ }
				}
			} 
		}

		ss = s.substring(0, 22);
		if (ss.matches("^\\[(\\d\\d\\.){2}\\d\\d\\s(\\d\\d:){3}(\\d){3}.*"))
		{
			i = s.indexOf(".");
			if (i > 1)
			{
				sb.append(s.substring(i - 2, i));
				sb.append(" ");
				sb.append(s.substring(i + 1, i + 3));
				sb.append(" 20");
				sb.append(s.substring(i + 4, i + 6));
				sb.append(" ");
				sb.append(s.substring(i + 7, i + 19));
				String dt = sb.toString();
				try 
				{
					ret = LocalDateTime.parse(dt, dtf);  
				} 
				catch (java.time.format.DateTimeParseException dte) 
				{ }
			}
		}
		return ret;
	}

	private String fetchEventType(String s, String[] whichEvents)
	{
		String ret = "";
		int i = 0, j = 0;

		for (j = 0; j < whichEvents.length; j++)
		{
			i = s.indexOf(whichEvents[j]);
			if (i > -1)
			{
				int k = whichEvents[j].length();
				ret = whichEvents[j].substring(1, k - 1);
				break;
			}
		}
		return ret;
	}

	private String fetchBMXcode(String s)
	{
		String ret = "";

		int i = s.indexOf(" BMX");

		if (i > -1)
		{
			int j = i + 1;

			while (j < s.length())
			{
				char c = s.charAt(j);
				if (c == ' ')
					break;
				j++;
			}

			ret = s.substring(i + 1, j);
		}
		return ret;
	}

	private String fetchField(String guidance, String s)
	{
		String ret = "";

		int i = s.indexOf(guidance);

		if (i > -1)
		{
			int j = i + guidance.length();

			while (j < s.length())
			{
				char c = s.charAt(j);
				if (c == ' ')
					break;
				j++;
			}

			ret = s.substring(i + guidance.length(), j);
		}
		return ret;
	}

	private String fetchServerIP(String s)
	{
		String ret = "";
		String pattern = " Server host: ";

		int i = s.indexOf(pattern);

		if (i > -1)
		{
			int j = i + pattern.length();

			while (j < s.length())
			{
				char c = s.charAt(j);
				if (c == ' ')
					break;
				j++;
			}

			if ('.' == s.charAt(j - 1))
			{
				j--;
			}

			ret = s.substring(i + pattern.length(), j);
		}
		return ret;
	}

	private String fetchServerName(String s)
	{
		String ret = "";
		String pattern = " Server name: ";

		int i = s.indexOf(pattern);

		if (i > -1)
		{
			int j = i + pattern.length();

			while (j < s.length())
			{
				char c = s.charAt(j);
				if ((c == '.') || (c == ' '))
					break;
				j++;
			}

			ret = s.substring(i + pattern.length(), j);
		}
		return ret;
	}

	private String pickFirstLine(byte[] buf, String[] whichEvents, int begin, int rowsiz)
	{
		int j;
		String ret = "";
		Charset charset = StandardCharsets.ISO_8859_1;

		byte[] row = new byte[rowsiz];

		for (int i = 0; i < rowsiz; i++)
		{
			row[i] = buf[begin + i];
		}
		String s = new String(row, charset);

		for (j = 0; j < whichEvents.length; j++)
		{
			if (s.matches(whichEvents[j]))
			{
				ret = s;
				break;
			}
		}

		if (j == whichEvents.length)
		{
			ret = "";
		}
		return ret;
	}

	private String pickNextLine(byte[] buf, int begin, int rowsiz)
	{
		byte[] row = new byte[rowsiz];
		Charset charset = StandardCharsets.ISO_8859_1;

		for (int i = 0; i < rowsiz; i++)
		{
			row[i] = buf[begin + i];
		}

		return new String(row, charset);
	}

	private void updateLoggerLastEvent(String logger, LocalDateTime eventLDT)
	{
		List <LoggerLastEvents> llle = as.getLle();
		int siz = llle.size();
		int i = 0;

		while (i < siz)
		{
			LoggerLastEvents lle = llle.get(i);

			if (logger.equals(lle.getLoggername()))
			{
				if (lle.getLastEventLDT() != null)
				{
					if (eventLDT.isAfter(lle.getLastEventLDT()))
					{
						lle.setLastEventLDT(eventLDT);
					}
				}
				else
				{
					lle.setLastEventLDT(eventLDT);
				}
				break;
			}
			i++;
		}
	}
}
