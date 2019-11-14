package com.vitimage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm.MinimumCostFlow;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class TestDijsktra {

	public TestDijsktra() {
		// TODO Auto-generated constructor stub
	}



	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		//		runTestLinkedList();
//		runTestArrayList();
		runTestMaxFlow();
		//prepareImages();
		//		runTestGraph();
		//runTestSimpleLinkedList();
		VitimageUtils.waitFor(1000000);
	}

	
	
	
	public class FunctionCapacities{
		
	}
	
	
	public static void runTestMaxFlow() {
		//////////////////////////////////VARIABLES/////////////////
		//Useful declarations
		System.out.println("Declarations");
		Graph<Integer,DefaultWeightedEdge>graph=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		long t0=System.currentTimeMillis();
		int[][]coordsTarget=new int[][] {{ 4 , 46 , 35 } , { 13 , 49 , 35 } , { 16 , 41 , 35 } , { 10 , 37 , 35 } , { 3 , 39 , 35 } };
//		int[][]coordsTarget=new int[][] {{ 4 , 46 , 35 } };
		int nUnitsOfFlow=coordsTarget.length;
		int zSource=120;
		DefaultWeightedEdge arcRetour;
		long t1;
		int source=0;
		int N=0;
		int incr=0;
		int ind=0;
		int label=0;
		int label2=0;
		float val;
		final int INPUT=100000000;
		final int OUTPUT=200000000;
		final int SOURCE=0;
		final int COLLECTOR=1;
		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		float alpha=0;
		float beta=0;
		float gamma=0;
		float delta=1;
		float sqr2=(float)Math.sqrt(2);
		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Local useful variables
		ImagePlus imgMri=getImageTest(2);
		VitimageUtils.printImageResume(imgMri,"imgMri");
		ImagePlus imgSeeds=getImageTest(1);
		VitimageUtils.printImageResume(imgSeeds,"imgSeeds");
		ImagePlus imgObject=getImageTest(3);
		VitimageUtils.printImageResume(imgObject,"imgObject");
		ImagePlus imgLabels=getImageTest(0);
		VitimageUtils.printImageResume(imgLabels,"imgLabels");
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);

		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each object voxel, from 0 to N and set N
		System.out.println("Declarations ok. Variables");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
		}
		for(int z=0;z<Z && z<zSource+1;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if( (valsObject[z][y*X+x] & 0xff) > 0 && z>0)valsLabels[z][y*X+x]=incr++;
				else valsLabels[z][y*X+x]=-1;
			}
		}
		N=incr;
		System.out.println("N="+N+" among "+(X*Y*Z)+" voxels");
		if(N>=INPUT) {
			System.out.println("Wrong dimensions : vertex number superior to maximal possible vertices with architecture. Abort.");System.exit(0);
		}
		
		//Build data structures
		int[][]coordinates=new int[N][3];
		float[]mriValue=new float[N];
		int []isSource=new int[N];
		int []isTarget=new int[N];
		
		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		graph.addVertex(SOURCE);
		graph.addVertex(COLLECTOR);
		coordinates[0]=new int[] {1,1,120};
		System.out.println("ok. Prepa");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z && z<zSource+1;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1
				if(valsLabels[z][y*X+x]<0)continue;
				label=(int)Math.round(valsLabels[z][y*X+x]);
				graph.addVertex(label+INPUT);
				graph.addVertex(label+OUTPUT);

				//Coordinates gets the x,y,z coordinates of each object point
				coordinates[label]=new int[] {x,y,z};

				//mriValue gets the value seen in mri
				mriValue[label]=(int)(valsMri[z][y*X+x]  & 0xff);
				if( ( z==zSource) ) {
					isSource[label]=1;
				}
				for(int d=0;d<coordsTarget.length;d++)if(z==coordsTarget[d][2] && y==coordsTarget[d][1] && x==coordsTarget[d][0])isTarget[label]=1;
			}
		}
		System.out.println("ok. Prepa2");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z && z<zSource+1;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				label=(int)Math.round(valsLabels[z][y*X+x]);
				if(label<0)continue;
				if(isSource[label]==1) {
					Graphs.addEdge(graph, SOURCE,label+INPUT, 0);					
				}
				if(isTarget[label]==1) {
					Graphs.addEdge(graph, label+OUTPUT,COLLECTOR,0);
				}
			
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					//System.out.println("Z="+z+"  X="+x+" Y="+y);
					label2=(int)Math.round(valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)]);
					if(label2<0)continue;
					if(label==label2)continue;
					val=alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[di*9+dj*3+dk]+delta*distancesToNeighbours[di*9+dj*3+dk]*(510-mriValue[label]-mriValue[label2]);
					
					if(isSource[label2]==0) {
						Graphs.addEdge(graph, label+OUTPUT, label2+INPUT,val);
					}
				}
				Graphs.addEdge(graph,label+INPUT,label+OUTPUT,0);
			}
		}		
		arcRetour=Graphs.addEdge(graph, COLLECTOR,SOURCE, 0);					
		
		
		System.out.println("Run");
		
		//Execute Dijkstra externally
		CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> minCostFlow=new CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge>();
		Function<DefaultWeightedEdge,Integer>minCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcRetour ? nUnitsOfFlow : 0);};
		Function<DefaultWeightedEdge,Integer>maxCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcRetour ? nUnitsOfFlow : 1);};
		Function<Integer,Integer>supplyMap=(Integer i)-> {return 0;};
		MinimumCostFlowProblem<Integer, DefaultWeightedEdge> minimumCostFlowProblem=new MinimumCostFlowProblem.MinimumCostFlowProblemImpl<Integer, DefaultWeightedEdge>(graph, supplyMap, maxCapacities,minCapacities);
		System.out.println("Start");
		MinimumCostFlow<DefaultWeightedEdge> result = minCostFlow.getMinimumCostFlow(minimumCostFlowProblem);
		System.out.println("End");
		int xx,yy,zz,x0,y0,z0;
		
		//Building fibers from the flows
		ArrayList<ArrayList<int[]> > list=new ArrayList<>();
		for(int start=0;start<coordsTarget.length;start++) {
			System.out.println("\n\n\nBuilding fiber number "+start);
			list.add(new ArrayList<int[]>());
			label=(int)Math.round(imgLabels.getStack().getVoxel(coordsTarget[start][0],coordsTarget[start][1] ,coordsTarget[start][2] ));
			System.out.println("Starting fiber ="+label);
			xx=coordinates[label][0];
			yy=coordinates[label][1];
			zz=coordinates[label][2];
			System.out.println(xx+" , "+yy+" , "+zz);
			list.get(start).add(new int[] {xx,yy,zz});			
			DefaultWeightedEdge link;
			double dx,dy,dz;
			double distanceTotale=0;
			double weights=0;
			double distanceMri=0;
			boolean endOfPath=false;
			while(!endOfPath) {
				Set<DefaultWeightedEdge>dweSet=graph.edgesOf(label+INPUT);
				System.out.println("Classe of set");
				Iterator<DefaultWeightedEdge> itr=dweSet.iterator();
				link=null;
				for(DefaultWeightedEdge ed:dweSet) {
					if(result.getFlow(ed)==1 && graph.getEdgeSource(ed) !=label+INPUT) {
						System.out.println("Selected");link=ed;
						System.out.println("Selected has target (vertex input connection) "+graph.getEdgeTarget(ed));
						System.out.println("Selected has source (vertex output connection) "+graph.getEdgeSource(ed));
					}
				}
				if(link==null) {System.out.println("No flow in start point number "+start);endOfPath=true;continue;}
				label=graph.getEdgeSource(link)-OUTPUT;
				if(isSource[label]==1)endOfPath=true;
				x0=xx;
				y0=yy;
				z0=zz;
				xx=coordinates[label][0];
				yy=coordinates[label][1];
				zz=coordinates[label][2];
				dx=xx-x0;
				dy=yy-y0;
				dz=zz-z0;
				list.get(start).add(new int[] {xx,yy,zz});			
				System.out.println("Added at incr " +incr+" new point "+TransformUtils.stringVector(new int[] {xx,yy,zz}, "")+" value IRM="+imgMri.getStack().getVoxel(xx, yy, zz)+" dx="+dx+" dy="+dy+" dz="+dz+" NormD="+TransformUtils.norm(new double[] {dx,dy,dz})+
						"weight="+graph.getEdgeWeight(link));
				distanceTotale+=Math.sqrt(Math.pow((xx-x0),2)+Math.pow((yy-y0),2)+Math.pow((zz-z0),2));
				distanceMri+=(255-imgMri.getStack().getVoxel(xx, yy, zz));
				weights+=graph.getEdgeWeight(link);
				System.out.println("after add : "+distanceMri);
			}		
			System.out.println("DistanceTotale="+distanceTotale);
			System.out.println("DistanceMri="+distanceMri);
		}
		showSexyFibers(imgMri,imgObject,list);
	}


	
	
	
	
	public static void runTestGraph() {
		//////////////////////////////////VARIABLES/////////////////
		//Useful declarations
		System.out.println("Declarations");
		Graph<Integer,DefaultWeightedEdge>graph=new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		long t0=System.currentTimeMillis();
		long t1;
		int source=0;
		int N=0;
		int incr=0;
		int ind=0;
		int label=0;
		int label2=0;
		float val;
		
		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		float alpha=510;//510
		float beta=1;//1
		float gamma=1;//1
		float delta=0;
		float sqr2=(float)Math.sqrt(2);
		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Local useful variables
		ImagePlus imgMri=getImageTest(2);
		ImagePlus imgSeeds=getImageTest(1);
		ImagePlus imgObject=getImageTest(0);
		ImagePlus imgLabels=getImageTest(0);
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);

		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each object voxel, from 0 to N and set N
		System.out.println("Declarations ok. Variables");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if(! ( (valsObject[z][y*X+x] & 0xff) > 0) )continue;
				valsLabels[z][y*X+x]=incr++;
			}
		}
		N=incr;
		System.out.println("N="+N+" among "+(X*Y*Z)+" voxels");
		
		//Build data structures
		int[][]coordinates=new int[N][3];
		float[]mriValue=new float[N];
		int []isSource=new int[N];
		
		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		System.out.println("ok. Prepa");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1
				if(valsLabels[z][y*X+x]<0)continue;
				label=(int)Math.round(valsLabels[z][y*X+x]);
				graph.addVertex(label);

				//Coordinates gets the x,y,z coordinates of each object point
				coordinates[label]=new int[] {x,y,z};

				//mriValue gets the value seen in mri
				mriValue[label]=(int)(valsMri[z][y*X+x]  & 0xff);
				if( ( (valsSeeds[z][y*X+x] & 0xff) > 0) ) {
					isSource[label]=1;
					source=label;
				}
			}
		}
		System.out.println("ok. Prepa2");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				label=(int)Math.round(valsLabels[z][y*X+x]);
				if(label<0)continue;
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					label2=(int)Math.round(valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)]);
					if(label2<0)continue;
					if(label==label2)continue;
					val=alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[di*9+dj*3+dk]+delta*distancesToNeighbours[di*9+dj*3+dk]*(510-mriValue[label]-mriValue[label2]);
					if(isSource[label]==1 && isSource[label2]==1) {
						Graphs.addEdge(graph, label, label2,0);
					}
					else Graphs.addEdge(graph, label, label2,val);
				}
			}
		}
		
		
		
		System.out.println("Run");
		int[]endingPoint=new int[] {9,42,34};//PtiPti
		int xx=endingPoint[0];
		int yy=endingPoint[1];
		int zz=endingPoint[2];
		int x0,y0,z0;
		label=(int)Math.round(imgLabels.getStack().getVoxel(xx,yy ,zz ));
		System.out.println("Label to retrieve = "+label);
		
		//Execute Dijkstra externally
		System.out.println("EXECUTE DJIKSTRA FROM "+source+TransformUtils.stringVector(coordinates[source],"")+" TO "+TransformUtils.stringVector(coordinates[label],""));
		DijkstraShortestPath<Integer,DefaultWeightedEdge> dsp=new DijkstraShortestPath<Integer,DefaultWeightedEdge>(graph, source,label);
		
		List<DefaultWeightedEdge> resultPath=dsp.findPathBetween(graph,source,label);
		System.out.println("Finished after ");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		System.out.println(resultPath.toString());
		
		
		ArrayList<int[]> list=new ArrayList<>();
		int lastZ=120;
		float distanceTotale=0;
		int distanceMri=0;
		float weights=0;
		incr=-1;
		System.out.println("Size of liste = "+resultPath.size());
		int dx,dy,dz;
		while(incr<resultPath.size()-1 ) {
			incr++;
			DefaultWeightedEdge dwe=resultPath.get(incr);
			label2=graph.getEdgeSource(dwe);
			x0=xx;
			y0=yy;
			z0=zz;
			xx=coordinates[label2][0];
			yy=coordinates[label2][1];
			zz=coordinates[label2][2];
			if(zz>120)continue;
			dx=xx-x0;
			dy=yy-y0;
			dz=zz-z0;
			list.add(new int[] {xx,yy,zz});			
			System.out.println("Added at incr " +incr+" new point "+TransformUtils.stringVector(new int[] {xx,yy,zz}, "")+" value IRM="+imgMri.getStack().getVoxel(xx, yy, zz)+" dx="+dx+" dy="+dy+" dz="+dz+" NormD="+TransformUtils.norm(new double[] {dx,dy,dz})+
					"weight="+graph.getEdgeWeight(dwe));
			distanceTotale+=Math.sqrt(Math.pow((xx-x0),2)+Math.pow((yy-y0),2)+Math.pow((zz-z0),2));
			distanceMri+=(255-imgMri.getStack().getVoxel(xx, yy, zz));
			weights+=graph.getEdgeWeight(dwe);
			System.out.println("after add : "+distanceMri);
		}		
		System.out.println("DistanceTotale="+distanceTotale);
		System.out.println("DistanceMri="+distanceMri);
		showSexyFiber(imgMri,imgObject,list);
	}


	
	
	
	public static void runTestSimpleLinkedList() {
		//////////////////////////////////VARIABLES/////////////////
		//Useful declarations
		System.out.println("Declarations");
		long t0=System.currentTimeMillis();
		long t1;
		int source=0;
		int N=0;
		int incr=0;
		int ind=0;
		int label=0;
		int label2=0;
		float val;
		
		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		float alpha=0;
		float beta=0;
		float gamma=0;
		float delta=1;
		float sqr2=(float)Math.sqrt(2);
		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Local useful variables
		ImagePlus imgMri=getImageTest(2);
		ImagePlus imgSeeds=getImageTest(1);
		ImagePlus imgObject=getImageTest(0);
		ImagePlus imgLabels=getImageTest(0);
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);

		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each object voxel, from 0 to N and set N
		System.out.println("Declarations ok. Variables");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if(! ( (valsObject[z][y*X+x] & 0xff) > 0) )continue;
				valsLabels[z][y*X+x]=incr++;
			}
		}
		N=incr;
		System.out.println("N="+N+" among "+(X*Y*Z)+" voxels");
		
		//Build data structures
		SimpleLinkedList settledNodes=new SimpleLinkedList("settled");
		SimpleLinkedList unsettledNodes=new SimpleLinkedList("unsettled");
		int[][]coordinates=new int[N][3];
		int[][]neighbours=new int[N][27];
		int[]parent=new int[N];
		float[]distToParent=new float[N];
		int[]isSettled=new int[N];
		float[]mriValue=new float[N];
		float[]distance=new float[N];
		int []isSource=new int[N];
		
		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		System.out.println("ok. Prepa");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1
				if(valsLabels[z][y*X+x]<0)continue;
				label=(int)Math.round(valsLabels[z][y*X+x]);
				//Coordinates gets the x,y,z coordinates of each object point
				coordinates[label]=new int[] {x,y,z};

				//neighbours gets the label of neighbours
				neighbours[label][13]=-1;
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					val=valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)];
					neighbours[label][di*9+dj*3+dk]= (val < 0) ? -1 : (int)Math.round(val);
				}

				//mriValue gets the value seen in mri
				mriValue[label]=(int)(valsMri[z][y*X+x]  & 0xff);
				
				//distance gets infinity
				if(((valsSeeds[z][y*X+x] & 0xff ) > 0)) {isSource[label]=1;source=label;	}				
				distance[label]= Integer.MAX_VALUE;
			}
		}
		unsettledNodes.add(new Node(source));
		distance[source]=0;
		
		///////////////////////////////////EXECUTION
		incr=0;
		t0=System.currentTimeMillis();
		while(unsettledNodes.getCount()>0) {
			incr++;
			if(incr%10000==0)System.out.print("Iteration "+incr+" / "+N+" = "+VitimageUtils.dou(incr*100.0 /N)+"%  un size="+unsettledNodes.getCount()+"  set size="+settledNodes.getCount());t1=System.currentTimeMillis();
			if(incr%10000==0)System.out.println("  time="+(t1-t0)+"     estimated time ="+( (t1-t0)*1.0/(incr*1000.0 /(1*N) ) ) );
			
			//Identifier le minimum de distance parmi tous les voisins recemment visités
			Node no=unsettledNodes.getFirst();
			Node bestNode=no;
			float min=distance[no.val];
			while(no!=null) {
				if(distance[no.val] < min) {min=distance[no.val];bestNode=no;}
				no=no.next;
			}
			unsettledNodes.remove(bestNode);
			settledNodes.add(bestNode);
			label=bestNode.val;
			isSettled[label]=1;
			
			//Ajouter tous les nouveaux voisins
			for(int n=0;n<27;n++) {
				label2=neighbours[label][n];
				if(label2<0)continue;
				if(isSettled[label2]==1)continue;
				if(isSource[label]*isSource[label2]==1)val=0;
				val=alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[n];
				if (val<0) {System.out.println("ALERTE ALPHA BETA");System.exit(0);}
				val=val+distance[label];
				if(val<distance[label2]) {
					distance[label2]=val;
					parent[label2]=label;
					distToParent[label2]=distancesToNeighbours[n];
					unsettledNodes.add(new Node(label2));
				}
			}
		}
		System.out.println("Finished after ");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		
		
		
		ArrayList<int[]> list=new ArrayList<>();
		int[]endingPoint=new int[] {9,42,34};//PtiPti
		int lastZ=120;
		float distanceTotale=0;
		int distanceMri=0;
		int xx=endingPoint[0];
		int yy=endingPoint[1];
		int zz=endingPoint[2];
		incr=0;
		while(zz<lastZ) {
			label=(int)Math.round(imgLabels.getStack().getVoxel(xx,yy ,zz ));
			incr++;
			label2=parent[label];
			xx=coordinates[label2][0];
			yy=coordinates[label2][1];
			zz=coordinates[label2][2];
			list.add(new int[] {xx,yy,zz});			
			//System.out.println("Added at incr " +incr+" new point "+TransformUtils.stringVector(new int[] {xx,yy,zz}, "")+" value IRM="+imgMri.getStack().getVoxel(xx, yy, zz));
			distanceTotale+=distToParent[label];
			distanceMri+=imgMri.getStack().getVoxel(xx, yy, zz);
		}		
		System.out.println("DistanceTotale="+distanceTotale);
		System.out.println("DistanceMri="+distanceMri);
		showSexyFiber(imgMri,imgObject,list);
	}

	
	public static void showSexyFibers(ImagePlus imgMri,ImagePlus imgObject,ArrayList<ArrayList<int[]>> list ,int radius) {
		ImagePlus imgFibers=VitimageUtils.nullImage(imgMri);
		IJ.run(imgFibers,"32-bit","");
		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		float[][]valsFibers=new float[Z][];
		double laplaceAlpha=0.03*255/4;
		double laplaceBeta=0.03 ;
		double laplaceGamma=8;
		double decX=radius==0 ? 0 : 0.3;
		double decY=radius==0 ? 0 : -0.4;
		double decZ=radius==0 ? 0 : -0.1;
		int fiberRayInt=2;
		for(int z=0;z<Z;z++) {
			valsFibers[z]=(float[])imgFibers.getStack().getProcessor(z+1).getPixels();
		}
		int rayUsed=radius;
		double distX;
		for(int start=0;start<list.size();start++) {
			for(int incrCurSexy=0;incrCurSexy<list.get(start).size();incrCurSexy++) {
				int x0=list.get(start).get(incrCurSexy)[0];
				int y0=list.get(start).get(incrCurSexy)[1];
				int z0=list.get(start).get(incrCurSexy)[2];
				if(radius==0) {valsFibers[z0][(y0)*X+(x0)]=150+start*10;}
				else {
					for(int di=-fiberRayInt-3;di<fiberRayInt+4;di++)for(int dj=-fiberRayInt-3;dj<fiberRayInt+4;dj++) for(int dk=-fiberRayInt-3;dk<fiberRayInt+4;dk++){
						distX=Math.sqrt( ((di+decX)*(di+decX))+(dj+decY)*(dj+decY)+(dk+decZ)*(dk+decZ) )/rayUsed;
						valsFibers[z0+dk][(y0+dj)*X+(x0+di)]+=(float)TestFibre.laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma);
					}
				}
			}
		}
		imgFibers.setDisplayRange(0,255);
//		IJ.run(imgFibers,"8-bit","");
		imgFibers.show();
		VitimageUtils.compositeOf(imgMri, imgFibers).show();
		VitimageUtils.compositeOf(imgObject, imgFibers).show();
	}
	
	
	public static void showSexyFiber(ImagePlus imgMri,ImagePlus imgObject,ArrayList<int[]> list ) {
		ImagePlus imgFibers=VitimageUtils.nullImage(imgMri);
		IJ.run(imgFibers,"32-bit","");
		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		float[][]valsFibers=new float[Z][];
		double laplaceAlpha=0.03*255/4;
		double laplaceBeta=0.03 ;
		double laplaceGamma=8;
		double decX=0.3;
		double decY=-0.4;
		double decZ=-0.1;
		int fiberRayInt=2;
		for(int z=0;z<Z;z++) {
			valsFibers[z]=(float[])imgFibers.getStack().getProcessor(z+1).getPixels();
		}
		int rayUsed=4;
		double distX;
		for(int incrCurSexy=0;incrCurSexy<list.size();incrCurSexy++) {
			int x0=list.get(incrCurSexy)[0];
			int y0=list.get(incrCurSexy)[1];
			int z0=list.get(incrCurSexy)[2];
			for(int di=-fiberRayInt-3;di<fiberRayInt+4;di++)for(int dj=-fiberRayInt-3;dj<fiberRayInt+4;dj++) for(int dk=-fiberRayInt-3;dk<fiberRayInt+4;dk++){
				distX=Math.sqrt( ((di+decX)*(di+decX))+(dj+decY)*(dj+decY)+(dk+decZ)*(dk+decZ) )/rayUsed;
				//System.out.println("rayUsed="+rayUsed+" didjdk="+(di+decX)+" , "+(dj+decY)+" , "+(dk+decZ)+" et distX="+distX+" et func="+laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma));
				valsFibers[z0+dk][(y0+dj)*X+(x0+di)]+=(float)TestFibre.laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma);
			}
		}
		imgFibers.setDisplayRange(0,255);
//		IJ.run(imgFibers,"8-bit","");
		imgFibers.show();
		VitimageUtils.compositeOf(imgMri, imgFibers).show();
		VitimageUtils.compositeOf(imgObject, imgFibers).show();
	}
	
	
	
	
	
	
	
	public static void runTestArrayList() {
		//////////////////////////////////VARIABLES/////////////////
		//Useful declarations
		System.out.println("Declarations");
		long t0=System.currentTimeMillis();
		long t1;
		int source=0;
		int N=0;
		int incr=0;
		int ind=0;
		int label=0;
		int label2=0;
		float val;
		
		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		float alpha=510;
		float beta=1;
		float gamma=10;
		float sqr2=(float)Math.sqrt(2);
		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Local useful variables
		ImagePlus imgMri=getImageTest(2);
		ImagePlus imgSeeds=getImageTest(1);
		ImagePlus imgObject=getImageTest(0);
		ImagePlus imgLabels=getImageTest(0);
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);

		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each object voxel, from 0 to N and set N
		System.out.println("Declarations ok. Variables");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if(! ( (valsObject[z][y*X+x] & 0xff) > 0) )continue;
				valsLabels[z][y*X+x]=incr++;
			}
		}
		N=incr;
		System.out.println("N="+N+" among "+(X*Y*Z)+" voxels");
		
		//Build data structures
		ArrayList<Integer> settledNodes=new ArrayList<Integer>();
		ArrayList<Integer> unsettledNodes=new ArrayList<Integer>();
//		ArrayList<Integer> settledNodes=new ArrayList<Integer>();
//		ArrayList<Integer> unsettledNodes=new ArrayList<Integer>();
		int[][]coordinates=new int[N][3];
		int[][]neighbours=new int[N][27];
		int[]parent=new int[N];
		int[]isSettled=new int[N];
		float[]mriValue=new float[N];
		float[]distance=new float[N];
		int []isSource=new int[N];
		
		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		System.out.println("ok. Prepa");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1
				if(valsLabels[z][y*X+x]<0)continue;
				label=(int)Math.round(valsLabels[z][y*X+x]);
				//Coordinates gets the x,y,z coordinates of each object point
				coordinates[label]=new int[] {x,y,z};

				//neighbours gets the label of neighbours
				neighbours[label][13]=-1;
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					val=valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)];
					neighbours[label][di*9+dj*3+dk]= (val < 0) ? -1 : (int)Math.round(val);
				}

				//mriValue gets the value seen in mri
				mriValue[label]=(int)(valsMri[z][y*X+x]  & 0xff);
				
				//distance gets infinity
				if(((valsSeeds[z][y*X+x] & 0xff ) > 0)) {isSource[label]=1;source=label;	}				
				distance[label]= Integer.MAX_VALUE;
			}
		}
		unsettledNodes.add(source);
		distance[source]=0;
		
		///////////////////////////////////EXECUTION
		incr=0;
		t0=System.currentTimeMillis();
		while(unsettledNodes.size()>0) {
			incr++;
			if(incr%10000==0)System.out.print("Iteration "+incr+" / "+N+" = "+VitimageUtils.dou(incr*100.0 /N)+"%  un size="+unsettledNodes.size()+"  set size="+settledNodes.size());t1=System.currentTimeMillis();
			if(incr%10000==0)System.out.println("  time="+(t1-t0)+"     estimated time ="+( (t1-t0)*1.0/(incr*1000.0 /(1*N) ) ) );
			
			//Identifier le minimum de distance parmi tous les voisins recemment visités
			ind=indOfMin(unsettledNodes,distance);
			label=unsettledNodes.get(ind);
			unsettledNodes.remove(ind);
			settledNodes.add(label);
			isSettled[label]=1;
			
			//Ajouter tous les nouveaux voisins
			for(int n=0;n<27;n++) {
				label2=neighbours[label][n];
				if(label2<0)continue;
				if(isSettled[label2]==1)continue;
				if(isSource[label]*isSource[label2]==1)val=0;
				val=alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[n];
				if (val<0) {System.out.println("ALERTE ALPHA BETA");System.exit(0);}
				val=val+distance[label];
				if(val<distance[label2]) {
					distance[label2]=val;
					parent[label2]=label;
					unsettledNodes.add(label2);
				}
			}
		}
		System.out.println("Finished after ");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
	}
	
	
	public static void runTestLinkedList() {
		//////////////////////////////////VARIABLES/////////////////
		//Useful declarations
		System.out.println("Declarations");
		long t0=System.currentTimeMillis();
		long t1;
		int source=0;
		int N=0;
		int incr=0;
		int ind=0;
		int label=0;
		int label2=0;
		float val;
		
		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		float alpha=510;
		float beta=1;
		float gamma=10;
		float sqr2=(float)Math.sqrt(2);
		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Local useful variables
		ImagePlus imgMri=getImageTest(2);
		ImagePlus imgSeeds=getImageTest(1);
		ImagePlus imgObject=getImageTest(0);
		ImagePlus imgLabels=getImageTest(0);
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);

		int X=imgObject.getWidth();
		int Y=imgObject.getHeight();
		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each object voxel, from 0 to N and set N
		System.out.println("Declarations ok. Variables");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if(! ( (valsObject[z][y*X+x] & 0xff) > 0) )continue;
				valsLabels[z][y*X+x]=incr++;
			}
		}
		N=incr;
		System.out.println("N="+N+" among "+(X*Y*Z)+" voxels");
		
		//Build data structures
		LinkedList<Integer> settledNodes=new LinkedList<Integer>();
		LinkedList<Integer> unsettledNodes=new LinkedList<Integer>();
//		ArrayList<Integer> settledNodes=new ArrayList<Integer>();
//		ArrayList<Integer> unsettledNodes=new ArrayList<Integer>();
		int[][]coordinates=new int[N][3];
		int[][]neighbours=new int[N][27];
		int[]parent=new int[N];
		int[]isSettled=new int[N];
		float[]mriValue=new float[N];
		float[]distance=new float[N];
		int []isSource=new int[N];
		
		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		System.out.println("ok. Prepa");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1
				if(valsLabels[z][y*X+x]<0)continue;
				label=(int)Math.round(valsLabels[z][y*X+x]);
				//Coordinates gets the x,y,z coordinates of each object point
				coordinates[label]=new int[] {x,y,z};

				//neighbours gets the label of neighbours
				neighbours[label][13]=-1;
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					val=valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)];
					neighbours[label][di*9+dj*3+dk]= (val < 0) ? -1 : (int)Math.round(val);
				}

				//mriValue gets the value seen in mri
				mriValue[label]=(int)(valsMri[z][y*X+x]  & 0xff);
				
				//distance gets infinity
				if(((valsSeeds[z][y*X+x] & 0xff ) > 0)) {isSource[label]=1;source=label;	}				
				distance[label]= Integer.MAX_VALUE;
			}
		}
		unsettledNodes.add(source);
		distance[source]=0;
		
		///////////////////////////////////EXECUTION
		incr=0;
		t0=System.currentTimeMillis();
		while(unsettledNodes.size()>0) {
			incr++;
			if(incr%10000==0)System.out.print("Iteration "+incr+" / "+N+" = "+VitimageUtils.dou(incr*100.0 /N)+"%  un size="+unsettledNodes.size()+"  set size="+settledNodes.size());t1=System.currentTimeMillis();
			if(incr%10000==0)System.out.println("  time="+(t1-t0)+"     estimated time ="+( (t1-t0)*1.0/(incr*1000.0 /(1*N) ) ) );
			
			//Identifier le minimum de distance parmi tous les voisins recemment visités
			ind=indOfMin(unsettledNodes,distance);
			label=unsettledNodes.get(ind);
			unsettledNodes.remove(ind);
			settledNodes.add(label);
			isSettled[label]=1;
			
			//Ajouter tous les nouveaux voisins
			for(int n=0;n<27;n++) {
				label2=neighbours[label][n];
				if(label2<0)continue;
				if(isSettled[label2]==1)continue;
				if(isSource[label]*isSource[label2]==1)val=0;
				val=alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[n];
				if (val<0) {System.out.println("ALERTE ALPHA BETA");System.exit(0);}
				val=val+distance[label];
				if(val<distance[label2]) {
					distance[label2]=val;
					parent[label2]=label;
					unsettledNodes.add(label2);
				}
			}
		}
		System.out.println("Finished after ");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
	}

	
	
	
	public static int indOfMin(LinkedList<Integer>list,float[]distance) {
		float min=(float)-10E10;
		int indmin=0;
		int ind;
		int incr=0;
		
		Iterator it=list.iterator();
		while(it.hasNext()) {
			ind=(int)it.next();
			if(distance[ind]<min) {
				min=distance[ind];
				indmin=incr;
			}
			incr++;
		}
		return indmin;
	}
	
	
	

	
	public static int indOfMin(ArrayList<Integer>list,float[]distance) {
		float min=(float)-10E10;
		int indmin=0;
		int incr=0;
		for(int ind=0;ind<list.size();ind++) {
			if(distance[list.get(ind)]<min) {
				min=distance[ind];
				indmin=ind;
			}
		}
		return indmin;
	}
	
	
	
	
	public static ImagePlus getImageTest(int a) {
		String test="ptipti";
		if(a==0)return IJ.openImage("/home/fernandr/Bureau/testDij_area_"+test+".tif");
		if(a==1)return IJ.openImage("/home/fernandr/Bureau/testDij_seeds_"+test+".tif");
		if(a==2)return IJ.openImage("/home/fernandr/Bureau/testDij_irm_"+test+".tif");
		if(a==3)return IJ.openImage("/home/fernandr/Bureau/testDij_area_border"+test+".tif");
		return null;
	}
	
	
	public static ImagePlus prepareImages() {
		String test="pti";
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/testDij_area_"+test+".tif");
		ImagePlus imgMri=IJ.openImage("/home/fernandr/Bureau/testDij_irm_"+test+".tif");
		//erosion
		ImagePlus imgEro2=VitimageUtils.morpho(img, 2, 1.5);
		ImagePlus imgEro4=VitimageUtils.morpho(img, 2, 4);
//		img.show();
//		VitimageUtils.compositeOf(imgMri, imgEro2).show();
//		VitimageUtils.compositeOf(imgMri, imgEro4).show();
		ImagePlus diff=VitimageUtils.binaryOperationBetweenTwoImages(imgEro2,imgEro4,4);
//		VitimageUtils.compositeOf(imgMri,diff).show();
		IJ.saveAsTiff(diff, "/home/fernandr/Bureau/testDij_area_border"+test+".tif");
		
	//	imgEro2.show();
	//	imgEro4.show();
	//	imgMri.show();
//		VitimageUtils.waitFor(100000000);
		
		return null;
	}


}
