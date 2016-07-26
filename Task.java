package weka.clusterers;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
	private int nClust;
	private int[] comb;
	private RunnableCatcher catcher;
	private int id;
	
	public Task(Instances data, int nClust, int[] comb, RunnableCatcher catcher, int id){
		this.data = data;
		this.nClust = nClust;
		this.comb = comb;
		this.id = id;
		this.catcher = catcher;
	}
	
	public void run(){
//		System.out.println("started thread " + id);
		Configuration config = new Configuration(data, nClust, comb);
//		System.out.println("finished thread " + id);
		catcher.signalResult(config, id);
	}
}