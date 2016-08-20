package weka.clusterers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class Manager1{
/*
	##################################################################
	Per le stringhe "C:/Users/dav_0/Desktop/stringTest.arff"
	"string" 2 10 "C:/Users/dav_0/Desktop/stringTest.arff" 1 500 0
	*/
	private  int alphaLen;
	private int N;
	private static int seed;
	private int minLen;
	private int maxLen;
	private char[] alphabet;
	private Instances data;
	
	private Configuration1 bestConfig = null;
	private static Configuration1 firstConfig;
	private static Configuration1 optimalNClust = null;
	private Thread[] threads;
	private Combinations comb;
	private static final int MAX_THREADS = 4;
	private boolean isChanged;
	private static int nClust;
	private Configuration1[] configs;
	private Configuration1 currentConfig;
	private static int K;
	private static int[][][] randCombs;
	private static ArrayList<Integer> sizes;
	private double delta;
	private int minClust, maxClust;
	
	public Manager1(String[] args) throws Exception{
		minClust = Integer.parseInt(args[1]);
		maxClust = Integer.parseInt(args[2]);
		String filePath = args[3];
		seed = Integer.parseInt(args[4]);
		K = Integer.parseInt(args[5]);
		delta = Double.parseDouble(args[6]);
		
		N = 100;
		minLen = 3;
		maxLen = 6;
		alphaLen = 10;
		threads = new Thread[MAX_THREADS];
		
		alphabet = alphaCreator(alphaLen);
		
		Random rand = new Random(seed);
		String[] s = new String[N];
		
		for(int i=0; i<N; i++){
			int len = rand.nextInt(alphaLen*maxLen);
			while(len < alphaLen*minLen || len > alphaLen*maxLen){
				len = rand.nextInt(26*maxLen);
			}
			s[i] = "";
			for(int j=0; j<len; j++){
				int val = rand.nextInt(alphaLen);
				s[i] += alphabet[val];
			}
		}
		createStringARFF(s);
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();
		
//		currentConfig = new Configuration1(data, nClust, seed);
//		firstConfig = currentConfig.clone();
//		firstConfig.printStatus();		
//		bestConfig = currentConfig.clone();
		configs = new Configuration1[MAX_THREADS];
		
		int N = 0;
		int count = 0;
		double bestCG = Double.MAX_VALUE;
		double cg = 0;
		boolean flag = true;
		
		//variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
//		currentConfig = new Configuration1(data, nClust, seed);
//		optimalNClust = currentConfig.clone();
		for(int m=minClust; m<=maxClust; m++){
			nClust = m;
			currentConfig = new Configuration1(data, nClust, seed);
			firstConfig = currentConfig.clone();
			bestConfig = currentConfig.clone();
			if(optimalNClust == null){
				optimalNClust = bestConfig.clone();
			}
			//In caso in cui K != inf
			if(!args[4].equalsIgnoreCase("inf")){
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
						threads[i] = new Thread(new Task3(data, nClust, i, randCombs[i], val, configs));
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
					configs = new Configuration1[MAX_THREADS];
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
						threads[i] = new Thread(new Task2(data, nClust, qty[i], sizes, firstCombination, comb.getCombination(qty[i]), i, configs));
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
			cg = clusterGoodness(bestConfig);
//			System.err.println(cg + " " +  bestCG);
			if(cg < bestCG){
				optimalNClust = bestConfig.clone();
				N = m;
				bestCG = cg;
			}
		}
//		System.out.println("threads terminated " + count);
		
//		printCluster(firstConfig, nClust);
		printCluster(bestConfig, nClust);
		printCluster(optimalNClust, N);
		
		
		//variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime)/1000000000;
		System.out.println("Execution time: " + time + " s");
//		firstConfig.outputOnFile(data, time, seed, distanceFunction);
//		bestConfig.outputStringARFF(data, args);
		optimalNClust.outputStringARFF(data, args);
	}

	private int minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }                                                                            
                                                                                 
    public int computeLevenshteinDistance(String a, String b) {      
        int[][] distance = new int[a.length() + 1][b.length() + 1];        
                                                                                 
        for (int i = 0; i <= a.length(); i++)                                 
            distance[i][0] = i;                                                  
        for (int j = 1; j <= b.length(); j++)                                 
            distance[0][j] = j;                                                  
                                                                                 
        for (int i = 1; i <= a.length(); i++)                                 
            for (int j = 1; j <= b.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + 1,                                  
                        distance[i][j - 1] + 1,                                  
                        distance[i - 1][j - 1] + ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1));
                                                                                 
        return distance[a.length()][b.length()];                           
    }

	public void createStringARFF(String[] s) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/stringTest.arff"));
		bw.write("@relation clustered");
		bw.newLine();
		bw.newLine();
		bw.append("@attribute att1 string");
		bw.newLine();
		bw.newLine();
		bw.append("@data");
		bw.newLine();
		bw.append("%");
		bw.newLine();
		bw.append("% " + N);
		bw.newLine();
		bw.append("%");
		bw.newLine();
		for(int i=0; i<N; i++){
			bw.append(s[i]);
			if(i <= N-2){
				bw.newLine();
			}
		}
		bw.close();
	}

	public char[] alphaCreator(int alphaLen){
		if(alphaLen < 2){
			alphaLen = 2;
		}
		if(alphaLen > 26){
			alphaLen = 26;
		}
		char[] alphabet = new char[alphaLen];
		for(int i=0; i<alphaLen; i++){
			alphabet[i] = (char)(i + 65);
//			 System.out.println(alphabet[i]);
		}
		return alphabet;
	}
	
	public static double clusterGoodness(Configuration1 c){
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
			Configuration1 firstConf, Instances data, double delta){
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
	
	public static void printCluster(Configuration1 c, int nClust){
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