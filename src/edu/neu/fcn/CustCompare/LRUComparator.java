package edu.neu.fcn.CustCompare;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import edu.neu.fcn.dto.URLMapper;

public class LRUComparator implements Comparator<Entry<String,URLMapper>>{

	
	@Override
	public int compare(Entry<String, URLMapper> o1, Entry<String, URLMapper> o2) {
		// 
		URLMapper mapper1=o1.getValue();
		URLMapper mapper2=o2.getValue();
		int difference=0;
		
		Long lruCount1=Long.parseLong(mapper1.getLruCount());
		Long lruCount2=Long.parseLong(mapper2.getLruCount());
		
		Long time1=mapper1.getLastUsedDate();
		Long time2=mapper2.getLastUsedDate();
		
		if(lruCount1==lruCount2)
			difference=(int) (time1-time2);
		else
			difference=(int) (lruCount1-lruCount2);
		return difference;
	}

}
