package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeanModifiedPlus implements KMeansVariant
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
	    
	    public SimpleKMeanModifiedPlus(MemHelper helper)    
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

	Random rand = new Random();

	ArrayList<Centroid> choenCentroids = new ArrayList<Centroid>(k);
	newCentroids = new ArrayList<Centroid>(k);  
	IntArrayList allCentroids = new IntArrayList();	// All distinct chosen centroids              

	int totalPoints			 = dataset.size();		// All users
	int C					 = 0;					// Centroid
	int possibleC			 = 0;					// A point from dataset
	double possibleCSim		 = 0;	 				// Sim of the point from the dataset


	for(int i = 0; i < k; i++) 					//for total number of clusters         
	{

		//-----------------------------------
		// For first loop, we find the point 
		// at uniformly random
		//-----------------------------------        	

		if(i==0) 
		{        		
			int dum = rand.nextInt(totalPoints-1);
			C= dataset.get(dum);
			allCentroids.add(C);
		}

		//-----------------------------------
		// Now choose points using KMeans Plus 
		// 
		//-----------------------------------        	

		else
		{
			double bottomSum =0;		//for finding KMeans++ prob

			//-----------------------
			// Find Sim for all users
			//-----------------------

			// good to make it local, as for each new centroid, we want new weights
			OpenIntDoubleHashMap uidToCentroidSim = new OpenIntDoubleHashMap();	
			OpenIntDoubleHashMap uidToCentroidProb = new OpenIntDoubleHashMap(); //For KMeans prob counting
			int currentCentroidsSize = allCentroids.size();
			int existingCentroid     = 0;
			double closestWeight	 = 2;

			for(int j=0;j<totalPoints;j++) //for all points
			{
				//Get a point
				possibleC  	  = dataset.get(j);		
				closestWeight = 10;

				for (int m=0;m<currentCentroidsSize; m++)
				{
					// Get an existing centroid
					existingCentroid =  allCentroids.get(m);

					//-----------------------------
					// Now we find distance of each
					// point from closest centroid
					// i.e. sim > largest 
					//-----------------------------

					//Now we find the similarity between a user and the chosen cluster.        			
					possibleCSim =  findSimBetweenACentroidAndUser(existingCentroid, possibleC);
					if(possibleCSim < closestWeight )
						closestWeight = possibleCSim;

				}

				// only add the distance of a point with the closest centorid
				uidToCentroidSim.put(possibleC, closestWeight);
				bottomSum +=(closestWeight * closestWeight);

			} // finsihed finding similarity b/w all users and the chosen centroid


			//-----------------------
			// Find the next centroid
			// Pro = D(x')^2/sum(D(x)^2)
			//-----------------------

			IntArrayList myUsers 	 	= uidToCentroidSim.keys();
			DoubleArrayList myWeights 	= uidToCentroidSim.values();
			int totalUsersSize 			= myUsers.size();

			for (int m=0;m<totalUsersSize;m++)
			{
				int uid 			=  myUsers.get(m);
				double pointXWeight =  myWeights.get(m);
				double prob 		=  (pointXWeight * pointXWeight) / bottomSum;

				uidToCentroidProb.put(uid,prob);		//Uid to Prob
			}

			// sort prob weights in ascending order (So first element has the lowest sim)	

			IntArrayList    myProbUsers 	 = uidToCentroidProb.keys();
			DoubleArrayList myProbWeights    = uidToCentroidProb.values();      		  	
			uidToCentroidSim.pairsSortedByValue(myProbUsers, myProbWeights);

			int toalPossibleC = uidToCentroidProb.size();

			// As both are sorted, so it should be in the first index
			// Make sure, we have not already added this in the list of centroids
			for (int j=0;j<toalPossibleC; j++ )
			{
				C = myProbUsers.get(j);
				int moviesSeenByUser = helper.getNumberOfMoviesSeen(C);

				if( !(allCentroids.contains(C)) && moviesSeenByUser>1)
				{	 
					allCentroids.add(C);        				  			
					break;        					  	
				}

			} // only the last one will be added


		} //end of else

		// Add the chosen centroid in the list of K centroids

		choenCentroids.add( new Centroid (C,helper));

	}

	return choenCentroids;

}
     
/*******************************************************************************************************/

    /**
     * Find the sim between a centroid and a point, we can use VS and PCC for it (This is VS) [0,1]
     * @param int center, int point  
     * @return double similarity
     */ 
     
  public double findSimBetweenACentroidAndUser (int center, int point)
  {
 	    
 	 int amplifyingFactor = 50;			//give more weight if users have more than 50 movies in common	 
 	 double functionResult = 0.0;
 	 
 	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
      topSum = bottomSumActive = bottomSumTarget = 0;
      
//      double activeAvg = helper.getAverageRatingForUser(center);
//      double targetAvg = helper.getAverageRatingForUser(point);
      
      ArrayList<Pair> ratings = helper.innerJoinOnMoviesOrRating(center,point, true);
      
      // If user have no ratings in common, send -2 back
        if(ratings.size() ==0)
      	return 0;
      
      for (Pair pair : ratings)         
      {
          rating1 = (double) MemHelper.parseRating(pair.a) ;
          rating2 = (double) MemHelper.parseRating(pair.b) ;
          
          topSum += rating1 * rating2;
      
          bottomSumActive += Math.pow(rating1, 2);
          bottomSumTarget += Math.pow(rating2, 2);
      }
      
      double n = ratings.size() - 1;     
      if(n == 0)
          n++;     
      
      bottomSumActive =Math.sqrt(bottomSumActive);
      bottomSumTarget =Math.sqrt(bottomSumTarget);
     
     if (bottomSumActive != 0 && bottomSumTarget != 0)
     {    	
     	functionResult = (1 * topSum) / (bottomSumActive * bottomSumTarget);  //why multiply by n?   	  	
     	return  functionResult * (n/amplifyingFactor); //amplified send    	
     }
     
     else     
     	return 0;			 
     
 	 
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

