package de.l3s.eumssi.ml;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class MyUtil {
	
	String indexpath = "/home/gtran/WikiTimesLucene/NewsArticlesIndexes";
    
	/**
	 * @param _fromdate
	 * @param _todate
	 * @return the absolute difference in date between 2 dates
	 * format yyyy-MM-dd
	 */
	public static int dateDiff(String _fromdate, String _todate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date fdate = formatter.parse(_fromdate);
			Date tdate = formatter.parse(_todate);
			return (int) Math.abs((tdate.getTime() - fdate.getTime())/1000/60/60/24);
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * sorting keys by values 
	 * @param keys
	 * @param h
	 */
	public static void qsort(ArrayList<String> keys, HashMap<String, Integer> h, int lo, int hi) {
		int l = lo, r = hi;
		int pivot  = h.get(keys.get((lo + hi)/2));
		while (l<=r) {
			while (h.get(keys.get(l)) > pivot ) l++;
			while (h.get(keys.get(r)) < pivot ) r--;
			if (l<=r) {
				String tmp = keys.get(l);
				keys.set(l, keys.get(r));
				keys.set(r, tmp);
				l++; r--;
			}
		}
		
		if (lo< r) qsort(keys, h, lo, r);
		if (l <hi) qsort(keys, h, l, hi);
	}
	
	public static void qsortInt(ArrayList<Integer> keys, HashMap<Integer, Integer> h, int lo, int hi) {
		int l = lo, r = hi;
		int pivot  = h.get(keys.get((lo + hi)/2));
		while (l<=r) {
			while (h.get(keys.get(l)) > pivot ) l++;
			while (h.get(keys.get(r)) < pivot ) r--;
			if (l<=r) {
				int tmp = keys.get(l);
				keys.set(l, keys.get(r));
				keys.set(r, tmp);
				l++; r--;
			}
		}
		
		if (lo< r) qsortInt(keys, h, lo, r);
		if (l <hi) qsortInt(keys, h, l, hi);
	}
	
	public static void qsortDouble(ArrayList<String> keys, HashMap<String, Double> h, int lo, int hi) {
		int l = lo, r = hi;
		double pivot  = h.get(keys.get((lo + hi)/2));
		while (l<=r) {
			while (h.get(keys.get(l)) > pivot ) l++;
			while (h.get(keys.get(r)) < pivot ) r--;
			if (l<=r) {
				String tmp = keys.get(l);
				keys.set(l, keys.get(r));
				keys.set(r, tmp);
				l++; r--;
			}
		}
		
		if (lo< r) qsortDouble(keys, h, lo, r);
		if (l <hi) qsortDouble(keys, h, l, hi);
	}
	
	public static void main(String[] args) {
		//test
		HashMap<String, Double > test = new HashMap<String, Double> ();
		test.put("1", 0.0);
		test.put("2", 0.0);
		test.put("3", 0.0);
		test.put("4", 0.0);
		ArrayList<String> keys = new ArrayList<String> ();
		keys.addAll(test.keySet());
		qsortDouble(keys, test, 0, keys.size()-1);
		for (int i = 0; i<keys.size(); i++) System.out.println(keys.get(i));
	}
}
