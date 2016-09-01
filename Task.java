package weka.clusterers;

import java.util.ArrayList;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
	private int nClust;
	private int id;
	private Configuration[] configs;
	private long qty;
	private int[][] randCombs_id;
	private DistanceType t;
	private Combinations comb;
	
	private Task(Instances data, int nClust, long qty, 
			int id, Configuration[] configs, DistanceType t){
		this.data = data;
		this.nClust = nClust;
		this.qty = qty;
		this.id = id;
		this.configs = configs;
		this.t= t;
	}
	
	public Task(Instances data, int nClust, int id,
			int[][] randCombs_id, int qty,
			Configuration[] configs, DistanceType t){
		this(data, nClust, qty, id, configs, t);
		this.randCombs_id = randCombs_id;
	}
	
	public Task(Instances data, int nClust, long qty, ArrayList<Integer> sizes, 
			int[] firstComb, int[] offsetComb, int id, 
			Configuration[] configs, DistanceType t){
		this(data, nClust, qty, id, configs, t);
		comb = new Combinations(sizes, firstComb, offsetComb);
	}
	
	public void run(){
		Configuration best = null;
//		System.out.println("started thread " + id + " at " + (double)System.nanoTime()/1000000000);
		for(int i=0; i<qty; i++){
			if(randCombs_id == null){
				configs[id] = new Configuration(data, nClust, comb.getCombination(), t);
			}
			else{
				configs[id] = new Configuration(data, nClust, randCombs_id[i], t);
			}
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