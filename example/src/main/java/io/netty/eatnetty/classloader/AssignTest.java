package io.netty.eatnetty.classloader;

import java.lang.reflect.Field;

public class AssignTest {
	public static void main(String[] args) throws Exception {
		Assign assign = new Assign();
		
		Field[] fields = assign.getClass().getDeclaredFields();
		for(Field f : fields){
			// setAccessible真是一个残暴的东西啊
			f.setAccessible(true);
			
			System.out.println(f.getName() + ":" + f.get(assign));
		}
	}
}
class Assign{
	private String code = "ccooddee";
	
	public String name = "nnaammee";

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
