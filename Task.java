package weka.clusterers;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
	private int nClust;
	private Combinations comb;
	private int id;
	private Configuration[] configs;
	
	public Task(Instances data, int nClust, int qty, ArrayList<Integer> sizes, Combinations firstComb,
			int[] offsetComb, int id, Configuration[] configs){
		this.data = data;
		this.nClust = nClust;
		//controllare l'offset e che non sfori
		comb = new Combinations(sizes, firstComb, offsetComb);
		this.id = id;
		this.configs = configs;
	}
	
	public void run(){
//		System.out.println("started thread " + id + " at " + (double)System.nanoTime()/1000000000);
		for(int i=0; i<qty; i++){
			configs[i] = new Configuration(data, nClust, comb.getCombination());
			//controllo la best per il costo
//		System.out.println("finished thread " + id + " at " + (double)System.nanoTime()/1000000000);
	}
}