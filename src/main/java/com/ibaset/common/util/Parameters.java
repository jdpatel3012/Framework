package com.ibaset.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hierarchical parameter storage.
 * */
public class Parameters {

	private Map<String, ArrayList<String>> index;
	
	public Parameters(String text) throws IOException
	{
		parse(text);
	}
	public Parameters()
	{
		index = Collections.emptyMap();
	}
	private ArrayList<String> addPath(String path)
	{
		ArrayList<String> list = index.get(path);
		if(list == null) 
		{
			list = new ArrayList<String>();
			index.put(path, list);
		}
		return list;
	}
	/**
	 * Parse configuration from formatted text.
	 * */
	public void parse(String text) throws IOException
	{
		index = new HashMap<String, ArrayList<String>>();
		BufferedReader r=new BufferedReader(new StringReader(text));
		String line = null;
		int lineCounter=0;
		String currentSection= null;
		while( (line=r.readLine())!=null )
		{
			++lineCounter;
			int len = line.length();
			int depth = 0;
			while(depth < len)
			{
				if(line.charAt(depth)==' ') ++depth;
				else
				{
					if(depth == 0)
					{
						//new section
						if(line.charAt(0)=='[')
						{
							line = line.trim();
							len = line.length();
							currentSection = line.substring(1, len-1);
							addPath(currentSection);
						}
						//new name in section
						else
						{
							if(currentSection == null) throw new IOException("No section found in line "+lineCounter);
							index.get(currentSection).add(line);
						}
					}
					//new sub name 
					else
					{
						if(currentSection == null) throw new IOException("No section found in line "+lineCounter);
						String name = line.trim();
						StringBuilder sb = new StringBuilder();
						sb.append(currentSection);
						for(int i=0;i<depth;++i)
						{
							ArrayList<String> list = index.get(sb.toString());
							if(list == null) throw new IOException("No parent found for "+name+" in line "+lineCounter);
							sb.append('.');
							sb.append(list.get(list.size()-1));
						}
						ArrayList<String> list = addPath(sb.toString());
						list.add(name);
					}
					break;
				}
			}
		}
	}
	/**
	 * Returns the value of a parameter by path and name. Returns defaultValue if no value found.
	 * @param path Period delimited list of key strings.  E.g. College.Course.Student
	 * @param name parameter name
	 * @param defaultValue default value
	 * */
	public String getStringValue(final String path,final String name, String defaultValue)
	{
		ArrayList<String> list = index.get(path);
		if(list!=null)
		{
			for(String s:list)
			{
				int valueIndex = name.length() + 1;
				if(s.startsWith(name) && s.length() > valueIndex && s.charAt(valueIndex-1)=='=')
				{
					return s.substring(valueIndex);
				}
			}
		}
		return defaultValue;
	}
	
	/**
	 * Returns the value of a parameter by path. Returns defaultValue if no value found.
	 * @param path Period delimited list of key strings.  E.g. College.Course.Student
	 * @param defaultValue default value
	 * @throws IllegalArgumentException if path does not include parameter name
	 * */
	public String getStringValue(final String path, String defaultValue)
	{
		int i=path.lastIndexOf('.');
		if(i==-1) throw new IllegalArgumentException("Path must include parameter name: "+path);
		return getStringValue(path.substring(0, i), path.substring(i+1), defaultValue);
	}
	
	/**
	 * Returns list of strings by path. Returns null if no strings found.
	 * @param path Period delimited list of key strings.  E.g. College.Course.Student
	 * */
	public List<String> getStringList(final String path)
	{
		return index.get(path);
	}
	
	private void collectNames(String path, int depth, ArrayList<String> list, StringBuilder sb)
	{
		for(String s : list)
		{
			for(int i=0;i<depth;++i) sb.append(' ');
			sb.append(s).append('\n');
			String subPath = path+'.'+s;
			if(index.containsKey(subPath)) collectNames(subPath, depth + 1, index.get(subPath), sb);
		}
	}
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		for(String path : index.keySet())
		{
			ArrayList<String> list = index.get(path);
			int i = path.indexOf('.');
			if(i==-1)
			{
				sb.append('[').append(path).append(']').append('\n');
				collectNames(path, 0, list, sb);
			}
		}
		return sb.toString();
	}

}
