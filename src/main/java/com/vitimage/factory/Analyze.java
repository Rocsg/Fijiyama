package com.vitimage.factory;

import java.util.ArrayList;

import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

public class Analyze {
	private String mother;
	private ArrayList<String> memoryCategory;
	private ArrayList<String> memoryExplanation;
	private ArrayList<String> memoryData;
	private ArrayList<ImagePlus> memoryImage;
	private ArrayList<Integer> memoryIndex1;
	private ArrayList<Integer> memoryIndex2;
	private int index1=0;
	private int index2=0;
	private int indexIdent1=-1;
	private int indexIdent2=0;
	private int indexExplore1=0;
	private int indexExplore2=0;
	private boolean explorationModeActivated=true;
	private boolean annaIsListening=true;
	private String interlocuteur="Romain";
	private ImagePlus[] currentShowing=null;
	private boolean matrixDisplayed=false;
	private boolean detailsDisplayed=false;
	
	/**
	 * High level methods and behaviours
	 * @param ind
	 * @return
	 */		
	public Analyze(String mother) {
		memoryCategory=new ArrayList<String>();
		memoryImage=new ArrayList<ImagePlus>();
		memoryExplanation=new ArrayList<String>();
		memoryData=new ArrayList<String>();
		memoryIndex1=new ArrayList<Integer>();
		memoryIndex2=new ArrayList<Integer>();

		this.mother=mother;
		notifyAlgo("Demarrage","Analyze en cours","");
	}

	public String talk(String ask,boolean audioMode){
		boolean dontGotIt;
		String ret="";
		boolean stopFonctionAuditive=( (ask.toLowerCase().contains("arret") || ask.toLowerCase().contains("stop") ) && ask.toLowerCase().contains("fonction") && ask.toLowerCase().contains("audit"));
		boolean repriseFonctionAuditive=( (ask.toLowerCase().contains("reprise") || ask.toLowerCase().contains("demarrage") )  && ask.toLowerCase().contains("fonction") && ask.toLowerCase().contains("audit"));
		boolean algo=(ask.toLowerCase().contains("algo") || ask.toLowerCase().contains("algorithme"));
		boolean current=(ask.toLowerCase().contains("actuel") || ask.toLowerCase().contains("courante") || ask.toLowerCase().contains("maintenant") || ask.toLowerCase().contains("on est ou"));
		boolean back=(ask.toLowerCase().contains("retour") || ask.toLowerCase().contains("arriere")  || ask.toLowerCase().contains("prec") || ask.toLowerCase().contains("arrière") || ask.toLowerCase().contains("previous"));
		boolean front=( ask.toLowerCase().contains("avan") || ask.toLowerCase().contains("apres")|| ask.toLowerCase().contains("suivant") || ask.toLowerCase().contains("suiv"));
		boolean reprise=( ask.toLowerCase().contains("reprise") || ask.toLowerCase().contains("redemarrage") || ask.toLowerCase().contains("fin"));
		boolean salutation=( ask.toLowerCase().contains("coucou") || ask.toLowerCase().contains("bonjour") || ask.toLowerCase().contains("salut") || ask.toLowerCase().contains("ola"));
		boolean explorer=( ask.toLowerCase().contains("stop") || ask.toLowerCase().contains("break") || ask.toLowerCase().contains("explo") || ask.toLowerCase().contains("analyse"));
		boolean getImages=( ask.toLowerCase().contains("disponible") || ask.toLowerCase().contains("voir") || ask.toLowerCase().contains("image") );
		boolean hideImages=( ask.toLowerCase().contains("fermer") || ask.toLowerCase().contains("ferm") || ask.toLowerCase().contains("image") );
		boolean history=( ask.toLowerCase().contains("hist") || ask.toLowerCase().contains("recap") || ask.toLowerCase().contains("tout") || explorer);
		boolean inspection=( ask.toLowerCase().contains("inspection") || ask.toLowerCase().contains("insp") || ask.toLowerCase().contains("slic") );
		boolean minHistory=(( ask.toLowerCase().contains("min") || back || front  || current) && (!history) );
		boolean matrix=ask.toLowerCase().contains("matrix");
		boolean details=ask.toLowerCase().contains("details");
		int ind=0;
		int val=0;
		if((ask.length()>0) && java.lang.Character.isDigit(ask.charAt(ind))) {
			val=Integer.parseInt(String.valueOf(ask.charAt(ind)));
			while((ind<ask.length()-1) &&java.lang.Character.isDigit(ask.charAt(++ind))){
				val*=10;
				val+=Integer.parseInt(String.valueOf(ask.charAt(ind)));
			}
		}
		if(val>0) {
			gotoIndex(val);
			minHistory=true;
		}

		dontGotIt=(val==0) && !details && !matrix && !minHistory && !hideImages &&
				! getImages && !explorer && !salutation && !reprise && !front && !back &&
				!current && !algo && !repriseFonctionAuditive;
		
		if(stopFonctionAuditive)annaIsListening=false;
		if(repriseFonctionAuditive)annaIsListening=true;
		if(annaIsListening) {
			if(matrix)matrixDisplayed=!matrixDisplayed;
			if(details)detailsDisplayed=!detailsDisplayed;
			if(!detailsDisplayed)matrixDisplayed=false;

			if(algo && front)ret+= ("Algorithme suivant. "+frontInTimeOneAlgo()); 	
			else if(algo&& back)ret+=  ("Algorithme precedent. "+backInTimeOneAlgo()); 
			else if(algo && current)ret+=  ("Algorithme actuel. "+tellMeThatThing(indexExplore2)); 
			else if(front)ret+=  ("Etape suivante. "+frontInTimeOneStep()); 
			else if(back)ret+= ("Etape precedente. "+backInTimeOneStep()); 
			else if(current)ret+= ("Etape actuelle. "+tellMeThatThing(indexExplore2)); 
			else if(reprise) ret+=  stopExploration();
			else if(salutation) ret+=  "Bonjour "+interlocuteur+".";
			else if(explorer) ret+= startExploration();
			else if(currentShowing==null && getImages)ret+=showImagesForThisStep(indexExplore2);
			else if(currentShowing != null && hideImages)ret+=hideImages();
			else if(currentShowing != null && inspection) {ImagePlus img=IJ.getImage();VitimageUtils.imageChecking(img);ret+="Inspection realisee";}
			else if(minHistory || history)ret=ret+"";
			else if(dontGotIt)ret+= ("Je n'ai pas compris. Mes capacités de comprehension sont encore limitées. Pourriez-vous enoncer plus distinctement votre requête ?");
/*			else{
				if(algo && front)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(algo && back)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(algo && current)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(front)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(back)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(current)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse"; 	
				else if(reprise)ret+= "L'analyse est deja desactivee";
				else if(salutation) ret+=  "Bonjour "+interlocuteur+".";
				else if(explorer) ret+= startExploration();
				else if(getImages || hideImages)ret+= "Pour effectuer cette action, il est nécessaire d'activer le mode analyse";
				else if(minHistory || history)ret=ret+"";
				else ret+= ("Je n'ai pas compris. Mes capacités auditives sont limitées. Pourriez-vous enoncer plus distinctement votre requête ?");
			}
*/
		}
		if(minHistory)ret=ret+"\n"+miniHistory();
		else if(history)ret=ret+"\n"+history();
		boolean cuteMode=false;
		
		if(audioMode) {}
		else {
			if(cuteMode)return("\n- "+interlocuteur+" : \""+ask+"\"\n- Anna : \""+ret+"\"");
			else return("\n\n"+ret+"\n\n");
		}
		return ret;
	}
	
	public String tellMeThatThing(int index) {
		if(isAlgo(index)) {
			return("Algorithme numero "+memoryIndex1.get(index)+" , "+memoryExplanation.get(index)+". "+
					((memoryData.get(index).equals("")) ? "" : "Données : "+memoryData.get(index)));
		}
		else if(isInfo(index)) {
			return("Algo "+memoryIndex1.get(index)+" , etape "+memoryIndex2.get(index)+" , information utile. "+memoryExplanation.get(index)+". "+
					((memoryData.get(index).equals("")) ? "" : "Données : "+memoryData.get(index)));

		}
		else if(isMatrix(index)) {
			if(matrixDisplayed)return("Algo "+memoryIndex1.get(index)+" , etape "+memoryIndex2.get(index)+" , information utile . "+memoryExplanation.get(index)+". "+
					((memoryData.get(index).equals("")) ? "" : "Données : "+memoryData.get(index)));
			else {
				return("Algo "+memoryIndex1.get(index)+" , etape "+memoryIndex2.get(index)+" , information utile . Matrice non affichee (demander\"matrix\" pour activer l affichage)");
			}
		
		}
		else if(isImage(index)) {
			return("Image conservee pour algo "+memoryIndex1.get(index)+", etape "+memoryIndex2.get(index)+" . Intitulé : "+memoryExplanation.get(index));

		}
		else{//step
			return("Algo "+memoryIndex1.get(index)+", etape "+memoryIndex2.get(index)+" . "+memoryExplanation.get(index)+". "+
					((memoryData.get(index).equals("")) ? "" : "Données : "+memoryData.get(index)));
		}
	}

	public String startExploration() {
		explorationModeActivated=true;
		indexExplore2=indexExplore1;
		return("Demarrage de l'exploration. Derniere etape réalisée : "+tellMeThatThing(indexExplore2)+"");
		
	 }
		
	public String stopExploration() {
		explorationModeActivated=false;
		indexExplore1=index1;
		indexExplore2=index2;
		return ("Fin de l'exploration. Reprise du processus initial : "+mother);
	}


	
	/**
	 * Callbacks to gather information from working external objects and processes
	 * @param ind
	 * @return
	 */		
	public void notifyAlgo(String algo,String explain, String data) {
		memoryCategory.add("Algorithme");
		memoryImage.add(new ImagePlus());
		memoryExplanation.add(algo+" . "+explain);
		memoryData.add(data);
		memoryIndex1.add(new Integer(++indexIdent1));
		indexIdent2=-1;
		memoryIndex2.add(new Integer(indexIdent2));

		index1=memoryCategory.size()-1;
		index2=memoryCategory.size();
		indexExplore1=index1;
		indexExplore2=index2-1;
		notifyStep("Départ algo","");
	}

	public void notifyStep(String explain, String data) {
		memoryCategory.add("Etape");
		memoryImage.add(new ImagePlus());
		memoryExplanation.add(explain);
		memoryData.add(data);
		memoryIndex1.add(new Integer(indexIdent1));
		memoryIndex2.add(new Integer(++indexIdent2));
		index2=memoryCategory.size();
		indexExplore1=index1;
		indexExplore2=index2-1;
	}

	public void storeImage(ImagePlus img,String explain) {
		memoryCategory.add("Image");
		memoryImage.add(new Duplicator().run(img,1,img.getStackSize()));
		memoryExplanation.add(explain);
		memoryData.add("");
		memoryIndex1.add(new Integer(indexIdent1));
		memoryIndex2.add(new Integer(indexIdent2));
		index2=memoryCategory.size();
	}

	public void storeEntryImage(ImagePlus img,String explain) {
		storeImage(img,explain);
		remember("Parametres de l'image "+img.getTitle()," (dimX, dimY, dimZ) , (VX, VY, VZ) =  ("+img.getWidth()+" , "+img.getHeight()+" , "+img.getStackSize()+") , ("
				+img.getCalibration().pixelWidth+" , "+img.getCalibration().pixelHeight+" , "+img.getCalibration().pixelDepth+")"+
				" Dynamique = ["+img.getProcessor().getMin()+" --> "+img.getProcessor().getMax()+"]");
	}

	public void remember(String explain, String data) {
		memoryCategory.add("Info");
		memoryImage.add(new ImagePlus());
		memoryExplanation.add(explain);
		memoryData.add(data);
		memoryIndex1.add(new Integer(indexIdent1));
		memoryIndex2.add(new Integer(indexIdent2));
		index2=memoryCategory.size();
	}

	public void rememberMatrix(String explain, String data) {
		memoryCategory.add("Matrix");
		memoryImage.add(new ImagePlus());
		memoryExplanation.add(explain);
		memoryData.add(data);
		memoryIndex1.add(new Integer(indexIdent1));
		memoryIndex2.add(new Integer(indexIdent2));
		index2=memoryCategory.size();
	}
	
	public void rememberImageDynamic(ImagePlus img, String explain) {
		remember(explain," dynamique = ["+img.getProcessor().getMin()+" --> "+img.getProcessor().getMax()+"]");
	}
	/**
	 * Go back and forth in the time
	 * @param ind
	 * @return
	 */	
	public String backInTimeOneStep() {
		if(indexExplore2 ==indexExplore1) return (backInTimeOneAlgo());
		else {
			while(( indexExplore2>-1) && (! isStep(--indexExplore2)) && (! isAlgo(indexExplore2)) ){}
			return (tellMeThatThing(indexExplore2));
		}
	}

	public String backInTimeOneAlgo() {
		int test=indexExplore1;
		while((test>0) && (! isAlgo(--test))) {}
		if(!isAlgo(test))return "Pas d'algorithme precedent detecté";
		else {
			indexExplore1=test;
			indexExplore2=test;
			return(tellMeThatThing(indexExplore2));
		}
	}

	public void backInTimeDebutAlgo() {
		indexExplore2=indexExplore1;
		tellMeThatThing(indexExplore2);
	}

	
	public String frontInTimeOneStep() {
		int test=indexExplore2;
		while( (test+1<nbInMem()) && (! isStep(++test)) && (! isAlgo(test)) ){}
		if( isStep(test) && (indexExplore2!=test)) {indexExplore2=test; return tellMeThatThing(indexExplore2);}
		else if( isAlgo(test) ) {indexExplore2=test-1; return(frontInTimeOneAlgo());}
		else return "Il n'y a pas d'étape suivante";
	}

	public String frontInTimeOneAlgo() {
		int test=indexExplore1;
		while( (test < nbInMem()-1) && (! isAlgo(++test)) ){}
		if(! isAlgo(test))return("Pas d'algorithme suivant detecté.");
		else {
			indexExplore1=test;
			indexExplore2=test;
			return (tellMeThatThing(indexExplore1));
		}
	}
		

	
	/**
	 * Displaying utilities
	 * @param ind
	 * @return
	 */
	public int nbAvailableImagesAtThisStep(int index) {
		int count=0;
		int ind=index+1;
		while(   (ind < nbInMem()) && (! isStep(ind)) && (! isAlgo(ind)) ) {
			if(isImage(ind))count++;
			ind++;
		}
		return count;
	}

	public String showImagesForThisStep(int index) {
		if(isStep(index) && nbAvailableImagesAtThisStep(index) > 0){
			currentShowing=new ImagePlus[nbAvailableImagesAtThisStep(index)];
			int iter=0;
			for(int i=0;i<currentShowing.length;i++) {
				while(! isImage(index+(++iter))){}
				currentShowing[i]=memoryImage.get(index+iter);
				currentShowing[i].show();
				IJ.run(currentShowing[i],"Fire","");
				currentShowing[i].setTitle(memoryExplanation.get(index+iter));
			}
			return("Affichage de "+currentShowing.length+" images pour l'étape "+memoryExplanation.get(index));			
		}
		return ("Pas d'image à cet index");
	}
		
	public String hideImages() {
		if(currentShowing != null);
		for(int i=0;i<currentShowing.length ;i++)currentShowing[i].hide();
		currentShowing=null;
		return("Fermeture des images");
	}
	
	public String history() {
		String ret="\n";
		for(int i=0;i<nbInMem();i++) {
			if(isStep(i)||isAlgo(i)||detailsDisplayed) {
				String tir;
				if(i!=indexExplore2)tir=(i<10 ? "     " : i<100 ? "    " : i<1000 ? "   " : " "); 
				else tir=(i<10 ? "))   " : i<100 ? "))" : i<1000 ? ")) " : "))"); 
				if(isAlgo(i))ret+=("\n             |\n             V\n"+((i==indexExplore2) ?"((":"") +"Index "+i+" "+tir+"|---> "+tellMeThatThing(i));
				else if(isStep(i))ret+=("\n             |       |\n"+((i==indexExplore2) ? "((" : "") +"Index "+i+" "+tir+"|       |--"+tellMeThatThing(i));
				else ret+=("\nIndex "+i+" "+tir+"|       |    o--"+tellMeThatThing(i));
			}
		}
		return ret;
	}

	public String miniHistory() {
		String ret="\n";
		for(int i=indexExplore1;i<nbInMem() && (  (i!=indexExplore1) && !(isAlgo(i)) ||  (i==indexExplore1));i++) {
			if(isStep(i)||isAlgo(i)||detailsDisplayed) {
				String tir;
				if(i!=indexExplore2)tir=(i<10 ? "     " : i<100 ? "    " : i<1000 ? "   " : " "); 
				else tir=(i<10 ? "))   " : i<100 ? "))" : i<1000 ? ")) " : "))"); 
				if(isAlgo(i))ret+=("\n             |\n             V\n"+((i==indexExplore2) ?"((":"") +"Index "+i+" "+tir+"|---> "+tellMeThatThing(i));
				else if(isStep(i))ret+=("\n             |       |\n"+((i==indexExplore2) ? "((" : "") +"Index "+i+" "+tir+"|       |--"+tellMeThatThing(i));
				else ret+=("\nIndex "+i+" "+tir+"|       |    o--"+tellMeThatThing(i));
			}
		}
		return ret;
	}


	
	
	/**
	 * Low level functions
	 * @param ind
	 * @return
	 */
	public int getIndex(int ind) {
		if(ind==1)return (this.index1);
		if(ind==2)return (this.index2);
		return 0;
	}
	
	public int getIndexAndIncrement(int ind) {
		if(ind==1)return (this.index1++);
		if(ind==2)return (this.index2++);
		return 0;
	}
	
	public int nbInMem() {return memoryCategory.size();}
	
	public boolean isImage(int index) {
		return memoryCategory.get(index).equals("Image");
	}

	public boolean isAlgo(int index) {
		return memoryCategory.get(index).equals("Algorithme");
	}

	public boolean isStep(int index) {
		return memoryCategory.get(index).equals("Etape");
	}

	public boolean isInfo(int index) {
		return memoryCategory.get(index).equals("Info");
	}

	public boolean isMatrix(int index) {
		return memoryCategory.get(index).equals("Matrix");
	}

	public void gotoIndex(int index) {
		int ind=(index<1) ? 1 : index> nbInMem() ? nbInMem()-1 : index;
		if(isAlgo(ind)) {
			indexExplore1=indexExplore2=ind;
		}
		else {
			indexExplore2=ind;
			if( ! isStep(indexExplore2)) {
				while((!isStep(indexExplore2)) && (!isAlgo(indexExplore2)) )indexExplore2--;
			}
			indexExplore1=indexExplore2;
			if( ! isAlgo(indexExplore1)) { 
				while((!isAlgo(indexExplore1)))indexExplore1--;
				
			}
		}
	}
	
	
}

