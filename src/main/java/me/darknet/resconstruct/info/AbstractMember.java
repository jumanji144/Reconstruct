package me.darknet.resconstruct.info;

import java.util.Objects;

/**
 * Common member type for fields and methods.
 *
 * @see FieldMember
 * @see MethodMember
 */
public abstract class AbstractMember {
	public final int access;
	public final String name;
	public final String desc;

	/**
	 * @param access
	 * 		Member access flags.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public AbstractMember(int access, String name, String desc) {
		this.access = access;
		this.name = name;
		this.desc = desc;
	}

	public abstract boolean isField();

	public boolean isMethod() {
		return !isField();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractMember member = (AbstractMember) o;
		return access == member.access && name.equals(member.name) && desc.equals(member.desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(access, name, desc);
	}
}
