package me.darknet.resconstruct.info;

public class MethodMember extends Member{
	public MethodMember(int access, String name, String desc) {
		super(access, name, desc);
	}

	@Override
	public String toString() {
		return "Method: " + name + desc;
	}
}
