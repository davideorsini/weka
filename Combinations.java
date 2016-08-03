package weka.clusterers;

import java.util.ArrayList;

public class Combinations{
	private int[] clustersSize; 
	private int[] currentCombination;
	private int[] firstCombination;
	private boolean flagEnded = false;
	private int[] bases;
	private long maxComb = 0;
	
	public Combinations(ArrayList<Integer> list, int[] firstCombination) {
		this.firstCombination = firstCombination;
		clustersSize = new int[list.size()];
		currentCombination = new int[list.size()];
		for (int i=0; i<list.size(); i++) {
			clustersSize[i] = list.get(i);
			currentCombination[i] = list.get(i);
		}
		bases = new int[clustersSize.length];
		int acc = 1;
		for(int i=clustersSize.length-1; i>=0; i--){
			bases[i] = acc;
			acc = acc * (clustersSize[i] + 1);
		}
		maxComb = acc;
//		printArray(bases);
//		printArray(clustersSize);
	}
	
	public Combinations(ArrayList<Integer> list, int[] firstCombination, int[] offsetComb) {
		this(list, firstCombination);
		this.currentCombination = offsetComb;
		printArray(offsetComb);
	}
	
	public static void printArray(int[] array) {
		if (array == null)
			return;
		String output = "{ ";
		for (int i=0; i<array.length; i++) {
			output += array[i] + ", ";
		}
		
		output += "}";
		System.out.println(output);
	}
	
	private void decreaseUnit(int index){
		if(currentCombination[index] == 0){
			currentCombination[index] = clustersSize[index];
			if(index > 0){
				decreaseUnit(index - 1);
			}
			else{
				System.out.println("Combination depleted");
				flagEnded = true;
			}
		}
		else{
			currentCombination[index]--;
		}
	}
	
	//prende la comb corrente e salta le pross combQty combinazioni
	public int[] getCombination(int combQty) throws IllegalArgumentException{
//		System.out.println(combQty);
//		System.out.println(comb2Int(currentCombination));
//		if(comb2Int(currentCombination) > combQty){
//			throw new IllegalArgumentException();
//		}
		if(flagEnded){
			return null;
		}
		//salto la prima configurazione quando capita
//		if(currentCombination == firstCombination){
//			decreaseUnit(currentCombination.length-1);
//		}
		
		int[] toRet = new int[clustersSize.length];
		toRet = currentCombination;
		long intVal = comb2Int(currentCombination);
		intVal = intVal - (combQty -1);
		currentCombination = int2Comb(intVal);
		return toRet;
	}	
	
	private long comb2Int(int[] comb){
		long counter = 0;
		for(int i=0; i<comb.length; i++){
			counter += bases[i] * comb[i];
		}
		return counter;
	}
	
	private int[] int2Comb(long counter){
		int[] comb = new int[clustersSize.length];
		for(int i=0; i< clustersSize.length; i++){
			comb[i] = ((int)counter) / bases[i];
			counter = counter % bases[i];
		}
		return comb;
	}
	
	public int[] getCombination(){
		if(flagEnded){
			return null;
		}
		//salto la prima configurazione quando capita
		if(currentCombination == firstCombination){
			decreaseUnit(currentCombination.length-1);
		}
		
		int[] toRet = new int[clustersSize.length];
		toRet = currentCombination;
		decreaseUnit(currentCombination.length-1);
		return toRet;
	}
	
	public boolean isDepleted(){
		return this.flagEnded;
	}
	
	public long getMaxComb(){
		return maxComb;
	}
	
	public int[] getBases(){
		return bases;
	}
	
	public void printComb(){
		System.err.println("Combination {");
		for(int i=0; i<currentCombination.length; i++){
			System.err.print(currentCombination[i] + "  ");
		}
		System.err.print("}");
	}
}