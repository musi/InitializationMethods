package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;
//import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;



/************************************************************************************************/
public class SimpleKMeansQuantile implements KMeansVariant
/************************************************************************************************/

{

	    private MemHelper 	helper;
	    
	    ArrayList<Centroid> centroids;
	    ArrayList<Centroid> newCentroids;
	    OpenIntIntHashMap   clusterMap;
	    boolean 			converged;					  //Algorithm converged or not
	    int 				simVersion;
	    
/************************************************************************************************/

	    /**
	     * Builds the RecTree and saves the resulting clusters.
	     */
	    
	    public SimpleKMeansQuantile(MemHelper helper)    
	    {
	        this.helper   = helper;
	   
	    }


/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid which are at varying distance form each other 
     *
     * @param  dataset  The list uids. 
     * @param  k  The number of centroids (clusters) desired. 
     * @return A List of randomly chosen centroids. 
     */

@Override
public ArrayList<Centroid> chooseCentroids(int variant, IntArrayList dataset,int k, double cliqueAverage) 
{

	System.out.println("=========================================");
	System.out.println("       " + getName(variant));
	System.out.println("=========================================");

	ArrayList<Centroid> centroids = new ArrayList<Centroid>(k);
	newCentroids = new ArrayList<Centroid>(k);
	IntArrayList chosenCentroids = new IntArrayList(); 

	int number 							= 0;
	IntArrayList centroidAlreadyThere	= new IntArrayList();   
	int C								= 0;
	int datasetSize 					= dataset.size();
	int M;

	//-----------------------------------
	// choose points at varying distance  
	// 
	//-----------------------------------  

	for(int i = 1; i <= k; i++)         
	{        
		while(true)        		
		{   

			M= (i-1)*datasetSize;
			number = 1+ M/k;
			C= dataset.get(number);     

			if(!centroidAlreadyThere.contains(C));
			{
				centroidAlreadyThere.add(C);
				break;
			}
		}

		centroids.add( new Centroid (C,helper));
		chosenCentroids.add(C);

	}

	return centroids;

}
	//----------------
	//  get variant name
	// ---------------

@Override
public String getName(int variant) {

String name = null;
KMeansVariant var = new KMeanRecNF();
name= var.getName(variant);
return name;
}
    
/*******************************************************************************************************/

}

