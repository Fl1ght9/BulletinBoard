package part2_multi;

import java.util.Random;

import java.util.concurrent.atomic.AtomicReference;

import part2_multi.Master;
import part2_multi.BB.Type;

public class Provider extends Thread implements User{
	public static final int MAX_INT = 3;
	public static final int MAX_USES = 5;
	public static final int ATTEMPTS = 50;
	public static boolean running = true;
	private static Random r = new Random();
	
	private BB<Integer> bulletin;
	
	private AtomicReference<Integer> request = new AtomicReference<Integer>(null);
	private AtomicReference<BB<Integer>.BulletinItem> post = new AtomicReference<BB<Integer>.BulletinItem>();
	
	public Provider(BB<Integer> bulletin, String name){
		super(name);
		this.bulletin = bulletin;
	}
	
	@Override
	public void run(){
		while(running){
			if (request.get() == null){
				request.set(r.nextInt(MAX_INT));
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

	//Post a service to the Bulletin Board
	private void postBulletin() throws InterruptedException{
		post.compareAndSet(null, bulletin.post(request.get(), Type.PROVIDED, this, r.nextInt(MAX_USES)+1));
	}
	
	private BB<Integer>.BulletinItem checkBulletin() throws InterruptedException{
		if (request.get() == null){
			return null;
		}		
		for (BB<Integer>.BulletinItem b : bulletin.view()){
			//Skip other requests
			if (b.getType() == Type.PROVIDED){
				continue;
			}			
			Integer req = request.get();
			if (req != null && req.equals(b.getValue())){
				return b;
			}
		}
		
		return null;
	}

	@Override
	public boolean match(User user, BB<Integer>.BulletinItem item) throws InterruptedException {
		if (user != this && user instanceof Provider){ System.err.println("Provider matched with Provider");}
		if (item == null || post.get() != item){return false;}
		if (item.hasRemaining()){return true;}
		
		boolean success = post.compareAndSet(item, null);
		
		//Match successful
		if (success){
			request.set(null);
			bulletin.remove(item);
		}		
		return success;
	}
}