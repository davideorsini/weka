package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//import java.util.ArrayList;

import weka.core.Instances;

public class Task implements Runnable{
	private Instances data;
	private int id;
	private DistanceType t;
	private int[] medoids;
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
		ArrayList<Costs> arrayCost = new ArrayList<Costs>();
		double currentCost = 0.0;
		double minCost = 0.0;
		double initCost = c.getMedoidAt(id).getTotalCost();
		if(I == 0.0){
			medoids[id] = c.getMedoidAt(id).id;
		}
		else{
			for(int i=0; i<nElem; i++){
				switch(t){
					case EUCLIDEAN:
						arrayCost.add(new Costs(Manager.euclideanDistance(data.instance(c.getMedoidAt(id).getID()), data.instance(c.getMedoidAt(id).getID(i)), data.numAttributes()),c.getMedoidAt(id).getID(i)));
					break;
					case LEVENSHTEIN:
						arrayCost.add(new Costs((double)Manager.computeLevenshteinDistance(data.instance(c.getMedoidAt(id).getID()).stringValue(0), data.instance(c.getMedoidAt(id).getID(i)).stringValue(0)),c.getMedoidAt(id).getID(i)));
					break;
					default:
						System.err.println("Distance Error");
					break;
				}
			}
			Collections.sort(arrayCost, new CostComparator());
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
				if(currentCost < minCost){
					minCost = currentCost;
					medoids[id] = arrayCost.get(i).getInstance();
				}
			}
		}
	}
	
	static class CostComparator implements Comparator<Costs>{
		@Override public int compare(Costs a, Costs b){
			return a.cost < b.cost ? -1 :
				a.cost == b.cost ? 0 : 1;
		}
	}
}