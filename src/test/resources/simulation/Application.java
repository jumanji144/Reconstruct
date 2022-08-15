package simulation;

import java.util.Random;

public class Application {
	private static final Random r = new Random();
	private static final int MAX_TICKS = 1000;
	private static final String[] CAT_NAMES = {
			"Tilly", "Max", "Bell", "Thurston Waffles"
	};
	private static final String[] PERSON_NAMES = {
			"Alex", "Steve", "John", "Daniel"
	};

	public static void main(String[] args) {
		int i = 0;
		// We will use a static method to create the world, but a default constructor should still be generated
		// (at least as a fallback) since it is not an interface.
		World world = World.create();
		while (i < MAX_TICKS) {
			int v = r.nextInt(100);
			// Variable 'spawned'
			//  - Assigned to "Cat" and "Person"
			//  - Should infer both are "Living" since variable called on with 'World.spawn(Living)'
			Living spawned = null;
			switch (v) {
				case 0:
					// Named/Cat
					spawned = new Cat(CAT_NAMES[r.nextInt(CAT_NAMES.length)]);
					break;
				case 10:
					// Named/Person
					spawned = new Person(PERSON_NAMES[r.nextInt(PERSON_NAMES.length)]);
					break;
				case 50:
				case 51:
				case 52:
					// Creature(Nameless)
					spawned = new Creature();
					break;
			}
			if (spawned != null) {
				world.spawn(spawned);
				// Casting to an interface, calling 'getName()' should imply the type 'Named' is an interface (not a class)
				//  - Because the compiler will emit an 'invoke-interface'
				if (spawned instanceof Named) {
					Named named = (Named) spawned;
					System.out.println(named.getClass().getSimpleName() + " '" + named.getName() + "' spawned at i=" + i);
				}
			}
			i++;
			world.update();
		}
	}
}
