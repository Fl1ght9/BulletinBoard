package part2_multi;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * The Main of the program as well as to stat track
 *
 */
public class Master {
	public static final int NUM_PROVIDERS = 100;
	public static final int NUM_CLIENTS = 100;

	private int postCount = 0;
	private int requestCount = 0;
	private int serviceCount = 0;
	
	private static Master _instance = new Master();
	private Master(){
	}
	
	private AtomicInteger matchCount = new AtomicInteger(0);
	private AtomicInteger failedCount = new AtomicInteger(0);
	private AtomicInteger droppedCount = new AtomicInteger(0);
	
	public void getPosts(BB<Integer> board) throws InterruptedException{
		Collection<BB<Integer>.BulletinItem> items = board.view();
		postCount = items.size();
		
		serviceCount = 0;
		requestCount = 0;
		
		for (BB<Integer>.BulletinItem b : items){
			if (b.getType() == BB.Type.PROVIDED){
				serviceCount++;
			} else {
				requestCount++;
			}
		}
		
		LinkedList<BB<Integer>.BulletinItem> list = (LinkedList<BB<Integer>.BulletinItem>)items;
		Collections.sort(list);
		System.out.println(list.toString());
	}
	
	private void watch(BB<Integer> board, List<Thread> pool){
		int intervals = 0;
		
		//Keep posting as long as intervals is less than 10 (i.e. post for 10 times)
		while(intervals++ < 10){
			try{
				//Sleep threads for 5000ms
				Thread.sleep(5000);

				//Thread states
				int stopped = 0;
				int blocked = 0;
				int active = 0;
				int waiting = 0;
				for (Thread t : pool){
					if (!t.isAlive()){
						stopped++;
					} else if (t.getState() == State.BLOCKED){
						blocked++;
					} else if (t.getState() == State.RUNNABLE){
						active++;
					} else if (t.getState() == State.TIMED_WAITING || t.getState() == State.WAITING){
						waiting++;
					} else {
						System.out.println(t.getState().toString());
					}
				}
				
				System.out.println((intervals*10)+" : THREADS "+stopped+" dead, "+blocked+" blocked, "+active+" runnable, "+waiting+" waiting");
				getPosts(board);
				
				System.out.println((intervals*10)+" : Successful Matches "+matchCount.get()+" : Failed Matches "+failedCount.get()+" : Dropped Posts "+droppedCount.get());
				System.out.println((intervals*10)+" : Current Posts "+postCount+" : Services "+serviceCount+" : Requests "+requestCount);
			} catch (InterruptedException e){
				System.out.println(e);
			}
		}	
		System.exit(0);
	}

	public void recordMatch(){
		matchCount.incrementAndGet();
	}
	
	public void recordFailedMatch(){
		failedCount.incrementAndGet();
	}
	
	public void recordDroppedPost(){
		droppedCount.incrementAndGet();
	}
	
	public static Master getInstance(){
		return _instance; 
	}
	
	public static void main(String[] args){
		List<Thread> pool = new ArrayList<Thread>();
		BB<Integer> board = new BB<Integer>();	
		System.out.printf("Starting simultion with %d clients and %d providers.\n", NUM_CLIENTS, NUM_PROVIDERS);
		
		for (int i = 0; i < NUM_PROVIDERS; i++){
			pool.add(new Provider(board, "Provider-"+(i+1)));
		}		
		for (int i = 0; i < NUM_CLIENTS; i++){
			pool.add(new Client(board, "Client-"+(i+1)));
		}		
		System.out.println("Starting threads...");
		for (Thread t : pool){
			t.start();
		}		
		//Start watching the board
		_instance.watch(board, pool);
	}
}