package netflix.backupclasses;

import java.io.BufferedWriter;


import java.io.FileWriter;
import java.util.*;


import netflix.DataSets.SimpleKMeanPlusAndPower;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.recommender.ItemItemRecommender;
import netflix.rmse.RMSECalculator;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.utilities.*;

/************************************************************************************************/
public class SimpleKMeanRecNF
{
    // initialize class here 
	private MyRecTree 						mixedTree;
	private SimpleKMean						simpleKTree;
	private SimpleKMeanPlus					simpleKPlusTree;
	private SimpleKMeanModifiedPlus			simpleKModifiedPlusTree;
	private SimpleKMeanPlusAndPower			simpleKPlusAndPowerTree;
	private SimpleKMeanPlusAndLogPower		simpleKPlusAndLogPowerTree;
	private SimpleKMeanPlusAndLogPower		simpleKPlusAndLogPowerTree_NoSimThr;
	private SimpleKMeanDistanceFromMean		simpleKDistane;

	private double 							alpha;					// coff for log and power 				
	private double 							beta; 
	private int								myClasses;
	private int								myTotalFolds;
	private int								myTotalFoldsForGSU;	 //if no gus is found for a fold, then do not include that fold in the prediction MAE etc of gsu
		
	//Objects of some classes
    MemHelper 			trainMMh;
    MemHelper 			allHelper;
    MemHelper 			testMMh;
    MeanOrSD			MEANORSD;
    Timer227 			timer;
    FilterAndWeight		myUserBasedFilter;   //Filter and Weight, for User-based CF
    ItemItemRecommender	myItemRec;		     //item-based CF    
    
    private int 		totalNonRecSamples;	 //Total number of sample for which we did not recommend anything
    private int 		totalRecSamples;
    private int 		howMuchClusterSize;
    private double 		threshold = 0.1;
    private long 		kMeanTime;
    private int         kClusters;
    BufferedWriter      writeData1;
    BufferedWriter      writeData2;
    
    private String      myPath;
    private String      SVDPath;
    private int         totalNan=0;
    private int         totalNegatives=0;
    private int			KMeansOrKMeansPlus; 
    private int			simVersion;
    
    //Related to finding the gray sheep user's predictions
    private int			graySheepUsers;							// total gray sheep users
    private int			graySheepSamples;						// total gray sheep predictions
    private int			isGraySheepCluserOrAllClusters;			// wanna find predictions for g.s.u.'s custers or all clusters 
    private int			powerUsersThreshold;					// power user size
    private double		simThreshold;							//
    private int			numberOfneighbours;						// no. of neighbouring cluster for an active user
   
    //Answered
    private int 		totalPerfectAnswers;
    private int 		totalAnswers;
    private int			totalIterations;						//no. of iterations required in the KMeans clustering 
    
    //Regarding Results
    double 								MAE;
    double								MAEPerUser;
    double 								RMSE;
    double 								RMSEPerUser;
    double								Roc;
    double								coverage;
    double								pValue;
    double								kMeanEigen_Nmae;
	double								kMeanCluster_Nmae;
    
    //SD in one fold or when we do hold-out like 20-80
    double								SDInMAE;
    double								SDInROC;
	double 								SDInTopN_Precision[];
	double 								SDInTopN_Recall[];
	double 								SDInTopN_F1[];	
	
    double            					precision[];		//evaluations   
    double              				recall[];   
    double              				F1[];    
    private OpenIntDoubleHashMap 		midToPredictions;	//will be used for top_n metrics (pred*100, actual)
    
    //1: fold, 2: k, 3:dim
    double              array_MAE[][];	      			// [gsu][fold]
    double              array_MAEPerUser[][];
    double              array_NMAE[][];
    double              array_NMAEPerUser[][];
    double              array_RMSE[][];
    double              array_RMSEPerUser[][];
    double              array_Coverage[][];
    double              array_ROC[][];
    double              array_BuildTime[][];
    double				array_GSUSamples[][];			//gs users found in [gsu][all folds]
    double				array_GSU[][];					//gs samples found in [gsu][all folds]
    
    
    double              array_Precision[][][]; 		   //[topnN][gsu][fold]
    double              array_Recall[][][];
    double              array_F1[][][];    
    
    //will store the grid results in the form of mean and sd
    double				gridResults_Mean_MAE[];
    double				gridResults_Mean_MAEPerUser[];
    double				gridResults_Mean_NMAE[];
    double				gridResults_Mean_NMAEPerUser[];
    double				gridResults_Mean_RMSE[];
    double				gridResults_Mean_RMSEPerUser[];
    double				gridResults_Mean_ROC[];
    double				gridResults_Mean_GSU[];
    double				gridResults_Mean_GSUSamples[];		//GSU and Samples			
    
    double				gridResults_Mean_Precision[][];   	//[TOPn][][]
    double				gridResults_Mean_Recall[][];
    double				gridResults_Mean_F1[][];
    double				gridResults_Mean_Coverage[];
    
    double				gridResults_Sd_MAE[];
    double				gridResults_Sd_MAEPerUser[];
    double				gridResults_Sd_NMAE[];
    double				gridResults_Sd_NMAEPerUser[];
    double				gridResults_Sd_RMSE[];
    double				gridResults_Sd_RMSEPerUser[];
    double				gridResults_Sd_ROC[];
    double				gridResults_sd_GSU[];
    double				gridResults_sd_GSUSamples[];		//GSU and Samples			
   
    double				gridResults_Sd_Precision[][];
    double				gridResults_Sd_Recall[][];
    double				gridResults_Sd_F1[][];
    double				gridResults_Sd_Coverage[];
    
    double              mean_MAE[];	      					// Means of results, got from diff folds
    double              mean_MAEPerUser[];
    double              mean_NMAE[];						// for each version
    double              mean_NMAEPerUser[];
    double              mean_RMSE[];
    double              mean_RMSEPerUser[];
    double              mean_Coverage[];
    double              mean_ROC[];
    double              mean_BuildTime[];
    double              mean_Precision[][];   
    double              mean_Recall[][];   
    double              mean_F1[][];       
    
    double              sd_MAE[];		      					// SD of results, got from diff folds
    double              sd_MAEPerUser[];
    double              sd_NMAE[];								// for each version
    double              sd_NMAEPerUser[];
    double              sd_RMSE[];
    double              sd_RMSEPerUser[];
    double              sd_Coverage[];
    double              sd_ROC[];
    double              sd_BuildTime[];
    double              sd_Precision[][];   
    double              sd_Recall[][];   
    double              sd_F1[][];   
        
    
    int 				myFlg;
    int 				currentFold;
    IntArrayList		myCentroids1, myCentroids2,myCentroids3,myCentroids4,myCentroids5;
    
    
/************************************************************************************************/
    
    public SimpleKMeanRecNF()    
    {
       
    	 totalNonRecSamples = 0;
    	 totalRecSamples 	= 0;
    	 howMuchClusterSize = 0;
    	 kMeanTime			= 0;    
    	 alpha 				= 0.0; // start from 0.0
    	 beta 				= 1.0; //start from 1.0
    	 myClasses			= 5;
    	 simVersion			= 1;  //1=PCCwithDefault, 2=PCCwithoutDefault
    	 						  //3=VSWithDefault,  4=VSWithDefault
    	 						  //5=PCC, 			  6=VS
    	 
    	 graySheepUsers   	 = 0;
    	 graySheepSamples 	 = 0;
    	 KMeansOrKMeansPlus  = 0;
    	 
    	 timer 				 = new Timer227();
    	 MEANORSD			 = new MeanOrSD();
         


         //-------------------------------------------------------
         //Answers
	        totalPerfectAnswers = 0;
	        totalAnswers 	    = 0;
	        numberOfneighbours  = 0;
	        totalIterations		= 0;
	         
         	MAE 				= 0;
	    	MAEPerUser			= 0;
	    	RMSE 				= 0;
			RMSEPerUser 		= 0;
	    	kMeanEigen_Nmae		= 0;
	    	kMeanCluster_Nmae	= 0;
	    	Roc 				= 0;
	    	coverage			= 0;
	    	pValue				= 0;
	    	SDInMAE				= 0;
	    	SDInROC				= 0;
	    	SDInTopN_Precision	= new double[8];
	    	SDInTopN_Recall		= new double[8];
	    	SDInTopN_F1			= new double[8];
	
	    	midToPredictions    = new OpenIntDoubleHashMap();     	  
	        precision    		= new double[8];		//topN; for six values of N (top5, 10, 15...30)
	    	recall  			= new double[8];		// Most probably we wil use top10, or top20
	    	F1					= new double[8];
	    	
	        //Initialize results, Mean and SD	    	
	    	 array_MAE  	 	=   new double[3][5];
	    	 array_MAEPerUser	=   new double[3][5]; 
	    	 array_NMAE		 	=   new double[3][5];
	    	 array_NMAEPerUser	=   new double[3][5];
	    	 array_RMSE 	 	=   new double[3][5];
	    	 array_RMSEPerUser 	=   new double[3][5];
	         array_Coverage  	=   new double[3][5];
	         array_ROC 		 	=   new double[3][5];
	         array_BuildTime 	=   new double[3][5];
	         array_GSU  	 	=   new double[3][5];
	         array_GSUSamples 	=   new double[3][5];
	         
	         array_Precision 	= new double[8][3][5]; //[topN][fold]
	         array_Recall 	 	= new double[8][3][5];
	         array_F1 		 	= new double[8][3][5];
	         	         
	         //So we have to print this grid result for each scheme,
	         //Print in the form of "mean + sd &" 
	         gridResults_Mean_MAE 			=   new double[3];	        
	         gridResults_Mean_NMAE			=   new double[3];        
	         gridResults_Mean_RMSE			=   new double[3];
	         gridResults_Mean_MAEPerUser	=   new double[3];
	         gridResults_Mean_RMSEPerUser	=   new double[3];
	         gridResults_Mean_NMAEPerUser	=   new double[3];
	         gridResults_Mean_ROC			=   new double[3];
	         gridResults_Mean_Coverage		=   new double[3];
	         gridResults_Mean_GSU			=   new double[3];
	         gridResults_Mean_GSUSamples	=   new double[3];
	         
	         gridResults_Mean_Precision		= new double[8][3]; 
	         gridResults_Mean_Recall		= new double[8][3];
	         gridResults_Mean_F1			= new double[8][3];       
	         	         
	         gridResults_Sd_MAE			= new double[3];	         
	         gridResults_Sd_NMAE		= new double[3];	         
	         gridResults_Sd_RMSE		= new double[3];
	         gridResults_Sd_NMAEPerUser	= new double[3];
	         gridResults_Sd_MAEPerUser	= new double[3];
	         gridResults_Sd_RMSEPerUser = new double[3];	         
	         gridResults_Sd_ROC			= new double[3];
	         gridResults_Sd_Coverage	= new double[3];
	         gridResults_sd_GSU			=   new double[3];
	         gridResults_sd_GSUSamples	=   new double[3];
	         
	         
	         gridResults_Sd_Precision	= new double[8][3];
	         gridResults_Sd_Recall		= new double[8][3];
	         gridResults_Sd_F1			= new double[8][3];
	         
	        // mean and sd, may be not required
	        mean_MAE 		= new double[3];	        
	        mean_NMAE 		= new double[3];	        
	        mean_RMSE 		= new double[3];
	        
	        mean_NMAEPerUser= new double[3];
	        mean_RMSEPerUser= new double[3];
	        mean_MAEPerUser = new double[3];
	        
	        mean_Coverage 	= new double[3];
	        mean_ROC 		= new double[3];
	        mean_BuildTime  = new double[5];
	        mean_Precision	= new double[8][3];
	        mean_Recall		= new double[8][3];
	        mean_F1			= new double[8][3];	        
	        
	        sd_MAE 			= new double[3];	        
	        sd_NMAE 		= new double[3];
	        sd_RMSE 		= new double[3];
	        
	        sd_MAEPerUser	= new double[3];
	        sd_NMAEPerUser 	= new double[3];
	        sd_RMSEPerUser	= new double[3];
	        
	        
	        sd_Coverage 	= new double[3];
	        sd_ROC 			= new double[3];
	        sd_BuildTime 	= new double[5];
	        sd_Precision 	= new double[8][3];
	        sd_Recall 		= new double[8][3];
	        sd_F1		 	= new double[8][3];       
	    			
	        myCentroids1   = new IntArrayList();
	        myCentroids2   = new IntArrayList();
	        myCentroids3   = new IntArrayList();
	        myCentroids4   = new IntArrayList();
	        myCentroids5   = new IntArrayList();
	        
	        
	    	
    }

/************************************************************************************************/

/**
 *  It initialise an object and call the method for building the three 
 */
    public void callKTree(int callNo, int MAX_ITERATIONS )     
    {
    	//5,3,3,3,3; KMeans was like 1.13 and remaining 1.08 RMSE
    	// 5,5,5,5,5; the diff is like 1.11 and 1.06
        //-----------------------
    	// K-Means
    	//-----------------------
    	
    	
    	if(KMeansOrKMeansPlus==1)
    	{
	    	timer.start();	              
	        simpleKTree.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	        //System.gc();
    	}
    	
        //-----------------------
    	// K-Means Plus
    	//-----------------------    	
        
    	
    	else if(KMeansOrKMeansPlus==2)
    	{
	        timer.start();          
	        simpleKPlusTree.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }
        

    	//-----------------------
    	// K-Means Modified Plus
    	//-----------------------    	
    	//change : Vs and Prob as in KMenas++ paper
        
    	else if(KMeansOrKMeansPlus==3)
    	{
    		myFlg =1;
	        timer.start(); 
	        IntArrayList allCentroids     = new IntArrayList();		    // All distinct chosen centroids 
	        simpleKModifiedPlusTree = new SimpleKMeanModifiedPlus(trainMMh);  
	        simpleKModifiedPlusTree.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion,allCentroids);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Modified Plus Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    

  
    	//-----------------------
    	// K-Means Plus and Power
    	//-----------------------    	
	    	
    	else if(KMeansOrKMeansPlus==4)
    	{
	        timer.start();           
	        simpleKPlusAndPowerTree.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Power Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    	
    	    	
      	//-----------------------
    	// K-Means Plus and 
    	// Log Power
    	//-----------------------    	
        
    //	else if(KMeansOrKMeansPlus>=5)
    	else if(KMeansOrKMeansPlus==5)
    	{
    		myFlg =1;
    		
	                 
	        IntArrayList allCentroids     = new IntArrayList();		    // All distinct chosen centroids  
	        timer.start(); 
	    	simpleKPlusAndLogPowerTree.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion, simThreshold, powerUsersThreshold,1, allCentroids);
	    //	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,1, allCentroids);
	    	
	    
		    	    
	    if(currentFold==1)
					myCentroids1   = 	simpleKPlusAndLogPowerTree.get_previous_Centroids();
		else if(currentFold==2)
					myCentroids2   = 	simpleKPlusAndLogPowerTree.get_previous_Centroids();
		else if(currentFold==3)
					myCentroids3   = 	simpleKPlusAndLogPowerTree.get_previous_Centroids();
		else if(currentFold==4)
					myCentroids4   = 	simpleKPlusAndLogPowerTree.get_previous_Centroids();
		else if(currentFold==5)
					myCentroids5   = 	simpleKPlusAndLogPowerTree.get_previous_Centroids();
			    
		    if(currentFold==1)
		    	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, 1, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,0,  myCentroids1);
		    else if(currentFold==2)
		    	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, 1, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,0,  myCentroids2);
		    else if(currentFold==3)
		    	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, 1, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,0,  myCentroids3);
		    else if(currentFold==4)
		    	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, 1, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,0,  myCentroids4);
		    else if(currentFold==5)
		    	simpleKPlusAndLogPowerTree_NoSimThr.cluster(kClusters, 1, MAX_ITERATIONS, simVersion, -10, powerUsersThreshold,0,  myCentroids5);
		    
		    timer.stop();
	    						
	   //     kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Log Power Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	        }
	       
    	else if(KMeansOrKMeansPlus==9)
    	{
	        timer.start();          
	        simpleKDistane.cluster(kClusters, callNo, MAX_ITERATIONS, simVersion);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Distance Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    } 
    	    	
    }   
        
    /**
     * Basic recommendation method for memory-based algorithms.
     * 
     * @param user
     * @param movie
     * @return the predicted rating, or -99 if it fails (mh error)
     */
 
 //We call it for active user and a target movie
    public double recommend(int activeUser, int targetMovie, int neighbours)    
    {
        double currWeight, weightSum = 0, voteSum = 0;
        int uid; 
        double  neighRating=0;
        IntArrayList simpleKUsers =null; 
        int limit = 50;
        
     // variable for priors, and sim * priors
	     double priors[] = new double[5];
	     double priorsMultipliedBySim[] = new double[5];
        
	     //Active User's class prior
	     double activeUserPriors[] = new double[5];
	     LongArrayList movies = trainMMh.getMoviesSeenByUser(activeUser);         
         int moviesSize = movies.size();
         for (int i=0;i<moviesSize;i++)
         {                	
         	  int mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
         	  double rating = trainMMh.getRating(activeUser, mid);
         	  int index = (int) rating;
         	 // activeUserPriors[index-1]++;
          	         	
         }

	   
	   //------------------------
	   //  neighbours priors
	   //------------------------
	     if (KMeansOrKMeansPlus == 5)        	
	     {
   		simpleKUsers = simpleKPlusAndLogPowerTree_NoSimThr.getClusterByUID(activeUser);           		
   		int activeClusterID = simpleKPlusAndLogPowerTree_NoSimThr.getClusterIDByUID(activeUser);

   		//find how to proceed ahead    	
   	  
   	  {    		       		    
	    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
	    		
	    		// Find sim b/w a user and the cluster he lies in        		
	    		double simWithMainCluster = simpleKPlusAndLogPowerTree_NoSimThr.findSimWithOtherClusters(activeUser, activeClusterID );
	    		
	    		// Find sim b/w a user and all the other clusters
	    		for(int i=0;i<kClusters; i++)
	    		{
	    			if(i!=activeClusterID)
	    			{
	    				double activeUserSim  = simpleKPlusAndLogPowerTree_NoSimThr.findSimWithOtherClusters(activeUser, i );
	    				
	    				if(activeUserSim!=0)					//zero must not be there?, as some negative are there (not sure)
	    					simMap.put(i,activeUserSim );      					
	    			} 
	    			
	    		} //end for
	    		
	    		// Put the mainCluster sim as well
	    		simMap.put(activeClusterID,simWithMainCluster );
	    		
	    		//sort the pairs (ascending order)
	    		IntArrayList keys = simMap.keys();
	    		DoubleArrayList vals = simMap.values();        		
	    	    simMap.pairsSortedByValue(keys, vals);        		
	    		int simSize = simMap.size();
	    		LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
	    		IntArrayList  allUsers  = new IntArrayList();
	    		
	    		//System.out.println(" all users who saw movies ="+ tempUsers.size());
	    		for(int i=0;i<tempUsers.size();i++)
	    		{
	    			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
	    			//System.out.println("Actual Uids="+allUsers.get(i));
	    		}       		
	    		//-----------------------------------
	    		// Find sim * priors
	    		//-----------------------------------
	    		// How much similar clusters to take into account? 
	    		// Let us take upto a certain sim into account, e.g. (>0.10) sim
	
	    		int total=0;
	    		for (int i=simSize-1;i>=0;i--)
	    		{
	    			//Get a cluster id
	    			int clusterId = keys.get(i);
	    			
	    			//Get currentCluster weight with the active user
	    			double clusterWeight = vals.get(i);
	    			
					//Get rating, average given by a cluster
					double clusterRating  = simpleKPlusAndLogPowerTree_NoSimThr.getRatingForAMovieInACluster(clusterId, targetMovie);
					double clusterAverage = simpleKPlusAndLogPowerTree_NoSimThr.getAverageForAMovieInACluster(clusterId, targetMovie);
					
					if(clusterRating!=0)
					{
						//Prediction
			       		weightSum += Math.abs(clusterWeight);      		
			           	voteSum+= (clusterWeight*(clusterRating-clusterAverage));
			           	
			           	if(total++ == numberOfneighbours) break;
					}
	    		 }
		            if (weightSum!=0)
		 	    	   voteSum *= 1.0 / weightSum;        
		         
		 	       
		            //CBF
		            double avgRat   = trainMMh.getAverageRatingForMovie(targetMovie);
		            double prob_rat = trainMMh.getNumberOfMoviesSeen(activeUser)/150.0;
		            double prob_sim = vals.get(simSize-1);		            
		            double alpha    = Math.min(1, Math.max(prob_sim+prob_rat, 0));
		            
		            double finalRat = (1-alpha)* avgRat ;  
		            
		 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
		 	       { 
		 	    	   	  return finalRat;
		 	         	 
		 	       	 //This is just for learning the training parameters
					  // return trainMMh.getAverageRatingForUser(activeUser);
		 	       }
		 	       	       
		 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;
		 	       finalRat =  ((1-alpha)* avgRat + alpha * answer);
		        // System.gc(); // It slows down the system to a great extent
	
		         //------------------------
		         // Send answer back
		         //------------------------          
		       
		          if(answer<=0)
		          {	
			       	 totalNegatives++;			         	  
			       //	 return 0;
			         	 
			       // This is just for learning the training parameters
			       // return trainMMh.getAverageRatingForUser(activeUser);
			       	  return finalRat;
		          }
		          
		          else {
		         	 totalRecSamples++;   
		         	 //return answer;
		         	 return finalRat;
		          }
   	   }//end if checking the size of the active cluster
	  	  	  
   	}
   
	   //---------------------------------------------
	   // Simple using CF--User-based or Item-based
	   //---------------------------------------------	   
 
	   
	   else if (KMeansOrKMeansPlus==6)
	   {	   
		   //first go in these programs and return "0" or at-least the averages if they fail to predict
		   double rat = myUserBasedFilter.recommendS(activeUser, targetMovie, 30, 1);
		   return rat;
		   
	   }	   
	   
	   else if (KMeansOrKMeansPlus==7)
	   {	   
		   double rat = myItemRec.recommend(trainMMh, activeUser, targetMovie, 15, 4);	   
		   return rat;
		   
	   }
	   
	   
	   else if (KMeansOrKMeansPlus==8)
	   {	   
					double		rat = (trainMMh.getAverageRatingForMovie(targetMovie) + 
    									 trainMMh.getAverageRatingForUser(activeUser))/2.0;
    			
					return rat;
	   }
	///////
	   
	   else if (KMeansOrKMeansPlus==9)
	   {	   
					double		rat = (trainMMh.getAverageRatingForMovie(targetMovie) + 
    									 trainMMh.getAverageRatingForUser(activeUser))/2.0;
    			
					return rat;
	   }
	   
	   return 0;
        
    }

/************************************************************************************************/
    
  public static void main(String[] args)    
  {
    	
	      String path ="";
	      int    fold = 1;
	    
	      path = "C:/Users/Sobia/tempRecommender/GitHubRecommender/netflix/netflix/DataSets/SML_ML/FiveFoldData/";

		    //create class object
		   SimpleKMeanRecNF rec = new SimpleKMeanRecNF(); 
		    
		    //Compute the resuts
		   rec.computeResults(path);	    
  }
  

 /************************************************************************************************/

  /**
   * Compute results over five fold and write them into a file
   * We are curently using Log and Power version of Clustering. 
   */
  
  public void computeResults(String path)
  {   
	   myPath = path;
	
	   //optimal clusters, (1) sml =150, (2) ft1= 100, ft5 = 140
	   
	   myTotalFolds = 1;   
	   int START    = 0;  // 0=gsu, 1=remaining users, 3=all users
	   int pThr = 30;
	   int k=90;
	   powerUsersThreshold = pThr;
	   myTotalFoldsForGSU  = 0;
	   kClusters  = k;
	   simVersion = 2;
	   openFile();

	   System.out.println("==========================================================================");
	   System.out.println(" Clusters = "+ k);
	   System.out.println("==========================================================================");
	 	  	   
	 	  
	 				 
		   KMeansOrKMeansPlus = 5;		 	 					 
		   int noNeigh = 30;
		   numberOfneighbours = noNeigh;
								  
			//Kepping everthing fixed, I have to change this manulally and check how it evolves (starts from 1 to 10), keeping the 
			//other parameters fixed (to the optimal ones)
			for(int noItr=2;noItr<=2;noItr++)
			{
				totalIterations = noItr; 
				
				for(int fold=1, foldForGSU=1 ;fold<=myTotalFolds;fold++)
				{ 	
			
					
				    String  trainFile  = path  +  "sml_trainSetStoredFold" + (fold)+ ".dat";
					String  testFile	= path  +  "sml_testSetStoredFold" + (fold)+ ".dat";
					String  mainFile	= trainFile;
					
					 allHelper = new MemHelper(mainFile);
					 trainMMh  = new MemHelper(trainFile);
					 testMMh 	= new MemHelper(testFile);	  
					  
					  //User based Filter setting
			          myUserBasedFilter = new FilterAndWeight(trainMMh,1); 		       //with mmh object
			  				 	      
			          //ibcf
			          myItemRec = new ItemItemRecommender(true, 5);
			           
					  long t1= System.currentTimeMillis();
					  
					   //Make the objects and keep them fixed throughout the program
						for (int v=5;v<=5;v++)
						{	  
							if(v==1) 	         
							        simpleKTree = new SimpleKMean(trainMMh);	
						 	else if(v==2)             
							        simpleKPlusTree = new SimpleKMeanPlus(trainMMh);          
						 	else if(v==3) 	         
						 			simpleKModifiedPlusTree = new SimpleKMeanModifiedPlus(trainMMh);
							else if(v==4) 		           
							        simpleKPlusAndPowerTree = new SimpleKMeanPlusAndPower(trainMMh); 
							else if(v==5)  
							{    				
							//alpha beta extraaaa .. remove them
								simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(trainMMh, alpha, beta);
								simpleKPlusAndLogPowerTree_NoSimThr = new SimpleKMeanPlusAndLogPower(trainMMh, alpha, beta);
							}
							else//because, we are using it for all
								simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(trainMMh, alpha, beta);
							
						}
							  
				  		 System.out.println("done reading objects");				   	  
						 System.out.println("=====================");
						 System.out.println(" Fold="+ fold);	 	
						 System.out.println("=====================");
	
						 //Build clusters
					      callKTree (myFlg , noItr);										//it is converging after 6-7 iterations	
					 //     testWithMemHelper(testMMh,10);					  
				      }//end of number of iterations
				  } //end fold					 
				 
				   
							timer.resetTimer(); 
					     	myTotalFoldsForGSU = 0;						//reset fold				
					     	
				  		
			


 		closeFile();
 	
    }
  
  
/***************************************************************************************************/  
 /**
   * We wanna build SVM Regression modelm, It will make predictions and will store them in the files (.dat)
   * We can read those files and can get predictions.   
   *
   */
  
  // But It is five fold data, means if we have separate test and train files then we have to build model SVM REGRESSION for each 
  // train set .........? think
  
  // NICE thing is build a sperate class or may be one class for all the classifiers (e.g. KNN, naive bayes) embeede in it,....and 
  // tey take a input test, train object, active user, target movie.. and return the prediction for it.
  
  
  public void buildSVMRegModel()
  {
	  for(int fold=1;fold<=myTotalFolds;fold++)
	   {
		  
		  //SML

		  String  trainFile = myPath +  "sml_trainSetStoredFold" + (fold)+ ".dat";
		  String  testFile	= myPath +  "sml_testSetStoredFold" + (fold)+ ".dat";
		  					  
		   String  mainFile	= trainFile;
		  
		  allHelper = new MemHelper(mainFile);
		  trainMMh  = new MemHelper(trainFile);
		  testMMh 	= new MemHelper(testFile);	  		
		
		  
	   }//end for	  
  }//end method
   

  
/***************************************************************************************************/
  
  
 //-----------------------------
    

    public void openFile()    
    {

   	 try {
        
   	       
   	   //// changed
   	     writeData1 = new BufferedWriter(new FileWriter(myPath + "new.csv", true));   			
   		   writeData2 = new BufferedWriter(new FileWriter(myPath + "new2.csv", true));	
   	       System.out.println("Rec File Created at"+ "new");
   	 											  
   	 }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	  System.exit(1);
        }
        
        
    }
    
    //----------------------------
    

    public void closeFile()    
    {
    
   	 try {
   		 	writeData1.close();
   		 	writeData2.close();
   		 	System.out.println("Files closed");
   		  }
   	     
        catch (Exception E)
        {
      	  System.out.println("error closing the roc file pointer");
        }
        
        
    }

    
    //---------------------------------------
    
   
}//end class