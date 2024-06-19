package simulation;

public class Creature implements Living {
	private int age;

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public void update(World world) {
		System.out.println(getUpdateStatus());
		age++;
		if (age > 100) {
			world.despawn(this);
		}
	}

	protected String getUpdateStatus() {
		return "[Creature:" + hashCode() + "] continues to exist";
	}
}
