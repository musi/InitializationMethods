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
public class SimpleKMeansVariance implements KMeansVariant
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
	    
	    public SimpleKMeansVariance(MemHelper helper)    
	    {
	        this.helper   = helper;

	    }


/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid at varying distance from overall mean
     *
     * @param  dataset  The list uids. 
     * @param  k  The number of centroids (clusters) desired. 
     * @return A List of randomly chosen centroids. 
     */
	    @Override
		public ArrayList<Centroid> chooseCentroids(int variant,IntArrayList dataset, int k, double cliqueAverage) 
		
		{

			System.out.println("=========================================");
			System.out.println("       " + getName(variant));
			System.out.println("=========================================");

	    	ArrayList<Centroid> chosenCentroids = new ArrayList<Centroid>(k);
	    	newCentroids = new ArrayList<Centroid>(k);        
	    	IntArrayList allCentroids = new IntArrayList();		    // All distinct chosen centroids              

	    	int totalPoints			 = dataset.size();			// All users
	    	int C					 = 0;						// Centroid			
	    	int possibleC			 = 0;						// A point from dataset
	    	double possibleCSim		 = 0;	 					// Sim of the point from the dataset
	    	double  avg				= helper.getGlobalAverage();
	    	int avgI					= (int)avg;

	    	for(int i = 0; i < k; i++) 					//for total number of clusters         
	    	{

	    		OpenIntDoubleHashMap uidToCentroidSim = new OpenIntDoubleHashMap();	

	    		//------------------------------
	    		// Find sim to centroid
	    		//------------------------------

	    		for(int j=0;j<totalPoints;j++) //for all points
	    		{
	    			//Get a point
	    			possibleC  	   = dataset.get(j);		
	    			possibleCSim =  findSimPCCBetweenACentroidAndUser(possibleC, avgI);
	    			uidToCentroidSim.put(possibleC, possibleCSim);

	    		}
	    		IntArrayList myUsers 	 	= uidToCentroidSim.keys();
	    		DoubleArrayList myWeights    =uidToCentroidSim.values();      		   
	    		uidToCentroidSim.pairsSortedByValue(myUsers, myWeights);

	    		int totalPossibleC = uidToCentroidSim.size();
	    		int number	=		0;
	    		int m;

	    		for (int j=1;j<totalPossibleC; j++ )
	    		{

	    			m= (j-1)*totalPoints;
	    			number = 1+ m/k;
	    			C= myUsers.get(number);


	    			if(allCentroids.contains(C)==false)
	    			{	 
	    				allCentroids.add(C);        				  			
	    				break;        					  	
	    			}

	    		} // only the last one will be added

	    		chosenCentroids.add( new Centroid (C,helper));
	    		this.centroids = chosenCentroids; 

	    	}

	    	return chosenCentroids;

		}


/*******************************************************************************************************/
    /**
     * Find the sim between a centroid and a point, we can use VS and PCC for it
     * @param int center, int point  
     * @return double similarity
     */ 
     

                 
              public double  findSimPCCBetweenACentroidAndUser (int center, int point)
              {
             	    
             	 int amplifyingFactor = 50;			//give more weight if users have more than 50 movies in common	 
             	 double functionResult = 0.0;
             	 
             	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
                  topSum = bottomSumActive = bottomSumTarget = 0;
                  
                  double activeAvg = helper.getAverageRatingForUser(center);
                  double targetAvg = helper.getAverageRatingForUser(point);
                  
                  ArrayList<Pair> ratings = helper.innerJoinOnMoviesOrRating(center,point, true);
                  
                  // If user have no ratings in common, send -2 back
                    if(ratings.size() ==0)
                  	return 0;
                  
                  for (Pair pair : ratings)         
                  {
                      rating1 = (double) MemHelper.parseRating(pair.a) - activeAvg;
                      rating2 = (double) MemHelper.parseRating(pair.b) - targetAvg;
                      
                      topSum += rating1 * rating2;
                  
                      bottomSumActive += Math.pow(rating1, 2);
                      bottomSumTarget += Math.pow(rating2, 2);
                  }
                  
                  double n = ratings.size() - 1;     
                  if(n == 0)
                      n++;     
                 
                 if (bottomSumActive != 0 && bottomSumTarget != 0)
                 {    	
                 	functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?   	
                 	// return  functionResult; //simple send    	
                 	 return  functionResult * (n/amplifyingFactor); //amplified send    	
                 }
                 
                 else     
                 	return 0;			 
                 
             	 
              }
                 
             //----------------------------------------------------------------
              
              public double  findSimVSBetweenACentroidAndUser (int center, int point)
              {
             	    
             	 int amplifyingFactor = 50;			//give more weight if users have more than 50 movies in common	 
             	 double functionResult = 0.0;
             	 
             	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
                  topSum = bottomSumActive = bottomSumTarget = 0;
                  
//                  double activeAvg = helper.getAverageRatingForUser(center);
//                  double targetAvg = helper.getAverageRatingForUser(point);
                  
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
                 
                 if (bottomSumActive != 0 && bottomSumTarget != 0)
                 {    	
                 	functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?   	
                 	// return  functionResult; //simple send    	
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

