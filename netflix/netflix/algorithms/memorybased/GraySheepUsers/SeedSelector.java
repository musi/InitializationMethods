package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.ArrayList;
import cern.colt.list.IntArrayList;

public interface SeedSelector {
	
    public ArrayList<Centroid> chooseCentroids(IntArrayList dataset,	//no of users in the database 
													  int k		  			//how much clusters?
	 											      );    
	

}
