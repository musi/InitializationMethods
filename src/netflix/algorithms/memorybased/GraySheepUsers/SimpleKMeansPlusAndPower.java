package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;
//import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeansPlusAndPower implements KMeansVariant
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
	    
	    public SimpleKMeansPlusAndPower(MemHelper helper)    
	    {
	        this.helper   = helper;

	    }



/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid which is at the farthest distant from the current one 
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

	    	ArrayList<Centroid> chosenCentroids = new ArrayList<Centroid>(k);
	    	newCentroids = new ArrayList<Centroid>(k);        
	    	IntArrayList allCentroids = new IntArrayList();		    // All distinct chosen centroids              
	    	OpenIntIntHashMap powerUsers	 = new OpenIntIntHashMap();	// All user who seen greater than Movies_threshold movies

	    	int totalPoints			 = dataset.size();			// All users
	    	int C					 = 0;						// Centroid
	    	//	        int previousC			 = 0;						// Previous centroid
	    	int possibleC			 = 0;						// A point from dataset
	    	int moviesThreshold		 = 50;						// power user defination
	    	double possibleCSim		 = 0;	 					// Sim of the point from the dataset

	  
	    	//---------------------------
	    	// Find power users
	    	//---------------------------

	    	for(int j=0;j<totalPoints;j++) //for all points
	    	{
	    		possibleC  	   = dataset.get(j);
	    		int moviesSeen = helper.getNumberOfMoviesSeen(possibleC);

	    		if( moviesSeen > moviesThreshold)
	    		{
	    			powerUsers.put(possibleC, moviesSeen);
	    		}
	    	}

	    	int powerUsersSize= powerUsers.size();

	    	IntArrayList myPowerUsers 	 	= powerUsers.keys();
	    	IntArrayList myPowerWeights    = powerUsers.values();      		   
	    	powerUsers.pairsSortedByValue(myPowerUsers, myPowerWeights);

	    	System.out.println("power users found = " + powerUsersSize);


	    	for(int i = 0; i < k; i++) 					//for total number of clusters         
	    	{

	    		//-----------------------------------
	    		// For first loop, we find the point 
	    		// at uniformly random
	    		//-----------------------------------        	

	    		if(i==0) 
	    		{
	    			C = myPowerUsers.get(powerUsersSize-1);
	    			allCentroids.add(C);
	    			chosenCentroids.add( new Centroid (C,helper));
	    			this.centroids = chosenCentroids; 
	    		}

	    		//-----------------------------------
	    		// Now choose points using KMeans Plus 
	    		// 
	    		//-----------------------------------        	

	    		else
	    		{
	    			// good to make it local, as for each new centroid, we want new weights
	    			OpenIntDoubleHashMap uidToCentroidSim = new OpenIntDoubleHashMap();	
	    			int currentCentroidsSize = allCentroids.size();
	    			//	         	    int existingCentroid     = 0;
	    			double closestWeight	 = 10;

	    			//------------------------------
	    			// Find sim for only power users
	    			//------------------------------

	    			for(int j=0;j<powerUsersSize;j++) //for all points
	    			{
	    				//Get a point
	    				possibleC  	  = myPowerUsers.get(j);		
	    				closestWeight = 10;

	    				for (int m=0;m<currentCentroidsSize; m++)
	    				{
	    					// Get an existing centroid
	    					//	         				existingCentroid =  allCentroids.get(m);

	    					//-----------------------------
	    					// Now we find distance of each
	    					// point from closest centroid
	    					// i.e. sim > largest 
	    					//-----------------------------

	    					//Now we find the similarity between a user and the chosen cluster.    
	    					//In fact, we will find the min possibleCSim here(means the farthest distance)
	    					possibleCSim =  findSimWithOtherClusters(possibleC, m);
	    					if(closestWeight > possibleCSim)
	    						closestWeight = possibleCSim;

	    				}

	    				// only add the distance of a point with the closest centorid
	    				uidToCentroidSim.put(possibleC, closestWeight);

	    			} // finsihed finding similarity b/w all users and the chosen centroid

	    			//-----------------------
	    			// Find the next centroid
	    			//-----------------------

	    			// sort weights in ascending order (So first element has the lowest sim)	
	    			IntArrayList myUsers 	 	= uidToCentroidSim.keys();
	    			DoubleArrayList myWeights = uidToCentroidSim.values();
	    			uidToCentroidSim.pairsSortedByValue(myUsers, myWeights);

	    			int toalPossibleC = uidToCentroidSim.size();

	    			// As both are sorted, so it should be in the first index
	    			// Make sure, we have not already added this in the list of centroids
	    			for (int j=0;j<toalPossibleC; j++ )
	    			{
	    				C = myUsers.get(j);

	    				if(allCentroids.contains(C)==false)
	    				{	 
	    					allCentroids.add(C);        				  			
	    					break;        					  	
	    				}

	    			} // only the last one will be added

	    			chosenCentroids.add( new Centroid (C,helper));
	    			this.centroids = chosenCentroids; 

	    		} //end of else


	    	}

	    	return chosenCentroids;
	    }


/*******************************************************************************************************/
    
    /**
     * Find the sim b/w a user and other clusters (other than the one in which a user lies)
     * @param uid
     * @return Sim between user and centroid
     */
    
    public double findSimWithOtherClusters(int uid, int i)
    {
   	 
   	 double distance =0.0;   
          
  	 if(simVersion==1)
   		 distance = centroids.get(i).distanceWithDefault(uid, helper.getGlobalAverage(), helper);
   	 else if(simVersion==2)
   		distance = centroids.get(i).distanceWithoutDefault(uid, helper.getGlobalAverage(), helper);
   	 else if(simVersion==3)
   		 distance = centroids.get(i).distanceWithDefaultVS(uid, helper.getGlobalAverage(), helper);
   	 else if(simVersion==4)
   		 distance = centroids.get(i).distanceWithoutDefaultVS(uid, helper.getGlobalAverage(), helper);
   	 else if(simVersion==5)
   			 distance = centroids.get(i).distanceWithPCC(uid, i, helper);   	 
   	else if(simVersion==6)
			 distance = centroids.get(i).distanceWithVS(uid, i, helper);
   	  	 
   	 return distance;	 
   	 
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

