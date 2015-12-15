package com.moosd.kitchensyncd.sync.proto;

import java.io.Serializable;

public class ReqFile implements Serializable, Comparable<ReqFile> {
	public String fname, checksum;
	public long ts;
	public long metaTs;

	public transient String ip;
	public transient int port;

	public ReqFile(String fname, long ts, String checksum) {
		this.fname = fname;
		this.ts = ts;
		this.checksum = checksum;
	}
	public ReqFile(String fname, long ts, long metaTs, String checksum) {
		this.fname = fname;
		this.ts = ts;
		this.metaTs = metaTs;
		this.checksum = checksum;
	}

	@Override
	public int compareTo(ReqFile arg0) {
		if(fname.equals(arg0.fname) && checksum.equals(arg0.checksum))
		return 0;
		else return (ts - arg0.ts<0?-1:1);
	}
	
	@Override
	public boolean equals(Object other) {
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof ReqFile))return false;
		ReqFile arg0 = (ReqFile) other;
		if(fname.equals(arg0.fname) && checksum.equals(arg0.checksum))
		return true;
		else return false;
	}
	
	@Override
	public int hashCode() {
        int hash = 1;
        hash = hash * 17 + checksum.hashCode();
        hash = hash * 31 + fname.hashCode();
	    return hash;
	}
}