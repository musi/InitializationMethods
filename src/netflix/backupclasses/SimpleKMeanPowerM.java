package netflix.backupclasses;

import java.io.BufferedWriter;

import java.io.FileWriter;
import java.util.*;

import netflix.algorithms.memorybased.GraySheepUsers.Centroid;
import netflix.algorithms.memorybased.GraySheepUsers.ClusterCollection;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;
import cern.jet.random.Normal;

// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeanPowerM
/************************************************************************************************/

{
	private MemHelper 	helper;
	private int 				MAX_ITERATIONS;
	private final int 			PARTION_MAX_SIZE 	= 400;
	private final int 			MAX_DEPTH 			= 2;
	private int       			howManyClusters   	= 8;
	private int       			initialClusters   	= 1;
	private int 	  			afterHowMuchSample 	= 0; //after how much samples to activate creation of new nodes
	private	int 				callNo;					 //call how many times this file been called
	private int					myCount;				 //calculate the no. of iterations
	private BufferedWriter 		myInfo;
	Timer227 					timer;   
	
	private ArrayList<IntArrayList> 	finalClusters;
	private OpenIntIntHashMap 			uidToCluster;
	
	ArrayList<Centroid> centroids;
	ArrayList<Centroid> newCentroids;
	OpenIntIntHashMap   clusterMap;
	boolean 			converged;					  //Algorithm converged or not
	int				    simVersion;
	ClusterCollection subClusters;
/************************************************************************************************/

	/**
	 * Builds the RecTree and saves the resulting clusters.
	*/
	
	public SimpleKMeanPowerM(MemHelper helper)     
	{
	    this.helper   = helper;
	    finalClusters = new ArrayList<IntArrayList>(); // Creates ArrayList with initial default capacity 10.
	    uidToCluster  = new OpenIntIntHashMap();       // <E> is for an element in the arraylist
	    clusterMap 	  = new OpenIntIntHashMap();
	      
	    myCount   	= 0;
	    callNo	  	= 0;
	    converged 	= false;
	    timer  	  	= new Timer227();
	}


/************************************************************************************************/ 
	//This is called after the constructor call 

	    
	    public void cluster(int kClusters,  int call, int iterations, int sVersion)    
	    {
	
	   	
	    	howManyClusters = kClusters;  
	    	MAX_ITERATIONS  = iterations;
	    	callNo			= call;
	    	simVersion		= sVersion;
		
	    	finalClusters 	= constructRecTreeM	(helper.getListOfUsers(), 
											howManyClusters, 
											helper.getGlobalAverage());
		
		//---------------------
		// SVD over Clustering
		//---------------------
		
		// let us write clusters into a file (It is for SVD over clusters)    	
		// writeClustersIntoAFile(finalClusters);
		
		//-------------------
		// Make map
		//-------------------
		//This is basically to make a map, a particular user is in which cluster
		
	    IntArrayList cluster;
	    
	    for(int i = 0; i < finalClusters.size(); i++)
	    {   	cluster = finalClusters.get(i);       
	    	for(int j = 0; j < cluster.size(); j++)			//a cluster is a collection of users, go through this             
	        {   uidToCluster.put(cluster.get(j), i);
	        }
	    } //end of for
	    
	    
	       for (int t=1; t<=howManyClusters; t++)
	        {
	        	System.out.print(finalClusters.get(t-1).size()+" ,");
	        	if(t>=40 && t%40==0)
	        	System.out.println(); 
	        //   	System.out.print(  " getting cluster "+ 	getClusterByID(t));
	        }
	    
	    
	   }

/************************************************************************************************/


/**
 * Gets the specified cluster by its positional id. 
 * @return  The cluster at location id in the clusters list.
 */

	public IntArrayList getClusterByID(int id)    
	{
	    return finalClusters.get(id);
	}

/************************************************************************************************/

/**
 * Gets the id for the cluster containing the specified
 * user. 
 * @return  The location of the cluster containing
 *          the specified uid in the clusters list. 
 */

	public int getClusterIDByUID(int uid)    
	{
	    return uidToCluster.get(uid);	//it will return the index of a single cluster within many (which are stored by index wise)
	}

/************************************************************************************************/

/**
 * Gets the cluster containing the specified user. 
 * @return  The cluster containing the speficied user. 
 */

	public IntArrayList getClusterByUID(int uid)    
	{
	    return finalClusters.get(uidToCluster.get(uid));	//it return the cluster
	}

/************************************************************************************************/

// I think Rec. tree is something other than the KMeans? 
// This call will return KMeans cluster data, here we have make it to behave like KMEans, else
// it was behaving like Binary tree with KMeans.

	public ArrayList<IntArrayList> constructRecTreeM(IntArrayList dataset,    // helper.getListOfUsers(),
	                                                 int currDepth,           // k,  
	                                                 double cliqueAverage)    // helper.getGlobalAverage());                                              
	{
	   ArrayList<IntArrayList> clusters = new ArrayList<IntArrayList>(currDepth);
	           	
	    //KMean -->what this returns
	   //***********************
	   
	   
	   int C								= 0;
       double avg;
       int totalPoints			 = dataset.size();			// All users
      	
       int possibleC			 = 0;						// A point from dataset
       int possibleCSim		 = 0;	 					// Sim of the point from the dataset


       avg= helper.getGlobalAverage();

       int avgI					= (int)avg;
       double bottomSum=0;
       IntArrayList dataset1 =new IntArrayList();
     


       for(int i = 0; i < currDepth; i++)         
       {    
    	   OpenIntIntHashMap uidToCentroidSim = new OpenIntIntHashMap();	

   	//------------------------------
   	// Find sim to centroid
   	//------------------------------

   	for(int j=0;j<totalPoints;j++) //for all points
   	{
   		//Get a point
   		possibleC  	   = dataset.get(j);		
   		possibleCSim =  (int) findSimPCCBetweenACentroidAndUser(possibleC, avgI);
   		uidToCentroidSim.put(possibleC, possibleCSim);
   		dataset1.add(possibleC);

   	}
//   	IntArrayList myUsers 	 	= uidToCentroidSim.keys();
//   	IntArrayList myWeights    =uidToCentroidSim.values();   
//   	//***
//		bottomSum +=(possibleCSim * possibleCSim);
//
//	  	int totalUsersSize 			= myUsers.size();
//	  	int pointXWeight;
//		  	
//		for (int m=0;m<totalUsersSize;m++)
//		{	
//			double alfa= 0.3;
//			int uid 			=  myUsers.get(m);
//			pointXWeight =  myWeights.get(m);
//			double prob 		=  alfa *((pointXWeight * pointXWeight) / bottomSum);
//			
//			int pr = (int) prob;
//			dataset1.add(pointXWeight);
////			uidToCentroidProb.put(uid,pr);		//Uid to Prob
//			
//			alfa= alfa * 0.95;
//		}
		
		// sort prob weights in ascending order (So first element has the lowest sim)	
		
//		IntArrayList    myProbUsers 	 = uidToCentroidProb.keys();
//		IntArrayList myProbWeights    = uidToCentroidProb.values();      		  	
////		uidToCentroidSim.pairsSortedByValue(myProbUsers, myProbWeights);
	   
	   
	   
	   
	   
	   //****************************
//	   double h; int p,point; double beta = 0.95;
//	   IntArrayList dataset1 =new IntArrayList();
//	   
//       for(p=0,h=0.3; p<=10;p++)
//       {
//
//       	for(int i = 0; i < dataset.size(); i++)            
//       	{
//    	  point =(int) (h* dataset.get(i));
//    	  dataset1.add(point);
//    	  
//    	// dataset1.set(i, point); 
//       	}
    
       	 subClusters = kMeans (dataset1 , 	
       			howManyClusters, 
       			cliqueAverage);
       	   
	
//	   h = h *beta;
       }
	   //*************************
	   
	   for(int t = 0; t < currDepth; t++) //for number of clusters we have (K)      
	   {
	        clusters.add(subClusters.getCluster(t));  
	        		
	    } 
	

	    return clusters;
	}

/************************************************************************************************/

/**
 * KMean: Make K clusters
 */

//It returns the cluster collection object

public ClusterCollection kMeans(IntArrayList dataset, 		//all users in the database
									int k, 					
	                                double cliqueAverage) 		//golbal average in the database    
	{
		//They will be initialized for every call
	    int newCluster = -1, point;        
	  
	    
	  //Initialise the centroids as k random points in the dataset.
	    if(callNo==1)
	    {        
	    	timer.start();	              
	    	this.centroids = choosePlusCentroids(dataset, howManyClusters);       
	        timer.stop();   
	        System.out.println("KMeans Plus centroids took " + timer.getTime() + " s to select");    	
	        timer.resetTimer();
	        
	    }
	    //****************************************
//	    double h; int p;
//	    for(p=0,h=0.3; p<=10;p++)
//	    {
//	    	for(int i=0; i < dataset.size(); i++)
//	    	{
//	    		   point = dataset.get(i); 
//	    		   point = (int) (point * h);
//	    		   
//	    	   System.out.println("Data set is :: " + dataset); 
//	    	   
//	    	}
//	    	
//	    }
	    
	    
	    
	    //****************************************
	    
	    
	      Centroid newCentroid;  
	    //Perform the clustering until the clusters converge or until
	    //we reach the maximum number of iterations.    
	//    System.out.println(" Going into ietrations..");
	    
//	while(!converged && myCount < MAX_ITERATIONS)        
	 while(!converged)
	{
	//      System.out.println("count " + count);
	        converged 	 = true;
	                
	        for(int i = 0; i < howManyClusters ; i++)             
	        {
	            newCentroid = ( new Centroid(centroids.get(i)));   	//assign the previously created centroid
	            newCentroids.add(newCentroid);
	        }
	
	        //___________________________________________________
	        
	        //For every point in the dataset, find the closest
	        //centroid. If this centroid is different from the 
	        //points previously assigned cluster then the 
	        //algorithm has not converged. 
	        
	       
	        	for(int i = 0; i < dataset.size(); i++)            
	        	{
	        		point = dataset.get(i);  						    // a uid             
	        		
	        		newCluster = findClosestCentroid(point, 			//This point is closest to this newCluster (return index of that centroid from centroids) 	
	        				centroids, 
	        				cliqueAverage);

	        		//----------------------------------------------------------
	        		//This is the first pass through the data. We add
	        		//the point to the appropriate cluster, and update
	        		//the new version of that clusters centroid. 
	        		//Infact..... This point is not in the clusterMap (which contains point-to-cluster mapping)	
	        		//So what we do, is to add this point to this clusterMap and in newCentroids 's cluster 
	        		//(if it is not a starting point)
	        		//----------------------------------------------------------

	        		if(!clusterMap.containsKey(point))     				//update the point to clusterMap           
	        		{
	        			converged = false;
	        			clusterMap.put(point, newCluster); 		

	        			//If the centroid was initialised to this point, we don't 
	        			//want to add it again. 

	        			if(centroids.get(newCluster).startingUid != point)  //update the point to newCentroid 
	        			{
	        				// System.out.println("Adding " + point + " to " + newCluster);
	        				newCentroids.get(newCluster).addPoint(point, helper);	
	        			}
	        		}

	        		//----------------------------------------------------------
	        		//The point has changed clusters. We add the
	        		//point to the new cluster and modify the centroid
	        		//for both the new cluster and the old cluster. 
	        		//Infact....Here point is already there in the clusterMap, so we check that if this mapping has been changed or not
	        		//Mapping = (point, clusterid), If this has been changed in the current iteration (point is given a new cluster as in Kmean iterations)
	        		//then we have to do two things: Update clusterMap (point, new cluster) by deleting the previous map and mapping the new one
	        		// and second, update this point in the newCentroid (Mean add this point to the new cluster as well)
	        		//----------------------------------------------------------

	        		else if(clusterMap.get(point) != newCluster)        // this is because, this while is called multiple times        
	        		{
	        			//            System.out.println("Current cluster is " + clusterMap.get(i));
	        			//            System.out.println("moving " + point + " to " + newCluster);

	        			newCentroids.get(clusterMap.get(point)).removePoint(point, helper);
	        			//              System.out.println("newCluster: " + newCluster);
	        			newCentroids.get(newCluster).addPoint(point, helper);
	        			converged = false;
	        			clusterMap.put(point, newCluster);
	        		}
	        	} //end of for, where we put all the points in some clusters

	  
	        //----------------------------------------------------------
	        //Replace centroids with newCentroids and 
	        //recompute the average for each one.
	        //In-fact....This particular cluster has been changed (means has added/deleted some points) and what we do is to update this cluster as well
	        //----------------------------------------------------------
	        
	        centroids = (ArrayList<Centroid>) newCentroids.clone();
	        
	        //TEMP: Goes through every point and finds the total distance
	        //to the centroids. If everything is working correctly, this 
	        //number should never increase. 
	        
	        double 	totalError = 0.0;
	        int 	tempCluster;
	
	        for(int i = 0; i < k; i++)					//Compute for all centroids            
	        {
	        	centroids.get(i).findAverage();				//compute average ratings in a centoid 
	        
	  //      	System.out.println("Centroid " + i);
	  //        centroids[i].printRatings();
	  //        System.out.println("New average: " + centroids[i].getAverage());
	  //        System.out.println();
	         }
	        
	        //As In the previous for, all points has been assigned to their respective clusters (depends on the distance)
	        //So we can go through these points, find their particular cluster (by clusterMap) and then can compute 
	        //the total distance from this point to that centroid.
	        //????......WE should not take into account the point for which we are taking the computing this distance
	        //          in the distance computation function? (it is because this point is already there in the centroid)
	        
	        for(int i=0; i < dataset.size(); i++)           
	        {
	            point = dataset.get(i);
	            tempCluster =  clusterMap.get(point);
	            
	           	 if(simVersion==1)
	           		totalError +=   centroids.get(tempCluster).distanceWithDefault(point, cliqueAverage, helper);
	           	 else if(simVersion==2)
	           		totalError +=   centroids.get(tempCluster).distanceWithoutDefault(point, cliqueAverage, helper);
	           	 else if(simVersion==3)
	           		totalError +=   centroids.get(tempCluster).distanceWithDefaultVS(point, cliqueAverage, helper);
	           	 else if(simVersion==4)
	           		totalError +=  centroids.get(tempCluster).distanceWithoutDefaultVS(point, cliqueAverage, helper);
	           	 else if(simVersion==5)
	           		totalError +=   centroids.get(tempCluster).distanceWithPCC(point, i, helper);   	 
	           	else if(simVersion==6)
	           		totalError +=   centroids.get(tempCluster).distanceWithVS(point, i, helper);
	           	 
	               
	       
	           	 
	        }
	        
            if(myCount >0 && myCount <8) 
	          	   System.out.println("Count = " + myCount + ", Similarity = " + totalError);
	        
	         //increment count
	           myCount++;
	           afterHowMuchSample++;
	 
	 }//end of while   
	
	    ClusterCollection clusters = new ClusterCollection(howManyClusters, helper);
	    clusterMap.forEachPair(clusters); //??????????????? (This calls the apply over-rided function in the clustercollection class)
	    
	    
	    
	    /*
	    //--------------------------------
	    // Here we can perform smoothing
	    //--------------------------------        
	    	performSmoothning();
	    */
	    
	    return clusters;
	 }


 /**********************************************************************************************/
        
        /**
         * @param int cluserId, int mid
         * @return the rating given by this cluster to the specified movie (In-fact this is the cluster avg/all users)
         */
        
        public double getRatingForAMovieInACluster (int clusterId, int mid)
        {   	
        	return centroids.get(clusterId).getRating(mid);
        	
        	
        }
        
        
 /**********************************************************************************************/
          
        	/**
             * @param int cluserId, int mid
             * @return the Average given by this cluster to the specified movie
             */
            
        
        public double getAverageForAMovieInACluster (int clusterId, int mid)
        {   	
        	return centroids.get(clusterId).getAverage();       	
        }       
            
 /**********************************************************************************************/


        /**
         * Finds the closest centroid to a specified  //more close a user is to a centroid, the more is the sim or distance
         * user. 
         *
         * @param  uid  The user to find a centroid for.
         * @param  centroids  The list of centroids. 
         * @retrun The index of the closest centroid to uid. 
         */
        
        
        //what is really he wants to find here?, max value or min value?
        
        private int findClosestCentroid(int uid, 
        								ArrayList<Centroid> centroids, 
                                        double cliqueAverage)     
        {
            double distance 	= -2;
            double min 			= -1.0;
            int minIndex 		= -1;
            double threshold 	= 0.1;
            int m				= 0;
                    
            for(int i = 0; i < howManyClusters; i++)        
            {
            	

                if(simVersion==1)
              		 distance = centroids.get(i).distanceWithDefault(uid, cliqueAverage, helper);
              	 else if(simVersion==2)
              		distance = centroids.get(i).distanceWithoutDefault(uid, cliqueAverage, helper);
              	 else if(simVersion==3)
              		 distance = centroids.get(i).distanceWithDefaultVS(uid, cliqueAverage, helper);
              	 else if(simVersion==4)
              		 distance = centroids.get(i).distanceWithoutDefaultVS(uid, cliqueAverage, helper);
              	 else if(simVersion==5)
              			 distance = centroids.get(i).distanceWithPCC(uid, i, helper);   	 
              	else if(simVersion==6)
           			 distance = centroids.get(i).distanceWithVS(uid, i, helper);
              	 
                
                /*
           //   if(Math.abs(distance) > min)  // we wanna find maximum distance        
                if((distance) > min)  // we wanna find maximum distance
                {
                    min = distance;
                    minIndex = i;
                }
                
                */
                
           /*     if (min < distance)
                {
                	min = distance;
                	minIndex =i;
                }*/
             
                   //Distance is the sim, which should be MAXIMUM for a Good cluster
                   // i.e. a new user will be assigned to the cluster with whom it got the highest sim  
                   
                   if (distance > min)
                   {
                   	min = distance;
                   	minIndex =i;
                   }            
                   
            }
            
            
              //If the similarity is less than a threshold, then craete a new centroid 
              //  if (min < threshold && afterHowMuchSample==200) 
            /*  if (min < threshold)
            						{ 
            							Centroid c1, c2;
            							c1= new Centroid (uid,helper);
            							centroids.add(c1);
            							c2 = ( new Centroid(c1));   	//assign the previously created centroid
            			                newCentroids.add(c2);
            			                m = howManyClusters++;
            			                System.out.println(" Distance < Threshold "+ distance + " < " + threshold + " Cluster found are: " + howManyClusters);
            			                minIndex = m;
            						 }*/
            
            
            
            return minIndex;
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
    
    private ArrayList<Centroid> choosePlusCentroids(IntArrayList dataset,	//no of users in the database 
    													int k		   		//how much clusters?
    										 		)     
    {

    	Random rand = new Random();
    	
    	ArrayList<Centroid> centroids = new ArrayList<Centroid>(k);
        newCentroids = new ArrayList<Centroid>(k);
        IntArrayList chosenCentroids = new IntArrayList(); 
       
         
         //_______________________________
         //code to check duplicate entries
         
         int number 							= 0;
         int myIndex 						= 0;
         IntArrayList centroidAlreadyThere	= new IntArrayList();       
         int C								= 0;
         int datasetSize 					= dataset.size();
         
         
         
         for(int i = 0; i < k; i++)         
         {        
         	while(true)        		
         	{        		
         	
         		number = rand.nextInt(datasetSize-1);
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
         
      /*  
        ArrayList<Centroid> chosenCentroids = new ArrayList<Centroid>(k);
        newCentroids = new ArrayList<Centroid>(k);        
        IntArrayList allCentroids = new IntArrayList();		    // All distinct chosen centroids              
        OpenIntIntHashMap powerUsers	 = new OpenIntIntHashMap();	// All user who seen greater than Movies_threshold movies
        
        int totalPoints			 = dataset.size();			// All users
        int C					 = 0;						// Centroid
        int previousC			 = 0;						// Previous centroid
        int possibleC			 = 0;						// A point from dataset
        int moviesThreshold		 = 50;						// power user defination
        double possibleCSim		 = 0;	 					// Sim of the point from the dataset
        int number 							= 0;
        double avgPower;
        
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
 	    
 	    /////////////////////////////////////***     trying power user avg in normal
 	    
 	   for(int j=0;j<powerUsersSize;j++) //for all points
		{
 			possibleC  	  = myPowerUsers.get(j);
 			
 		//   average rating of power user
 		//	avgPower= helper.getAverageRatingForUser(possibleC);
 			
 			avgPower= helper.getStandardDeviationForUser(possibleC);
 			  for(int i = 0; i < k; i++) 					//for total number of clusters         
 		      {
 		 		 while(true)        		
 		     	{   
 		 			 
 		      		number=(int) Normal.staticNextDouble(avgPower, 0.1);
 		      	//	number=(int) Normal.staticNextDouble(avg, 0.1);
 		      		

 		      		C=dataset.get(number);
 		     		if(!allCentroids.contains(C));
 		     		{
 		     			allCentroids.add(C);
 		     			break;
 		     		}
 		     	}
 		     	
 		 		chosenCentroids.add( new Centroid (C,helper));
 		 		this.centroids = chosenCentroids;  
 		 		  
 		      }
		}
 	
 	   */
 	   
 	    ////////////////////////////////////***
 	    
 	/*    
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
         	    int existingCentroid     = 0;
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
         				existingCentroid =  allCentroids.get(m);
 	        			
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
         */
         
     /*  // print centroids
         int totalCentroids = allCentroids.size();
         for (int i=0;i<totalCentroids;i++)
         	{
         		int uid = allCentroids.get(i);		//The user id which is
         		int movies = helper.getNumberOfMoviesSeen(uid);
         		System.out.println(" Centroid uid   " + (i+1) + "="+ uid);
         		System.out.println(" Centroid movies" + (i+1) + "="+ movies);
         		
         	}
         
                
         return chosenCentroids;
    }
    */
    

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
           	 
          	 
           	// System.out.println(" finsinhed finding sim with cluster " + i);
           	 
           	 
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
                
            //----------------------------------------------------------------
             
             public double  findSimVSBetweenACentroidAndUser (int center, int point)
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

 /*******************************************************************************************************/
/*  
    public void writeClustersIntoAFile(ArrayList<IntArrayList> myClusters)
    {
    	
    	String 			path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\Clusters\\";
    	
    	IntArrayList 	cluster;
        int K 			= myClusters.size();
        BufferedWriter  writeData[] = new BufferedWriter[K];
        BufferedWriter writeInfo=null;
        
        try {
        		writeInfo   = new BufferedWriter(new FileWriter(path + "ClusterInfo.dat", true));
			}
     
	     catch (Exception E)
	     {
	   	  System.out.println("error opening the file pointer of info");
	   	  System.exit(1);
	     }
	     	     
        int mid=0;
        int uid=0;
        double rating =0;
        
        //needed dimensions of matrix for SVD
        IntArrayList allUsersInACluster = new IntArrayList();
        IntArrayList allMoviesInACluster = new IntArrayList();
        String clusterInfo = "";
        
        //open files
        openFile(writeData,path,K);
        openFile(writeInfo,path);        
        
        //open training set
        MemHelper helper = new MemHelper
        ("C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\sml_TrainSetStored.dat"); //training set, will be used for SVD as well

                
        for(int i = 0; i < K; i++) //for all clusters 
        {  
        	cluster = myClusters.get(i);
        	           
          for(int j = 0; j < cluster.size(); j++)	//a cluster is a collection of users, go through this             
           { 
        	  uid =  cluster.get(j);           
        	  LongArrayList movies = helper.getMoviesSeenByUser(uid); //get all movies seen by this user
        	 
        	  if( !(allUsersInACluster.contains(uid)) )				 //get size of all distinct users	 
        		  allUsersInACluster.add(uid);
        	   
        	  //write this data into a file -->all movies 
        	  for (int r = 0; r < movies.size(); r++)             
              {            	
                  mid = MemHelper.parseUserOrMovie(movies.getQuick(r));
                  rating = helper.getRating(uid, mid);	
                  
                  // if (rating <1 || rating>5) System.out.println("rating =" + rating);
                  // if (i==0 && uid==43) System.out.println(uid + "," + mid + "," + rating);
                  
                  if( !(allMoviesInACluster.contains(mid)) ) 		//get size of all distinct movies
            		  allMoviesInACluster.add(mid);
                  
                  //write one sample in a file
              	try {
    	    		writeData[i].write(uid + "," + mid + "," + rating); //uid, mid, rating
    	    		writeData[i].newLine();
    	    	}
    	    	catch (Exception E)
    	         {
    	       	  System.out.println("error writing the file pointer of cluster writing");
    	       	  System.exit(1);
    	         }//end of writing
        	  
            }//end of all movues seen by a user
         }//end of all users in a clusters
          
          //Now write info in a String
          clusterInfo+= "Cluster = " + (i+1) + "," + allUsersInACluster.size() + "," + allMoviesInACluster.size();
         //  System.out.println(clusterInfo);
          clusterInfo+="\n";
          
          allUsersInACluster.clear();
          allMoviesInACluster.clear();
          
        } //end of all clusters
        
    
    	//_____________________________________________
        //Write Info in a file
        
    	try {
    		writeInfo.write(clusterInfo);
    	  }	
    	catch (Exception E)
         {
       	  System.out.println("error writing the file pointer of info");
       	  E.printStackTrace();
       	  System.exit(1);
         }//end of writing
	  
        
        //close all files
    	closeFile(writeData, K);
    	closeFile(writeInfo);
    	//_____________________________________________
    	//Now we want to write these files into memory
    	   MemReader	 myR = new MemReader();
    	   
    	   for(int i=0;i<K;i++)
    	   {
    		   myR.writeIntoDisk(path+"Cluster" + (i+1) + ".dat" , path+"StoredCluster" + (i+1) + ".dat", true);
    	   }
    	
    }

    

    //-----------------------------
    

    public void openFile(BufferedWriter writeData[], String myPath, int n)    
    {

   	 try {

   		 for(int i=0;i<n;i++)
   		   writeData[i] = new BufferedWriter(new FileWriter(myPath + "Cluster" + (i+1) + ".dat", true));
   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of cluster files");
      	  System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //--------------------------------
    
    public void openFile(BufferedWriter w,String myPath)    
    {

   	 try {

   		    w = new BufferedWriter(new FileWriter(myPath + "ClusterInfo.dat", true));
   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of info");
      	  System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //----------------------------
    

    public void closeFile(BufferedWriter writeData[], int n)    
    {
    
   	 try {
   	
   		 for(int i=0;i<n;i++)
   		 writeData[i].close();}
   	     
        catch (Exception E)
        {
      	  System.out.println("error closing the clustering file pointer");
        }
        
    }
    
    //-----------------------------
    public void closeFile(BufferedWriter writeData)    
    {
    
   	 try {
   	
   	  writeData.close();}
   	     
        catch (Exception E)
        {
      	  System.out.println("error closing the info file pointer");
        }
        
    }
    */
}

