package com.ibaset.common.util.gettext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;

/**
 * Utility class for creating GNU gettext MO files. 
 * */
public class GettextObject {

	private TreeMap<String, String> polines;
	
	private static final int MAGIC =  0x950412de;

	public GettextObject() {
		super();
		this.polines = new TreeMap<String, String>();
		polines.put("", "MIME-Version: 1.0\nContent-Type: text/plain; charset=UTF-8\nContent-Transfer-Encoding: 8bit\n");
	}

	public void addLine(String msgid, String msgstr){
		polines.put(msgid, msgstr);
	}
	
	public int size(){
		return polines.size();
	}
	
	static void writeInt(OutputStream os, int i) throws IOException {
	    os.write((i) & 0xFF);
	    os.write((i >>> 8) & 0xFF);
	    os.write((i >>> 16) & 0xFF);
	    os.write((i >>> 24) & 0xFF);
	}
	
	public void writeToMO(OutputStream os) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    int size = polines.size();
	    int[] indices = new int[size*2];
	    int[] lengths = new int[size*2];
	    int idx = 0;
	    // write the strings and translations to a byte array and remember offsets and length in bytes
	    for (String key : polines.keySet()) {
	        byte[] utf = key.getBytes("utf-8");
	        indices[idx] = bos.size();
	        lengths[idx] = utf.length;
	        bos.write(utf);
	        bos.write(0);
	        idx++;
	    }
	    for (String val : polines.values()) {
	        byte[] utf = val.getBytes("utf-8");
	        indices[idx] = bos.size();
	        lengths[idx] = utf.length;
	        bos.write(utf);
	        bos.write(0);
	        idx++;
	    }
	    try {
	        int headerLength = 7*4;
	        int tableLength = size*2*2*4;
	        writeInt(os, MAGIC);                   		// magic
	        writeInt(os, 0);                            // file format revision
	        writeInt(os, size);                         //number of strings
	        writeInt(os, headerLength);                 // offset of table with original strings
	        writeInt(os, headerLength + tableLength/2); // offset of table with translation strings
	        writeInt(os, 0);                            // size of hashing table
	        writeInt(os, headerLength + tableLength);   // offset of hashing table, not used since length is 0

	        for (int i=0; i<size*2; i++) {
	            writeInt(os, lengths[i]);
	            writeInt(os, headerLength + tableLength + indices[i]);
	        }

	        // copy keys and translations
	        bos.writeTo(os);

	    } finally {
	        os.close();
	    }	    
	}
	
}
