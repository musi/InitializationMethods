package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.ArrayList;

import cern.colt.list.IntArrayList;

public interface KMeansVariant {
	
	
public abstract ArrayList<Centroid> chooseCentroids(int variant, IntArrayList dataset, 		//all users in the database
		int k, 					
        double cliqueAverage);


String getName(int variant);

//ArrayList<Centroid> chooseCentroid(int variant, IntArrayList dataset, int k,
//		double cliqueAverage);





}
