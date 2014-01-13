package thesis;

import java.util.*;

import utilities.*;

import thesis.Centroid;
import memreader.*;
import cern.colt.list.*;
import cern.colt.map.*;

// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeansPlusAndLogPower
/************************************************************************************************/

{

	
	    private MemHelper 	helper;
	    private int 				MAX_ITERATIONS;
	    private int       			howManyClusters   	= 8;
	    int							orgClusterSize		= 0; //this will remain the fixed, no matter we are increasing the cluster for GSU	

	    private	int 				callNo;					 //call how many times this file been called
	    private int					myCount;				 //calculate the no. of iterations
	    Timer227 					timer;

	    private ArrayList<IntArrayList> 	finalClusters;
	    private OpenIntIntHashMap 			uidToCluster;
	    
	    ArrayList<Centroid> centroids;
	    ArrayList<Centroid> newCentroids;
	    OpenIntIntHashMap   clusterMap;
	    boolean 			converged;					  //Algorithm converged or not
	    int 				simVersion;
	    int					powerUserThreshold;			  //to determine the power users, we need a threshold, i.e. a user with >100 movies is a power user
	    ArrayList<Centroid> previousCentroid_dummy;
	        
	    int					wantToCallCentroidSelection;
	    IntArrayList 		allCentroids;
	    double 				simThreshold;				  //sim threshold to be used (i.e. 0.1, 0.2 etc)

	        
	    
/************************************************************************************************/

	    /**
	     * Builds the RecTree and saves the resulting clusters.
	     */
	    
	    public SimpleKMeansPlusAndLogPower(MemHelper helper)    
	    {
	        this.helper   = helper;
	        finalClusters = new ArrayList<IntArrayList>(); //Creates ArrayList with initial default capacity 10.
	        uidToCluster  = new OpenIntIntHashMap();       // <E> is for an element in the arraylist
	        clusterMap 	  = new OpenIntIntHashMap();
	        
	        myCount   = 0;
	        callNo	  = 0;
	        converged = false;
	        timer  	  = new Timer227();
	    }

	 /************************************************************************************************/ 
	 //This is called after the constructor call 
	    
	    public void cluster(int kClusters,  int call, int iterations, int sVersion, double sThreshold, int pThreshold, int callCentroidOrNot, IntArrayList allCentroids)    
	    {
	 /*       finalClusters = constructRecTree(helper.getListOfUsers(), 
	                                         0, 
	                                         helper.getGlobalAverage());
	       
	   */	
	   	
	    	howManyClusters 	=orgClusterSize =  kClusters;  
	    	MAX_ITERATIONS  	= iterations;
	    	callNo				= call;
	    	simVersion			= sVersion;
	    	simThreshold		= sThreshold;
	    	powerUserThreshold  = pThreshold;
	    	
	    	wantToCallCentroidSelection = callCentroidOrNot;
	    	this.allCentroids	=  allCentroids ; 
	    	finalClusters 	= constructRecTreeM	(helper.getListOfUsers(), 
	    										orgClusterSize, 
	    										helper.getGlobalAverage());
	    	
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

	        //Print clusters
      
	        for (int t=1; t<=howManyClusters; t++)
	        {
	        	System.out.print(finalClusters.get(t-1).size()+" ,");
	        	if(t>=40 && t%40==0)
	        	System.out.println(); 
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
	     * Gets the size of the cluster by ID
	     * @return  The size of the cluster 
	     * */
	    
	    public int getClusterSizeByID(int id)    
	    {
	        return finalClusters.size();
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
	               
	        ClusterCollection subClusters = kMeans (dataset, 	
	        										orgClusterSize, 
	        									    cliqueAverage);

	       
	       for(int i = 0; i < currDepth; i++) //for number of clusters we have (K)      
	       {
	            clusters.add(subClusters.getCluster(i));  
	            		
	        }

	        return clusters;
	    }
	    
	/************************************************************************************************/
	    
	    /**
	     * KMean: Make K clusters
	     */
	    
	    //It returns the cluster collection object
	    
	    @SuppressWarnings("unchecked")
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
	        	this.centroids = choosePlusLogPowerCentroids(dataset, orgClusterSize);   
//	        	this.centroids = chooseLogCentroids_old(dataset, orgClusterSize);
	        	
		        timer.stop();   
		        System.out.println("KMeans Plus & Log Power centroids took " + timer.getTime() + " s to select");    	
		        timer.resetTimer();
		           
	        }
	        
	          Centroid newCentroid;  
	        //Perform the clustering until the clusters converge or until
	        //we reach the maximum number of iterations.    
//	        System.out.println(" Going into ietrations..");
	        
	  //  while(!converged && myCount < MAX_ITERATIONS)        
	          while(!converged)
	          {

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

	        			  newCentroids.get(clusterMap.get(point)).removePoint(point, helper);
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

	          }//end of while   

	        ClusterCollection clusters = new ClusterCollection(howManyClusters, helper);
	        clusterMap.forEachPair(clusters); //??????????????? (This calls the apply over-rided function in the clustercollection class)
     
	        return clusters;
	    }


 /*********************************************************************************************/
       
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

            for(int i = 0; i < orgClusterSize; i++)        
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
              	                    
                   if (distance > min)
                   {
                   	min = distance;
                   	minIndex =i;
                   }            
                   
            }
                      
            return minIndex;
        }

        

/**********************************************************************************************/
 
        public double seedProb ()
        {
        	Random rand = new Random();
        	int RAND_MAX = 32767;
        	int i1;
        	int i2;        	
        	
        	i1 =  rand.nextInt(RAND_MAX-1);
        	i2 =  rand.nextInt(RAND_MAX-1);        	
        	
        	double mx = RAND_MAX;        	
        	return ((i1+ ((i2*1)/mx))/mx);
        	
        //	return rand.nextDouble();
        	
        }
        
    /**
     * Chooses k users to serve as intial centroids for 
     * the kMeansPlus algorithm.
     * Each time, we have to choose the centorid which is at the farthest distant from the current one 
     *
     * @param  dataset  The list uids. 
     * @param  k  The number of centroids (clusters) desired. 
     * @return A List of randomly chosen centroids. 
     */
    
    private ArrayList<Centroid> choosePlusLogPowerCentroids(IntArrayList dataset,	//no of users in the database 
    												int k		  			//how much clusters?
    										 		)     
    {

        ArrayList<Centroid> chosenCentroids = new ArrayList<Centroid>(k);
        
       if (wantToCallCentroidSelection==1)
       {
        newCentroids = new ArrayList<Centroid>(k);        
                 
        OpenIntIntHashMap powerUsers	 = new OpenIntIntHashMap();	    // All user who seen greater than Movies_threshold movies
        
        int totalPoints			 = dataset.size();			// All users
        int C					 = 0;						// Centroid
        int previousC			 = 0;						// Previous centroid
        int possibleC			 = 0;						// A point from dataset
        int moviesThreshold		 = powerUserThreshold;	    // power user defination
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
         		System.out.println(" movies seen = " + myPowerWeights.get(powerUsersSize-1));
         		
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
         		double bottomSum = 0;	
         		
         		OpenIntDoubleHashMap uidToCentroidSim = new OpenIntDoubleHashMap();	
          	    OpenIntDoubleHashMap uidToCentroidProb = new OpenIntDoubleHashMap(); //For KMeans prob counting
          	    int currentCentroidsSize = allCentroids.size();
          	    int existingCentroid     = 0;
           	    double closestWeight	 = 2;
           	   
           	    
         	    //------------------------------
         		// Find sim for only power users
         		//------------------------------
         		       	    
         		for(int j=0;j<powerUsersSize;j++) //for all points
         		{
         			//Get a point
         			possibleC  	  = myPowerUsers.get(j);		
         			closestWeight = 1000;
         			
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
  	        			possibleCSim =  findSimWithOtherClusters(possibleC, m);
  	        			if(simVersion<=2 || simVersion==5)					//Add one to all PCC weights
  	        				possibleCSim = possibleCSim+1;
  	        			
  	        			if(possibleCSim!=0)
 	        				possibleCSim = 1/possibleCSim;
 	        			else
 	        				possibleCSim = 1000;
         			
  	        			
  	        			if(possibleCSim < closestWeight)
  	        				closestWeight = possibleCSim;
  	        				        			
          			}         			
         			
         			// only add the distance of a point with the closest centorid
         			uidToCentroidSim.put(possibleC, closestWeight * closestWeight);
         			bottomSum +=(closestWeight * closestWeight);
         			
         		} // finsihed finding similarity b/w all users and the chosen centroid
         		

         		//-----------------------
         		// Find the next centroid
         		// Pro = D(x')^2/sum(D(x)^2)
         		//-----------------------
         		
         		IntArrayList myUsers 	 	= uidToCentroidSim.keys();
       		  	DoubleArrayList myWeights 	= uidToCentroidSim.values();
       		  	int totalUsersSize 			= myPowerUsers.size();
       		  	
       		  	
       		  	double total = 0;
         		for (int m=0;m<totalUsersSize;m++)
         		{
         			int uid 			=  myUsers.get(m);
         			double pointXWeight =  myWeights.get(m);
         			double prob 		=  pointXWeight / bottomSum;
         		
         			
         		/*	if(prob<0 || prob >=1.0001000000000007)
         				System.out.println("prob="+prob);
         			
         			total+=prob;*/
         				
         			uidToCentroidProb.put(uid,prob);		//Uid to Prob
         		}
  /*
         		if(total<0 || total >=1.0001000000000007)
         		System.out.println(total);*/
         		
         		//---------------------------------
         		//Inverse CDF
         		//---------------------------------
         		// sort prob weights in ascending order (So first element has the lowest sim)            		
         		IntArrayList    myProbUsers 	 = uidToCentroidProb.keys();
       		  	DoubleArrayList myProbWeights    = uidToCentroidProb.values();      		  	
         		            		  
//         		System.out.println(myProbWeights);
         		
         		int toalPossibleC = uidToCentroidProb.size();

         		
         		double myS		= 0;
         		double myV  	= seedProb();
         		boolean found   = false;
         		
         		for(int j=0;j<toalPossibleC;j++)
         		{
         			double p     = myProbWeights.get(j);
         				   C 	 = myProbUsers.get(j);
         			
         			myS  = myS + p;
         			
         			if(myV <=myS && allCentroids.contains(C)==false)
         			{	 
             				  allCentroids.add(C); 
             				  found = true;
             				  break;
             			  
         			} //end if
         			
         			
         			//my approach
      /*   			if(myV <= p && allCentroids.contains(C)==false)
         			{	 
             				  allCentroids.add(C); 
             				  found = true;
             				  break;
             			  
         			} //end if 
 */            			
         		} //end for
         		
         		//If we are not able to find the centroid then add any one
         		if(found ==false)
         		{
             		for (int j=0;j<toalPossibleC; j++ )
             		 {
             			  C = myProbUsers.get(j);
             			  int moviesSeenByUser = helper.getNumberOfMoviesSeen(C);
             			  
             			  if( !(allCentroids.contains(C)))
             			  {	 
             				  allCentroids.add(C);        				  			
             				  break;        					  	
             			   }//end if
             			  
             		 } //end for            		  
         		 }//end if
         		
         		  chosenCentroids.add( new Centroid (C,helper));
          		  this.centroids = chosenCentroids;
          		  
         	} //end of else
         	        	
         
         	previousC = C;
             							 
         }
       
     }
         
         //--------------------------
         if (wantToCallCentroidSelection==0)
         {
         	for(int i=0;i<allCentroids.size();i++)
         	{
         		  chosenCentroids.add( new Centroid (allCentroids.get(i),helper));             		  
         	}
         }
 
         
         return chosenCentroids; 
     
    }
    
/*******************************************************************************************************/
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
    
    private ArrayList<Centroid> chooseLogCentroids_old(IntArrayList dataset,	//no of users in the database 
    													int k		  		//how much clusters?
    										 		)     
    {
        Random rand = new Random();
        
       ArrayList<Centroid> chosenCentroids = new ArrayList<Centroid>(k);
       newCentroids = new ArrayList<Centroid>(k);  
       centroids = new ArrayList<Centroid>(k);
       IntArrayList allCentroids = new IntArrayList();				// All distinct chosen centroids              
       OpenIntIntHashMap powerUsers	 = new OpenIntIntHashMap();		// All user who seen greater than Movies_threshold movies
       
       int 		totalPoints			 = dataset.size();			// All users
       int 		C					 = 0;						// Centroid
       int 		previousC			 = 0;						// Previous centroid
       int 		possibleC			 = 0;						// A point from dataset
       int 	  	moviesThreshold		 = powerUserThreshold;		// power user defination
       double 	possibleCSim		 = 0;	 					// Sim of the point from the dataset
       int    	topPowerUserIndex	 = 0;
       int    	topPowerUserMovies 	 = 0; 
  	    //---------------------------
		// Find all power users
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
		  
	    //System.out.println("power users found = " + powerUsersSize);	    
	    	    
	    //---------------------------
		// Find top power user index,
	    // movies seen
		//---------------------------
		
	 /*   for (int i=0;i<powerUsers.size();i++)
	    {
	    	System.out.println("power user Id =" + myPowerUsers.get(i) + ", movies = " + myPowerWeights.get(i));
	    }*/
	    
	    topPowerUserIndex = myPowerUsers.get(powerUsersSize-1);
	    topPowerUserMovies = myPowerWeights.get(powerUsersSize-1);
	    //System.out.println("top power user saw movies = " + topPowerUserMovies);
	    
	    
	    //--------------------------------------------------
		// Generate clusters
		//---------------------------------------------------
			    
        for(int i = 0; i < k; i++) 					//for total number of clusters         
        {
        
        	//-----------------------------------
        	// For first loop, we find the point 
        	// at uniformly random
        	//-----------------------------------        	
        	
        	if(i==0) 
        	{
        		/*int number = rand.nextInt(powerUsersSize-1);
        	    C = myPowerUsers.get(number);
        		
        		//C = myPowerUsers.get(powerUsersSize-1);
        		allCentroids.add(C);
        		*/
        		
        		allCentroids.add(topPowerUserIndex);
        		chosenCentroids.add( new Centroid (topPowerUserIndex,helper)); 
        		
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
        	    double closestWeight	 = 0;
        	    
        	    //System.out.println(" <allCentroids>  ="+ currentCentroidsSize);
        	    //------------------------------
        		// Find sim for users
        		//------------------------------
        		       	    
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
	        			possibleCSim =  findSimVSBetweenACentroidAndUser(existingCentroid, possibleC);
	        			
	        			/*if(possibleCSim !=-2)
	        				System.out.println("sim="+possibleCSim);
        			*/
        		
	        				// only add the distance of a point with the closest centorid        			
        		  //----------------------------------
          		  // weights = D(x) + log[1/P(x) +1])
          		  //----------------------------------
        			
          		     // we already have the D(x) stored in the closest weight
        			 // px = log [(1/px) +1)], will contain minimum value if a user
        			 // is the power user
        				       
	        		  double px = 0;
	        			  
        			  double    moviesSeenByCurrentUser = helper.getNumberOfMoviesSeen(possibleC);
        			  px =  moviesSeenByCurrentUser /topPowerUserMovies;
        			
        			  px = (1/px) + 1;
        			  px = Math.log10(px);
        			        			  
        			  //if(px>2) px =2.0;
        			  //px = px-1.3;
        			 
        			 /* if(px!=0) px = Math.log10(px);
        			  else px = -1;				//for user who is even close to a power user
        			  
        			  px = px * -1;				//change the sign
        			 
        			*/
        			
	        			//double combinedWeight = alpha* possibleCSim + beta * px;
        			 
        			  double combinedWeight = 0;			  
        			  combinedWeight = possibleCSim + px;
        			  
        			 
        			 
        			 // check the closest distance now
        				if(closestWeight > combinedWeight)
	        				closestWeight = combinedWeight;
        				
        			} // finsihed finding similarity b/w all users and the chosen centroid
        			
        			uidToCentroidSim.put(possibleC, (closestWeight));
        		        			
        		} //end of all users
        		
        		//-------------------------------------------
        		// Find the next centroid
        		//-------------------------------------------
        		
        		  // sort weights in ascending order (So first element has the lowest sim)	
        		  IntArrayList myUsers 	= uidToCentroidSim.keys();
        		  DoubleArrayList myWeights = uidToCentroidSim.values();
        		  uidToCentroidSim.pairsSortedByValue(myUsers, myWeights);
        		  
        		  int totalPossibleC = uidToCentroidSim.size();    		  
        		  
        		  // As both are sorted, so it should be in the first index
        		  // Make sure, we have not already added this in the list of centroids
        		  for (int j=0, h=(totalPossibleC-1);j<=totalPossibleC; j++, h-- )
        		  {
        			  C = myUsers.get(j);	// Let check if we add max (opposite of KMeans Plus)
        			  
        			  //System.out.print(","+ myWeights.get(i));
        			  if( allCentroids.contains(C) == false)
        			  {	 
        				// Add the chosen centroid in the list of K centroids
        				  allCentroids.add(C);           				  
        				  chosenCentroids.add( new Centroid (C,helper));      				  			
        				  
        				 break;        					  	
        			   }
        			                                          
        		  } // only the last one will be added
        		  
        		   		        		
        	} //end of else
        	        	
        	//System.out.println();
        	
        
        	//centroids.add( new Centroid (C,helper));

        	 
        	
        }
              
  /*      // print centroids
        int totalCentroids = allCentroids.size();
        for (int i=0;i<totalCentroids;i++)
        	{
        		int uid = allCentroids.get(i);		//The user id which is
        		int movies = helper.getNumberOfMoviesSeen(uid);
        		System.out.println(" Centroid uid   " + (i) + "="+ uid+ ", movies="+ movies);
        		
        	}*/
        

        return chosenCentroids;
    }
    
     
    ///--------------------------------
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
     
 /*******************************************************************************************************/
//----------------------------------------------------------------
  
 public double  findSimVSBetweenACentroidAndUser (int center, int point)
 {
	    
	 int amplifyingFactor = 50;			//give more weight if users have more than 50 movies in common	 
	 double functionResult = 0.0;
	 
	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
     topSum = bottomSumActive = bottomSumTarget = 0;
     
//     double activeAvg = helper.getAverageRatingForUser(center);
//     double targetAvg = helper.getAverageRatingForUser(point);
     
     ArrayList<Pair> ratings = helper.innerJoinOnMoviesOrRating(center,point, true);
     
     // If user have no ratings in common, send -2 back
       if(ratings.size() ==0)
     	return 10;
       
       /*if(ratings.size() > 0)
    	   System.out.println("size>0");*/
     
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
 
}

