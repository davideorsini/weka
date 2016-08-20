package weka.clusterers;

import java.util.ArrayList;
import weka.core.Instances;

public class Task3 implements Runnable{
	private Instances data;
	private int nClust;
	private int id;
	private Configuration1[] configs;
	private int[][] randCombs_id;
	private int qty;
	
	public Task3(Instances data, int nClust, int id,
			int[][] randCombs_id, int qty,
			Configuration1[] configs){
		this.data = data;
		this.nClust = nClust;
		this.id = id;
		this.configs = configs;
		this.qty = qty;
		this.randCombs_id = randCombs_id;
	}
	
	public void run(){
		Configuration1 best = null;
//		System.out.println("started thread " + id + " at " + (double)System.nanoTime()/1000000000);
		for(int i=0; i<qty; i++){
			configs[id] = new Configuration1(data, nClust, randCombs_id[i]);
			//controllo la best per il costo
			if(best == null){
				best = configs[id];
			}
			if(configs[id].isBetterThan(best)){
	        	 best = configs[id].clone();
	         }
		}
//		System.out.println("finished thread " + id + " at " + (double)System.nanoTime()/1000000000);
		configs[id] = best;
	}
}