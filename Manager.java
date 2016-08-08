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
	private int[] combToThread;
	private int K;
	
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
		
		int count = 0;
		boolean flag = true;
		
		//variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		
		//In caso in cui K != inf
		if(!args[4].equalsIgnoreCase("inf")){
			K = Integer.parseInt(args[4]);
			combToThread = new int[K];
			Random rand = new Random(seed);
			ArrayList<Integer> sizes = new ArrayList<Integer>();
			for (int i=0; i<nClust; i++) {
				sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
			}
			int[] firstCombination = new int[nClust];
			for(int i=0; i<nClust; i++){
				firstCombination[i] = firstConfig.getCentroidAt(i).getID();
			}
			firstConfig.printStatus();
			comb = new Combinations(sizes, firstCombination);
			
			while(flag){
				configs = new Configuration[MAX_THREADS];
				final long configToTest = comb.getMaxComb();
				System.out.println("Max comb: " + configToTest);
				System.out.println("Combination per thread: " + K/MAX_THREADS + " resto: " + K%MAX_THREADS);
				for(int i=0; i<K; i++){
					int j = 0;
					int val = rand.nextInt((int)configToTest);
					while(j<nClust){
						if(val == combToThread[j]){
							val = rand.nextInt((int)configToTest);
							j = 0;
						}
						j++;
					}
					combToThread[i] = val;
				}

				for(int i=0; i<MAX_THREADS; i++){
	//				int[] currentCombination = comb.getCombination();
					if(i == MAX_THREADS-1){
						threads[i] = new Thread(new Task1(data, nClust, combToThread, i, seed, 
								configs, comb, i*MAX_THREADS, (K/MAX_THREADS)+(K%MAX_THREADS)));
					}
					else{
						threads[i] = new Thread(new Task1(data, nClust, combToThread, i, seed, 
								configs, comb, i*MAX_THREADS, (K/MAX_THREADS)));
					}
					threads[i].start();
				}
				for(int i=0; i<MAX_THREADS; i++){
					threads[i].join();
				}
				for(int i=0; i<MAX_THREADS; i++){
					if(configs[i].isBetterThan(bestConfig)){
						bestConfig = configs[i].clone();
					}
				}
				bestConfig.printStatus();
				firstConfig.printStatus();
	//			printCluster(firstConfig, nClust);
	//			printCluster(bestConfig, nClust);
				flag = bestConfig.isChanged(firstConfig);
				System.out.println(flag);
				firstConfig = bestConfig.clone();
				count++;
			}
		}
		else{
			while(flag){
				ArrayList<Integer> sizes = new ArrayList<Integer>();
				for (int i=0; i<nClust; i++) {
					sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
				}
				int[] firstCombination = new int[nClust];
				for(int i=0; i<nClust; i++){
					firstCombination[i] = firstConfig.getCentroidAt(i).getID();
				}
				firstConfig.printStatus();
				comb = new Combinations(sizes, firstCombination);
				configs = new Configuration[MAX_THREADS];
				final long configToTest = comb.getMaxComb();
				System.out.println("Max comb: " + configToTest);
				long valQty = configToTest / MAX_THREADS;
				System.out.println(valQty + " " + (configToTest - (valQty*MAX_THREADS)));
				long[] qty = new long[MAX_THREADS];
				for(int i=0; i<MAX_THREADS; i++){
					qty[i] = valQty;
					if(i == (MAX_THREADS - 1)){
						qty[i] += configToTest - (valQty * MAX_THREADS);
					}
				}
				for(int i=0; i<MAX_THREADS; i++){
					threads[i] = new Thread(new Task(data, nClust, qty[i], sizes, firstCombination, comb.getCombination(qty[i]), i, configs));
					threads[i].start();
				}
				for(int i=0; i<MAX_THREADS; i++){
					threads[i].join();
				}
				for(int i=0; i<MAX_THREADS; i++){
					if(configs[i].isBetterThan(bestConfig)){
						bestConfig = configs[i].clone();
					}
				}
				bestConfig.printStatus();
				firstConfig.printStatus();
	//			printCluster(firstConfig, nClust);
	//			printCluster(bestConfig, nClust);
				flag = bestConfig.isChanged(firstConfig);
				System.out.println(flag);
				firstConfig = bestConfig.clone();
				count++;
			}
			
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