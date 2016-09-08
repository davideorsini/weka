package weka.clusterers;

import java.util.ArrayList;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
//	private int nClust;
	private int id;
//	private Configuration[] configs;
//	private long qty;
//	private int[][] randCombs_id;
	private DistanceType t;
//	private Combinations comb;
	private int[] medoids;
//	private ArrayList<Integer> sizes;
	private int nElem;
	private Configuration c;
	private double I;
	
	/*private Task(Instances data, int nClust, long qty, 
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
	}*/
	
	public Task(int id, Instances data, int nElem, Configuration c, DistanceType t, int[] medoids, double I){
		this.id = id;
		this.data = data;
		this.nElem = nElem;
		this.c = c;
		this.t= t;
		this.medoids = medoids;
		this.I = I;
	}
	
	public void run(){
//		System.out.println(nElem);
		double currentCost = 0.0;
		double minCost = Double.MAX_VALUE;
		double maxCost = 0.0;
		if(I == 0.0){
			medoids[id] = c.getCentroidAt(id).id;
		}
		else{
			for(int i=0; i<nElem; i++){
				currentCost = 0.0;
				switch(t){
					case EUCLIDEAN:
						currentCost = Manager.euclideanDistance(data.instance(c.getCentroidAt(id).id), data.instance(i), data.numAttributes());
					break;
					case LEVENSHTEIN:
						currentCost = Manager.computeLevenshteinDistance(data.instance(c.getCentroidAt(id).id).stringValue(0), data.instance(i).stringValue(0));
					break;
					default:
						System.err.println("Distance Error");
					break;
				}
				if(currentCost > maxCost){
					maxCost = currentCost;
				}
			}
			for(int i=0; i<nElem; i++){
				currentCost = 0.0;
				if(c.getCentroidAt(id).id == c.getCentroidAt(id).getID(i) && i<nElem-1){
					i++;
				}
				switch(t){
					case EUCLIDEAN:
						currentCost = Manager.euclideanDistance(data.instance(c.getCentroidAt(id).id), data.instance(i), data.numAttributes());
					break;
					case LEVENSHTEIN:
						currentCost = Manager.computeLevenshteinDistance(data.instance(c.getCentroidAt(id).id).stringValue(0), data.instance(i).stringValue(0));
					break;
					default:
						System.err.println("Distance Error");
					break;
				}
				if(currentCost > I*maxCost && i<nElem-1){
					i++;
				}
				else{
					currentCost = 0.0;
					for(int j=0; j<nElem; j++){
						switch(t){
							case EUCLIDEAN:
								currentCost += Manager.euclideanDistance(data.instance(i), data.instance(j), data.numAttributes());
								break;
							case LEVENSHTEIN:
								currentCost += Manager.computeLevenshteinDistance(data.instance(i).stringValue(0), data.instance(j).stringValue(0));
								break;
							default:
								System.err.println("Distance Error");
								break;
						}
					}
					if(currentCost < minCost){
						minCost = currentCost;
						medoids[id] = c.getCentroidAt(id).getID(i);
					}
				}
			}
		}
	}
		
}