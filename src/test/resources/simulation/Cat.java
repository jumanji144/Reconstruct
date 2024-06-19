package simulation;

import java.util.Random;

public class Cat extends Creature implements Named {
	private static final Random r = new Random();
	private final String name;

	public Cat(String name) {
		this.name = name;
	}

	@Override
	protected String getUpdateStatus() {
		String prefix = "[Cat:" + getName() + "] ";
		if (r.nextBoolean())
			return prefix + " goes *meow*";
		return prefix + "continues to exist";
	}

	@Override
	public String getName() {
		return name;
	}
}
