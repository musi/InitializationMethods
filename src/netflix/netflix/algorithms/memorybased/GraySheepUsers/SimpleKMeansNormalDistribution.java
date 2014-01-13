package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;

import cern.colt.list.*;
import cern.colt.map.*;
import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;



/************************************************************************************************/
public class SimpleKMeansNormalDistribution  implements KMeansVariant
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
	    
	    public SimpleKMeansNormalDistribution(MemHelper helper)    
	    {
	        this.helper   = helper;
	
	    }

/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid based on normal distribution
     *
     * @param  dataset  The list uids. 
     * @param  k  The number of centroids (clusters) desired. 
     * @return A List of randomly chosen centroids. 
     */

		@Override
		public ArrayList<Centroid> chooseCentroids(int variant, IntArrayList dataset, int k, double cliqueAverage) 
		{


			System.out.println("=========================================");
			System.out.println("       " + getName(variant));
			System.out.println("=========================================");
			
			ArrayList<Centroid> centroids = new ArrayList<Centroid>(k);
			newCentroids = new ArrayList<Centroid>(k);
			IntArrayList chosenCentroids = new IntArrayList(); 


			RandomEngine eng;							
			IntArrayList centroidAlreadyThere	= new IntArrayList();   
			int C								= 0;

			// double avg; 
			double avgMov;

			//avg= helper.getGlobalAverage();
			avgMov= helper.getGlobalMovAverage();
			Normal norm ;
			int number;

			for(int i = 1; i <= k; i++)         
			{        
				while(true)        		
				{  

					//------------------
					// Normal distribution	
					//------------------ 			

					eng= Normal.makeDefaultGenerator();
					norm = new Normal(avgMov, 0.05, eng);
					//norm = new Normal(avg, 0.1, eng);
					number= (int) norm.nextInt();

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

    //---------------------
    public double findSimWithOtherClusters(int uid, int i, int version)
    {
   	 
   	 double distance =0.0;   

   	 if(version==1)
   		 distance = centroids.get(i).distanceWithDefault(uid, helper.getGlobalAverage(), helper);
   	 else if(version==2)
   		distance = centroids.get(i).distanceWithoutDefault(uid, helper.getGlobalAverage(), helper);
   	 else if(version==3)
   		 distance = centroids.get(i).distanceWithDefaultVS(uid, helper.getGlobalAverage(), helper);
   	 else if(version==4)
   		 distance = centroids.get(i).distanceWithoutDefaultVS(uid, helper.getGlobalAverage(), helper);
   	 else if(version==5)
   			 distance = centroids.get(i).distanceWithPCC(uid, i, helper);   	 
   	else if(version==6)
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

