package simulation;

import java.util.Random;

public class Person extends Creature implements Named {
	private static final Random r = new Random();
	private final String name;

	public Person(String name) {
		this.name = name;
	}

	@Override
	protected String getUpdateStatus() {
		String prefix = "[Person:" + getName() + "] ";
		if (r.nextBoolean())
			return prefix + " ponders life";
		return prefix + "continues to exist";
	}

	@Override
	public String getName() {
		return name;
	}
}
