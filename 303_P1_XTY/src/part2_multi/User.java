package part2_multi;

public interface User {
	
	/**
	 *
	 * Match user to service
	 * @param user
	 *
	 */
	public boolean match(User user, BB<Integer>.BulletinItem item) throws InterruptedException;
}
