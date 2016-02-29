package part1_single;

import java.util.Collection;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

/**
*
* Bulletin Board made from a list consisting of clients and providers
* able to add and remove services
*
*/
public class BB<E> {
	enum Type{ 
		PROVIDED, REQUESTED 
	}
	
	private List<BulletinItem> board = new LinkedList<BulletinItem>();

	private Semaphore read = new Semaphore(1, true);
	private Semaphore write = new Semaphore(1, true);

	private AtomicInteger num = new AtomicInteger(0);
	
	/**
	 *
	 * Post a service to Bulletin Board
	 *
	 * @param request
	 * @return
	 *
	 */
	public BulletinItem post(E item, Type type, User auth) throws InterruptedException{
		if (item == null){
			return null;
		}	
		BulletinItem object = new BulletinItem(item, type, auth);
		this.add(object);
		return object;
	}
	
	/**
	 * 
	 * Add an item to the board
	 *
	 * @param item
	 * @throws InterruptedException
	 *
	 */
	private void add(BulletinItem item) throws InterruptedException{
		read.acquire();
		write.acquire();
		board.add(item);
		write.release();
		read.release();
	}

	/**
	 *
	 * Preview list of services available on Bulletin Board
	 *
	 * @return
	 *
	 */
	public Collection<BulletinItem> view() throws InterruptedException{
		Collection<BulletinItem> copy;
		read.acquire();
		if(num.incrementAndGet() == 1){
			write.acquire();
		}
		read.release();
		
		//Copy of the board
		copy = new LinkedList<BulletinItem>(board);		
		if(num.decrementAndGet() == 0){
			write.release();
		}
		return copy;
	}
	
	/**
	 *
	 * Remove an item from the board
	 *
	 * @param item
	 * @throws InterruptedException
	 *
	 */
	public void remove(BulletinItem item) throws InterruptedException{
		read.acquire();
		write.acquire();		
		board.remove(item);		
		write.release();
		read.release();
	}
	
	/**
	 * 
	 * Bulletin Board item class
	 *
	 */
	public class BulletinItem implements Comparable<BulletinItem>{
		private final E value;
		private final Type type;
		private final User auth;
		
		private AtomicBoolean available;
		
		public BulletinItem(E value, Type type, User auth){
			this.value = value;
			this.type = type;
			this.auth = auth;
			available = new AtomicBoolean(false);
		}
		
		//Requests success of failure
		public boolean request() throws InterruptedException{
			boolean success = available.compareAndSet(false, true);
			//true or false checker
			if (success){
				BB.this.remove(this);
			}		
			return success;
		}

		//Helpers
		public E getValue(){
			return value;
		}
		public Type getType(){
			return type;
		}
		public User getauth(){
			return auth;
		}

		/**
		*
		* Compare to method comparing values to string
		*
		*/
		@Override
		public int compareTo(BulletinItem o) {
			return value.toString().compareTo(o.value.toString());
		}
		
		@Override
		public String toString(){
			String type = this.type == Type.PROVIDED ? "P" : "R";
			return type+value.toString();
		}
	}
}