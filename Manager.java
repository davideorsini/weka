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

	private int alphaLen;
	private int N;
	private int minLen;
	private int maxLen;
	private char[] alphabet;

	private Random rand;
	private Configuration bestConfig = null;
	private static Configuration firstConfig;
	private static Configuration optimalNClust = null;
	private Thread[] threads;
	private Combinations comb;
	private static int MAX_THREADS = 4;
	private static Instances data;
	private boolean isChanged;
	private static int nClust;
	private static int seed;
	private Configuration[] configs;
	private Configuration currentConfig;
	private static int K = 0;
	private static int[][][] randCombs;
	private static ArrayList<Integer> sizes;
	private double delta;
	private int minClust, maxClust;
	private static DistanceType t;
	private static GoodnessType cgt;
	/*
	 * "ED" 2 10 "C:/Users/dav_0/Desktop/pam_big.arff" 1 20 0 
	 * "LD" 2 10 "C:/Users/dav_0/Desktop/stringTest.arff" 1 20 0
	 */

	public Manager(String[] args) throws Exception {
		switch (args[0]) {
		case "ED":
			t = DistanceType.EUCLIDEAN;
			break;
		case "LD":
			t = DistanceType.LEVENSHTEIN;
			break;
		}
		
		cgt = GoodnessType.ELEMENT_QTY;
		
		minClust = Integer.parseInt(args[1]);
		maxClust = Integer.parseInt(args[2]);
		// nClust = Integer.parseInt(args[1]);
		String filePath = args[3];
		seed = Integer.parseInt(args[4]);
		threads = new Thread[MAX_THREADS];
		if (!args[5].equalsIgnoreCase("inf")) {
			K = Integer.parseInt(args[5]);
		}
		delta = Double.parseDouble(args[6]);
		
		N = 1000;
		minLen = 3;
		maxLen = 6;
		alphaLen = 10;
		
		alphabet = alphaCreator(alphaLen);

		rand = new Random(seed);
		String[] s = new String[N];

		for (int i = 0; i < N; i++) {
			int len = rand.nextInt(alphaLen * maxLen);
			while (len < alphaLen * minLen || len > alphaLen * maxLen) {
				len = rand.nextInt(26 * maxLen);
			}
			s[i] = "";
			for (int j = 0; j < len; j++) {
				int val = rand.nextInt(alphaLen);
				s[i] += alphabet[val];
			}
		}
		createStringARFF(s);

		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();

		// currentConfig = new Configuration(data, nClust, seed);
		// firstConfig = currentConfig.clone();
		// firstConfig.printStatus();
		// bestConfig = currentConfig.clone();
		configs = new Configuration[MAX_THREADS];

		int count = 0;
		int n_test = 3;
		double bestCG = Double.MAX_VALUE;
		double cg = 0.0;
		boolean flag = true;
		int nc = 0;

		// variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();

		for (int m = minClust; m <= maxClust; m++) {
			for(int n=0; n<n_test; n++){
				nClust = m;
				currentConfig = new Configuration(data, nClust, rand, t);
				firstConfig = currentConfig.clone();
//				printCluster(firstConfig, m);
				if (optimalNClust == null) {
					optimalNClust = currentConfig.clone();
				}
	//			firstConfig.printStatus();
				bestConfig = currentConfig.clone();
				flag = true;
				while (flag) {
	//				printCluster(firstConfig, m);
					sizes = new ArrayList<Integer>();
					for (int i = 0; i < nClust; i++) {
						sizes.add(firstConfig.getCentroidAt(i).getInstanceList().size());
					}
					// System.err.println();
					// System.err.println();
					// firstConfig.printStatus();
					// System.out.println("Combination per thread: " + K/MAX_THREADS
					// + " resto: " + K%MAX_THREADS);
					if (K != 0) {
						if (delta == 0) {
							randCombs = randCombinations(sizes, rand);
						}
						if (delta != 0) {
							randCombs = randCombinationsDelta(sizes, firstConfig, data, delta);
						}
						// printRandCombs(randCombs);
						int val = K / MAX_THREADS;
						for (int i = 0; i < MAX_THREADS; i++) {
							if (i == MAX_THREADS - 1) {
								val = K / MAX_THREADS + K % MAX_THREADS;
							}
							threads[i] = new Thread(new Task(data, nClust, i, randCombs[i], val, configs, t));
							threads[i].start();
						}
					} else {
						int[] firstCombination = new int[nClust];
						for (int i = 0; i < nClust; i++) {
							firstCombination[i] = firstConfig.getCentroidAt(i).getID();
							// System.err.println(firstConfig.getCentroidAt(i).getID());
						}
						// firstConfig.printStatus();
						comb = new Combinations(sizes, firstCombination);
						configs = new Configuration[MAX_THREADS];
						final long configToTest = comb.getMaxComb();
						if (configToTest < MAX_THREADS) {
							MAX_THREADS = (int) configToTest;
						}
						System.out.println("Max comb: " + configToTest);
						long valQty = configToTest / MAX_THREADS;
						// System.out.println(valQty + " " + (configToTest -
						// (valQty*MAX_THREADS)));
						long[] qty = new long[MAX_THREADS];
						for (int i = 0; i < MAX_THREADS; i++) {
							qty[i] = valQty;
							if (i == (MAX_THREADS - 1)) {
								qty[i] += configToTest - (valQty * MAX_THREADS);
							}
						}
						for (int i = 0; i < MAX_THREADS; i++) {
							threads[i] = new Thread(new Task(data, nClust, qty[i], sizes, firstCombination,
									comb.getCombination(qty[i]), i, configs, t));
							threads[i].start();
						}
					}
					for (int i = 0; i < MAX_THREADS; i++) {
						threads[i].join();
					}
					for (int i = 0; i < MAX_THREADS; i++) {
						if (configs[i].isBetterThan(bestConfig)) {
							bestConfig = configs[i].clone();
						}
					}
	//				printCluster(bestConfig, m);
					flag = bestConfig.isChanged(firstConfig);
					firstConfig = bestConfig.clone();
					count++;
				}
				
				 cg = clusterGoodness(bestConfig);
				 System.err.println(cg + " " + bestCG); 
				 if(cg < bestCG){ 
					 optimalNClust = bestConfig.clone();
					 nc = m; 
					 bestCG = cg; 
				 }
			}
		}
		// System.out.println("threads terminated " + count);

		// printCluster(firstConfig, nClust);
		// printCluster(bestConfig, nClust);
		printCluster(optimalNClust, nc);

		// double p = clusterGoodness(bestConfig);

		// variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime) / 1000000000;
		System.out.println("Execution time: " + time + " s");
		// bestConfig.outputARFF(data, args);
		switch(t){
			case EUCLIDEAN:
				optimalNClust.outputARFF(data, args);
				break;
			case LEVENSHTEIN:
				optimalNClust.outputStringARFF(data, args);
				break;
			default:
				System.err.println("Error output file");
		}
	}

	public static double clusterGoodness(Configuration c) {
		switch(cgt){
			case ELEMENT_QTY:
				double min = Double.MAX_VALUE;
				double max = 0.0;
				int val = 0;
				for (int i = 0; i < c.retClusterCount(); i++) {
					val = c.getCentroidAt(i).getNumElements();
					if (val < min) {
						min = val;
					}
					if (val > max) {
						max = val;
					}
				}
				// System.out.println("Cluster Goodness: " + max/min);
				return max / min;
			default:
				System.err.println("GoodnessType Error");
				return -0.1;
		}
	}

	public static int[][][] randCombinations(ArrayList<Integer> sizes, Random rand) {
		int[][][] randCombs = new int[MAX_THREADS][(K / MAX_THREADS) + (K % MAX_THREADS)][nClust];
		int val;
		int qty = K / MAX_THREADS;
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					int range = sizes.get(h);
					if (range == 0) {
						range = 1;
					}
					val = rand.nextInt(range);
					randCombs[i][j][h] = firstConfig.getCentroidAt(h).getID(val);
				}
			}
		}
		return randCombs;
	}

	public static int[][][] randCombinationsDelta(ArrayList<Integer> sizes, Configuration firstConf, Instances data,
			double delta) {
		int[][][] randCombs = new int[MAX_THREADS][(K / MAX_THREADS) + (K % MAX_THREADS)][nClust];
		Random rand = new Random(seed);
		int val = 0;
		int qty = K / MAX_THREADS;
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					int range = sizes.get(h);
					if (range == 0) {
						range = 1;
					}

					while (true) {
						double cost = 0;
						val = rand.nextInt(range);
						for (int ii = 0; ii < data.instance(val).numAttributes(); ii++) {
							cost += Math.pow(data.instance(val).value(ii)
									- data.instance(firstConf.getCentroidAt(h).getID()).value(ii), 2);
						}
						cost = Math.sqrt(cost);
						if (cost >= delta) {
							break;
						}
					}
					randCombs[i][j][h] = firstConfig.getCentroidAt(h).getID(val);
				}
			}
		}
		return randCombs;
	}

	public static void printRandCombs(int[][][] randCombs) {
		int qty = K / MAX_THREADS;
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					System.err.print(randCombs[i][j][h] + " ");
				}
				System.err.println();
			}
		}
	}

	public static void printCluster(Configuration c, int nClust) {
		for (int i = 0; i < nClust; i++) {
			System.out.println("Cluster " + "[" + i + "]");
			System.out.println("Centroid " + "[" + c.getCentroidAt(i).id + "]");
			System.out.print("Elements [ ");
			for (int l : c.getCentroidAt(i).getAllInstances()) {
				System.out.print(l + " ");
			}
			System.out.print("]");
			System.out.println();
			System.out.println("Num. of elements " + c.getCentroidAt(i).getNumElements());
			System.out.println();
		}
	}

	public static void printStatus(ArrayList<Centroid> cluster) {
		System.out.print("Centroids: ");
		for (int i = 0; i < cluster.size(); i++) {
			System.out.print(cluster.get(i).getID() + " ");
		}
	}

	public char[] alphaCreator(int alphaLen) {
		if (alphaLen < 2) {
			alphaLen = 2;
		}
		if (alphaLen > 26) {
			alphaLen = 26;
		}
		char[] alphabet = new char[alphaLen];
		for (int i = 0; i < alphaLen; i++) {
			alphabet[i] = (char) (i + 65);
			// System.out.println(alphabet[i]);
		}
		return alphabet;
	}

	public void createStringARFF(String[] s) throws IOException {
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
		for (int i = 0; i < N; i++) {
			bw.append(s[i]);
			if (i <= N - 2) {
				bw.newLine();
			}
		}
		bw.close();
	}
}