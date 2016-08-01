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
			public synchronized void signalResult(Configuration config, int id) {
//				System.out.println("signalResults");
				//controlla se il costo e' inferiore
				if(config.isBetterThan(bestConfig)){
//					System.out.println("Found better status");
					bestConfig = config.clone();
				}
				isChanged = bestConfig.isChanged(firstConfig);	
				
				//se c'e' altro lavoro da fare...
				int[] currentComb = comb.getCombination();
				if(currentComb != null){
					threads[id] = new Thread(new Task(data, nClust, currentComb, catcher, id));
					threads[id].start();
					try {
						threads[id].join();
					}
					catch(Exception e) {
						System.out.println(e);
					}
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
		
		ArrayList<Integer> sizes = new ArrayList<Integer>();
		for (int i=0; i<nClust; i++) {
			sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
		}
		int[] firstCombination = new int[nClust];
		for(int i=0; i<nClust; i++){
			firstCombination[i] = firstConfig.getCentroidAt(i).getID();
		}
		comb = new Combinations(sizes, firstCombination);
		for(int i=0; i<MAX_THREADS; i++){
			int[] currentCombination = comb.getCombination();
			threads[i] = new Thread(new Task(data, nClust, currentCombination, catcher, i));
			threads[i].start();
			try {
		         threads[i].join();
		    }
			catch(Exception e){ 
		         System.out.println(e.toString());
		    }
		}
		
		try {
			printCluster(firstConfig, nClust);
			printCluster(bestConfig, nClust);
		}
		catch(Exception e){
			System.out.println(e);
		}
		
		//variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime)/1000000000;
		System.out.println("Execution time: " + time + " s");
//		firstConfig.outputOnFile(data, time, seed, distanceFunction);
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
			System.out.print("Elements [ ");
//			for(int l : c.getCentroidAt(i).getAllInstances()){
//				System.out.print(l + " ");
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