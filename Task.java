package weka.clusterers;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
	private int nClust;
	private int[] comb;
	private int id;
	private Configuration[] configs;
	
	public Task(Instances data, int nClust, int[] comb, int id, Configuration[] configs){
		this.data = data;
		this.nClust = nClust;
		this.comb = comb;
		this.id = id;
		this.configs = configs;
	}
	
	public void run(){
//		System.out.println("started thread " + id);
		configs[id] = new Configuration(data, nClust, comb);
//		System.out.println("finished thread " + id);
	}
}