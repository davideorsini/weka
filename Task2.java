package weka.clusterers;

import java.util.ArrayList;
import weka.core.Instances;

public class Task2 implements Runnable{
	private Instances data;
	private int nClust;
	private Combinations comb;
	private int id;
	private Configuration1[] configs;
	private long qty;
	
	public Task2(Instances data, int nClust, long qty, ArrayList<Integer> sizes, 
			int[] firstComb, int[] offsetComb, int id, 
			Configuration1[] configs){
		this.data = data;
		this.nClust = nClust;
		this.qty = qty;
		//controllare l'offset e che non sfori
		comb = new Combinations(sizes, firstComb, offsetComb);
		this.id = id;
		this.configs = configs;
	}
	
	public void run(){
		Configuration1 best = null;
//		System.out.println("started thread " + id + " at " + (double)System.nanoTime()/1000000000);
		for(int i=0; i<qty; i++){
//			int[] combi = comb.getCombination();
//			System.err.print("{ ");
//			for(int j=0; j<3; j++){
//				System.err.print(combi[j] + " ");
//			}
//			System.err.println("}" + " " + "id: " + id);
			configs[id] = new Configuration1(data, nClust, comb.getCombination());
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