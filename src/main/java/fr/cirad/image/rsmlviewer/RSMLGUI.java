package fr.cirad.image.rsmlviewer;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 * 
 * RSML importer interface
 */

import javax.swing.*;

import fr.cirad.image.rsmlviewer.FSR;
import fr.cirad.image.rsmlviewer.RSMLGUI;
import fr.cirad.image.rsmlviewer.RootModel;

import java.awt.*;
import java.awt.event.*;

import ij.*;
import ij.measure.ResultsTable;

import java.io.*;

public class RSMLGUI extends JFrame implements ActionListener {
	private ImagePlus imgSource=null;
	private static final long serialVersionUID = 1L;
	private static RSMLGUI instance = null;
	private JCheckBox batchExport, batchResults, batchImage, batchRealWidth, batchConvex, batchSave;
	private JTextField batchSourceFolder, batchLineWidth;
	private JComboBox  batchJCB, batchColorJCB;
	private JButton batchSourceButton, batchButton;	
	private JTabbedPane tp;
	private JLabel batchLabel1;
	
   	Font font = new Font("Dialog", Font.PLAIN, 12);
	/**
	 * Build the RSML importer interface
	 */
	
   public RSMLGUI() {
	      super("RSML Exporter");
	      int a=1;
	      instance = this;
	      tp = new JTabbedPane();
	      tp.setFont(font);
	      tp.setSize(300, 600);
	      getContentPane().add(tp);
	      tp.addTab("Data transfer", getDataTransfersTab());      
	      	
	      pack();
	      setVisible(true);
      }


   
   
   /**
    * get the Data transfers tab
    * @return
    */
   private JScrollPane getDataTransfersTab(){

	      batchLabel1 = new JLabel("Choose source folder:"); batchLabel1.setFont(font); 
	      
	      batchExport = new JCheckBox("Batch export", true);
	      batchExport.setFont(font);
	      batchExport.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      batchRealWidth = new JCheckBox("Real width", false);
	      batchRealWidth.setFont(font);
	      batchRealWidth.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      batchConvex = new JCheckBox("Draw convexhull", false);
	      batchConvex.setFont(font);
	      batchConvex.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      String[] color = {"Color image", "Black and white"};
	      batchColorJCB = new JComboBox(color);
	      batchColorJCB.setSelectedItem(0);   
	      
	      String[] batchTables = {"Image", "Root", "Node"};
	      batchJCB = new JComboBox(batchTables);
	      batchJCB.setFont(font);
	      batchJCB.setSelectedIndex(0);
	      batchJCB.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      batchLineWidth = new JTextField("1", 5);
	      batchLineWidth.setFont(font);
	      batchLineWidth.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      batchSourceFolder = new JTextField("[Choose folder containing the RSML files]", 25);
	      batchSourceFolder.setFont(font);
	      batchSourceFolder.setAlignmentX(Component.LEFT_ALIGNMENT);
	           
	      batchResults = new JCheckBox("Export results to table", false);
	      batchResults.setFont(font);
	      batchResults.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      
	      batchImage = new JCheckBox("Display images", true);
	      batchImage.setFont(font);
	      batchImage.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      
	      batchSave = new JCheckBox("Save images", false);
	      batchSave.setFont(font);
	      batchSave.setAlignmentX(Component.LEFT_ALIGNMENT);
	      
	      batchButton = new JButton("Run batch export");
	      batchButton.setFont(font);
	      batchButton.setActionCommand("BATCH_EXPORT");
	      batchButton.addActionListener(this);

	      batchSourceButton = new JButton("Choose");
	      batchSourceButton.setFont(font);
	      batchSourceButton.setActionCommand("BATCH_SOURCE_FOLDER");
	      batchSourceButton.addActionListener(this);
	      
	      GridBagLayout trsfGb = new GridBagLayout();

	      JPanel batchPanel = new JPanel();
	      batchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints trsfc3 = new GridBagConstraints();
	      trsfc3.anchor = GridBagConstraints.WEST;
	      batchPanel.setLayout(trsfGb);
	      
	      trsfc3.gridy = 1;
	      trsfc3.gridx = 0;
	      batchPanel.add(batchLabel1, trsfc3);
	      trsfc3.gridx = 1;
	      batchPanel.add(batchSourceFolder, trsfc3);
	      trsfc3.gridx = 2;
	      batchPanel.add(batchSourceButton, trsfc3);     
	      
	      trsfc3.gridy = 3;
	      trsfc3.gridx = 0;
	      batchPanel.add(batchResults, trsfc3);
	      trsfc3.gridx = 1;
	      batchPanel.add(batchJCB, trsfc3);
	      
	      trsfc3.gridy = 4;
	      trsfc3.gridx = 0;
	      batchPanel.add(batchImage, trsfc3);
	      trsfc3.gridx = 1;
	      batchPanel.add(batchColorJCB, trsfc3);
	      trsfc3.gridx = 2;
	      batchPanel.add(batchLineWidth, trsfc3);
	      trsfc3.gridy = 5;
	      trsfc3.gridx = 0;
	      batchPanel.add(batchSave, trsfc3);	      
	      trsfc3.gridx = 2;
	      batchPanel.add(batchRealWidth, trsfc3);
	      trsfc3.gridy = 6;
	      trsfc3.gridx = 2;
	      batchPanel.add(batchConvex, trsfc3);	      
	      
	      JPanel batchPanel2 = new JPanel(new BorderLayout());
	      batchPanel2.setBorder(BorderFactory.createLineBorder(Color.gray));
	      batchPanel2.add(batchPanel, BorderLayout.WEST);
	      
	      JPanel batchPanel3 = new JPanel(new BorderLayout());
	      batchPanel3.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 20));
	      batchPanel3.add(batchPanel2);
	      
	      JPanel batchButtonPanel = new JPanel(new BorderLayout());
	      batchButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
	      batchButtonPanel.add(batchButton, BorderLayout.EAST);
	      
	      
	      JPanel batchPanel4 = new JPanel(new BorderLayout());
	      batchPanel4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      batchPanel4.add(batchPanel3, BorderLayout.CENTER);
	      batchPanel4.add(batchButtonPanel, BorderLayout.SOUTH);
	            
	      // Assemble all
	      
	      JPanel finalPanel1 = new JPanel(new BorderLayout());
	      finalPanel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      finalPanel1.add(batchPanel4, BorderLayout.NORTH);
	      	      
	      
	      JScrollPane finalPanel = new JScrollPane(finalPanel1);

	      return finalPanel;	   
   }
   

   /**
    * Ge the GUI instance
    * @return
    */
   static public RSMLGUI getInstance() {return instance;}   

  
   /**
    * Define the different action possible in the SRWin window
    */
   public void actionPerformed(ActionEvent e) {

      if (e.getActionCommand() == "BATCH_SOURCE_FOLDER") { 
    	  JFileChooser fc = new JFileChooser();

		   fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		   int returnVal = fc.showDialog(RSMLGUI.this, "Choose");
		   
          if (returnVal == JFileChooser.APPROVE_OPTION){ 
        	  String fName = fc.getSelectedFile().toString();
        	  batchSourceFolder.setText(fName);
          }
          else FSR.write("Choose folder cancelled.");     
      }
      
      else if(e.getActionCommand() == "BATCH_EXPORT"){
    	  batchExport();
      }
      
   }

   /**
    * Dispose
    */
   public void dispose() {
      instance = null;
      super.dispose();
      }
   
   
   /**
    * Batch export all the data 
    */   
    private void batchExport(){
    	    	
    	// Retrieve all the rsml files
    	File f = new File(batchSourceFolder.getText());
    	File[] rsml = f.listFiles(new FilenameFilter() {
    		public boolean accept(File directory, String fileName) {
    			return fileName.endsWith(".rsml");
    		}
    	});
  	  	
    	if(rsml.length < 100){
	    	FSR.write("Batch export started for "+rsml.length+" files");
	
	    	// Open the different RSML files, retriev their data and get their size.
	    	RootModel[] models = new RootModel[rsml.length];
	    	int w = 0; int h = 0;
	  	  	for(int i = 0; i < rsml.length; i++){
	  	  		models[i] = new RootModel(rsml[i].getAbsolutePath());
	  	  		if(models[i].getWidth(true) > w) w = models[i].getWidth(true);
	  	  		if(models[i].getHeight(true) > h) h = models[i].getHeight(true);
	  	  	}
	  	  	
	  	  	if(imgSource!=null) {
	  	  		w=imgSource.getWidth();
	  	  		h=imgSource.getHeight();
	  	  	}
	  	  	
	  	  	ResultsTable rt = new ResultsTable();
	  	  	ImageStack is= new ImageStack(w, h);
	  	  
	  	  
	  	  	for(int i = 0; i < rsml.length; i++){
	  	  		// Send the imagr to the stack
	  	  		if(batchImage.isSelected()) {
	  	  			ImagePlus ip = new ImagePlus(rsml[i].getName(),models[i].createImage(batchColorJCB.getSelectedIndex() == 0, Integer.valueOf(batchLineWidth.getText()), batchRealWidth.isSelected(), w, h, batchConvex.isSelected()));  
	  	  			is.addSlice(ip.getProcessor());
	  	  	  		// Save a single image
	  	  	  		if(batchSave.isSelected()){
	  	  	  			IJ.save(ip, this.batchSourceFolder.getText()+"/images/"+rsml[i].getName()+".jpg");
	
	  	  	  		}
	  	  		}
		  	  	if(!batchImage.isSelected() && batchSave.isSelected()){
		  	  		FSR.write(rsml[i].getName());
	  	  			ImagePlus ip = new ImagePlus(rsml[i].getName(),models[i].createImage(batchColorJCB.getSelectedIndex() == 0, Integer.valueOf(batchLineWidth.getText()), batchRealWidth.isSelected(), batchConvex.isSelected()));  
		  	  		IJ.save(ip, this.batchSourceFolder.getText()+"/images/"+rsml[i].getName()+".jpg");
		  	  	}	 	  		
	  	  		
	  	  		// Send the results to the Result Table
	  	  		if(batchResults.isSelected()){
	  	  			int sel = batchJCB.getSelectedIndex();
	  	  			switch (sel) {  
	  	  				case 0: models[i].sendImageData(rt,  rsml[i].getName()); break;
	  	  				case 1: models[i].sendRootData(rt,  rsml[i].getName()); break;
	  	  				case 2: models[i].sendNodeData(rt, rsml[i].getName()); break;
	  	  			}
	  	  		}
	  	  	}
	  	  
	  	  	if(batchResults.isSelected()) rt.show(batchJCB.getSelectedItem().toString()+" data");
	  	  	if(batchImage.isSelected()){
	  	  		ImagePlus ip = new ImagePlus("RSML images");
	  	  		ip.setStack(is);
	  	  		ip.show();
	  	  	}			
    	}
    	else{
	    	FSR.write("Batch export started for "+rsml.length+" files");
	  	  	ResultsTable rt = new ResultsTable();
	    	// Open the different RSML files, retriev their data and get their size.
	    	RootModel model;
	  	  	for(int i = 0; i < rsml.length; i++){
	  	  		model = new RootModel(rsml[i].getAbsolutePath());
		  	  	if(batchSave.isSelected()){
	  	  			ImagePlus ip = new ImagePlus(rsml[i].getName(),model.createImage(batchColorJCB.getSelectedIndex() == 0, Integer.valueOf(batchLineWidth.getText()), batchRealWidth.isSelected(), batchConvex.isSelected()));  
		  	  		IJ.save(ip, this.batchSourceFolder.getText()+"/images/"+rsml[i].getName()+".jpg");
		  	  	}	 	  		
	  	  		
	  	  		// Send the results to the Result Table
	  	  		if(batchResults.isSelected()){
	  	  			int sel = batchJCB.getSelectedIndex();
	  	  			switch (sel) {  
	  	  				case 0: model.sendImageData(rt,  rsml[i].getName()); break;
	  	  				case 1: model.sendRootData(rt,  rsml[i].getName()); break;
	  	  				case 2: model.sendNodeData(rt, rsml[i].getName()); break;
	  	  			}
	  	  		}
	  	  	}
    		rt.show(batchJCB.getSelectedItem().toString());
    	}
    	
  	  	FSR.write("Export done for "+rsml.length+" files"); 
    }      
}

