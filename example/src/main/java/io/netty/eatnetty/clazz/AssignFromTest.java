package io.netty.eatnetty.clazz;

import java.util.Date;

public class AssignFromTest {
	public static void main(String[] args) {
		Date d1 = new Date();
		Date d2 = new Date();
		
		java.sql.Date d3 = new java.sql.Date(2323l);
		
		System.out.println(d1.getClass().isAssignableFrom(d2.getClass()));
		
		// java.util.DateÊÇjava.sql.DateµÄ¸¸Àà
		System.out.println(d1.getClass().isAssignableFrom(d3.getClass()));
		
		System.out.println(d3.getClass().isAssignableFrom(d1.getClass()));
	}
} 
