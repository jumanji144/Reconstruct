package me.darknet.resconstruct.info;

/**
 * Method information wrapper.
 */
public class MethodMember extends AbstractMember {
	private boolean isInterface;

	public MethodMember(int access, String name, String desc) {
		super(access, name, desc);
	}

	/**
	 * @return {@code true} when the method is accessed via {@link org.objectweb.asm.Opcodes#INVOKEINTERFACE}.
	 */
	public boolean isInterface() {
		return isInterface;
	}

	/**
	 * Mark method as an interface declaration.
	 */
	public void setInterface() {
		isInterface = true;
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
