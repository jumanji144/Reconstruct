package me.darknet.resconstruct.info;

import java.util.Objects;

public class Member {
	public final int access;
	public final String name;
	public final String desc;

	public Member(int access, String name, String desc) {
		this.access = access;
		this.name = name;
		this.desc = desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Member member = (Member) o;
		return access == member.access && name.equals(member.name) && desc.equals(member.desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(access, name, desc);
	}
}
