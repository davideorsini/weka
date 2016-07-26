package weka.clusterers;

import java.util.*;

import weka.attributeSelection.BestFirst;
import weka.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.converters.ArffSaver;

public class Manager {	
	private static Configuration bestConfig = null;
	private static Configuration firstConfig;
	private static Thread[] threads;
	private static Combinations comb;
	private static final int MAX_THREADS = 10;
	private static Instances data;
	private static RunnableCatcher catcher;
	private static boolean isChanged;
	
	public static void main(String[] args) throws Exception{
		ArrayList<Instance> instances;
		Configuration currentConfig;
		int nClust = Integer.parseInt(args[0]);
		String filePath = args[1];
		int seed = Integer.parseInt(args[2]);
		String distanceFunction = args[3];
		threads = new Thread[MAX_THREADS];
		
		catcher = new RunnableCatcher() {
			@Override
			public void signalResult(Configuration config, int id) {
				//controlla se il costo e' inferiore
				if(config.isBetterThan(bestConfig)){
//					System.out.println("Found better status");
					bestConfig = config.clone();
				}
				isChanged = config.isChanged(firstConfig);	
				
				//se c'e' altro lavoro da fare...
				int[] currentComb = comb.getCombination();
				if(currentComb != null && isChanged){
					threads[id] = new Thread(new Task(data, nClust, currentComb, catcher, id));
					threads[id].start();
				}
			}
		};
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();	
		
		currentConfig = new Configuration(data, nClust, seed);
		firstConfig = currentConfig.clone();
		firstConfig.printStatus();		
		bestConfig = currentConfig.clone();
		
		//variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		isChanged = true;
//		while(isChanged){
			ArrayList<Integer> sizes = new ArrayList();
			for (int i=0; i<nClust; i++) {
				sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
			}
			comb = new Combinations(sizes);
			for(int i=0; i<MAX_THREADS; i++){
				int[] currentCombination = comb.getCombination();
				threads[i] = new Thread(new Task(data, nClust, currentCombination, catcher, i));
				threads[i].start();
			}
//			isChanged = bestConfig.isChanged(firstConfig);
//			firstConfig = bestConfig.clone();
//		}
		
		/*isChanged = true;
		int c = 0;	
		while(isChanged){
			//tutte le combinazioni possibili
			ArrayList<Integer> sizes = new ArrayList();
			for (int i=0; i<nClust; i++) {
				sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
			}
			comb = new Combinations(sizes);
			c++;
			int counter =0;
//			while(true){
			for(int i=0; i<MAX_THREADS; i++){
				int[] currentCombination = comb.getCombination();
				threads[i] = new Thread(new Task(data, nClust, currentCombination, catcher, i));
				//threads[i].start();
			}
				//se sono terminate le combinazioni
//				if(currentCombination == null)
//					break;
//				
//				counter++;
				//commento per parallelizzare
				//currentConfig = new Configuration(data, nClust, currentCombination);
//				if(currentConfig.isBetterThan(bestConfig)){
//					System.out.println("Found better status");
//					bestConfig = currentConfig.clone();
				
//				}
//			}
			bestConfig.printStatus();
//			if(bestConfig.isChanged(firstConfig))
//				isChanged = false;
//			isChanged = bestConfig.isChanged(firstConfig);
//			firstConfig = bestConfig.clone();
		}*/
		//System.out.println(c);
		printCluster(firstConfig, nClust);
		printCluster(bestConfig, nClust);
		
		//variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime)/1000000000;
		System.out.println("Execution time: " + time + " s");
		bestConfig.outputOnFile(data, time, seed, distanceFunction);
		
		/*ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File("C:/Users/dav_0/Desktop/output.arff"));
		saver.writeBatch();*/
	}
	
	public synchronized static void printCluster(Configuration c, int nClust){
		for(int i=0; i<nClust; i++){
			System.out.println("Cluster " + "[" + i + "]");
			System.out.println("Centroid " + "[" + c.getCentroidAt(i).id + "]");
			int count = 0;
			System.out.print("Elements [ ");
//			for(int l : c.getCentroidAt(i).getAllInstances()){
//				System.out.print(l + " ");
//				count++;
//			}
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