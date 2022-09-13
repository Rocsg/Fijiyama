package net.imagej.fijiyama.rsml;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.scijava.vecmath.Point3d;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.Memory;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import net.imagej.fijiyama.common.Timer;
import net.imagej.fijiyama.common.VitiDialogs;
import net.imagej.fijiyama.common.VitimageUtils;

public class RsmlExpert_Plugin extends PlugInFrame implements KeyListener, ActionListener{

	
	/* Plugin entry points for test/debug or run in production ******************************************************************/
	public static void main(String[]args) {		
		ImageJ ij=new ImageJ();
		String testDir="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00021"+ ""; 
		RsmlExpert_Plugin plugin=new RsmlExpert_Plugin();	
		plugin.run(testDir);//testDir);	
	}

	public RsmlExpert_Plugin(){		super("");	}
	
	public RsmlExpert_Plugin(String arg){		super(arg);	}

	public void run(String arg) {				this.startPlugin(arg);		}
	
	
	
	
	
	
	/* Internal variables ******************************************************************************/
	private static final long serialVersionUID = 1L;
	private String dataDir;
	private ImagePlus registeredStack=null;
	private ImagePlus currentImage=null;
	private RootModel currentModel=null;
	private int stepOfModel=0;
	private int Nt;
	private ImagePlus imgInitSize;
	private FSR sr;
	private ImagePlus[] tabReg;
	private ImagePlus[] tabRes;
	private static final int OK=1;
	private static final int UNDO=2;
	private static final int MOVE=3;
	private static final int REMOVE=4;
	private static final int ADD=5;
	private static final int SWITCH=6;
	private static final int CREATE=7;
	private static final int EXTEND=8;
	private static final int INFO=9;
	private static final int CHANGE=10;
	private static final int[]all=new int[] {OK,UNDO,MOVE,REMOVE,ADD,SWITCH,CREATE,EXTEND,INFO,CHANGE};
		
	private Timer t;
	private JFrame frame;
	private JButton buttonOk=new JButton("OK");
	private JButton buttonUndo=new JButton("Undo last action");
	private JButton buttonMove=new JButton("Move a point");
	private JButton buttonRemove=new JButton("Remove a point");
	private JButton buttonAdd=new JButton("Add a middle point");
	private JButton buttonSwitch=new JButton("Switch a false cross");
	private JButton buttonCreate=new JButton("Create a new branch");
	private JButton buttonExtend=new JButton("Extend a branch");
	private JButton buttonInfo=new JButton("Information about a node and a branch");
	private JButton buttonChange=new JButton("Change time");
	
	private int screenWidth;
	private JTextArea logArea=new JTextArea("", 11,10);
	private JPanel buttonsPanel;
	private JPanel panelGlobal;
	private boolean okClicked;
	private int zoomFactor=2;
	private double USER_PRECISION_ON_CLICK=20;
	private int count=0;
	private String modelDir;

	/* Helpers of the Gui ************************************************************************************/	

	
	

	/* Callbacks  ********************************************************************************************/
	public void handleKeyPress(KeyEvent e) {
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() {	
		if(e.getKeyChar()=='q' && buttonOk.isEnabled())   {disable(OK);pointStart();actionOkClicked();return; }
			}
		});
	}
	
	public void actionPerformed(ActionEvent e) {
		System.out.println("Got an event : "+e);
		
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() {	
				if(e.getSource()==buttonOk && buttonOk.isEnabled())   {disable(OK);pointStart();actionOkClicked();return; }
				if(e.getSource()==buttonUndo && buttonUndo.isEnabled()) {disable(UNDO);pointStart();actionUndo(); return;     }
				if(e.getSource()==buttonMove && buttonMove.isEnabled()) {disable(MOVE);pointStart();actionMovePoint(); return;     }
				if(e.getSource()==buttonRemove && buttonRemove.isEnabled()) {disable(REMOVE);pointStart();actionRemovePoint(); return;     }
				if(e.getSource()==buttonAdd && buttonAdd.isEnabled()) {disable(ADD);pointStart();actionAddMiddlePoints(); return;     }
				if(e.getSource()==buttonSwitch && buttonSwitch.isEnabled()) {disable(SWITCH);pointStart();actionSwitchPoint(); return;     }
				if(e.getSource()==buttonCreate && buttonCreate.isEnabled()) {disable(CREATE);pointStart();actionCreateBranch(); return;     }
				if(e.getSource()==buttonExtend && buttonExtend.isEnabled()) {disable(EXTEND);pointStart();actionExtendBranch(); return;     }
				if(e.getSource()==buttonInfo && buttonInfo.isEnabled()) {disable(INFO);pointStart();actionInfo(); return;     }
				if(e.getSource()==buttonChange && buttonChange.isEnabled()) {disable(CHANGE);pointStart();actionChangeTime(); return;     }
			}
		});
	}

	public void actionUndo() {
		currentImage.deleteRoi();
		RoiManager.getRoiManager().reset();
		RoiManager.getRoiManager().close();
		addLog("Running action \"Undo !\" ...", -1);
		disable(all);
		String s=getRsmlName();
		if(s.contains(".rsml"))new File(s).delete();
		
		stepOfModel--;
		currentModel=RootModel.RootModelWildReadFromRsml(getRsmlName());
		currentModel.cleanWildRsml() ;
		currentModel.resampleFlyingRoots();
		VitimageUtils.actualizeData(projectRsmlOnImage(currentModel),currentImage);
		addLog("Ok.", 2);
		finishActionAborted();
		disable(UNDO);
	}
	
	public void actionOkClicked() {
		okClicked=true;
	}
	
	public void actionMovePoint() {
		System.out.println("M1");
		boolean did=false;
		addLog("Running action \"Move a point\" ...",-1);
		addLog(" Click on the point to move, then the target destination.",1);
		disable(all);		
		System.out.println("M3");
		Point3d[]tabPt=getAndAdaptCurrentPoints(waitPoints(2));
		System.out.println("M4");
		if(tabPt!=null) {
			System.out.println("M5, len="+tabPt.length);
			movePointInModel(tabPt);
			System.out.println("M7");
			did=true;
		}
		System.out.println("M8");
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();
		System.out.println("M9");
	}
	
	public void actionRemovePoint() {
		System.out.println("Rem0");
		addLog("Running action \"Remove point\" ...",-1);
		addLog(" Remove the point and all the children points of the root. Click on a point.",1);
		System.out.println("Rem01");
		Point3d []tabPt=getAndAdaptCurrentPoints(waitPoints(1));
		System.out.println("Rem02");
		boolean did=false;
		if(tabPt!=null) {
			System.out.println("Rem2");
			did=removePointInModel(tabPt);
		}
		System.out.println("Rem5");
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();
		System.out.println("Rem7");
		
	}
	
	public void actionAddMiddlePoints() {
		boolean did=false;
		addLog("Running action \"Add point\" ...",-1);
		addLog(" Add point. Click on a line, then click on the middle point to add.",1);
		enable(OK);
		waitOkClicked();
		Point3d []tabPt=getAndAdaptCurrentPoints((PointRoi)currentImage.getRoi());
		if(tabPt!=null) did=addMiddlePointsInModel(tabPt);
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();
	}
			
	public void actionSwitchPoint() {
		boolean did=false;
		addLog("Running action \"Switch cross\" ...",-1);
		addLog(" Resolve a X cross. Click on the first point of Root A before cross, and first point of Root B before cross.",1);
		Point3d []tabPt=getAndAdaptCurrentPoints(waitPoints(2));
		if(tabPt!=null) {
			did=switchPointInModel(tabPt);
		}
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();
	}
	
	public void actionExtendBranch() {
		boolean did=false;
		addLog("Running action \"Extend branch\" ...",-1);
		addLog("Click on the extremity of a branch, then draw the line for each following observations.",1);
		enable(OK);
		waitOkClicked();
		Point3d[]tabPt=getAndAdaptCurrentPoints((PointRoi)currentImage.getRoi());
		if(tabPt!=null)did=extendBranchInModel(tabPt);
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();		
	}

	public void actionCreateBranch() {
		boolean did=false;
		addLog("Running action \"Create branch\" ...",-1);
		addLog("Click on the start point of the branch at the emergence time, then draw the line for each following observations.",1);
		enable(OK);
		waitOkClicked();
		Point3d[]tabPt=getAndAdaptCurrentPoints((PointRoi)currentImage.getRoi());
		if(tabPt!=null)did=createBranchInModel(tabPt);
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();		
		
	}

	public void actionInfo() {
		System.out.println("I1");
		boolean did=false;
		addLog("Running action \"Inform about a node and a root\" ...",-1);
		addLog(" Click on the node you want to inspect.",1);
		disable(all);
		System.out.println("I3");
		Point3d[]tabPt=getAndAdaptCurrentPoints(waitPoints(1));
		System.out.println("I4");
		if(tabPt!=null) {
			System.out.println("I5, len="+tabPt.length);
			informAboutPointInModel(tabPt);
			System.out.println("I7");
			did=true;
		}
		System.out.println("I8");
		finishActionAborted();
		System.out.println("I9");
	
	}

	public void actionChangeTime() {
		System.out.println("I1");
		boolean did=false;
		addLog("Running action \"Change time of a node\" ...",-1);
		addLog(" Click on the node you want to change time.",1);
		disable(all);
		System.out.println("I3");
		Point3d[]tabPt=getAndAdaptCurrentPoints(waitPoints(1));
		System.out.println("I4");
		if(tabPt!=null) {
			System.out.println("I5, len="+tabPt.length);
			did=changeTimeInPointInModel(tabPt);
			System.out.println("I7");
		}
		System.out.println("I8");
		if(did)finishActionThenGoOnStepSaveRsmlAndUpdateImage();	
		else finishActionAborted();		
	}

	
	
	
	
	/* Corresponding operations on the model *******************************************************************/
	public void movePointInModel(Point3d[]tabPt) {
		Object[]obj=currentModel.getClosestNode(tabPt[0]);
		Node n=(Node) obj[0];
		Root r=(Root) obj[1];
		System.out.println("Moving :\n --> Node "+n+"\n --> Of root "+r );
		n.x=(float) tabPt[1].x;
		n.y=(float) tabPt[1].y;
		r.updateTiming();
	}
		
	public boolean removePointInModel(Point3d[]tabPt) {
		System.out.println("Rem21");
		Object[]obj=currentModel.getClosestNode(tabPt[0]);
		Node n1=(Node) obj[0];
		Root r1=(Root) obj[1];

		//Identify the first parent to stay
		System.out.println("Rem22");
		while(n1.parent!=null && !n1.parent.hasExactBirthTime()) {
			n1=n1.parent;
		}
		
		System.out.println("Removing :\n --> Node "+n1+"\n --> Of root "+r1 );
		//Case where we remove a part of a primary
		System.out.println("Rem23");
		if(r1.childList !=null && r1.childList.size()>1) {
			for(Root rChi:r1.childList) {
				Node n=rChi.getNodeOfParentJustAfterMyAttachment();
				if(n1.isParentOrEqual(n)) {
					System.out.println("But first, removing :\n --> Node "+n1+"\n --> Of root "+r1 );
					removePointInModel(new Point3d[] {new Point3d(rChi.firstNode.x,rChi.firstNode.y,rChi.firstNode.birthTime)});
				}
			}
		}
		
		System.out.println("Rem24");
		//Case where we remove a tip
		if(n1!=r1.firstNode) {
			r1.lastNode=n1.parent;
			r1.lastNode.child=null;
			r1.updateTiming();
			return true;
		}
		else {//Removing a full root
			System.out.println("Rem25");
			if(r1.getParent()!=null) {		//Case where we remove the first point of a lateral root
				ArrayList<Root>childs=r1.getParent().childList;
				ArrayList<Root>newList=new ArrayList<Root>();
				for(Root r: childs)if(r!=r1)newList.add(r);
				r1.getParent().childList=newList;
				System.out.println("Removing from childlist "+r1);
			}
			//General case : a lateral or a primary
			System.out.println("Rem26");
			ArrayList<Root> newList=new ArrayList<Root>();
			System.out.println("Removing from rootList "+r1);
			for(Root r: currentModel.rootList)if(r!=r1)newList.add(r);
			currentModel.rootList=newList;
			System.out.println("Rem27");
			return true;
		}
	}
	
	public boolean addMiddlePointsInModel(Point3d[]tabPts) {
		System.out.println("H1");
		if(tabPts==null || tabPts.length<2) {
			IJ.showMessage("This action needs you to click on 1) the line to change and 2) the point to add. Abort");
			return false;
		}
		
		System.out.println("H2");
		Object []obj=currentModel.getNearestRootSegment(tabPts[0],USER_PRECISION_ON_CLICK);
		if(obj[0]==null) {
			IJ.showMessage("Please click better, we have not found the corresponding segment");
			return false;
		}
		System.out.println("H3");
		Node nParent=(Node)obj[0];
		Root rParent=(Root)obj[1];
		Node nChild=nParent.child;
		System.out.println("Adding nodes in segment :\n --> Node 1 "+nParent+"\n --> Node 2 "+nChild+"\n --> Of root "+rParent );

		for(int i=1;i<tabPts.length;i++) {
			Node nPlus=new Node((float)tabPts[i].x,(float)tabPts[i].y, nParent, true);
			nPlus.birthTime=0.5f;
			nParent.child=nPlus; nPlus.parent=nParent;
			nParent=nPlus;			
		}
		nParent.child=nChild;
		nChild.parent=nParent;		
		rParent.updateTiming();
		return true;
	}

	public boolean switchPointInModel(Point3d[]tabPt) {
		Object[]obj1=currentModel.getClosestNode(tabPt[0]);
		Object[]obj2=currentModel.getClosestNode(tabPt[1]);
		Node n1=(Node) obj1[0];
		Root r1=(Root) obj1[1];
		Node n2=(Node) obj2[0];
		Root r2=(Root) obj2[1];

		boolean isFeasible=true;
		if(n1.parent.birthTime>=n2.birthTime)isFeasible=false;
		if(n2.parent.birthTime>=n1.birthTime)isFeasible=false;
		if(n1.child.birthTime<=n2.birthTime)isFeasible=false;
		if(n2.child.birthTime<=n1.birthTime)isFeasible=false;
		System.out.println("Trying to switch :\n --> Node "+n1+"\n and node n2 "+n2 );

		if(!isFeasible) {
			IJ.showMessage("This switch is not possible");
			return false;
		}
		Node par1=n1.parent; Node chi1=n1.child;
		n1.parent=n2.parent; n1.child=n2.child;
		n2.parent=par1;		 n2.child=chi1;
		r1.resampleFlyingPoints();r1.updateTiming();
		r2.resampleFlyingPoints();r2.updateTiming();
		return true;
	}
	
	public boolean createBranchInModel(Point3d[]tabPt) {
		System.out.println("Create 1");
		if(tabPt.length<2)return false;
		int N=tabPt.length;
		Object[]obj=currentModel.getClosestNodeInPrimary(tabPt[0]);
		Node n=(Node) obj[0];
		Root r=(Root) obj[1];
		System.out.println("Create 2");
		System.out.println("Creating branch from :\n --> Node "+n+"\n --> Of root "+r );
		boolean[]extremity=new boolean[N];
		for(int i=0;i<N-1;i++) {
			if(tabPt[i+1].z-tabPt[i].z <0) {IJ.showMessage("You gave point in reverse time order. Abort.");return false;}
			if(tabPt[i+1].z-tabPt[i].z >1) {IJ.showMessage("You gave points that does not follow in time, you missed a slice. Abort.");return false;}
			if(tabPt[i+1].z-tabPt[i].z ==1) {extremity[i]=true;}
		}
		extremity[N-1]=true;
		System.out.println("Create 3");

		n=new Node((float)tabPt[0].x,(float)tabPt[0].y, null, false);
		n.birthTime=(float) tabPt[0].z;
		Node firstNode=n;
		Node nPar=n;
		int incr=1;
		System.out.println("Create 3");
		while(incr<N) {
			Node nn=new Node((float)tabPt[incr].x,(float)tabPt[incr].y,nPar,true);
			nn.parent=nPar;nPar.child=nn;
			if(extremity[incr])nn.birthTime=(float) tabPt[incr].z;
			else               nn.birthTime=(float) (tabPt[incr].z-0.5);
			System.out.println("\nJust added the node "+nn+" \n  with parent="+nPar);
			incr++;
			nPar=nn;
		}
		System.out.println("Create 4");//542 708
		
		
		Root rNew=new Root(null, currentModel,"",2);
		System.out.println("Create 41");
		rNew.firstNode=firstNode;
		System.out.println("Create 42");
		rNew.lastNode=nPar;		
		rNew.updateTiming();
		System.out.println("Create 43");
		System.out.println(rNew);
		System.out.println(r);
		System.out.println("Create 44");
		r.attachChild(rNew);
		System.out.println("Create 44");
		rNew.attachParent(r);
		System.out.println("Create 45");
		currentModel.rootList.add(rNew);
		System.out.println("Create 5");

		return true;		
	}

	public boolean extendBranchInModel(Point3d[]tabPt) {
		if(tabPt.length<2)return false;
		int N=tabPt.length;
		Object[]obj=currentModel.getClosestNode(tabPt[0]);
		Node n=(Node) obj[0];
		Root r=(Root) obj[1];
		System.out.println("Extending branch from :\n --> Node "+n+"\n --> Of root "+r );
		boolean[]extremity=new boolean[N];
		if(n!=r.lastNode) {			  IJ.showMessage("Please select the last point of the branch you want to extend. Abort.");return false;		}
		if(n.birthTime!=tabPt[0].z) { IJ.showMessage("Please be wise : when selecting extremity, be on the right slice. Abort.");return false;}
		for(int i=0;i<N-1;i++) {
			if(tabPt[i+1].z-tabPt[i].z <0) {IJ.showMessage("You gave point in reverse time order. Abort.");return false;}
			if(tabPt[i+1].z-tabPt[i].z >1) {IJ.showMessage("You gave points that does not follow in time, you missed a slice. Abort.");return false;}
			if(tabPt[i+1].z-tabPt[i].z ==1) {extremity[i]=true;}
		}
		extremity[N-1]=true;

		int incr=1;
		Node nPar=n;
		while(incr<N) {
			Node nn=new Node((float)tabPt[incr].x,(float)tabPt[incr].y,nPar,true);
			nn.parent=nPar;nPar.child=nn;
			if(extremity[incr])nn.birthTime=(float) tabPt[incr].z;
			else               nn.birthTime=(float) (tabPt[incr].z-0.5);
			System.out.println("\nJust added the node "+nn+" \n  with parent="+nPar);
			incr++;
			nPar=nn;
		}
		r.lastNode=nPar;		
		r.updateTiming();
		return true;
	}

	public void informAboutPointInModel(Point3d[]tabPt) {
		Object[]obj=currentModel.getClosestNode(tabPt[0]);
		Node n=(Node) obj[0];
		Root r=(Root) obj[1];
		IJ.showMessage("Informations at coordinates "+tabPt[0]+" :\n --> Node "+n+"\n --> Of root "+r );
	}
		
	public boolean changeTimeInPointInModel(Point3d[]tabPt) {
		Object[]obj=currentModel.getClosestNode(tabPt[0]);
		Node n=(Node) obj[0];
		Root r=(Root) obj[1];
		if(n==null)return false;
		double tt=VitiDialogs.getDoubleUI("New time", "time", n.birthTime);
		n.birthTime=(float) tt;
		r.resampleFlyingPoints();
		r.updateTiming();
		return true;
	}
	
	
	
	/* Helpers for starting and finishing actions *******************************************************************/
	public void finishActionAborted(){	
		IJ.setTool("hand");
		addLog(" action aborted.",2);
		enable(all);
		disable(OK);		
	}
	
	public void finishActionThenGoOnStepSaveRsmlAndUpdateImage(){
		IJ.setTool("hand");
		addLog(" action ok.",2);
		addLog("Updating image...", 0);
		stepOfModel++;
		this.currentModel.writeRSML3D(getRsmlName(), "",true,false);
		VitimageUtils.actualizeData(projectRsmlOnImage(currentModel),currentImage);
		addLog("Ok.", 2);
		enable(all);
		disable(OK);		
	}
	
	public void pointStart() {
		disable(all);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("multipoint");
	}

	public Point3d[]getAndAdaptCurrentPoints(PointRoi pr){
		if(pr==null) {
			currentImage.deleteRoi();
			RoiManager.getRoiManager().reset();
			RoiManager.getRoiManager().close();
			return null;
		}
		Point[]tab2D=pr.getContainedPoints();
		Point3d[]tabPt=new Point3d[tab2D.length];
		for(int i=0;i<tabPt.length;i++) {
			tabPt[i]=new Point3d(tab2D[i].x/zoomFactor,tab2D[i].y/zoomFactor,pr.getPointPosition(i));
			System.out.println("Processed point "+i+": "+tabPt[i]);
		}
		currentImage.deleteRoi();
		RoiManager.getRoiManager().reset();
		RoiManager.getRoiManager().close();
		return tabPt;
	}
	
	ImagePlus projectRsmlOnImage(RootModel rm) {
		Timer t=new Timer();
		for(int i=0;i<Nt;i++) {
			ImagePlus imgRSML=rm.createGrayScaleImageWithTime(imgInitSize,zoomFactor,false,(i+1),true,new boolean[] {true,true,true,false,true},new double[] {2,2});
			imgRSML.setDisplayRange(0, Nt+3);
			tabRes[i]=RGBStackMerge.mergeChannels(new ImagePlus[] {tabReg[i],imgRSML}, true);
			IJ.run(tabRes[i],"RGB Color","");
		}
		t.print("Updating root model took : ");
		ImagePlus res=VitimageUtils.slicesToStack(tabRes);
		String nom="Model_at_step_"+(stepOfModel<1000 ? "0":"")+(stepOfModel<100 ? "0":"")+(stepOfModel<10 ? "0":"") +stepOfModel;
		res.setTitle(nom);		
		return res;
	}
 	


	
	
	
	
	/* Setup of plugin and GUI ************************************************************************************/
	public void startPlugin(String arg) {
		t=new Timer();
		if(arg!=null && arg.length()>0)dataDir=arg;
		else dataDir=VitiDialogs.chooseDirectoryUI("Choose a boite directory", "Ok");
		if(!verifyDataDir())return;
		t.mark();
		setupImageAndRsml();
		addLog(t.gather("Setup image and rsml took : "),0);
		this.currentImage.show();
		t.mark();
		startGui();
		welcomeAndInformAboutComputerCapabilities();
		IJ.setTool("hand");
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( 
	            new KeyEventDispatcher()  {  
	                public boolean dispatchKeyEvent(KeyEvent e){
	                    if(e.getID() == KeyEvent.KEY_PRESSED){
	                        handleKeyPress(e);
	                    }
	                    return false;
	                }  
	        });	 
	}

	public void startGui() {
		this.screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		setupButtons();
		if(this.screenWidth>1920)this.screenWidth/=2;
		frame=new JFrame();
		JPanel consolePanel=new JPanel();
		consolePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		consolePanel.setLayout(new GridLayout(1,1,0,0));
		logArea.setSize(300 ,80);
		logArea.setBackground(new Color(10,10,10));
		logArea.setForeground(new Color(245,255,245));
		logArea.setFont(new Font(Font.DIALOG,Font.PLAIN,14));
		JScrollPane jscroll=new JScrollPane(logArea);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setEditable(false);	
        
               
		frame=new JFrame();
		panelGlobal=new JPanel();
		frame.setSize(600,680);
		panelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		panelGlobal.setLayout(new BoxLayout(panelGlobal, BoxLayout.Y_AXIS));
		panelGlobal.add(new JSeparator());
		panelGlobal.add(jscroll);
		panelGlobal.add(new JSeparator());
		panelGlobal.add(buttonsPanel);
		panelGlobal.add(new JSeparator());
		frame.add(panelGlobal);
		frame.setTitle("RSML Expert");
		frame.pack();
		frame.setSize(600,680);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   IJ.showMessage("See you next time !");
                   frame.setVisible(false);
                   closeAllViews();
               }
		});
		frame.setVisible(true);
		frame.repaint();
		VitimageUtils.adjustFrameOnScreen(frame,2,0);		
		logArea.setVisible(true);
		logArea.repaint();
		this.addLog("Starting Rsml Expert interface",0);
		enable(all);		
		disable(OK);
		disable(UNDO);
	}
	
	public void setupButtons(){
		buttonsPanel=new JPanel();
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
		buttonsPanel.setLayout(new GridLayout(5,2,40,40));
		buttonUndo.addActionListener(this);buttonUndo.setToolTipText("<html><p width=\"500\">" +"Undo last action"+"</p></html>");
		buttonOk.addActionListener(this);buttonOk.setToolTipText("<html><p width=\"500\">" +"Click here to validate current points"+"</p></html>");
		buttonMove.addActionListener(this);buttonMove.setToolTipText("<html><p width=\"500\">" +"Change the position of a point"+"</p></html>");
		buttonRemove.addActionListener(this);buttonRemove.setToolTipText("<html><p width=\"500\">" +"Remove a point"+"</p></html>");
		buttonAdd.addActionListener(this);buttonAdd.setToolTipText("<html><p width=\"500\">" +"Add a new point"+"</p></html>");
		buttonSwitch.addActionListener(this);buttonSwitch.setToolTipText("<html><p width=\"500\">" +"Switch two crossing branches"+"</p></html>");
		buttonExtend.addActionListener(this);buttonExtend.setToolTipText("<html><p width=\"500\">" +"Extend an existing branch"+"</p></html>");
		buttonCreate.addActionListener(this);buttonCreate.setToolTipText("<html><p width=\"500\">" +"Create a new branch"+"</p></html>");
		buttonInfo.addActionListener(this);buttonInfo.setToolTipText("<html><p width=\"500\">" +"Inform about a node and its root"+"</p></html>");
		buttonChange.addActionListener(this);buttonChange.setToolTipText("<html><p width=\"500\">" +"Change time of a node"+"</p></html>");
		buttonsPanel.add(buttonOk);
		buttonsPanel.add(buttonUndo);
		buttonsPanel.add(buttonMove);
		buttonsPanel.add(buttonRemove);
		buttonsPanel.add(buttonAdd);
		buttonsPanel.add(buttonSwitch);
		buttonsPanel.add(buttonExtend);
		buttonsPanel.add(buttonCreate);
		buttonsPanel.add(buttonInfo);
		buttonsPanel.add(buttonChange);
	}

	public boolean verifyDataDir() {
		if(dataDir.contains("Expertized"))dataDir=new File(dataDir).getParent();
		if(!new File(dataDir,"1_5_RegisteredSequence.tif").exists()) {
			IJ.showMessage("Something was not found in the given dataDir : "+dataDir+" \n File expected 1_5_RegisteredSequence.tif");
			return false;
		}
		if(!new File(dataDir,"4_2_Model.rsml").exists()) {
			IJ.showMessage("Something was not found in the given dataDir : "+dataDir+" \n File expected 4_2_Model.rsml");
			return false;
		}
		modelDir=new File(dataDir,"Expertized_models").getAbsolutePath();
		if(!new File(dataDir,"Expertized_models").exists()) {
			new File(modelDir).mkdirs();
			Path source=FileSystems.getDefault().getPath(new File(dataDir,"4_2_Model.rsml").getAbsolutePath());
			Path target=FileSystems.getDefault().getPath(new File(modelDir,"4_2_Model_0000.rsml").getAbsolutePath());
			try {Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);} catch (IOException e) {e.printStackTrace();}
		}
		return true;
	}
	
	public void setupImageAndRsml() {
		registeredStack=IJ.openImage(new File(dataDir,"1_5_RegisteredSequence.tif").getAbsolutePath());
		chooseModelNumber();
		sr=new FSR();
		sr.initialize();
		currentModel=RootModel.RootModelWildReadFromRsml(getRsmlName());
		System.out.println(currentModel.cleanWildRsml()) ;
		System.out.println(currentModel.resampleFlyingRoots());
		tabReg=VitimageUtils.stackToSlices(registeredStack);
		imgInitSize=tabReg[0].duplicate();
		Nt=tabReg.length;
		tabRes=new ImagePlus[Nt];
		for(int i=0;i<tabReg.length;i++) 	tabReg[i]=VitimageUtils.resize(tabReg[i], tabReg[i].getWidth()*zoomFactor, tabReg[i].getHeight()*zoomFactor, 1);
		currentImage=projectRsmlOnImage(currentModel);
		currentImage.show();
	}

	public void chooseModelNumber() {
		String[]models=new File(modelDir).list();
		stepOfModel=-1;for(String mod : models)if(mod.contains(".rsml"))stepOfModel++;
	}
	

	
	
	public void waitOkClicked() {	
		while(!okClicked) {
			VitimageUtils.waitFor(100);
		}
		okClicked=false;
	}

	public PointRoi waitPoints(int nbExpected) {	
		Roi r=null;PointRoi pr=null;
		while(count!=nbExpected) {
			VitimageUtils.waitFor(100);
			r=currentImage.getRoi();
			if(r!=null) {
				pr=((PointRoi)r);
				count=pr.getContainedPoints().length;
			}
		}
		count=0;
		return pr;
	}

	public void addLog(String t,int level) {
		if(level==-1)logArea.append("\n\n > "+t);
		if(level==0)logArea.append("\n > "+t);
		if(level==1)logArea.append("\n "+t);
		if(level==2)logArea.append(" "+t);
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	public void keyPressed(KeyEvent arg0) {
	}

	public void keyReleased(KeyEvent arg0) {
	}

	public void keyTyped(KeyEvent arg0) {
	}

	public void closeAllViews() {
		if(currentImage!=null)currentImage.close();
		if (RoiManager.getInstance()!=null)RoiManager.getInstance().close();
	}	

	
	public void enable(int but) {enable(new int[] {but});}

	public void disable(int but) {disable(new int[] {but});}	

	public void enable(int[]tabBut) {		setState(tabBut,true);	}

	public void disable(int[]tabBut) {		setState(tabBut,false);	}

	public void setState(int[]tabBut,boolean state) {
		for(int but:tabBut) {
			switch(but) {
			case OK:this.buttonOk.setEnabled(state);break;
			case CREATE:this.buttonCreate.setEnabled(state);break;
			case EXTEND:this.buttonExtend.setEnabled(state);break;
			case ADD:this.buttonAdd.setEnabled(state);break;
			case REMOVE:this.buttonRemove.setEnabled(state);break;
			case MOVE:this.buttonMove.setEnabled(state);break;
			case SWITCH:this.buttonSwitch.setEnabled(state);break;
			case UNDO:this.buttonUndo.setEnabled(state);break;
			case INFO:this.buttonInfo.setEnabled(state);break;
			case CHANGE:this.buttonChange.setEnabled(state);break;
			}	
		}	
	}

	public String []checkComputerCapacity(boolean verbose) {
		int nbCpu=Runtime.getRuntime().availableProcessors();
		int jvmMemory=(int)((new Memory().maxMemory() /(1024*1024)));//Java virtual machine available memory (in Megabytes)
		long memoryFullSize=0;
		String []str=new String[] {"",""};
		try {
			memoryFullSize = ( ((com.sun.management.OperatingSystemMXBean) ManagementFactory
		        .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() )/(1024*1024);
		}	catch(Exception e) {return str;}		

		str[0]="Welcome to RSML Expert ";
		str[1]="System check. Available memory in JVM="+jvmMemory+" MB over "+memoryFullSize+" MB. #Available processor cores="+nbCpu+".";
		if(verbose)		return str;
		else return new String[] {"",""};
	}

	public void welcomeAndInformAboutComputerCapabilities() {		
		String[]str=checkComputerCapacity(true);
		addLog(str[0],0);
		addLog(str[1],0);		
	}

	public String getRsmlName() {
		return (  new File(modelDir,"4_2_Model_"+ (stepOfModel<1000 ? ("0") : "" ) + (stepOfModel<100 ? ("0") : "" ) + (stepOfModel<10 ? ("0") : "" ) + stepOfModel+".rsml" ).getAbsolutePath());
	}

}
