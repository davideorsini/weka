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
	private static Configuration firstConfig;
	private static Configuration optimalNClust = null;
	private Thread[] threads;
	private Combinations comb;
	private static final int MAX_THREADS = 4;
	private Instances data;
	private boolean isChanged;
	private static int nClust;
	private static int seed;
	private Configuration[] configs;
	private Configuration currentConfig;
	private static int K;
	private static int[][][] randCombs;
	private static ArrayList<Integer> sizes;
	private double delta;
	private int minClust, maxClust;
	
	/*
	 "numeric" 10 "C:\Users\dav_0\Desktop\pam_big.arff" 1 "ED" 500 0
	 "numeric" 2 10 "C:\Users\dav_0\Desktop\pam_big.arff" 1 500 0
	 */
	
	public Manager(String[] args) throws Exception{
		ArrayList<Instance> instances;
		minClust = Integer.parseInt(args[1]);
		maxClust = Integer.parseInt(args[2]);
//		nClust = Integer.parseInt(args[1]);
		String filePath = args[3];
		seed = Integer.parseInt(args[4]);
		threads = new Thread[MAX_THREADS];
		delta = Double.parseDouble(args[6]);
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();	
		
//		currentConfig = new Configuration(data, nClust, seed);
//		firstConfig = currentConfig.clone();
//		firstConfig.printStatus();		
//		bestConfig = currentConfig.clone();
		configs = new Configuration[MAX_THREADS];
		
		int count = 0;
		double bestCG = Double.MAX_VALUE;
		double cg = 0;
		boolean flag = true;
		int nc = 0;
		
		//variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		
		
		for(int m=minClust; m<=maxClust; m++){
			nClust = m;
			currentConfig = new Configuration(data, nClust, seed);
			firstConfig = currentConfig.clone();
			if(optimalNClust == null){
				optimalNClust = currentConfig.clone();
			}
//			firstConfig.printStatus();		
			bestConfig = currentConfig.clone();
			flag = true;
			//In caso in cui K != inf
			if(!args[5].equalsIgnoreCase("inf")){
				K = Integer.parseInt(args[5]);
				while(flag){
					sizes = new ArrayList<Integer>();
					for (int i=0; i<nClust; i++) {
						sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
					}
//					System.err.println();
//					System.err.println();
	//				firstConfig.printStatus();
	//				System.out.println("Combination per thread: " + K/MAX_THREADS + " resto: " + K%MAX_THREADS);
					if(delta == 0){
						randCombs = randCombinations(sizes);
					}
					if(delta != 0){
						randCombs = randCombinationsDelta(sizes, firstConfig, data, delta);
					}
	//				printRandCombs(randCombs);
					int val = K/MAX_THREADS;
					for(int i=0; i<MAX_THREADS; i++){
						if(i == MAX_THREADS-1){
							val = K/MAX_THREADS + K%MAX_THREADS;
						}
						threads[i] = new Thread(new Task1(data, nClust, i, randCombs[i], val, configs));
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
					flag = bestConfig.isChanged(firstConfig);
					firstConfig = bestConfig.clone();
					count++;
				}
			}
			else{
				while(flag){
					 sizes = new ArrayList<Integer>();
					for (int i=0; i<nClust; i++) {
						sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
					}
					int[] firstCombination = new int[nClust];
					for(int i=0; i<nClust; i++){
						firstCombination[i] = firstConfig.getCentroidAt(i).getID();
					}
	//				firstConfig.printStatus();
					comb = new Combinations(sizes, firstCombination);
					configs = new Configuration[MAX_THREADS];
					final long configToTest = comb.getMaxComb();
	//				System.out.println("Max comb: " + configToTest);
					long valQty = configToTest / MAX_THREADS;
	//				System.out.println(valQty + " " + (configToTest - (valQty*MAX_THREADS)));
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
					flag = bestConfig.isChanged(firstConfig);
					firstConfig = bestConfig.clone();
					count++;
				}
			}
			/*cg = clusterGoodness(bestConfig);
//			System.err.println(cg + " " +  bestCG);
			if(cg < bestCG){
				optimalNClust = bestConfig.clone();
				N = m;
				bestCG = cg;
			}*/
			cg = bestConfig.getTotalCost();
			System.err.println(cg + " " +  bestCG);
			if(cg < bestCG){
				optimalNClust = bestConfig.clone();
				nc = m;
				bestCG = cg;
			}
		}
//		System.out.println("threads terminated " + count);		
		
//		printCluster(firstConfig, nClust);
//		printCluster(bestConfig, nClust);
		printCluster(optimalNClust, nc);
		
//		double p = clusterGoodness(bestConfig);
		
		//variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime)/1000000000;
		System.out.println("Execution time: " + time + " s");
//		bestConfig.outputARFF(data, args);
		optimalNClust.outputARFF(data, args);
	}
	
	public static double clusterGoodness(Configuration c){
		double min = Double.MAX_VALUE;
		double max = 0.0;
		int val = 0;
		for(int i=0; i<c.retClusterCount(); i++){
			val = c.getCentroidAt(i).getNumElements();
			if(val < min){
				min = val;
			}
			if(val > max){
				max = val;
			}
		}
//		System.out.println("Cluster Goodness: " + max/min);
		return max/min;
	}
	
	public static int[][][] randCombinations(ArrayList<Integer> sizes){
		int[][][] randCombs = new int[MAX_THREADS][(K/MAX_THREADS) + (K%MAX_THREADS)][nClust];
		Random rand = new Random(seed);
		int val;
		int qty = K/MAX_THREADS;
		for(int i=0; i<MAX_THREADS; i++){
			if(i == MAX_THREADS-1){
				qty = (K/MAX_THREADS) + (K%MAX_THREADS);
			}
			for(int j=0; j<qty; j++){
				for(int h=0; h<nClust; h++){
					int range = sizes.get(h);
					if(range == 0){
						range = 1;
					}
					val = rand.nextInt(range);
					randCombs[i][j][h] = firstConfig.getCentroidAt(h).getID(val);
				}
			}
		}
		return randCombs;
	}
	
	public static int[][][] randCombinationsDelta(ArrayList<Integer> sizes,
			Configuration firstConf, Instances data, double delta){
		int[][][] randCombs = new int[MAX_THREADS][(K/MAX_THREADS) + (K%MAX_THREADS)][nClust];
		Random rand = new Random(seed);
		int val = 0;
		int qty = K/MAX_THREADS;
		for(int i=0; i<MAX_THREADS; i++){
			if(i == MAX_THREADS-1){
				qty = (K/MAX_THREADS) + (K%MAX_THREADS);
			}
			for(int j=0; j<qty; j++){
				for(int h=0; h<nClust; h++){
					int range = sizes.get(h);
					if(range == 0){
						range = 1;
					}
					
					while(true){
						double cost = 0;	
						val = rand.nextInt(range);
						for(int ii=0; ii<data.instance(val).numAttributes(); ii++){
							cost += Math.pow(data.instance(val).value(ii) - data.instance(firstConf.getCentroidAt(h).getID()).value(ii),2);
						}
						cost = Math.sqrt(cost);
						if(cost >= delta){
							break;
						}
					}
					randCombs[i][j][h] = firstConfig.getCentroidAt(h).getID(val);
				}
			}
		}
		return randCombs;
	}
	
	public static void printRandCombs(int[][][] randCombs){
		int qty = K/MAX_THREADS;
		for(int i=0; i<MAX_THREADS; i++){
			if(i == MAX_THREADS-1){
				qty = (K/MAX_THREADS) + (K%MAX_THREADS);
			}
			for(int j=0; j<qty; j++){
				for(int h=0; h<nClust; h++){
					System.err.print(randCombs[i][j][h] + " ");
				}
				System.err.println();
			}
		}
	}
	
	public static void printCluster(Configuration c, int nClust){
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
		System.out.println(clusterGoodness(c));
	}
	
	public static void printStatus(ArrayList<Centroid> cluster){
		System.out.print("Centroids: ");
		for(int i=0; i<cluster.size(); i++){
			System.out.print(cluster.get(i).getID() + " ");
		}
	}
}