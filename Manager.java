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

public class Manager{	
	private Configuration bestConfig = null;
	private Configuration firstConfig;
	private Thread[] threads;
	private Combinations comb;
	private final int MAX_THREADS = 4;
	private Instances data;
	private boolean isChanged;
	private int nClust;
	private int seed;
	private Configuration[] configs;
	private Configuration currentConfig;
	
	public Manager(String[] args) throws Exception{
		ArrayList<Instance> instances;
		nClust = Integer.parseInt(args[0]);
		String filePath = args[1];
		seed = Integer.parseInt(args[2]);
		String distanceFunction = args[3];
		threads = new Thread[MAX_THREADS];
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();	
		
		currentConfig = new Configuration(data, nClust, seed);
		firstConfig = currentConfig.clone();
		firstConfig.printStatus();		
		bestConfig = currentConfig.clone();
		
		//variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		
//		ArrayList<Integer> sizes = new ArrayList<Integer>();
//		for (int i=0; i<nClust; i++) {
//			sizes.add(currentConfig.getCentroidAt(i).getInstanceList().size());
//		}
//		int[] firstCombination = new int[nClust];
//		for(int i=0; i<nClust; i++){
//			firstCombination[i] = firstConfig.getCentroidAt(i).getID();
//		}
		
//		comb = new Combinations(sizes, firstCombination);
//		configs = new Configuration[MAX_THREADS];
//		for(int i=0; i<MAX_THREADS; i++){
//			int[] currentCombination = comb.getCombination();
//			threads[i] = new Thread(new Task(data, nClust, currentCombination, i, configs));
//			threads[i].start();
//		}
		int count = 0;
		boolean flag = true;
		while(flag){
			ArrayList<Integer> sizes = new ArrayList<Integer>();
			for (int i=0; i<nClust; i++) {
				sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
			}
//			sizes.add(2);
//			sizes.add(5);
//			sizes.add(3);
			int[] firstCombination = new int[nClust];
			for(int i=0; i<nClust; i++){
				firstCombination[i] = firstConfig.getCentroidAt(i).getID();
			}
//			firstConfig.printStatus();
			comb = new Combinations(sizes, firstCombination);
			configs = new Configuration[MAX_THREADS];
			final long configToTest = comb.getMaxComb();
			int qty = (int)configToTest / MAX_THREADS;
			for(int i=0; i<MAX_THREADS; i++){
//				int[] currentCombination = comb.getCombination();
				threads[i] = new Thread(new Task(data, nClust, comb.getCombination(), i, configs));
				threads[i].start();
			}
			for(int i=0; i<MAX_THREADS; i++){
				threads[i].join();
			}
			int j = 0;
			while(!comb.isDepleted()){
				try {
					if(!comb.isDepleted()){
			        	 threads[j] = new Thread(new Task(data, nClust, comb.getCombination(), j, configs));
			        	 threads[j].start();
			         }
//			         threads[j].join();
//			         comb.printComb();
	//		         configs[j].printStatus();
			         if(configs[j].isBetterThan(bestConfig) == true){
			        	 bestConfig = configs[j].clone();
			         }
			    }
				catch(Exception e){ 
			         System.out.println(e.toString());
			    }
				
				//se � terminato l'ultimo thread ma c'� ancora del lavoro da fare
				//rilancio i thread
				if(j == MAX_THREADS-1 && !comb.isDepleted()){
		        	j = 0;
		        	for(int i=0; i<MAX_THREADS; i++){
						threads[i].join();
					}
		        }
				if(comb.isDepleted()){
					for(int i=0; i<MAX_THREADS; i++){
						threads[i].join();
					}
				}
				j++;
			}
			printCluster(firstConfig, nClust);
			printCluster(bestConfig, nClust);
			flag = !bestConfig.isChanged(firstConfig);
			System.out.println(flag);
			firstConfig = bestConfig.clone();
			count++;
		}
		
		
		System.out.println("threads terminated " + count);
		
//		printCluster(firstConfig, nClust);
		printCluster(bestConfig, nClust);
		
		//variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime)/1000000000;
		System.out.println("Execution time: " + time + " s");
//		firstConfig.outputOnFile(data, time, seed, distanceFunction);
		bestConfig.outputOnFile(data, time, seed, distanceFunction);
	}
	
	public synchronized static void printCluster(Configuration c, int nClust){
		for(int i=0; i<nClust; i++){
			System.out.println("Cluster " + "[" + i + "]");
			System.out.println("Centroid " + "[" + c.getCentroidAt(i).id + "]");
			System.out.print("Elements [ ");
			for(int l : c.getCentroidAt(i).getAllInstances()){
				System.out.print(l + " ");
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