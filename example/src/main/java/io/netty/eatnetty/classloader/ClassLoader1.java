package io.netty.eatnetty.classloader;

public class ClassLoader1 {
	public static void main(String[] args) {
		try {
			Class.forName("io.netty.eatnetty.classloader.Loader1", false, ClassLoader.getSystemClassLoader());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}