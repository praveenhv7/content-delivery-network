package edu.neu.fcn.dto;

import java.util.Date;

public class URLMapper {
	
	private String contentPath;
	private long size;
	private String lruCount;
	private long lastUsedDate;
	
	public long getLastUsedDate() {
		return lastUsedDate;
	}
	public void setLastUsedDate(long lastUsed) {
		this.lastUsedDate = lastUsed;
	}
	public String getContentPath() {
		return contentPath;
	}
	public void setContentPath(String content) {
		this.contentPath = content;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getLruCount() {
		return lruCount;
	}
	public void setLruCount(String lruCount) {
		this.lruCount = lruCount;
	}
	
	

}
