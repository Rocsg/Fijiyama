package com.vitimage;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import inra.ijpb.binary.geodesic.GeodesicDistanceTransform3DFloat;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;

import java.io.File;
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
	
	public static final int SPEC_START=0;
	public static final int SPEC_LAST=11;
	public static final boolean SHOWING=false;
	public static final int RAY_MIN=0;
	public static final int RAY_MAX=59;
	public static final int NUMBER_RAYS=60;
	public static final int THICK_SECTION_SIZE=2;
	
	public static double distanceToOrthogonalPlane(double[]v1,double[]v2) {
		double[]v1Norm=TransformUtils.normalize(v1);
		return Math.abs(TransformUtils.scalarProduct(v1Norm, v2));
	}
	
	public static void main(String[]args) {
		
		ImageJ ij=new ImageJ();
		boolean actionView=false;
		if(actionView) {
			String extension="PD";
			int numSpec=5;
			String spec=TestFibre.getSpecimens()[numSpec];
			int dzeta=0;
			
//			ImagePlus imgA=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+".tif");
			//			ImagePlus imgB=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+"colored.tif");
			dzeta=50;
			ImagePlus imgA=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+".tif");
			ImagePlus imgMRI=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+"colored.tif");
			ImagePlus imgNECR=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+"NECRcolored.tif");
			ImagePlus imgAMAD=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+spec+"/Composite_Dzeta_"+dzeta+extension+"AMADcolored.tif");
//			imgA.show();
//			imgB.show();
//			VitimageUtils.showImageIn3D(imgA);
			//			VitimageUtils.showImageIn3D(imgB);
//			VitimageUtils.showImageIn3D(imgA);
//			VitimageUtils.showImageIn3D(imgMRI);
			VitimageUtils.showImageIn3D(imgNECR);
			VitimageUtils.showImageIn3D(imgAMAD);
		}
		else {
			String []extensions=new String[] {"PD"};
			for(String extension : extensions) {
				System.out.println("Computing extension "+extension);
				for(int specimen=SPEC_START;specimen<=SPEC_LAST;specimen++)makeTestGammaProgressive(specimen,extension);
			}
		}
		VitimageUtils.waitFor(10000000);
	}


	public static void makeTest1Viewing(int numSpec) {
		boolean doStepOne=false;

		/* Prepare data*/
		double targetVoxelSize=1;
		String spec=TestFibre.getSpecimens()[numSpec];
		String repSourceML="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/";
		if(doStepOne)prepareDataSourceForBigFibers(repSourceML,spec);
		String extension="PD";
		ImagePlus imgM=IJ.openImage(repSourceML+spec+"/cambium_"+extension+".tif");
		ImagePlus imgA=IJ.openImage(repSourceML+spec+"/cambium_area.tif");
		imgA.show();
		imgM.show();

		
		/* Compute fibers*/
		int zMax=(int)Math.round(VitimageUtils.getSliceRootStocksDown()[numSpec]/targetVoxelSize);
		int[][][]targets=VitimageUtils.getContactPointsAs3DArrayInLittleVoxels(targetVoxelSize,numSpec,imgA,false);
		Object[]obj=null;//estimateFibersCoords(imgM,imgA,zMax,targets,true);
		int[][][][]tabFibers=(int[][][][]) obj[0];
		int[][][]fiberWaterState=(int[][][]) obj[1];
		ImagePlus imgFibers=(ImagePlus) obj[2];
		imgFibers.show();
	}
	
	
	
	public static void makeTestGammaProgressive(int numSpec,String extension) {
		System.out.println("\n\n\nRunning gamma progressive on "+numSpec);
		boolean doStepOne=false;

		/* Prepare data*/
		double targetVoxelSize=1;
		String spec=TestFibre.getSpecimens()[numSpec];
		String repSourceML="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/";
		String repTargetML="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_7_FIBERS/"+VitimageUtils.getSpecimenNames()[numSpec]+"/Data_Fibers/";
		File f=new File(repTargetML);
		f.mkdirs();
		if(doStepOne)prepareDataSourceForBigFibers(repSourceML,spec);
		ImagePlus imgM=IJ.openImage(repSourceML+spec+"/cambium_"+extension+".tif");
		ImagePlus imgA=IJ.openImage(repSourceML+spec+"/cambium_area.tif");
		ImagePlus imgS=IJ.openImage(repSourceML+spec+"/segmentation.tif");
		
		System.out.println("");
		VitimageUtils.printImageResume(imgM,"imgM");
		VitimageUtils.printImageResume(imgA,"imgA");
		VitimageUtils.printImageResume(imgS,"imgS");
		
		int[][][]targets=VitimageUtils.getContactPointsAs3DArrayInLittleVoxels(targetVoxelSize,numSpec,imgA,false);
		int zMax=(int)Math.round(VitimageUtils.getSliceRootStocksDown()[numSpec]/targetVoxelSize);
		ImagePlus imgM2=VitimageUtils.imageCopy(imgM);
		imgM2=VitimageUtils.drawParallepipedInImage(imgM2, 0, 0, zMax,imgM2.getWidth()-1,imgM2.getHeight()-1,zMax,255);
		for(int br=0;br<targets.length;br++) {
			for(int pat=0;pat<targets[br].length;pat++) {
				imgM2=VitimageUtils.drawCircleInImage(imgM2, 3, targets[br][pat][0], targets[br][pat][1], targets[br][pat][2]);
				System.out.println("Impression du point "+targets[br][pat][0]+" , "+targets[br][pat][1]+" , "+targets[br][pat][2]);
			}
		}
		
		if(false) {
			imgM2.show();
			imgM2.setTitle("Seed and contact points");
			VitimageUtils.waitFor(1000000);
		}
		/* Compute fibers*/
//		float[] testDzetas=new float[] {60,80,100,120,140,160,180,200};
		float[] testDzetas=new float[] {50};
		int nExp=testDzetas.length;
		int nData=5;
		double[][][][]tabDataResults=new double[nExp][targets.length][][];
		String strOutput="";
		for (int i=0;i<nExp;i++){
			float dzeta=testDzetas[i];
			strOutput+="\n\n\nComposite_Dzeta_"+((int)Math.round(dzeta));
			String titl="Composite_Dzeta_"+((int)Math.round(dzeta));
			float dzetaMax=255f;
			float dzetaMin=0f;
			if(dzeta==0) {
				dzeta=400;dzetaMin=300;dzetaMax=500;
			}
			float disqualifyingHighCost=1000000f;
			float minCost=0.000000000001f;
			Object[]obj=estimateFibersCoords(imgM,imgA,imgS,zMax,targets,true,dzeta,dzetaMax,dzetaMin,disqualifyingHighCost,minCost,titl);		//return new Object[] {tabFibers,fiberWaterState,imgFibers,fiberWaterScores,fiberLength};

			int[][][][]tabFibers=(int[][][][]) obj[0];
			double[][][]fiberWaterState=(double[][][]) obj[1];
			ImagePlus imgFibers=(ImagePlus) obj[2];
			double[][][]fiberWaterScores=(double[][][]) obj[3];
			double[][]fiberWaterLength=(double[][]) obj[4];
			ImagePlus imgFibersColored=(ImagePlus) obj[5];
			ImagePlus imgFibersNecr=(ImagePlus) obj[6];
			ImagePlus imgFibersAmad=(ImagePlus) obj[7];
			double [][][][]dataOut=(double[][][][])obj[8];
			double [][][][][]dataOut2=(double[][][][][])obj[9];
			double [][][][][]dataOut3=(double[][][][][])obj[10];
			ImagePlus imgSeg=(ImagePlus)obj[11];
			IJ.saveAsTiff(imgFibers,repTargetML+titl+extension+".tif");
			IJ.saveAsTiff(imgFibersColored,repTargetML+titl+extension+"colored.tif");
			IJ.saveAsTiff(imgFibersNecr,repTargetML+titl+extension+"BOWL01colored.tif");
			IJ.saveAsTiff(imgFibersAmad,repTargetML+titl+extension+"BOWL05colored.tif");
			IJ.saveAsTiff(imgSeg, repTargetML+titl+extension+"imgSeg.tif");
			VitimageUtils.writeStringInFile(""+tabFibers.length,repTargetML+"N_BR"+".txt");
			for(int br=0;br<tabFibers.length;br++) {
				VitimageUtils.writeStringInFile(""+tabFibers[br].length,repTargetML+"BR_"+(br)+"_NPAT.txt");
				tabDataResults[i][br]=new double[tabFibers[br].length][5];
				for(int pat=0;pat<tabFibers[br].length;pat++) {
					String strCoords="";
					strOutput+="\n\n-> Computed fiber I="+i+"  BR="+br+"  PAT="+pat+"         length="+fiberWaterLength[br][pat];
					strOutput+="\n         mri moy="+fiberWaterScores[br][pat][0];
					strOutput+="\n         mri sig="+fiberWaterScores[br][pat][1];
					strOutput+="\n         mri min="+fiberWaterScores[br][pat][2];
					strOutput+="\n         mri max="+fiberWaterScores[br][pat][3];

					String strVal="";
					int nM=dataOut[br][pat][0].length;
					VitimageUtils.writeStringInFile(""+nM,repTargetML+"BR_"+(br)+"PA_"+pat+"_NPTS.txt");
					for (int n=0;n<dataOut[br][pat][0].length;n++) {
						strCoords+=(n+" "+tabFibers[br][pat][nM-1-n][0]+" "+tabFibers[br][pat][nM-1-n][1]+" "+tabFibers[br][pat][nM-1-n][2])+"\n";
						String strValBowl="";
						String strValSect="";
						String strValBowlTmp="";
						String strValSectTmp="";
						for(int r=RAY_MIN;r<=RAY_MAX;r++) {
							strValBowlTmp="";
							strValSectTmp="";
							for(int dat=0;dat<11;dat++)strValBowlTmp+=(dataOut2[br][pat][nM-1-n][r][dat]+" ");
							for(int dat=0;dat<11;dat++)strValSectTmp+=(dataOut3[br][pat][nM-1-n][r][dat]+" ");
							strValBowlTmp+=(dataOut2[br][pat][nM-1-n][r][11]+"\n");
							strValSectTmp+=(dataOut3[br][pat][nM-1-n][r][11]+"\n");

							strValBowl+=strValBowlTmp;
							strValSect+=strValSectTmp;
						}
						VitimageUtils.writeStringInFile(strValBowl,repTargetML+"valBowl_BR_"+br+"__PA_"+pat+"__PT_"+n+".txt");
						VitimageUtils.writeStringInFile(strValSect,repTargetML+"valSect_BR_"+br+"__PA_"+pat+"__PT_"+n+".txt");
					}
					VitimageUtils.writeStringInFile(strCoords,repTargetML+"Coords_BR_"+br+"__PA_"+pat+".txt");
					tabDataResults[i][br][pat]=new double[] {fiberWaterLength[br][pat],fiberWaterScores[br][pat][0],fiberWaterScores[br][pat][1],fiberWaterScores[br][pat][2],fiberWaterScores[br][pat][3]};
				}
			}
		}
		VitimageUtils.writeStringInFile(strOutput, repTargetML+"Data_"+extension+".txt");
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

		

	
	public static Object[] estimateFibersCoords(ImagePlus imgMri,ImagePlus imgObject,ImagePlus imgSeg,int zSource,int[][][]targets,boolean thickThinningWaysWithTirait,float dzetaParam,float dzetaMaxParam,float dzetaMinParam,float disqualifyingHighCost,float minimalNegligibleCost,String titleComposite) {
		System.out.println("\nStarting computing flow with "+dzetaMinParam+" - "+dzetaParam+" - "+dzetaMaxParam+" |  max$="+disqualifyingHighCost+"  | min$="+minimalNegligibleCost);
		ArrayList<Double>listCoefs=new ArrayList<Double>();
		long t0=System.currentTimeMillis();
		long t1;
		
		//////////////////////////////////VARIABLES/////////////////
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

/**		
		if(typeFunc==0) val=(float)(alpha-beta*(mriValue[label]+mriValue[label2])+gamma*distancesToNeighbours[di*9+dj*3+dk]);						
		if(typeFunc==1) val=(float)(delta*distancesToNeighbours[di*9+dj*3+dk]*510*Math.pow((510-mriValue[label]-mriValue[label2])/510.0,epsilon));
		if(typeFunc==2) val=(float)(distancesToNeighbours[di*9+dj*3+dk]*( (mriValue[label]-mriValue[label2])/2 > dzeta ? (1-0.005*((mriValue[label]-mriValue[label2])/2 - dzeta ) ) : (1+0.1*(dzeta-(mriValue[label]-mriValue[label2])/2+2) )));

		Cost in case of type func 0  : (255 - mri(i)) + (255 - mri(j))*lambda + gamma*distance (attention si gamma 
		Cost in case of type func 1  : (255 - mri(i)) + (255 - mri(j))^epsilon * delta*distance 
		Cost in case of type func 2  : ( fonction legerement decroissante au dela de 120 d'intensité moyenne, et tres penalisante en deça de 120  ) * distance  

		
	*/	
		int typeFunc=2;
		float alpha=510;		float beta=1;
		float gamma=gamma=2000;
		float lambda=1;
		float delta=1; float dzeta=dzetaParam;float dzetaMax=dzetaMaxParam;//255
		float dzetaPlus=0.005f;
		float dzetaMoins=0.1f;
		float sqr2=(float)Math.sqrt(2);		float sqr3=(float)Math.sqrt(3);
		float epsilon=1;
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
		t1=System.currentTimeMillis();
//		System.out.println("Init labels t="+VitimageUtils.dou((t1-t0)/1000)+" s");
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
		System.out.print("Computing topology between "+N+" voxels (among "+(X*Y*Z)+" vox. in total image)  ");
		if(N>=INPUT) {System.out.println("Wrong dimensions : vertex number superior to maximal possible vertices with architecture. Abort.");System.exit(0);	}
		
		//Build data structures
		int[][]coordinates=new int[N][3];
		float[]mriValue=new float[N];
		int []isSource=new int[N];
		int []isTarget=new int[N];
		coordinates[SOURCE]=new int[] {1,1,120};//Don t care
		boolean targetsFound[][]=new boolean[targets.length][];
		for(int d1=0;d1<targets.length;d1++) {
			targetsFound[d1]=new boolean[targets[d1].length];
			for(int d2=0;d2<targets[d1].length;d2++)targetsFound[d1][d2]=false;
		}

		//////////////////////////////////DATA PREPARATION 2/2 /////////////////
	//	System.out.println("ok. Adding one vertex for each potential fiber voxel, and identifying sources and targets");		
		t1=System.currentTimeMillis();
//		System.out.println("Init labels t="+VitimageUtils.dou((t1-t0)/1000)+" s");
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
				for(int d1=0;d1<targets.length;d1++)for(int d2=0;d2<targets[d1].length;d2++)if(z==targets[d1][d2][2] && y==targets[d1][d2][1] && x==targets[d1][d2][0]) {
						targetsFound[d1][d2]=true;
						isTarget[label]=1;
				}
			}
		}

		for(int d1=0;d1<targets.length;d1++) for(int d2=0;d2<targets[d1].length;d2++)if(!targetsFound[d1][d2]) {
				System.out.println("Error : target unavailable : "+TransformUtils.stringVectorN(targets[d1][d2],""));
				System.exit(0);
		}

		
		//////////////////////////////////DATA PREPARATION 3/2 (Damn !)/////////////////
		t1=System.currentTimeMillis();
//		System.out.println("Collecting edges t="+VitimageUtils.dou((t1-t0)/1000)+" s");
		float moy=0;
		for(int z=0;z<Z && z<zSource+1;z++) {
			if(z%50==0)System.out.print(z+"/"+Z+"  ");
//			System.out.println(z + "  nV="+(2*N+2)+" vertices and "+Nedges+" edges  ("+(Nedges/(2*N+2))+" edges/vertex ");
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
					if(typeFunc==0) val=(float)(1/alpha)*(alpha-beta*(mriValue[label]+mriValue[label2])*lambda+gamma*distancesToNeighbours[di*9+dj*3+dk]);						
					if(typeFunc==1) val=(float)(delta*distancesToNeighbours[di*9+dj*3+dk]*510*Math.pow((510-mriValue[label]-mriValue[label2])/510.0,epsilon));
					if(typeFunc==2) {
						moy=(mriValue[label]+mriValue[label2])/2;
						val=(float)(distancesToNeighbours[di*9+dj*3+dk]);
						if(moy>dzeta) {
							 val=val* (dzetaMax-moy)/(dzetaMax-dzeta);
						}
						else if(moy>dzetaMinParam) {
							 val=val* (1+(disqualifyingHighCost-1)*(dzeta-moy ) );
						}
						else val*=(dzeta-dzetaMinParam)*disqualifyingHighCost;
//						System.out.println("Application func2 : "+m);
					}
					if(val<minimalNegligibleCost)val=minimalNegligibleCost;
					//System.out.println("Exemple : "+mriValue[label]+"  , "+mriValue[label2]+" donne "+val);
					if(isSource[label2]==0) {
						val=val/disqualifyingHighCost;
						listCoefs.add(new Double(val));
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
		
		
		t1=System.currentTimeMillis();
		double []tabCoefs=new double[listCoefs.size()];
		for(int i=0;i<tabCoefs.length;i++)tabCoefs[i]=listCoefs.get(i);
//		System.out.println("Stats : "+TransformUtils.stringVectorN(VitimageUtils.statistics1D(tabCoefs),""));
//		System.out.println("minMax : "+TransformUtils.stringVectorN(VitimageUtils.minAndMaxOfTab(tabCoefs),""));
//		System.out.println("COST INF ="+CapacityScalingMinimumCostFlow.COST_INF);
		
		//Execute algorithm of min-cost flow
		CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> minCostFlow=new CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge>();
		Function<DefaultWeightedEdge,Integer>minCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcBack ? nUnitsOfFlow : 0);};
		Function<DefaultWeightedEdge,Integer>maxCapacities=(DefaultWeightedEdge dwe)-> {return (dwe==arcBack ? nUnitsOfFlow : 1);};
		Function<Integer,Integer>supplyMap=(Integer i)-> {return 0;};
		MinimumCostFlowProblem<Integer, DefaultWeightedEdge> minimumCostFlowProblem=new MinimumCostFlowProblem.MinimumCostFlowProblemImpl<Integer, DefaultWeightedEdge>(graph, supplyMap, maxCapacities,minCapacities);
		t1=System.currentTimeMillis();
		System.out.print("\nRun  t="+VitimageUtils.dou((t1-t0)/1000)+" s");
		System.out.print("  starting  algorithm on graph with "+(2*N+2)+" vertices and "+Nedges+" edges  ("+(Nedges/(2*N+2))+" edges/vertex ");
		MinimumCostFlow<DefaultWeightedEdge> result = minCostFlow.getMinimumCostFlow(minimumCostFlowProblem);
		t1=System.currentTimeMillis();
		System.out.println(" --> Ending at  t="+VitimageUtils.dou((t1-t0)/1000)+" s");

		
		
		
		//Building coordinates tab for the flow
		int[][][][]tabFibers=new int[targets.length][][][];
		double[][][]fiberWaterState=new double[targets.length][][];
		double[][][]fiberWaterScores=new double[targets.length][][];
		double[][]fiberLength=new double[targets.length][];
		int xx,yy,zz,x0,y0,z0;
		//System.out.println("Building branches");
		for(int branche =0;branche<targets.length;branche++) {
			tabFibers[branche]=new int[targets[branche].length][][];
			fiberWaterState[branche]=new double[targets[branche].length][];
			fiberWaterScores[branche]=new double[targets[branche].length][3];
			fiberLength[branche]=new double[targets[branche].length];
			for(int fib=0;fib<tabFibers[branche].length;fib++) {
				ArrayList<int[]> list=new ArrayList<>();
				//System.out.print("  Building branche "+branche+" fiber number "+fib);
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
//				System.out.print("    DistanceTotale="+distanceTotale);
				//				System.out.println("    DistanceMri="+distanceMri);
				tabFibers[branche][fib]=new int[list.size()][3];
				fiberWaterState[branche][fib]=new double[list.size()];
				for(int co=0;co<list.size();co++) {
					tabFibers[branche][fib][co]=new int[] {list.get(co)[0],list.get(co)[1],list.get(co)[2]};
					fiberWaterState[branche][fib][co]=imgMri.getStack().getVoxel(list.get(co)[0],list.get(co)[1],list.get(co)[2]);
				}
				fiberWaterScores[branche][fib]=new double[] {VitimageUtils.statistics1D(fiberWaterState[branche][fib])[0],VitimageUtils.statistics1D(fiberWaterState[branche][fib])[1],
															VitimageUtils.minAndMaxOfTab(fiberWaterState[branche][fib])[0],VitimageUtils.minAndMaxOfTab(fiberWaterState[branche][fib])[1]	};
				fiberLength[branche][fib]=distanceTotale;
			}
		}
		Object[] obj=getSexyFibers(imgMri,imgObject,imgSeg,tabFibers,thickThinningWaysWithTirait,titleComposite);
		ImagePlus imgFibers=(ImagePlus)(obj[0]);
		ImagePlus imgFibersColored=(ImagePlus)(obj[1]);
		ImagePlus imgFibersNecr=(ImagePlus)(obj[2]);
		ImagePlus imgFibersAmad=(ImagePlus)(obj[3]);
		double[][][][]dataOut=(double[][][][])obj[4];
		double[][][][][]dataOut2=(double[][][][][])obj[5];
		double[][][][][]dataOut3=(double[][][][][])obj[6];
		ImagePlus imgSeg2=(ImagePlus)(obj[7]);
		return new Object[] {tabFibers,fiberWaterState,imgFibers,fiberWaterScores,fiberLength,imgFibersColored,imgFibersNecr,imgFibersAmad,dataOut,dataOut2,dataOut3,imgSeg2};
	}		
	
	
	
	public static Object[] getSexyFibers(ImagePlus imgMri,ImagePlus imgCambium,ImagePlus imgSegInit,int[][][][] tabFibers,boolean thickThinningWaysWithTirait,String titleComposite) {
		ImagePlus imgSegLowRes=VitimageUtils.subXYZPerso(imgSegInit,VitimageUtils.getVoxelSizes(imgSegInit),false,0);
		ImagePlus imgDistToAmadou=VitimageUtils.computeGeodesicDistanceMap(imgSegLowRes, 3, 1, 3, 2);
		ImagePlus imgDistToNecrose=VitimageUtils.computeGeodesicDistanceMap(imgSegLowRes, 2, 1, 2, 2);
		ImagePlus imgSeg=VitimageUtils.imageCopy(imgSegLowRes);
		ImagePlus imgFibers=VitimageUtils.nullImage(imgMri);
		ImagePlus imgFibersAmad=VitimageUtils.nullImage(imgMri);
		ImagePlus imgFibersNecr=VitimageUtils.nullImage(imgMri);
		ImagePlus imgFibersColored=VitimageUtils.nullImage(imgMri);
		ImagePlus imgSeeds=null;
		if(thickThinningWaysWithTirait)imgSeeds=VitimageUtils.nullImage(imgFibers);
		IJ.run(imgFibers,"32-bit","");
		IJ.run(imgFibersNecr,"32-bit","");
		IJ.run(imgFibersAmad,"32-bit","");
		
		int X=imgFibers.getWidth();		int Y=imgFibers.getHeight();		int Z=imgFibers.getStackSize();
		VitimageUtils.printImageResume(imgFibers,"imgFibers");
		VitimageUtils.printImageResume(imgSeg,"imgSeg");
		VitimageUtils.printImageResume(imgSegInit,"imgSegInit");
		
		byte[][]valsMri=new byte[Z][];
		byte[][]valsCambium=new byte[Z][];
		byte[][]valsSeeds=new byte[Z][];

		float[][]valsFibers=new float[Z][];
		float[][]valsDistAmad=new float[Z][];
		float[][]valsDistNecr=new float[Z][];
		int nData=3;
		
		byte[][]valsSeg=new byte[Z][];
		byte[][]valsFibersColored=new byte[Z][];
		float[][]valsFibersAmad=new float[Z][];
		float[][]valsFibersNecr=new float[Z][];
		double[][][][]tabDataOut=new double[tabFibers.length][][][];
		double[][][][][]tabDataOut2=new double[tabFibers.length][][][][];
		double[][][][][]tabDataOut3=new double[tabFibers.length][][][][];
		
		int radius= thickThinningWaysWithTirait ? 4 : 0;
		double laplaceAlpha=0.03*255/4;		double laplaceBeta=0.03 ;		double laplaceGamma=8;
		double decX=radius==0 ? 0 : 0.3;		double decY=radius==0 ? 0 : -0.4;		double decZ=radius==0 ? 0 : -0.1;
		VitimageUtils.printImageResume(imgFibersColored);
		for(int z=0;z<Z;z++) {
			valsFibers[z]=(float[])imgFibers.getStack().getProcessor(z+1).getPixels();
			valsFibersAmad[z]=(float[])imgFibersAmad.getStack().getProcessor(z+1).getPixels();
			valsFibersNecr[z]=(float[])imgFibersNecr.getStack().getProcessor(z+1).getPixels();
			valsDistAmad[z]=(float[])imgDistToAmadou.getStack().getProcessor(z+1).getPixels();
			valsDistNecr[z]=(float[])imgDistToNecrose.getStack().getProcessor(z+1).getPixels();
			valsCambium[z]=(byte[])imgCambium.getStack().getProcessor(z+1).getPixels();
			valsMri[z]=(byte[])imgMri.getStack().getProcessor(z+1).getPixels();
			valsSeg[z]=(byte[])imgSeg.getStack().getProcessor(z+1).getPixels();
			valsFibersColored[z]=(byte[])imgFibersColored.getStack().getProcessor(z+1).getPixels();
			if(thickThinningWaysWithTirait )valsSeeds[z]=(byte[])imgSeeds.getStack().getProcessor(z+1).getPixels(); }
		int rayUsed=radius;
		int fiberRayInt=radius+2;
		double maxDistAmad=50;
		double maxDistNecr=50;
		double minDistAmad=0;
		double minDistNecr=0;
		double distX;
		int x0,y0,z0;
		int i0=0,i1=0;
		double vectDir[]=new double[3];
		double vectPt[]=new double[3];
		double dirX,dirY,dirZ;
		for(int branche =0;branche<tabFibers.length;branche++) {
			tabDataOut[branche]=new double[tabFibers[branche].length][][];
			tabDataOut2[branche]=new double[tabFibers[branche].length][][][];
			tabDataOut3[branche]=new double[tabFibers[branche].length][][][];
			
			for(int fib=0;fib<tabFibers[branche].length;fib++) {
				tabDataOut[branche][fib]=new double[nData][tabFibers[branche][fib].length];
				tabDataOut2[branche][fib]=new double[tabFibers[branche][fib].length][NUMBER_RAYS][12];
				tabDataOut3[branche][fib]=new double[tabFibers[branche][fib].length][NUMBER_RAYS][12];
				for(int incrCurSexy=0;incrCurSexy<tabFibers[branche][fib].length;incrCurSexy++) {
					x0=tabFibers[branche][fib][incrCurSexy][0];
					y0=tabFibers[branche][fib][incrCurSexy][1];
					z0=tabFibers[branche][fib][incrCurSexy][2];
					i0=incrCurSexy-2;
					if(i0<0)i0=0;
					if(i0>tabFibers[branche][fib].length-5)i0=tabFibers[branche][fib].length-5;
					i1=i0+4;
					for(int d=0;d<3;d++)vectDir[d]= tabFibers[branche][fib][i1][d]-tabFibers[branche][fib][i0][d];

					
					int incrCurSexyPlus=incrCurSexy+1;
					if(incrCurSexy==0)incrCurSexyPlus=incrCurSexy-1;
					
					boolean computeStatsBowls=true;
					boolean debug=false;
					//Pour chaque voxel fibre, 
						//pour chaque talile de voisinage, MESURER LE NOMBRE DE VOXELS CONDSAIN CONDNOIR CONDAMAD NOCONDSAIN NOCONDNOIR NOCONDAMAD   CONDNOIRPOND CONDAMADPOND  
					int inc=0;
					int[]indices=new int[]    {-1,-1,-1,-1,-1,-1,-1};
					if(computeStatsBowls) {
						if(SHOWING)imgSeg.show();
						int isCond=((int)(valsCambium[z0][(y0)*X+(x0)] & 0xff)>0 ? 1 : 0);
						if(incrCurSexy%30==0)System.out.println("Computing stats for incr "+(incrCurSexy)+ "coords = "+x0+" , "+y0+" , "+z0+" conducteur="+isCond);
						int rayMin=RAY_MIN;
						int rayMax=RAY_MAX;
						double ray=0;
						int nbVals=NUMBER_RAYS;
						double [][][]values=new double[nbVals][][];
						double [][][]valuesSection=new double[nbVals][][];
						for(int r=0;r<=rayMax;r++) {
							values[r]=new double[4][3];
							valuesSection[r]=new double[4][3];
						}
						for(int dx=-rayMax;dx<=rayMax;dx++) {
							if( ((x0+dx)<0) || ((x0+dx)>=X ))continue;
							for(int dy=-rayMax;dy<=rayMax;dy++) {
								if( ((y0+dy)<0) || ((y0+dy)>=Y ))continue;
								for(int dz=-rayMax;dz<=rayMax;dz++) {
									if( ((z0+dz)<0) || ((z0+dz)>=Z ))continue;
//									debug=(x0==155 && y0==82 && z0==286);
									ray=Math.sqrt(dx*dx+dy*dy+dz*dz);
									int classe=(int)(valsSeg[z0+dz][(y0+dy)*X+(x0+dx)]&0xff);
									isCond=((int)(valsCambium[z0+dz][(y0+dy)*X+(x0+dx)] & 0xff)>0 ? 1 : 0);
									if(debug)System.out.println("\nDEBUG : evaluation de "+dx+" , "+dy+" , "+dz+" de classe "+classe+" isCond="+isCond);
									//System.out.println("x0+dx="+(x0+dx)+" y0+dy="+(y0+dy)+" z0+dz"+(z0+dz)+"  isCond="+isCond);
									if(debug)System.out.println("  AJOUT compta aux cases["+isCond+"]["+(classe-1)+"] : ");
									if(classe>0 && classe <4) {
										for(int ind=(int) Math.ceil(ray);ind<=rayMax;ind++) {
											if(debug)if(ind%10==0)System.out.println();
											if(debug)System.out.print(" "+ind);
											values[ind][isCond][classe-1]++;
										}
									}
									if(debug)System.out.println();
									double valAdd=(1/(1+ray*ray));
									double valAddLin=1/(1+ray);
									if(debug)System.out.println("  AJOUT influence "+valAdd+" aux cases["+isCond+"]["+(classe-1)+"] : ");
									if(classe >1 && classe <4) {
										for(int ind=(int) Math.ceil(ray);ind<=rayMax;ind++) {
											if(debug)if(ind%10==0)System.out.println();
											values[ind][2][classe-2]+=valAdd;
											values[ind][3][classe-2]+=valAddLin;
											if(debug)System.out.print(" "+ind);
										}
									}
									if(debug)System.out.println();
									vectPt[0]=dx;vectPt[1]=dy;vectPt[2]=dz;
									if(distanceToOrthogonalPlane(vectDir,vectPt)<THICK_SECTION_SIZE) {
										if(classe>0 && classe <4) {
											for(int ind=(int) Math.ceil(ray);ind<=rayMax;ind++)valuesSection[ind][isCond][classe-1]++;
										}
										if(classe >1 && classe <4) {
											for(int ind=(int) Math.ceil(ray);ind<=rayMax;ind++) {
												valuesSection[ind][2][classe-2]+=valAdd;
												valuesSection[ind][3][classe-2]+=valAddLin;
											}
										}
									}
								}
							}
						}
						if(debug)VitimageUtils.waitFor(100000000);
						//Mesurer la premiere couche pour laquelle on a ajoute une proportion de voxels qui fait plus que :
						//1/10   1/4  1/3  1/2  2/3  3/4  9/10 
						indices=new int[]    {-1,-1,-1,-1,-1,-1,-1};
						double[]thre=new double[] {0.1 , 0.25 , 0.33 , 0.5 , 0.67, 0.75 , 0.9};
						int nbThre=thre.length;
						for(int r=1;r<=rayMax;r++) {
							tabDataOut2[branche][fib][incrCurSexy][r][0]=values[r][0][0];//Bois sain conducteur
							tabDataOut2[branche][fib][incrCurSexy][r][1]=values[r][0][1];//Bois noir conducteur
							tabDataOut2[branche][fib][incrCurSexy][r][2]=values[r][0][2];//Bois amadou conducteur
							tabDataOut2[branche][fib][incrCurSexy][r][3]=values[r][1][0];//Bois sain nonconducteur
							tabDataOut2[branche][fib][incrCurSexy][r][4]=values[r][1][1];//Bois noir nonconducteur
							tabDataOut2[branche][fib][incrCurSexy][r][5]=values[r][1][2];//Bois amadou nonconducteur
							tabDataOut2[branche][fib][incrCurSexy][r][6]=values[r][2][0];//Bois noir etoile de la mort
							tabDataOut2[branche][fib][incrCurSexy][r][7]=values[r][2][1];//Bois amadou etoile de la mort
							tabDataOut2[branche][fib][incrCurSexy][r][8]=values[r][2][2];//Rien
							tabDataOut2[branche][fib][incrCurSexy][r][9]=values[r][3][0];//Bois noir etoile de la mort lineaire
							tabDataOut2[branche][fib][incrCurSexy][r][10]=values[r][3][1];//Bois amadou etoile de la mort lneaire
							tabDataOut2[branche][fib][incrCurSexy][r][11]=values[r][3][2];//Rien

							tabDataOut3[branche][fib][incrCurSexy][r][0]=valuesSection[r][0][0];//Bois sain conducteur
							tabDataOut3[branche][fib][incrCurSexy][r][1]=valuesSection[r][0][1];//Bois noir conducteur
							tabDataOut3[branche][fib][incrCurSexy][r][2]=valuesSection[r][0][2];//Bois amadou conducteur
							tabDataOut3[branche][fib][incrCurSexy][r][3]=valuesSection[r][1][0];//Bois sain nonconducteur
							tabDataOut3[branche][fib][incrCurSexy][r][4]=valuesSection[r][1][1];//Bois noir nonconducteur
							tabDataOut3[branche][fib][incrCurSexy][r][5]=valuesSection[r][1][2];//Bois amadou nonconducteur
							tabDataOut3[branche][fib][incrCurSexy][r][6]=valuesSection[r][2][0];//Bois noir etoile de la mort
							tabDataOut3[branche][fib][incrCurSexy][r][7]=valuesSection[r][2][1];//Bois amadou etoile de la mort
							tabDataOut3[branche][fib][incrCurSexy][r][8]=valuesSection[r][2][2];//Rien
							tabDataOut3[branche][fib][incrCurSexy][r][9]=valuesSection[r][3][0];//Bois noir etoile de la mort lineaire
							tabDataOut3[branche][fib][incrCurSexy][r][10]=valuesSection[r][3][1];//Bois amadou etoile de la mort lneaire
							tabDataOut3[branche][fib][incrCurSexy][r][11]=valuesSection[r][3][2];//Rien

							//							System.out.println(TransformUtils.stringMatrixMN("R="+r+"  ",values[r]));
							double diffSain=values[r][0][0]+values[r][1][0]-values[r-1][0][0]-values[r-1][1][0];
							double diffNoSain=values[r][0][1]+values[r][1][1]-values[r-1][0][1]-values[r-1][1][1]+values[r][0][2]+values[r][1][2]-values[r-1][0][2]-values[r-1][1][2];
							double necroticRatio=(diffSain+diffNoSain) < 5 ? 0 : (diffNoSain/(diffSain+diffNoSain));
							for(int t=0;t<nbThre;t++) {
								if(necroticRatio>thre[t] && indices[t]<0)indices[t]=r;
							}							
						}
						for(int i=0;i<7;i++)if(indices[i]<0)indices[i]=rayMax+1;
	//					System.out.println("\n\n  Vals calculées : "+TransformUtils.stringVectorN(indices,""));
					}
						
					
					
					
					
					
					
					//TAGGER LES IMAGES DE FIBRES
					for(int di=-2;di<3;di++)for(int dj=-2;dj<3;dj++)if(di*di+dj*dj<8) {
						valsFibersAmad[z0][(y0+dj)*X+(x0+di)]=valsDistAmad[z0][(y0)*X+(x0)];
						valsFibersNecr[z0][(y0+dj)*X+(x0+di)]=valsDistNecr[z0][(y0)*X+(x0)];
						if(computeStatsBowls) {
							valsFibersAmad[z0][(y0+dj)*X+(x0+di)]=indices[0];
							valsFibersNecr[z0][(y0+dj)*X+(x0+di)]=indices[2];
						}
						
						
						valsFibersColored[z0][(y0+dj)*X+(x0+di)]=valsMri[z0][(y0)*X+(x0)];
						tabDataOut[branche][fib][0][incrCurSexy]=(int)(valsMri[z0][(y0)*X+(x0)]&0xff);
						tabDataOut[branche][fib][1][incrCurSexy]=valsDistNecr[z0][(y0)*X+(x0)];
						tabDataOut[branche][fib][2][incrCurSexy]=valsDistAmad[z0][(y0)*X+(x0)];
						//System.out.println("Lu on branche "+branche+" on fib "+fib+" on incr "+incrCurSexy+" at xyz="+x0+","+y0+","+z0+" : "+valsDistNecr[z0][(y0)*X+(x0)]+" , "+valsDistAmad[z0][(y0)*X+(x0)]);
					}
					
					
					if(incrCurSexy==tabFibers[branche][fib].length-1)valsSeeds[z0][y0*X+x0]=(byte)(255 & 0xff);
					if(radius==0) {valsFibers[z0][(y0)*X+(x0)]=255;}
					else {
						for(int di=-fiberRayInt-3;di<fiberRayInt+4;di++)for(int dj=-fiberRayInt-3;dj<fiberRayInt+4;dj++) for(int dk=-fiberRayInt-3;dk<fiberRayInt+4;dk++){
							distX=Math.sqrt( ((di+decX)*(di+decX))+(dj+decY)*(dj+decY)+(dk+decZ)*(dk+decZ) )/rayUsed;
							valsFibers[z0+dk][(y0+dj)*X+(x0+di)]+=(float)TestFibre.laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma);
 						}
					}
				}
//				VitimageUtils.waitFor(1000000);
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
			distanceFibers.setTitle("distanceFibers after distance map");
			ImagePlus distanceTrait=TestFibre.watemarkFloatData(distanceFibers, 0, 10000, 10, 20, 2, 0);
			distanceTrait=VitimageUtils.thresholdFloatImage(distanceTrait,0.0001,100000);
			ImagePlus imgComp=VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), imgFibers,imgMri, titleComposite, false, 2);
			imgComp.setTitle(titleComposite);

			ImagePlus maskNonFiber=VitimageUtils.thresholdByteImage(imgFibersColored, 0, 1);
			maskNonFiber=VitimageUtils.switchValueInImage(maskNonFiber, 255, 1);
			ImagePlus mriMasked=VitimageUtils.makeOperationBetweenTwoImages(imgMri, maskNonFiber, 2,false);
			ImagePlus imgCompColored=VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), mriMasked, imgFibersColored, titleComposite+"colored", false, 2);
			imgFibersAmad.setDisplayRange(minDistAmad, maxDistAmad);
			imgFibersNecr.setDisplayRange(minDistNecr, maxDistNecr);
			IJ.run(imgFibersAmad,"8-bit","");
			IJ.run(imgFibersNecr,"8-bit","");
			imgFibersAmad=VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), mriMasked, imgFibersAmad, titleComposite+"Dist0_1colored", false, 2);
			imgFibersNecr=VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), mriMasked, imgFibersNecr, titleComposite+"Dist0_25colored", false, 2);
			if(SHOWING)imgFibersAmad.show();
			if(SHOWING)imgFibersNecr.show();
			return new Object[] {imgComp,imgCompColored,imgFibersNecr,imgFibersAmad,tabDataOut,tabDataOut2,tabDataOut3,imgSeg};
		}
		return new Object[] {imgFibers,imgFibersColored,imgFibersAmad,imgFibersNecr};
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
