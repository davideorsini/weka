package weka.clusterers;

import java.util.*;
import weka.core.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class Manager{
//	private ArrayList<Instance> instances;
//	private ArrayList<Centroid> firstConfiguration;
//	private ArrayList<Centroid> bestConfiguration;
//	private ArrayList<Centroid> currentConfiguration;
//	private int[][] combinationChecked;
//	private int nClust;
//	private String filePath;
//	private int seed;
//	private String distanceFunction;
	
	/*public Manager(int nClust, String filePath, int seed, String distanceFunction){
		this.nClust = nClust;
		this.filePath = filePath;
		this.seed = seed;
		this.distanceFunction = distanceFunction;
	}*/
	
	public static void main(String[] args) throws Exception{
		//Manager manager = new Manager(Integer.parseInt(args[0]), args[1],
		//								Integer.parseInt(args[2]), args[3]);
		ArrayList<Instance> instances;
		//ArrayList<Centroid> firstConfiguration;
//		ArrayList<Centroid> bestConfiguration;
//		ArrayList<Centroid> currentConfiguration;
		Configuration currentConfig;
		Configuration bestConfig = null;
		int nClust = Integer.parseInt(args[0]);
		String filePath = args[1];
		int seed = Integer.parseInt(args[2]);
		//String distanceFunction = args[3];
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		Instances data = arff.getData();
		
//		currentConfiguration = chooseRandomCentroid(nClust, seed, data);
		currentConfig = new Configuration(chooseRandomCentroid(nClust, seed, data));
		
		System.out.println();
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[currentConfig.retClusterCount()];
			int j = 0;
			for(int k=0; k<currentConfig.retClusterCount(); k++){
				while(i == currentConfig.getCentroidAt(k).getID()){
					i++;
				}
				costs[j] = currentConfig.getCentroidAt(k).euclideanDistance(i, data);
				//System.out.println(costs[j] + " ");
				j++;
			}
			int index = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<currentConfig.retClusterCount(); jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
				}
			}
			currentConfig.getCentroidAt(index).addInstance(i);
		}
		
		//firstConfiguration = new ArrayList<Centroid>();
		//firstConfiguration = clone(currentConfiguration);
		//currentConfiguration.get(0).getInstanceList().clear();
		//printStatus(currentConfiguration);
		
		//stampo i cluster
		System.out.println();
		//Commento per verificare il funzionamento di git
		for(int h=0; h<currentConfig.retClusterCount(); h++){
			System.out.print("[" + h + "]" + " ");
			for(int k=0; k<currentConfig.getCentroidAt(h).getInstanceList().size();k++){
				System.out.print(currentConfig.getCentroidAt(h).getInstanceList().get(k) + " ");
			}
			System.out.println();
		}
		
		if(bestConfig == null || currentConfig.isBetterThan(bestConfig)){
			bestConfig = currentConfig.clone();
		}
		
		
		//tutte le combinazioni possibili
		ArrayList<Integer> sizes = new ArrayList();
		for (int i=0; i<nClust; i++) {
			sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
		}
		Combinations comb = new Combinations(sizes);
		
	}
	
	public static ArrayList<Centroid> clone(ArrayList<Centroid> list){
		ArrayList<Centroid> clone = new ArrayList<Centroid>();
		for(int i=0; i<list.size(); i++){
			Centroid c = new Centroid(list.get(i).getID());
			c = list.get(i);
			//Centroid c1 = c.c			
		}
		clone = (ArrayList<Centroid>) list.clone();
		return clone;
	}
	
	public static void printStatus(ArrayList<Centroid> cluster){
		System.out.print("Centroids: ");
		for(int i=0; i<cluster.size(); i++){
			System.out.print(cluster.get(i).getID() + " ");
		}
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
	
	
	public void checkCombinations(){
		
	}
}