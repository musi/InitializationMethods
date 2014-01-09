package netflix.algorithms.memorybased.GraySheepUsers;

import netflix.memreader.MemHelper;

public class KMeansSelection {

	static SeedSelector mySelectorInterface = null;
	static StandardKMeans mySelectorClass = null;
	
	public static SeedSelector getMethodByInterface (int choice, MemHelper helper)
	{
		if(choice ==0){
			mySelectorInterface = new SimpleKMeanModifiedPlus(helper);
		}
		else 
			mySelectorInterface = new SimpleKMeans(helper);
		
		return mySelectorInterface;
				
	}
	
	public static StandardKMeans getMethodByClass (int choice, MemHelper helper)
	{
		if(choice ==0){
			mySelectorClass = new SimpleKMeanModifiedPlus(helper);
		}
		else 
			mySelectorClass = new SimpleKMeans(helper);
		
		return mySelectorClass;
				
	}
}
