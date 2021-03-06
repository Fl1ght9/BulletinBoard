package part1_single;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import part1_single.BB.Type;

/**
 * A client to interact with the Bulletin board, and request services
 *
 */
public class Client extends Thread implements User{
	public static final int MAX_INT = 10;
	public static final int ATTEMPTS = 20;
	public static boolean running = true;
	private static Random r = new Random();
	
	private BB<Integer> bulletin;
	
	private AtomicReference<Integer> request = new AtomicReference<Integer>(null);
	private AtomicReference<BB<Integer>.BulletinItem> post = new AtomicReference<BB<Integer>.BulletinItem>();
	
	public Client(BB<Integer> bulletin, String name){
		super(name);
		this.bulletin = bulletin;
	}
	
	@Override
	public void run(){
		int go = 0;
		while(running){
			if (++go == ATTEMPTS){
				try{
					Master.getInstance().recordDroppedPost();
					this.match(this, post.get());
				} catch (InterruptedException e){
				}
			}			
			if (request.get() == null){
				request.set(r.nextInt(MAX_INT));
				go = 0;
			}

			try{
				//Sleep thread for a random amount of time
				Thread.sleep(r.nextInt(1000));		
				if (post.get() == null && r.nextBoolean()){
					postBulletin();					
				} else {
					BB<Integer>.BulletinItem b = this.checkBulletin();					
					if (b == null){
						//No post found matched
						if (post == null){
							postBulletin();
						}						
					} else {
						//Else sleep the thread for another random amount of time
						Thread.sleep(r.nextInt(500));					
						if (b.request()){
							if (b.getauth().match(this, b)){
								Master.getInstance().recordMatch();
								this.match(b.getauth(), post.get());
							} else {
								Master.getInstance().recordFailedMatch();
							}
						} else {
							Master.getInstance().recordFailedMatch();
						}
					}
				}
			} catch (InterruptedException e){
				System.out.println(e);
			}		
		}
	}
	
	private BB<Integer>.BulletinItem checkBulletin() throws InterruptedException{
		if (request.get() == null){
			return null;
		}	
		for (BB<Integer>.BulletinItem b : bulletin.view()){
			//Skip other requests
			if (b.getType() == Type.REQUESTED){
				continue;
			}			
			Integer req = request.get();
			if (req != null && req.equals(b.getValue())){
				return b;
			}
		}		
		return null;
	}
	
	//Post a service to the Bulletin Board
	private void postBulletin() throws InterruptedException{
		post.compareAndSet(null, bulletin.post(request.get(), Type.REQUESTED, this));
	}

	//Matches the provider and client through user and item
	@Override
	public boolean match(User user, BB<Integer>.BulletinItem item) throws InterruptedException {
		if (user != this && user instanceof Client){ 
			System.err.println("Client matched with Client");
		}
		if (item == null || item != post.get()){
			return false;
		}
		boolean success = post.compareAndSet(item, null);		
		//Match successful
		if (success){
			System.out.println(this.getName()+" matched with "+((Thread)user).getName()+" on value "+item.getValue());
			request.set(null);
			bulletin.remove(item);
		}		
		return success;
	}
}