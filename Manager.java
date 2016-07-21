package weka.clusterers;

import java.util.*;

import weka.attributeSelection.BestFirst;
import weka.core.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.converters.ArffSaver;

public class Manager{	
	
	public static void main(String[] args) throws Exception{
		ArrayList<Instance> instances;
		Configuration firstConfig;
		Configuration currentConfig;
		Configuration bestConfig = null;
		int nClust = Integer.parseInt(args[0]);
		String filePath = args[1];
		int seed = Integer.parseInt(args[2]);
		//String distanceFunction = args[3];
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		Instances data = arff.getData();	
		
		currentConfig = new Configuration(data, nClust, seed);
		firstConfig = currentConfig.clone();
		firstConfig.printStatus();		
		bestConfig = currentConfig.clone();
		
		boolean isChanged = true;
		int c = 0;
		while(isChanged){
			//tutte le combinazioni possibili
			ArrayList<Integer> sizes = new ArrayList();
			for (int i=0; i<nClust; i++) {
				sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
			}
			Combinations comb = new Combinations(sizes);
			c++;
			int counter =0;
			while(true){
				int[] currentCombination = comb.getCombination();
				if(currentCombination == null)
					break;
				counter++;
				currentConfig = new Configuration(data, nClust, currentCombination);
				if(currentConfig.isBetterThan(bestConfig)){
//					System.out.println("Found better status");
					bestConfig = currentConfig.clone();
				}
	//			currentConfig.printStatus();
			}
			bestConfig.printStatus();
			System.out.println(counter);
//			if(bestConfig.isChanged(firstConfig))
//				isChanged = false;
			isChanged = bestConfig.isChanged(firstConfig);
			firstConfig = bestConfig.clone();
		}
		System.out.println(c);
		printCluster(bestConfig, nClust);
		/*ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File("C:/Users/dav_0/Desktop/output.arff"));
		saver.writeBatch();*/
	}
	
	public static void printCluster(Configuration c, int nClust){
		for(int i=0; i<nClust; i++){
			System.out.println("Cluster " + "[" + i + "]");
			System.out.println("Centroid " + "[" + c.getCentroidAt(i).id + "]");
			int count = 0;
			System.out.print("Elements [ ");
			for(int l : c.getCentroidAt(i).getAllInstances()){
				System.out.print(l + " ");
				count++;
			}
			System.out.print("]");
			System.out.println();
			System.out.println("Num. of elements " + c.getCentroidAt(i).getNumElements());
			System.out.println();
		}
	}
	
	public static void printStatus(ArrayList<Centroid> cluster){
		System.out.print("Centroids: ");
		for(int i=0; i<cluster.size(); i++){
			System.out.print(cluster.get(i).getID() + " ");
		}
	}
	
}