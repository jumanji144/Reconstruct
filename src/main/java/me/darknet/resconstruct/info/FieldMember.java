package me.darknet.resconstruct.info;

public class FieldMember extends Member {
	public FieldMember(int access, String name, String desc) {
		super(access, name, desc);
	}

	@Override
	public String toString() {
		return "Field: " + name + " " + desc;
	}
}