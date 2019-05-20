/**
 * Proprietary and Confidential
 * Copyright 1995-2018 iBASEt, Inc.
 * Unpublished-rights reserved under the Copyright Laws of the United States
 * US Government Procurements:
 * Commercial Software licensed with Restricted Rights.
 * Use, reproduction, or disclosure is subject to restrictions set forth in
 * license agreement and purchase contract.
 * iBASEt, Inc. 27442 Portola Parkway, Suite 300, Foothill Ranch, CA 92610
 *
 * Solumina software may be subject to United States Dept of Commerce Export Controls.
 * Contact iBASEt for specific Expert Control Classification information.
 */

package com.ibaset.solumina.logging.appender;

import static com.ibaset.common.FrameworkConstants.SOLUMINA_CONFIG_FILE;
import static com.ibaset.common.FrameworkConstants.SOLUMINA_LOG_DIR;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.contains;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;

/**

	SoluminaRollingFileAppender extends {@link FileAppender} so that the
	underlying file is rolled over at a user chosen frequency as well as when
	file reach to a certain size.
	
	<p>The rolling schedule is specified by the <b>DatePattern</b> option. This pattern
	should follow the {@link SimpleDateFormat} conventions. In particular, you
	<em>must</em> escape literal text within a pair of single quotes. A formatted version
	of the date pattern is used as the suffix for the rolled file name.
	
	<p>For example, if the <b>File</b> option is set to <code>/foo/bar.log</code> and the DatePattern
	set to <code>'.'yyyy-MM-dd</code>, on 2001-02-16 at midnight, the logging file/foo/bar.log
	will be copied to <code>/foo/bar.log.2001-02-16-00-00.0</code> and logging for 2001-02-17
	will continue in <code>/foo/bar.log</code> until it rolls over the next day.
	
	<p>Is is possible to specify monthly, weekly, half-daily, daily, hourly, or
	minutely rollover schedules.
	
	<p><table border="1" cellpadding="2">
	<tr>
	<th>DatePattern</th>
	<th>Rollover schedule</th>
	<th>Example</th>
	
	<tr>
	<td><code>'.'yyyy-MM</code>
	<td>Rollover at the beginning of each month</td>
	
	<td>At midnight of May 31st, 2002 <code>/foo/bar.log</code> will be
	copied to <code>/foo/bar.log.2002-05</code>. Logging for the month
	of June will be output to <code>/foo/bar.log</code> until it is
	also rolled over the next month.
	
	<tr>
	<td><code>'.'yyyy-ww</code>
	
	<td>Rollover at the first day of each week. The first day of the
	week depends on the locale.</td>
	
	<td>Assuming the first day of the week is Sunday, on Saturday
	midnight, June 9th 2002, the file <i>/foo/bar.log</i> will be
	copied to <i>/foo/bar.log.2002-23</i>.  Logging for the 24th week
	of 2002 will be output to <code>/foo/bar.log</code> until it is
	rolled over the next week.
	
	<tr>
	<td><code>'.'yyyy-MM-dd</code>
	
	<td>Rollover at midnight each day.</td>
	
	<td>At midnight, on March 8th, 2002, <code>/foo/bar.log</code> will
	be copied to <code>/foo/bar.log.2002-03-08</code>. Logging for the
	9th day of March will be output to <code>/foo/bar.log</code> until
	it is rolled over the next day.
	
	<tr>
	<td><code>'.'yyyy-MM-dd-a</code>
	
	<td>Rollover at midnight and midday of each day.</td>
	
	<td>At noon, on March 9th, 2002, <code>/foo/bar.log</code> will be
	copied to <code>/foo/bar.log.2002-03-09-AM</code>. Logging for the
	afternoon of the 9th will be output to <code>/foo/bar.log</code>
	until it is rolled over at midnight.
	
	<tr>
	<td><code>'.'yyyy-MM-dd-HH</code>
	
	<td>Rollover at the top of every hour.</td>
	
	<td>At approximately 11:00.000 o'clock on March 9th, 2002,
	<code>/foo/bar.log</code> will be copied to
	<code>/foo/bar.log.2002-03-09-10</code>. Logging for the 11th hour
	of the 9th of March will be output to <code>/foo/bar.log</code>
	until it is rolled over at the beginning of the next hour.
	
	
	<tr>
	<td><code>'.'yyyy-MM-dd-HH-mm</code>
	
	<td>Rollover at the beginning of every minute.</td>
	
	<td>At approximately 11:23,000, on March 9th, 2001,
	<code>/foo/bar.log</code> will be copied to
	<code>/foo/bar.log.2001-03-09-10-22</code>. Logging for the minute
	of 11:23 (9th of March) will be output to
	<code>/foo/bar.log</code> until it is rolled over the next minute.
	
	</table>

	<p>Do not use the colon ":" character in anywhere in the
	<b>DatePattern</b> option. The text before the colon is interpeted
	as the protocol specificaion of a URL which is probably not what
	you want.

	@author pmehta

*/

public class SoluminaRollingFileAppender extends FileAppender {

	// The code assumes that the following constants are in a increasing
	// sequence.
	static final int TOP_OF_TROUBLE = -1;
	static final int TOP_OF_MINUTE = 0;
	static final int TOP_OF_HOUR = 1;
	static final int HALF_DAY = 2;
	static final int TOP_OF_DAY = 3;
	static final int TOP_OF_WEEK = 4;
	static final int TOP_OF_MONTH = 5;
	static final String DELETION = "Deletion";
	static final String COMPRESSION = "Compression";
	
	private static final String HYPHEN = "-";
	
	private static String contextPath = EMPTY;
	private static final String DEFAULT_LOGFILE_NAME_PREFIX = "solumina";

	/**
	 * The date pattern. By default, the pattern is set to "'.'yyyy-MM-dd"
	 * meaning daily rollover.
	 */
	private String datePattern = "'.'yyyy-MM-dd";

	/**
	 * The log file will be renamed to the value of the scheduledFilename
	 * variable when the next interval is entered. For example, if the rollover
	 * period is one hour, the log file will be renamed to the value of
	 * "scheduledFilename" at the beginning of the next hour.
	 * 
	 * The precise time when a rollover occurs depends on logging activity.
	 */
	private String scheduledFilename;

	/**
	 * The next time we estimate a rollover should occur.
	 */
	private long nextCheck = System.currentTimeMillis() - 1;

	Date now = new Date();

	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'.'yyyy-MM-dd-HH-mm");

	int checkPeriod = TOP_OF_TROUBLE;

	// This object is used for Rolling
	SoluminaRollingCalendar rollingCalendar = new SoluminaRollingCalendar();

	// The gmtTimeZone is used only in computeCheckPeriod() method.
	static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

	/**
	 * The default maximum file size is 20MB.
	 */
	protected long maxFileSize = 20 * 1024 * 1024L;

	/**
	 * There is 5 backup file by default.
	 */
	protected int maxBackupIndex = 5;

	private long nextRollover = 0;

	/**
	 * By default, compression of backup files is false
	 */
	private boolean compressBackups = false;

	/**
	 * By default, Compression max days set to 1
	 */
	private int compressionMaxDays = 1;

	/**
	 * The next time we estimate a compression should occur.
	 */
	private long compressNextCheck = System.currentTimeMillis() - 1;

	// This object is used for compression
	SoluminaRollingCalendar compressionRollingCalendar = new SoluminaRollingCalendar(
			SoluminaRollingFileAppender.TOP_OF_DAY);

	/**
	 * By default, deletion of backup files is false
	 */
	private boolean deleteBackups = false;

	/**
	 * By default, Max number of days for Deletion is set to 7
	 */
	private int deletionMaxDays = 7;

	/**
	 * The next time we estimate a deletion should occur.
	 */
	private long deleteNextCheck = System.currentTimeMillis() - 1;
	
	// This object is used for Deletion
	SoluminaRollingCalendar deletionRollingCalendar = new SoluminaRollingCalendar(
			SoluminaRollingFileAppender.TOP_OF_DAY);

	public SoluminaRollingFileAppender() {
		setInitialLoggingDirectory();
	}

	private void setInitialLoggingDirectory() {
		String logFileDir = EMPTY;
		try (InputStream soluminaConfig = getClass().getClassLoader().getResourceAsStream(SOLUMINA_CONFIG_FILE)) {
			if (soluminaConfig != null) {
				Properties properties = new Properties();
				properties.load(soluminaConfig);
				logFileDir = (String) properties.get(SOLUMINA_LOG_DIR);
			}
		} catch (IOException e) {
			LogLog.warn(
					"Solumina configuration file(" + SOLUMINA_CONFIG_FILE + ") not found on application classpath!");
		}
		
		if (logFileDir != null && isNotEmpty(logFileDir)) {
			fileName = logFileDir;
		} else {
			String catalinaBase = System.getProperty("catalina.base");
			String catalinaHome = System.getProperty("catalina.home");
			if (catalinaBase != null && isNotEmpty(catalinaBase))
				fileName = catalinaBase + "/logs/logger.log";
			else if (catalinaHome != null && isNotEmpty(catalinaHome))
				fileName = catalinaHome + "/logs/logger.log";
			else
				fileName = System.getProperty("user.home") + "/SoluminaLogs/logger.log";
		}
	}

	/**
	 * Instantiate a FileAppender and open the file designated by filename. The
	 * opened filename will become the output destination for this appender.
	 * 
	 * The file will be appended to.
	 */
	public SoluminaRollingFileAppender(Layout layout, String filename) throws IOException {
		super(layout, filename);
	}

	/**
	 * Instantiate a SoluminaRollingFileAppender and open the file designated by
	 * filename. The opened filename will become the output destination for this
	 * appender.
	 * 
	 * If the append parameter is true, the file will be appended to. Otherwise,
	 * the file designated by filename will be truncated before being opened.
	 */
	public SoluminaRollingFileAppender(Layout layout, String filename, boolean append) throws IOException {
		super(layout, filename, append);
	}

	/**
	 * Instantiate a SoluminaRollingFileAppender and open the file designated by
	 * filename. The opened filename will become the output destination for this
	 * appender.
	 */
	public SoluminaRollingFileAppender(Layout layout, String filename, String datePattern) throws IOException {
		super(layout, filename, true);
		this.datePattern = datePattern;
		activateOptions();
	}

	public void activateOptions() {
		super.activateOptions();
		if (datePattern != null && fileName != null) {
			now.setTime(System.currentTimeMillis());
			int type = computeCheckPeriod();
			printPeriodicity(type);
			rollingCalendar.setType(type);
			File file = new File(fileName);
			scheduledFilename = fileName + simpleDateFormat.format(new Date(file.lastModified())) + '.' + 0;
			// nextCheck Should be changed here as we can change the roll over
			// time dynamically
			nextCheck = rollingCalendar.getNextCheckMillis(now);

		} else {
			LogLog.error("Either File or DatePattern options are not set for appender [" + name + "].");
		}
	}

	/**
	 * This method computes the roll over period by looping over the periods,
	 * starting with the shortest, and stopping when the r0 is different from
	 * from r1, where r0 is the epoch formatted according the datePattern
	 * (supplied by the user) and r1 is the epoch+nextMillis(i) formatted
	 * according to datePattern. All date formatting is done in GMT and not
	 * local format because the test logic is based on comparisons relative to
	 * 1970-01-01 00:00:00 GMT (the epoch).
	 * 
	 * @return
	 */
	int computeCheckPeriod() {
		SoluminaRollingCalendar soluminaRollingCalendar = new SoluminaRollingCalendar(gmtTimeZone, Locale.getDefault());
		// set sate to 1970-01-01 00:00:00 GMT
		Date epoch = new Date(0);
		if (datePattern != null) {
			for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
				simpleDateFormat.setTimeZone(gmtTimeZone); // do all date
															// formatting in GMT
				String r0 = simpleDateFormat.format(epoch);
				soluminaRollingCalendar.setType(i);
				Date next = new Date(soluminaRollingCalendar.getNextCheckMillis(epoch));
				String r1 = simpleDateFormat.format(next);
				// System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
				if (r0 != null && r1 != null && !r0.equals(r1)) {
					return i;
				}
			}
		}
		return TOP_OF_TROUBLE; // Deliberately head for trouble...
	}

	void printPeriodicity(int type) {
		switch (type) {
		case TOP_OF_MINUTE:
			LogLog.debug("Appender [" + name + "] to be rolled every minute.");
			break;
		case TOP_OF_HOUR:
			LogLog.debug("Appender [" + name + "] to be rolled on top of every hour.");
			break;
		case HALF_DAY:
			LogLog.debug("Appender [" + name + "] to be rolled at midday and midnight.");
			break;
		case TOP_OF_DAY:
			LogLog.debug("Appender [" + name + "] to be rolled at midnight.");
			break;
		case TOP_OF_WEEK:
			LogLog.debug("Appender [" + name + "] to be rolled at start of week.");
			break;
		case TOP_OF_MONTH:
			LogLog.debug("Appender [" + name + "] to be rolled at start of every month.");
			break;
		default:
			LogLog.warn("Unknown periodicity for appender [" + name + "].");
		}
	}

	/**
	 * This method differentiates SoluminaRollingFileAppender from its super
	 * class.
	 *
	 * Before actually logging, This method will check Following :
	 * 
	 * 1. Whether it is time to do a rollover. If it is, it will check whether
	 * the delete backups is scheduled or not. If delete backup is scheduled, it
	 * will delete the backup files and then schedule the next rollover time and
	 * then rollover. And if delete backup is not scheduled, it will schedule
	 * the next rollover time and then rollover.
	 * 
	 * 2. Whether file size reaches to it maximum limit or not. If it is, it
	 * will roll over the current file to backup file.
	 * 
	 * 3. Whether the compressio backup is enabled and scheduled. If it is, it
	 * will compress the backup files.
	 * 
	 */
	protected void subAppend(LoggingEvent event) {
		long n = System.currentTimeMillis();

		if (isDeleteBackups() && n >= deleteNextCheck) {
			now.setTime(n);
			deleteNextCheck = deletionRollingCalendar.getNextCheckMillis(now);
			try {
				cleanupAndRollOverTimely();
			} catch (IOException e) {
				if (e instanceof InterruptedIOException) {
					Thread.currentThread().interrupt();
				}
				LogLog.error("cleanupAndRollOver() failed.", e);
			}
		} else if (n >= nextCheck) {
			now.setTime(n);
			nextCheck = rollingCalendar.getNextCheckMillis(now);
			try {
				rollOverTimeBase();
			} catch (IOException ioe) {
				if (ioe instanceof InterruptedIOException) {
					Thread.currentThread().interrupt();
				}
				LogLog.error("rollOver() failed.", ioe);
			}
		}

		if (fileName != null && qw != null) {
			long size = ((CountingQuietWriter) qw).getCount();
			if (size >= maxFileSize && size >= nextRollover) {
				rollOverSizeBase();
			}
		}

		if (isCompressBackups() && n >= compressNextCheck) {
			now.setTime(n);
			compressNextCheck = compressionRollingCalendar.getNextCheckMillis(now);
			doCompression();
		}

		super.subAppend(event);
	}

	/**
	 * This method checks to see if we're exceeding the number of log backups
	 * that we are supposed to keep, and if so, deletes the offending files. It
	 * then calls the rolloverDailyBase method to rollover to a new file if
	 * required.
	 * 
	 * @throws IOException
	 */
	protected void cleanupAndRollOverTimely() throws IOException {
		// Delete old backup files
		doCompressionOrDeletion(DELETION);
		rollOverTimeBase();
	}

	private void doCompressionOrDeletion(String whatToDo) {
		int maximumDays = 0;
		if (StringUtils.equals(whatToDo, DELETION)) {
			maximumDays = getDeletionMaxDays();
		} else if (StringUtils.equals(whatToDo, COMPRESSION)) {
			maximumDays = getCompressionMaxDays();
		}
		Date cutoffDate = getCutoffDate(maximumDays);
		deleteOrCompressFiles(whatToDo, cutoffDate);
	}

	private void deleteOrCompressFiles(String whatToDo, Date cutoffDate) {
		File file = new File(fileName);
		if (file.getParentFile().exists()) {
			File[] files = file.getParentFile().listFiles(new StartsWithFileFilter(file.getName(), false));
			int nameLength = file.getName().length();
			for (int i = 0; i < files.length; i++) {
				String datePart = null;
				try {
					datePart = files[i].getName().substring(nameLength);
					Date date = simpleDateFormat.parse(datePart);
					if (date.before(cutoffDate)) {
						if (StringUtils.equals(whatToDo, DELETION)) {
							files[i].delete();
						} else if (StringUtils.equals(whatToDo, COMPRESSION)) {
							compressAndDeleteFile(files[i]);
						}
					}
				} catch (Exception pe) {
					// This file isn't named correctly or we should not delete
					// or compress it.
				}
			}
		}
	}

	private Date getCutoffDate(int maximumDays) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -maximumDays);
		Date cutoffDate = calendar.getTime();
		return cutoffDate;
	}

	/**
	 * Rollover the current file to a new file based on time.
	 */
	void rollOverTimeBase() throws IOException {

		/* Compute filename, but only if datePattern is specified */
		if (datePattern == null) {
			errorHandler.error("Missing DatePattern option in rollOver().");
			return;
		}

		String datedFilename = fileName + simpleDateFormat.format(now) + '.' + 0;
		// It is too early to roll over because we are still within the
		// bounds of the current interval. Rollover will occur once the
		// next interval is reached.
		if (scheduledFilename.equals(datedFilename)) {
			return;
		}

		File target = new File(scheduledFilename);
		closeAndRenameCurrentFile(target);

		try {
			// This will also close the file. This is OK since multiple
			// close operations are safe.
			super.setFile(fileName, true, this.bufferedIO, this.bufferSize);
		} catch (IOException e) {
			errorHandler.error("setFile(" + fileName + ", true) call failed.");
		}
		scheduledFilename = datedFilename;
	}

	private boolean closeAndRenameCurrentFile(File target) {
		// close current file, and rename it to datedFilename
		this.closeFile();

		if (target.exists()) {
			target.delete();
		}

		File file = new File(fileName);
		boolean result = file.renameTo(target);
		if (result) {
			LogLog.debug(fileName + " -> " + target.getName());
		} else {
			LogLog.error("Failed to rename [" + fileName + "] to [" + target.getName() + "].");
		}
		return result;
	}

	public// synchronization not necessary since doAppend is already synched
	void rollOverSizeBase() {
		File target;

		setNextRollOverSize();

		LogLog.debug("maxBackupIndex=" + maxBackupIndex);
		boolean renameSucceeded = true;
		// If maxBackups <= 0, then there is no file renaming to be done.
		if (maxBackupIndex > 0) {

			renameSucceeded = deleteOldestBackupFile();

			renameSucceeded = renameBackupFilesToNextIndex(renameSucceeded);

			if (renameSucceeded) {
				target = new File(fileName + simpleDateFormat.format(now) + '.' + 1);
				renameSucceeded = closeAndRenameCurrentFile(target);

				// if file rename failed, reopen file with append = true
				if (!renameSucceeded) {
					try {
						this.setFile(fileName, true, bufferedIO, bufferSize);
					} catch (IOException e) {
						if (e instanceof InterruptedIOException) {
							Thread.currentThread().interrupt();
						}
						LogLog.error("setFile(" + fileName + ", true) call failed.", e);
					}
				}
			}
		}

		// if all renames were successful, then
		if (renameSucceeded) {
			try {
				// This will also close the file. This is OK since multiple
				// close operations are safe.
				this.setFile(fileName, false, bufferedIO, bufferSize);
				nextRollover = 0;
			} catch (IOException e) {
				if (e instanceof InterruptedIOException) {
					Thread.currentThread().interrupt();
				}
				LogLog.error("setFile(" + fileName + ", false) call failed.", e);
			}
		}
	}

	private void setNextRollOverSize() {
		if (qw != null) {
			long size = ((CountingQuietWriter) qw).getCount();
			LogLog.debug("rolling over count=" + size);
			// if operation fails, do not roll again until
			// maxFileSize more bytes are written
			nextRollover = size + maxFileSize;
		}
	}

	private boolean renameBackupFilesToNextIndex(boolean renameSucceeded) {
		File target;
		File file;
		for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
			file = new File(fileName + simpleDateFormat.format(now) + '.' + i);
			if (file.exists()) {
				target = new File(fileName + simpleDateFormat.format(now) + '.' + (i + 1));
				LogLog.debug("Renaming file " + file + " to " + target);
				renameSucceeded = file.renameTo(target);
			}
		}
		return renameSucceeded;
	}

	private boolean deleteOldestBackupFile() {
		File file;
		boolean renameSucceeded = true;
		file = new File(fileName + simpleDateFormat.format(now) + '.' + maxBackupIndex);
		if (file.exists())
			renameSucceeded = file.delete();
		return renameSucceeded;
	}

	public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize)
			throws IOException {
		fileName = FilenameUtils.separatorsToSystem(fileName);
		if (fileName != null && isNotEmpty(fileName))
			fileName = addWebAppNameToFileName(fileName);
		super.setFile(fileName, append, this.bufferedIO, this.bufferSize);
		LogLog.debug("Log file path is set to : " + fileName);
		if (append) {
			File f = new File(fileName);
			((CountingQuietWriter) qw).setCount(f.length());
		}
	}

	private String addWebAppNameToFileName(String fileName) {
		String logFileNamePrefix = DEFAULT_LOGFILE_NAME_PREFIX;
		if (isNotEmpty(contextPath.trim())) {
			logFileNamePrefix = contextPath.trim().substring(1).replace("/", HYPHEN);
		}
		int index = fileName.lastIndexOf(File.separatorChar);
		String loggerFileName = fileName.substring(index + 1);
		if (!contains(loggerFileName, logFileNamePrefix))
			fileName = fileName.replace(loggerFileName, logFileNamePrefix + HYPHEN + loggerFileName);
		return fileName;
	}

	/**
	 * Compress the backup files based on the Time scheduled for compression
	 */
	public void doCompression() {
		// Compress the old backup files
		doCompressionOrDeletion(COMPRESSION);
	}

	/**
	 * Compresses the file to a .zip file, stores the .zip in the same directory
	 * as the passed file, and then deletes the original file
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void compressAndDeleteFile(File file) throws IOException {
		if (!file.getName().endsWith(".zip")) {
			File compressFile = new File(file.getParent(), file.getName() + ".zip");
			FileInputStream fileInputStream = new FileInputStream(file);
			FileOutputStream fileOutputStream = new FileOutputStream(compressFile);
			ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
			ZipEntry zipEntry = new ZipEntry(file.getName());
			zipOutputStream.putNextEntry(zipEntry);

			byte[] buffer = new byte[4096];
			while (true) {
				int bytesRead = fileInputStream.read(buffer);
				if (bytesRead == -1)
					break;
				else {
					zipOutputStream.write(buffer, 0, bytesRead);
				}
			}
			zipOutputStream.closeEntry();
			fileInputStream.close();
			zipOutputStream.close();
			file.delete();
		}
	}

	protected void setQWForFiles(Writer writer) {
		this.qw = new CountingQuietWriter(writer, errorHandler);
	}

	/** Returns the value of the DatePattern option. */
	public String getDatePattern() {
		return datePattern;
	}

	/**
	 * The DatePattern takes a string in the same format as expected by
	 * {@link SimpleDateFormat}. This options determines the rollover schedule.
	 */
	public void setDatePattern(String pattern) {
		datePattern = pattern;
	}

	/**
	 * Returns the value of the MaxBackupIndex option.
	 */
	public int getMaxBackupIndex() {
		return maxBackupIndex;
	}

	/**
	 * Set the maximum number of backup files to keep around.
	 * 
	 * The MaxBackupIndex option determines how many backup files are kept
	 * before the oldest is erased. This option takes a positive integer value.
	 * If set to zero, then there will be no backup files and the log file will
	 * be truncated when it reaches MaxFileSize.
	 */
	public void setMaxBackupIndex(int maxBackups) {
		this.maxBackupIndex = maxBackups;
	}

	/**
	 * Get the maximum size that the output file is allowed to reach before
	 * being rolled over to backup files.
	 * 
	 */
	public long getMaxFileSize() {
		return maxFileSize;
	}

	/**
	 * Set the maximum size that the output file is allowed to reach before
	 * being rolled over to backup files.
	 * 
	 * This method is equivalent to {@link #setMaxFileSize} except that it is
	 * required for differentiating the setter taking a long argument from the
	 * setter taking a String argument by the JavaBeans
	 * {@link java.beans.Introspector Introspector}.
	 * 
	 * @see #setMaximumFileSize(String)
	 */
	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	/**
	 * Set the maximum size that the output file is allowed to reach before
	 * being rolled over to backup files.
	 * 
	 * In configuration files, the MaxFileSize option takes an long integer in
	 * the range 0 - 2^63. You can specify the value with the suffixes "KB",
	 * "MB" or "GB" so that the integer is interpreted being expressed
	 * respectively in kilobytes, megabytes or gigabytes. For example, the value
	 * "10KB" will be interpreted as 10240.
	 */
	public void setMaximumFileSize(String value) {
		this.maxFileSize = OptionConverter.toFileSize(value, maxFileSize + 1);
	}

	/** Returns the value of the CompressBackups option. */
	public boolean isCompressBackups() {
		return compressBackups;
	}

	/** The CompressBackups takes a boolean */
	public void setCompressBackups(boolean compressBackups) {
		this.compressBackups = compressBackups;
	}

	/** Returns the value of CompressionMaxDays */
	public int getCompressionMaxDays() {
		return compressionMaxDays;
	}

	/** The CompressionMaxDays takes the int value as a number of days */
	public void setCompressionMaxDays(int compressionMaxDays) {
		this.compressionMaxDays = compressionMaxDays;
	}

	/** Returns the value of the DeleteBackups option. */
	public boolean isDeleteBackups() {
		return deleteBackups;
	}

	/** The DeleteBackups takes a boolean */
	public void setDeleteBackups(boolean deleteBackups) {
		this.deleteBackups = deleteBackups;
	}

	/** Returns the value of DeletionMaxDays */
	public int getDeletionMaxDays() {
		return deletionMaxDays;
	}

	/** The DeletionMaxDays takes the int value as a number of days */
	public void setDeletionMaxDays(int maxNumberOfDays) {
		this.deletionMaxDays = maxNumberOfDays;
	}
	
	public static String getContextPath() {
		return contextPath;
	}
	
	public static void setContextPath(String contextPath) {
		SoluminaRollingFileAppender.contextPath = contextPath;
	}

}

/**
 * StartsWithFileFilter is a helper class to SoluminaRollingFileAppender. Given
 * a File Name and include directories, it filters the files
 * 
 * @author pmehta
 *
 */
class StartsWithFileFilter implements FileFilter {
	private String startsWith;
	private boolean inclDirs = false;

	/**
	 * 
	 * @param startsWith
	 * @param includeDirectories
	 */
	public StartsWithFileFilter(String startsWith, boolean includeDirectories) {
		super();
		this.startsWith = startsWith.toUpperCase();
		inclDirs = includeDirectories;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File pathname) {
		if (!inclDirs && pathname.isDirectory()) {
			return false;
		} else
			return pathname.getName().toUpperCase().startsWith(startsWith);
	}
}

/**
 * SoluminaRollingCalendar is a helper class to SoluminaRollingFileAppender.
 * Given a periodicity type and the current time, it computes the start of the
 * next interval.
 * 
 * @author pmehta
 *
 */
class SoluminaRollingCalendar extends GregorianCalendar {
	private static final long serialVersionUID = -3560331770601814177L;

	int type = SoluminaRollingFileAppender.TOP_OF_TROUBLE;

	SoluminaRollingCalendar() {
		super();
	}

	SoluminaRollingCalendar(TimeZone tz, Locale locale) {
		super(tz, locale);
	}

	SoluminaRollingCalendar(int type) {
		super();
		this.type = type;
	}

	void setType(int type) {
		this.type = type;
	}

	public long getNextCheckMillis(Date now) {
		return getNextCheckDate(now).getTime();
	}

	public Date getNextCheckDate(Date now) {
		this.setTime(now);

		switch (type) {
		case SoluminaRollingFileAppender.TOP_OF_MINUTE:
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.MINUTE, 1);
			break;
		case SoluminaRollingFileAppender.TOP_OF_HOUR:
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.HOUR_OF_DAY, 1);
			break;
		case SoluminaRollingFileAppender.HALF_DAY:
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			int hour = get(Calendar.HOUR_OF_DAY);
			if (hour < 12) {
				this.set(Calendar.HOUR_OF_DAY, 12);
			} else {
				this.set(Calendar.HOUR_OF_DAY, 0);
				this.add(Calendar.DAY_OF_MONTH, 1);
			}
			break;
		case SoluminaRollingFileAppender.TOP_OF_DAY:
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.DATE, 1);
			break;
		case SoluminaRollingFileAppender.TOP_OF_WEEK:
			this.set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.WEEK_OF_YEAR, 1);
			break;
		case SoluminaRollingFileAppender.TOP_OF_MONTH:
			this.set(Calendar.DATE, 1);
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.MONTH, 1);
			break;
		default:
			throw new IllegalStateException("Unknown periodicity type.");
		}
		return getTime();
	}
}
