package netflix.algorithms.memorybased.GraySheepUsers;

import java.util.*;

import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

// k=4, 0.97
// k=8, 1.0
// k =1, 0.9523 (simple CF)

/************************************************************************************************/
public class SimpleKMeans extends StandardKMeans
/************************************************************************************************/

{
		private MemHelper 			helper;
		private ArrayList<Centroid> centroids;
		private ArrayList<Centroid> newCentroids;
		private OpenIntIntHashMap   clusterMap;
		private boolean 			converged;					  //Algorithm converged or not
		private int 				simVersion;
	    
/************************************************************************************************/

	    /**
	     * Builds the RecTree and saves the resulting clusters.
	     */
	    
	    public SimpleKMeans(MemHelper helper)    
	    {
	    	super(helper);
	        this.helper   = helper;
	        timer  	      = new Timer227();
	    }

	 
	@Override
	public ArrayList<Centroid> chooseCentroids(IntArrayList dataset, int k) {
		Random rand = new Random();
        
        ArrayList<Centroid> centroids = new ArrayList<Centroid>(k);
        newCentroids = new ArrayList<Centroid>(k);
        IntArrayList chosenCentroids = new IntArrayList(); 
              
         //_______________________________
         //code to check duplicate entries
         
         int number 							= 0;
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
    
 
}

