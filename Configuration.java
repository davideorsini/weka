package weka.clusterers;
import java.util.*;

import weka.core.Instances;

public class Configuration{
	private ArrayList<Centroid> clusterStatus;
	private double result = 0.0;
	private int clusterCount;
	
	public Configuration(Instances data, int nClust, int seed){
		buildFirstConf(data, nClust, seed);
		computeCost();
	}
	
	public Configuration(Instances data, int nClust, int[] centroidsID) {
		buildNextConf(data, nClust, centroidsID);
		computeCost();
	}
	
	private Configuration(ArrayList<Centroid> clusterStatus) {
		this.clusterStatus = clusterStatus;
		computeCost();
	}
	
	private void buildFirstConf(Instances data, int nClust, int seed){
		clusterCount = nClust;
		clusterStatus = chooseRandomCentroid(nClust, seed, data);
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			int j = 0;
			for(int k=0; k<nClust; k++){
				while(i == getCentroidAt(k).getID()){
					i++;
				}
				costs[j] = getCentroidAt(k).euclideanDistance(i, data);
				//System.out.println(costs[j] + " ");
				j++;
			}
			int index = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
					getCentroidAt(index).addTotalCost(costs[jj]);
				}
			}
			
			getCentroidAt(index).addInstance(i);
		}		
	}
	
	private void buildNextConf(Instances data, int nClust, int[] centroidsID){
		clusterCount = nClust;
		clusterStatus = new ArrayList<Centroid>();
		for(Integer i : centroidsID){
			clusterStatus.add(new Centroid(i));
		}
//		for(int i=0;i<nClust;i++)
			//	System.out.println(clusterStatus.get(i).getID());
			//System.out.println(centroidsID[i]);
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			int j = 0;
			for(int k=0; k<nClust; k++){
				while(i == getCentroidAt(k).getID()){
					i++;
				}
				costs[j] = getCentroidAt(k).euclideanDistance(i, data);
				//System.out.println(costs[j] + " ");
				j++;
			}
			int index = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
					getCentroidAt(index).addTotalCost(costs[jj]);
				}
			}
			getCentroidAt(index).addInstance(i);
		}
	}
	
	private void computeCost() {
		this.result = 0.0;
		for(Centroid c : clusterStatus){
			result += c.getTotalCost();
		}
		clusterCount = clusterStatus.size();
	}
	
	public void printStatus(){
		System.out.println();
		for(int i=0; i<clusterCount; i++){
			System.out.print("[" + i + "]" + " ");
			for(int j=0; j<getCentroidAt(i).getInstanceList().size(); j++){
				System.out.print(getCentroidAt(i).getInstanceList().get(j) + " ");
			}
			System.out.println();
		}
	}
	
	public double getTotalCost(){	
		return result;
	}
	
	public Centroid getCentroidAt(int index){
		return this.clusterStatus.get(index);
	}
	
	public boolean isBetterThan(Configuration c){
		//controllo che il costo totale sia minore
		System.out.println(c.getTotalCost() + " >= " + result + "?");
		if(c.getTotalCost() < result){
			return false;
		}
		return true;
		/*//controllo che non ci siano stati scambi tra i cluster
		boolean flag = true;
		for(int i=0; i<clusterStatus.size(); i++){
			if(!clusterStatus.get(i).equals(c.getCentroidAt(i))){
				flag = false;
				break;
			}
		}
		return flag;*/
		
	}
	
	public static ArrayList<Centroid> chooseRandomCentroid(int nClust, int seed, Instances data){
		Random randInstanceIndex = new Random(seed);
		ArrayList<Centroid> centroidList = new ArrayList<Centroid>();
		int centroidIndex = randInstanceIndex.nextInt(data.numInstances());
		centroidList.add(new Centroid(centroidIndex));
		for(int i=0; i<nClust-1; i++){
			int j=0;
			centroidIndex = randInstanceIndex.nextInt(data.numInstances());
			while(j<centroidList.size()){
				if(centroidIndex != ((ArrayList<Centroid>) centroidList).get(j).getID()){
					j++;
				}
				else{
					centroidIndex = randInstanceIndex.nextInt(data.numInstances());
				}
			}
			centroidList.add(new Centroid(centroidIndex));
			centroidList.get(i).setClusterID(i);
		}
		//printStatus(centroidList);
		return centroidList;
	}
	
	public int retClusterCount(){
		return this.clusterCount;
	}
	
	public Configuration clone(){
		return new Configuration(clusterStatus);
	}
}