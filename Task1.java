package weka.clusterers;

import java.util.ArrayList;
import weka.core.Instances;

public class Task1 implements Runnable{
	private Instances data;
	private int nClust;
	private int id;
	private Configuration[] configs;
	private ArrayList<ArrayList<Long>> randCombs_id;
	
	public Task1(Instances data, int nClust, int id,
			ArrayList<ArrayList<Long>> randCombs_id,
					Configuration[] configs){
		this.data = data;
		this.nClust = nClust;
		this.id = id;
		this.configs = configs;
		this.randCombs_id = randCombs_id;
	}
	
	public void run(){
		Configuration best = null;
//		System.out.println("started thread " + id + " at " + (double)System.nanoTime()/1000000000);
		for(int i=offset; i<offset+stride; i++){
//			System.out.println(offset + " - " + stride);
//			System.err.println("}" + " " + "id: " + id);
			configs[id] = new Configuration(data, nClust, firstComb.int2Comb(combToThread[i]));
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