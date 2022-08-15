package me.darknet.resconstruct.info;

/**
 * Method information wrapper.
 */
public class MethodMember extends AbstractMember {
	public MethodMember(int access, String name, String desc) {
		super(access, name, desc);
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public String toString() {
		return "Method: " + name + desc;
	}
}
