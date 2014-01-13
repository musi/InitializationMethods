package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;
//import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;
import cern.jet.random.HyperGeometric;


// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeansHyperGeometric  implements KMeansVariant
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
	    
	    public SimpleKMeansHyperGeometric(MemHelper helper)    
	    {
	        this.helper   = helper;

	    }

/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid based on hyperGeometric distribution
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

     int totalPoints			 = dataset.size();			// All users


     for(int i = 0; i < k; i++)         
     {   
  		while(true)        		
 		{ 
   		 //------------------
   		 // HyperGeometric distribution	
   		 //------------------ 	

 			number=  (int) HyperGeometric.staticNextInt(totalPoints, 10, k);  

 			C=dataset.get(number);
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

