/**
 * Proprietary and Confidential
 * Copyright 1995-2014 iBASEt, Inc.
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
package com.ibaset.common.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class ThreadUtils 
{
	/**
	 * This should be used only when the whole context is coming down.
	 * Enumerate all active threads and issue deprecated stop signal to all threads
	 * This should be used only when the whole context is coming down.
	 */
	@SuppressWarnings("deprecation")
	public static void stopAllActiveThreads()
	{
		Thread[] activeThreads = new Thread[Thread.activeCount()];
		Thread.enumerate(activeThreads);
		for (Thread thisThread : activeThreads)
		{
			if (thisThread != null)
			{
				cleanThreadLocals(thisThread);
				try
				{
					thisThread.stop();
				}
				catch (Throwable t)
				{
					// Do nothing
				}
			}
		}	
	}
	
	/**
	 * This should be used only when the whole context is coming down.
	 * Enumerate all active threads and issue deprecated stop signal to threads
	 * whose name contains the given nameString
	 * @param Sub-string of the thread name (case sensitive)
	 */
	@SuppressWarnings("deprecation")
	public static void stopThreads(String threadName)
	{
		Thread[] activeThreads = new Thread[Thread.activeCount()];
		Thread.enumerate(activeThreads);
		for (Thread thisThread : activeThreads)
		{
			if (thisThread != null && thisThread.getName().contains(threadName))
			{
				cleanThreadLocals(thisThread);
				try
				{
					thisThread.stop();
				}
				catch (Throwable t)
				{
					// Do nothing
				}
			}
		}
	}
	
	/**
	 * This should be used only when the whole context is coming down.
	 */
	public static void cleanAllThreadLocals()
	{
		Thread[] activeThreads = new Thread[Thread.activeCount()];
		Thread.enumerate(activeThreads);
		for (Thread t : activeThreads)
		{
			if (t != null)
			{
				cleanThreadLocals(t);
			}
		}	
	}
	
	@SuppressWarnings("rawtypes")
	private static void cleanThreadLocals(Thread t)
	{
		try
		{
			Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
			threadLocalsField.setAccessible(true);

			Class threadLocalMapKlazz = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
			Field tableField = threadLocalMapKlazz.getDeclaredField("table");
			tableField.setAccessible(true);

			Object fieldLocal = threadLocalsField.get(t);
			if (fieldLocal == null) {
				return;
			}
			Object table = tableField.get(fieldLocal);

			int threadLocalCount = Array.getLength(table);

			for (int i = 0; i < threadLocalCount; i++) 
			{
				Object entry = Array.get(table, i);
				if (entry != null) 
				{
					Field valueField = entry.getClass().getDeclaredField("value");
					valueField.setAccessible(true);
					Object value = valueField.get(entry);
					if (value != null) 
					{
						System.out.println("Class name for map value: "+ value.getClass().getName());
						valueField.set(entry, null);
					}

				}
			}
		}
		catch(Throwable e)
		{
			// Do nothing on any exception
		}
	}

}
