package com.moosd.kitchensyncd.sync.proto;

import java.io.Serializable;

public class FileSend implements Serializable  {
	public String fname, checksum;
	public byte[] data;
	public long ts;
	public long metaTs;
	public boolean err;
	public int chunk;
}