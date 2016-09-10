package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//import java.util.ArrayList;

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
//		System.out.println("Start thread " + id);
		ArrayList<Costs> arrayCost = new ArrayList<Costs>();
		double currentCost = 0.0;
		double minCost = 0.0;
		double initCost = c.getCentroidAt(id).getTotalCost();
//		System.out.println(initCost + " " + c.getTotalCost());
		if(I == 0.0){
			medoids[id] = c.getCentroidAt(id).id;
		}
		else{
			for(int i=0; i<nElem; i++){
				switch(t){
					case EUCLIDEAN:
						arrayCost.add(new Costs(Manager.euclideanDistance(data.instance(c.getCentroidAt(id).getID()), data.instance(c.getCentroidAt(id).getID(i)), data.numAttributes()),c.getCentroidAt(id).getID(i)));
					break;
					case LEVENSHTEIN:
						arrayCost.add(new Costs((double)Manager.computeLevenshteinDistance(data.instance(c.getCentroidAt(id).getID()).stringValue(0), data.instance(c.getCentroidAt(id).getID(i)).stringValue(0)),c.getCentroidAt(id).getID(i)));
					break;
					default:
						System.err.println("Distance Error");
					break;
				}
			}
//			System.out.println(currentCost + " " + id);
			Collections.sort(arrayCost, new CostComparator());
//			for(int i=0; i<nElem; i++){
//				System.out.println("costo: " + arrayCost.get(i).getCost() + " - istanza: " + arrayCost.get(i).getInstance() + " id: " + id);
//			}
//			System.out.println("Cluster " + id + " old cost " + initCost);
			minCost = initCost;
			for(int i=0; i<Math.floor(I*nElem); i++){
				currentCost = 0.0;
				for(int j=0; j<nElem; j++){
					switch(t){
						case EUCLIDEAN:
							currentCost += Manager.euclideanDistance(data.instance(arrayCost.get(i).getInstance()), data.instance(arrayCost.get(j).getInstance()), data.numAttributes());
							break;
						case LEVENSHTEIN:
							currentCost += Manager.computeLevenshteinDistance(data.instance(arrayCost.get(i).getInstance()).stringValue(0), data.instance(arrayCost.get(j).getInstance()).stringValue(0));
							break;
						default:
							System.err.println("Distance Error");
							break;
					}
				}
				if(currentCost <= minCost){
					minCost = currentCost;
					medoids[id] = arrayCost.get(i).getInstance();
				}
			}
		}
//		System.out.println("Cluster " + id + " costo " + minCost);
//		System.out.println("Finish thread " + id);
		
	}
	
	static class CostComparator implements Comparator<Costs>{
		@Override public int compare(Costs a, Costs b){
			return a.cost < b.cost ? -1 :
				a.cost == b.cost ? 0 : 1;
		}
	}
}