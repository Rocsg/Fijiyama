package com.vitimage;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import inra.ijpb.binary.geodesic.GeodesicDistanceTransform3DFloat;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm.MinimumCostFlow;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

public class FibersEstimator {
	
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		makeTest1Viewing(0);
		VitimageUtils.waitFor(10000000);
	}

	
	public static void makeTest1Viewing(int numSpec) {
		boolean doStepOne=true;
		double targetVoxelSize=1;
		String spec=TestFibre.getSpecimens()[numSpec];
		String repSourceML="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/";
		if(doStepOne)prepareDataSourceForBigFibers(repSourceML,spec);
		ImagePlus imgM=IJ.openImage(repSourceML+spec+"/cambium_T1.tif");
		ImagePlus imgA=IJ.openImage(repSourceML+spec+"/cambium_area.tif");
		System.out.println("Ouvrir "+repSourceML+spec+"/cambium_area.tif");
		if(doStepOne && false) {
			imgA.show();
			imgM.show();
			VitimageUtils.waitFor(100000000);
		}

		
		int zMax=(int)Math.round(TestFibre.getSliceRootStocksDown()[numSpec]/targetVoxelSize);
		int[][][]targets=TestFibre.getContactPointsAs3DArrayInLittleVoxels(targetVoxelSize,numSpec,imgA,false);
		Object[]obj=estimateFibersCoords(imgM,imgA,zMax,targets,true);
		int[][][][]tabFibers=(int[][][][]) obj[0];
		int[][][]fiberWaterState=(int[][][]) obj[1];
		ImagePlus imgFibers=(ImagePlus) obj[2];
//		imgFibers.show();
	}
	
	public static void prepareDataSourceForBigFibers(String path,String spec) {
		double targetVoxelSize=1;
		double []initialVoxelSize= {0.7224,0.7224,1.0};
		double []factors=TransformUtils.multiplyVector(initialVoxelSize, 1/targetVoxelSize);
		System.out.println("Preparing data for big fibers");
		ImagePlus segAniso=IJ.openImage(path+spec+"/segmentation.tif");
		System.out.println("ok. Boundaries");
		ImagePlus boundary14=VitimageUtils.boundaryZone(segAniso, 1,4,6,255);		
		ImagePlus boundary10=VitimageUtils.boundaryZone(segAniso, 1,0,6,255);
		ImagePlus boundaries=VitimageUtils.binaryOperationBetweenTwoImages(boundary14, boundary10, 1);
		System.out.println("ok. Dilation 1");
		Strel3D str=inra.ijpb.morphology.strel.BallStrel.fromDiameter(6);
		boundaries =new ImagePlus("",Morphology.dilation(boundaries.getImageStack(),str));
		System.out.println("ok. Dilation 2");
		str=inra.ijpb.morphology.strel.BallStrel.fromDiameter(2);
//		boundaries =new ImagePlus("",Morphology.dilation(boundaries.getImageStack(),str));
		System.out.println("ok. selection");
		ImagePlus segSain=VitimageUtils.thresholdByteImage(segAniso, 1, 2);
		boundaries=VitimageUtils.binaryOperationBetweenTwoImages(boundaries, segSain, 2);
		System.out.println("ok. Dilation 3");
		str=inra.ijpb.morphology.strel.BallStrel.fromDiameter(3);
		boundaries =new ImagePlus("",Morphology.dilation(boundaries.getImageStack(),str));
		ImagePlus boundariesMono=VitimageUtils.switchValueInImage(boundaries,255	,1);
		IJ.saveAsTiff(boundaries,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_area_no_sub.tif");
		boundaries=VitimageUtils.subXYZPerso(boundaries, factors, false,0);
		boundaries=VitimageUtils.thresholdByteImage(boundaries, 127.5, 256);
//		VitimageUtils.waitFor(10000000);
		IJ.saveAsTiff(boundaries,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_area.tif");
			
		System.out.println("ok. Masking");
		String []outName= {"T1","T2","M0"};
		String []inName= {"Mod_2_THIN_low_mri_t1.tif","Mod_3_THIN_low_mri_t2.tif","Mod_4_THIN_low_mri_m0.tif"};
		for(int mod=0;mod<3;mod++) {
			System.out.println("   Processing mod "+outName[mod]);
			ImagePlus mri=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/"+inName[mod]);
			System.out.println("   ok. masking");
			mri=VitimageUtils.makeOperationBetweenTwoImages(mri, boundariesMono, 2, true);
			System.out.println("   ok. gauss");
			//mri=VitimageUtils.gaussianFiltering(mri, 0.6, 0.6, 0.6);
			System.out.println("   ok. upsampling");
			mri=VitimageUtils.subXYZPerso(mri, factors, true,0);
			System.out.println("   ok. save");
			mri.setDisplayRange(0, 255);
			IJ.run(mri,"8-bit","");
			IJ.saveAsTiff(mri,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_"+outName[mod]+".tif");
		}
	}

		

	
	public static Object[] estimateFibersCoords(ImagePlus imgMri,ImagePlus imgObject,int zSource,int[][][]targets,boolean thickThinningWaysWithTirait) {
		//imgMri.show();
		//imgMri.setTitle("mr");
		//imgObject.show();
		//		imgObject.setTitle("area");
//		VitimageUtils.waitFor(100000000);
		long t0=System.currentTimeMillis();
		long t1;
		VitimageUtils.printImageResume(imgMri,"imgMri");
		VitimageUtils.printImageResume(imgObject,"imgObject");
		
		//////////////////////////////////VARIABLES/////////////////
		System.out.println("Declarations");
		Graph<Integer,DefaultWeightedEdge>graph=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		DefaultWeightedEdge arcBack;
		final int INPUT=100000000;
		final int OUTPUT=200000000;
		final int SOURCE=0;
		final int COLLECTOR=1;
		graph.addVertex(SOURCE);
		graph.addVertex(COLLECTOR);
		int nUnits=0;
		int Nedges=0;
		for(int dim=0;dim<targets.length;dim++)nUnits+=targets[dim].length;
		final int nUnitsOfFlow=nUnits;
		int incr=0;		int ind=0;		int label=0;		int label2=0;		float val; int N=0;

		//Parameters for cost between voxels
		//  cost to link neighbours voxels i,j = alpha - beta * ( valIRM(i) + valIRM(j) ) + gamma * physical_distance (i,j)
		int typeFunc=1;
		float alpha=510;		float beta=1;		float gamma=2000;		float delta=1; float epsilon=1f; float dzeta=240;
		float sqr2=(float)Math.sqrt(2);		float sqr3=(float)Math.sqrt(3);
		float[]distancesToNeighbours=new float[] { sqr3,sqr2,sqr3   ,   sqr2,1,sqr2  , sqr3,sqr2,sqr3  ,  
                                                  sqr2,  1 ,sqr2    ,  1,0, 1        ,sqr2, 1  ,sqr2   , 
                                                  sqr3,sqr2,sqr3    ,  sqr2,1,sqr2   ,sqr3,sqr2,sqr3   };		
		
		//Images used
		ImagePlus imgLabels=VitimageUtils.imageCopy(imgMri);
		imgLabels=VitimageUtils.nullImage(imgLabels);
		IJ.run(imgLabels,"32-bit","");
		VitimageUtils.makeOperationOnOneImage(imgLabels,1, -1,false);
		int X=imgObject.getWidth();		int Y=imgObject.getHeight();		int Z=imgObject.getStackSize();
		byte[][]valsObject=new byte[Z][];
		byte[][]valsMri=new byte[Z][];
		float[][]valsLabels=new float[Z][];

		
		
		
		//////////////////////////////////DATA PREPARATION 1/2 /////////////////
		//Set an etiquette to each voxel that can be a future fiber passage, from 0 to N and set N
		System.out.println("Init labels");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z;z++) {
			valsObject[z]=(byte[])imgObject.getStack().getProcessor(z+1).getPixels();
			valsLabels[z]=(float[])imgLabels.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
		}
		for(int z=0;z<Z && z<zSource+1;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				if( (valsObject[z][y*X+x] & 0xff) > 0 && z>0 && z<Z-1 && x>0 && x< X-1 && y>0 && y<Y-1)valsLabels[z][y*X+x]=incr++;
				else valsLabels[z][y*X+x]=-1;
			}
		}
		N=incr;
		System.out.println("Number of processed voxels="+N+" among "+(X*Y*Z)+" voxels");
		if(N>=INPUT) {System.out.println("Wrong dimensions : vertex number superior to maximal possible vertices with architecture. Abort.");System.exit(0);	}
		
		//Build data structures
		int[][]coordinates=new int[N][3];
		float[]mriValue=new float[N];
		int []isSource=new int[N];
		int []isTarget=new int[N];
		coordinates[SOURCE]=new int[] {1,1,120};//Don t care


		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
		System.out.println("ok. Adding one vertex for each potential fiber voxel, and identifying sources and targets");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z && z<zSource+1;z++) {
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				//For each voxel of labels with a number>-1, add two vertices. One for entry, one for output
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
				for(int d1=0;d1<targets.length;d1++)for(int d2=0;d2<targets[d1].length;d2++)if(z==targets[d1][d2][2] && y==targets[d1][d2][1] && x==targets[d1][d2][0])isTarget[label]=1;
			}
		}

		//////////////////////////////////DATA PREPARATION 3/2 (Damn !)/////////////////
		System.out.println("ok. Collecting edges");t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		for(int z=0;z<Z && z<zSource+1;z++) {
			System.out.println(z + "  nV="+(2*N+2)+" vertices and "+Nedges+" edges  ("+(Nedges/(2*N+2))+" edges/vertex ");
			for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
				label=(int)Math.round(valsLabels[z][y*X+x]);
				if(label<0)continue;
				if(isSource[label]==1) {
					Graphs.addEdge(graph, SOURCE,label+INPUT, 0);		
					Nedges++;
				}
				if(isTarget[label]==1) {
					Graphs.addEdge(graph, label+OUTPUT,COLLECTOR,0);
					Nedges++;
				}
			
				for(int di=0;di<3;di++)for(int dj=0;dj<3;dj++)for(int dk=0;dk<3;dk++) {
					label2=(int)Math.round(valsLabels[z+dk-1][(y+dj-1)*X+(x+di-1)]);
					if(label2<0)continue;
					if(label==label2)continue;
					val=0;
					if(typeFunc==0) val=(float)(alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[di*9+dj*3+dk]);						
					if(typeFunc==1) val=(float)(delta*distancesToNeighbours[di*9+dj*3+dk]*510*Math.pow((510-mriValue[label]-mriValue[label2])/510.0,epsilon));
					if(typeFunc==2) val=(float)(distancesToNeighbours[di*9+dj*3+dk]*( (mriValue[label]-mriValue[label2])/2 > dzeta ? (1-0.005*((mriValue[label]-mriValue[label2])/2 - dzeta ) ) : (1+0.1*(dzeta-(mriValue[label]-mriValue[label2])/2+2) )));
					//System.out.println("Exemple : "+mriValue[label]+"  , "+mriValue[label2]+" donne "+val);
					if(isSource[label2]==0) {
						Graphs.addEdge(graph, label+OUTPUT, label2+INPUT,val);
						Nedges++;
					}
				}
				Graphs.addEdge(graph,label+INPUT,label+OUTPUT,0);
				Nedges++;
			}
		}		
		arcBack=Graphs.addEdge(graph, COLLECTOR,SOURCE, 0);					
		Nedges++;
		
		
		System.out.println("Run");
		
		//Execute algorithm of min-cost flow
		CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> minCostFlow=new CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge>();
		Function<DefaultWeightedEdge,Integer>minCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcBack ? nUnitsOfFlow : 0);};
		Function<DefaultWeightedEdge,Integer>maxCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcBack ? nUnitsOfFlow : 1);};
		Function<Integer,Integer>supplyMap=(Integer i)-> {return 0;};
		MinimumCostFlowProblem<Integer, DefaultWeightedEdge> minimumCostFlowProblem=new MinimumCostFlowProblem.MinimumCostFlowProblemImpl<Integer, DefaultWeightedEdge>(graph, supplyMap, maxCapacities,minCapacities);
		System.out.println("Starting  algorithm on graph with "+(2*N+2)+" vertices and "+Nedges+" edges  ("+(Nedges/(2*N+2))+" edges/vertex ");
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));
		MinimumCostFlow<DefaultWeightedEdge> result = minCostFlow.getMinimumCostFlow(minimumCostFlowProblem);
		System.out.println("Algorithm finished");
		t1=System.currentTimeMillis();System.out.println(VitimageUtils.dou((t1-t0)/1000));

		
		
		
		//Building coordinates tab for the flow
		int[][][][]tabFibers=new int[targets.length][][][];
		int[][][]fiberWaterState=new int[targets.length][][];
		int xx,yy,zz,x0,y0,z0;
		for(int branche =0;branche<targets.length;branche++) {
			tabFibers[branche]=new int[targets[branche].length][][];
			fiberWaterState[branche]=new int[targets[branche].length][];
			for(int fib=0;fib<tabFibers[branche].length;fib++) {
				ArrayList<int[]> list=new ArrayList<>();
				System.out.print("\n\n\nBuilding branche "+branche+" fiber number "+fib);
				label=(int)Math.round(imgLabels.getStack().getVoxel(targets[branche][fib][0],targets[branche][fib][1] ,targets[branche][fib][2] ));
				xx=coordinates[label][0];				yy=coordinates[label][1];				zz=coordinates[label][2];
				list.add(new int[] {xx,yy,zz});			
				DefaultWeightedEdge link;
				double distanceTotale=0;
				double weights=0;
				double distanceMri=0;
				boolean endOfPath=false;
				while(!endOfPath) {
					//Looking for sourcing vertex
					Set<DefaultWeightedEdge>dweSet=graph.edgesOf(label+INPUT);
					link=null;
					for(DefaultWeightedEdge ed:dweSet) if(result.getFlow(ed)==1 && graph.getEdgeSource(ed) !=label+INPUT)link=ed;					
					if(link==null) {System.out.println("No flow here");endOfPath=true;continue;}

					label=graph.getEdgeSource(link)-OUTPUT;
					if(isSource[label]==1)endOfPath=true;
					x0=xx;					y0=yy;					z0=zz;
					xx=coordinates[label][0];					yy=coordinates[label][1];					zz=coordinates[label][2];
					list.add(new int[] {xx,yy,zz});			
					distanceTotale+=Math.sqrt(Math.pow((xx-x0),2)+Math.pow((yy-y0),2)+Math.pow((zz-z0),2));
					distanceMri+=(255-imgMri.getStack().getVoxel(xx, yy, zz));
					weights+=graph.getEdgeWeight(link);
				}		
				System.out.print("    DistanceTotale="+distanceTotale);
				System.out.println("    DistanceMri="+distanceMri);
				tabFibers[branche][fib]=new int[list.size()][3];
				fiberWaterState[branche][fib]=new int[list.size()];
				for(int co=0;co<list.size();co++) {
					tabFibers[branche][fib][co]=new int[] {list.get(co)[0],list.get(co)[1],list.get(co)[2]};
					fiberWaterState[branche][fib][co]=(int)Math.round(imgMri.getStack().getVoxel(list.get(co)[0],list.get(co)[1],list.get(co)[2]));
				}
			}
		}
		ImagePlus imgFibers=getSexyFibers(imgMri,tabFibers,thickThinningWaysWithTirait);
		return new Object[] {tabFibers,fiberWaterState,imgFibers};
	}		
		
	
	
	
	
	public static ImagePlus getSexyFibers(ImagePlus imgMri,int[][][][] tabFibers,boolean thickThinningWaysWithTirait) {
		ImagePlus imgFibers=VitimageUtils.nullImage(imgMri);
		imgMri.show();
		ImagePlus imgSeeds=null;
		if(thickThinningWaysWithTirait)imgSeeds=VitimageUtils.nullImage(imgFibers);
		IJ.run(imgFibers,"32-bit","");
		int X=imgFibers.getWidth();		int Y=imgFibers.getHeight();		int Z=imgFibers.getStackSize();
		float[][]valsFibers=new float[Z][];
		byte[][]valsSeeds=new byte[Z][];
		int radius= thickThinningWaysWithTirait ? 4 : 0;
		double laplaceAlpha=0.03*255/4;		double laplaceBeta=0.03 ;		double laplaceGamma=8;
		double decX=radius==0 ? 0 : 0.3;		double decY=radius==0 ? 0 : -0.4;		double decZ=radius==0 ? 0 : -0.1;
		for(int z=0;z<Z;z++) {			valsFibers[z]=(float[])imgFibers.getStack().getProcessor(z+1).getPixels(); if(thickThinningWaysWithTirait )valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels(); }
		int rayUsed=radius;
		int fiberRayInt=radius+2;
		double distX;
		int x0,y0,z0;
		for(int branche =0;branche<tabFibers.length;branche++) {
			for(int fib=0;fib<tabFibers[branche].length;fib++) {
				for(int incrCurSexy=0;incrCurSexy<tabFibers[branche][fib].length;incrCurSexy++) {
					x0=tabFibers[branche][fib][incrCurSexy][0];
					y0=tabFibers[branche][fib][incrCurSexy][1];
					z0=tabFibers[branche][fib][incrCurSexy][2];
					if(incrCurSexy==tabFibers[branche][fib].length-1)valsSeeds[z0][y0*X+x0]=(byte)(255 & 0xff);
					if(radius==0) {valsFibers[z0][(y0)*X+(x0)]=255;}
					else {
						for(int di=-fiberRayInt-3;di<fiberRayInt+4;di++)for(int dj=-fiberRayInt-3;dj<fiberRayInt+4;dj++) for(int dk=-fiberRayInt-3;dk<fiberRayInt+4;dk++){
							distX=Math.sqrt( ((di+decX)*(di+decX))+(dj+decY)*(dj+decY)+(dk+decZ)*(dk+decZ) )/rayUsed;
							valsFibers[z0+dk][(y0+dj)*X+(x0+di)]+=(float)TestFibre.laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma);
						}
					}
				}
			}
		}
		imgFibers.setDisplayRange(0,255);
		if(thickThinningWaysWithTirait) {
			float[] floatWeights = new float[] {1000,1414,1732};
			ImagePlus maskFibers=VitimageUtils.thresholdFloatImage(imgFibers,1,10000000);
			ImagePlus distanceFibers=new ImagePlus(
					"geodistance",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(imgSeeds.getStack(), maskFibers.getStack())
					);		
			distanceFibers=VitimageUtils.noNanInFloat(distanceFibers,(float) -1);
			distanceFibers.show();
			distanceFibers.setTitle("distanceFibers after distance map");
			ImagePlus distanceTrait=TestFibre.watemarkFloatData(distanceFibers, 0, 10000, 10, 20, 2, 0);
			distanceTrait=VitimageUtils.thresholdFloatImage(distanceTrait,0.0001,100000);
			VitimageUtils.compositeOf(distanceTrait, imgMri).show();
			return distanceTrait;
		}
		return imgFibers;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
		
	
	
	public static int[][][]estimateFibersWaterState(){
		
		
		return null;
	}
	
	public static ImagePlus interpolateFibersInImage(double radiusStart,double radiusFinish,boolean tiraits) {
		
		
		
		return null;
	}
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private ImagePlus imgMri;
	private ImagePlus imgArea;
	private int zSource;
	private int[][][]targets;
	private double[][][][]fibersCoords;
	private Graph<Integer,DefaultWeightedEdge> graph;
	private int[][][]fibersWaterState;

	
	
	public FibersEstimator(ImagePlus imgMri,ImagePlus imgArea, int zSource,int[][][]targets) {
		this.imgMri=imgMri;
		this.imgArea=imgArea;
		this.zSource=zSource;
		this.targets=targets;
	}

	
	public void prepareImagesForFiberTracking() {
		
	}
	
	
	public void computeFibersInWholeTissue(boolean onlyOneFiberPerAxis) {
		
		
		
	}
	
	public ImagePlus interpolateFibersInImagePlus(double scaleFactor,double radius,boolean makeTiraits) {
		
		return null;
	}
	
	
}
