package simulation;

import java.util.HashSet;
import java.util.Set;

public class World {
	private final Set<Living> livingThings = new HashSet<>();

	public static World create() {
		return new World();
	}

	public void update() {
		for (Living thing : new HashSet<>(livingThings)) {
			thing.update(this);
		}
	}

	public void spawn(Living living) {
		livingThings.add(living);
	}

	public void despawn(Living living) {
		livingThings.remove(living);
	}
}
