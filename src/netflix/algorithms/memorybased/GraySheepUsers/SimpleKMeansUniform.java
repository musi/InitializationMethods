package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;
import cern.jet.random.Uniform;


/************************************************************************************************/
public class SimpleKMeansUniform  implements KMeansVariant
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
	    
	    public SimpleKMeansUniform(MemHelper helper)    
	    {
	        this.helper   = helper;

	    }



/**********************************************************************************************/
 
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid based on uniform distribution
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
			
			ArrayList<Centroid> centroids = new ArrayList<Centroid>(k);
			newCentroids = new ArrayList<Centroid>(k);
			IntArrayList chosenCentroids = new IntArrayList(); 

			int number 							= 0;
			IntArrayList centroidAlreadyThere	= new IntArrayList();   
			int C								= 0;
			double avg;
			//	         double avgMov;
			int totalPoints			 = dataset.size();			// All users

			int possibleC			 = 0;						// A point from dataset
			double possibleCSim		 = 0;	 					// Sim of the point from the dataset


			avg= helper.getGlobalAverage();
			//	         avgMov= helper.getGlobalMovAverage();

			int avgI					= (int)avg;


			for(int i = 0; i < k; i++)         
			{     OpenIntDoubleHashMap uidToCentroidSim = new OpenIntDoubleHashMap();	

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

			//	     	int totalPossibleC = uidToCentroidSim.size();   
			int  	min = myUsers.get(1);
			int 	max= myUsers.get(totalPoints-1);
			while(true)        		
			{   
				//------------------
				//Uniform distribution 
				//------------------

				//	              Uniform unif;
				//	      		unif=new Uniform(0,k,avgI);
				//	     			number= unif.nextInt();

				//-------------------------------
				//Uniform distribution with min max
				//--------------------------------

				number= Uniform.staticNextIntFromTo(min, max);


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

             //----------------------------------------------------------------      
/*******************************************************************************************************/
     
}

