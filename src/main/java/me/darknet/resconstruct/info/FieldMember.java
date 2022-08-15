package me.darknet.resconstruct.info;

/**
 * Field information wrapper.
 */
public class FieldMember extends AbstractMember {
	public FieldMember(int access, String name, String desc) {
		super(access, name, desc);
	}

	@Override
	public boolean isField() {
		return true;
	}

	@Override
	public String toString() {
		return "Field: " + name + " " + desc;
	}
}
