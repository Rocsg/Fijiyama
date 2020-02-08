package com.vitimage.factory;

/*
 * This example is from the book "Java Foundation Classes in a Nutshell".
 * Written by David Flanagan. Copyright (c) 1999 by O'Reilly & Associates.  
 * You may distribute this source code for non-commercial purposes only.
 * You may study, modify, and use this example for any purpose, as long as
 * this notice is retained.  Note that this example is provided "as is",
 * WITHOUT WARRANTY of any kind either expressed or implied.
 */

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.vitimage.common.VitimageUtils.AcquisitionType;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Date;

public class FileTreeDemo {
  public static void main(String[] args) {
    // Figure out where in the filesystem to start displaying
    IntelligentFile root;
    if (args.length > 0) root = new IntelligentFile(args[0]);
    else root = new IntelligentFile("/home/fernandr/Bureau/Test");

    // Create a TreeModel object to represent our tree of files
    IntelligentFileTreeModel model = new IntelligentFileTreeModel(root);
    model.collectInformationsByBFS(new String[] {"TR","TE"});
    //model.BFS();
   //model.collectInformationsAbout("B001");
    // Create a JTree and tell it to display our model
    JTree tree = new JTree();
    tree.setModel(model);
    

    // The JTree can get big, so allow it to scroll.
    JScrollPane scrollpane = new JScrollPane(tree);
    
    /* Display it all in a window and make the window appear
    JFrame frame = new JFrame("FileTreeDemo");
    frame.getContentPane().add(scrollpane, "Center");
    frame.setSize(400,600);
    frame.setVisible(true);*/
  }
}

/**
 * The methods in this class allow the JTree component to traverse
 * the file system tree, and display the files and directories.
 **/
class IntelligentFileTreeModel implements TreeModel {
  // We specify the root directory when we create the model.
  protected IntelligentFile root;
  public IntelligentFileTreeModel(IntelligentFile root) { this.root = root; }

  // The model knows how to return the root object of the tree
  public Object getRoot() { return root; }

  // Tell JTree whether an object in the tree is a leaf or not
  public boolean isLeaf(Object node) {  return ((IntelligentFile)node).isFile(); }

  // Tell JTree how many children a node has
  public int getChildCount(Object parent) {
    String[] children = ((IntelligentFile)parent).list();
    if (children == null) return 0;
    return children.length;
  }

  // Fetch any numbered child of a node for the JTree.
  // Our model returns File objects for all nodes in the tree.  The
  // JTree displays these by calling the File.toString() method.
  public Object getChild(Object parent, int index) {
    String[] children = ((IntelligentFile)parent).list();
    if ((children == null) || (index >= children.length)) return null;
    return new IntelligentFile((IntelligentFile) parent, children[index]);
  }

  // Figure out a child's position in its parent node.
  public int getIndexOfChild(Object parent, Object child) {
    String[] children = ((IntelligentFile)parent).list();
    if (children == null) return -1;
    String childname = ((IntelligentFile)child).getName();
    for(int i = 0; i < children.length; i++) {
      if (childname.equals(children[i])) return i;
    }
    return -1;
  }

  
  public void BFS() {
	//Appliquer le parcours en profondeur, et afficher la consultation de chacun des noeuds au fur et à mesure
	System.out.println(root.getAbsolutePath());
	ArrayDeque<IntelligentFile>visitor=new ArrayDeque<IntelligentFile>();
	visitor.add(root);

	while(visitor.size()>0) {
		File test=visitor.poll();
		if(test!=null) {
			int nbCh=getChildCount(test);
			if(nbCh>0)System.out.println(test);//System.out.println("new node : "+ test.getAbsolutePath()+"  , with "+nbCh+" children");
			for(int i=0;i<nbCh;i++)visitor.addLast((IntelligentFile)getChild(test,i));
		}
	}
  }
  
  public void collectInformationsByBFS(String []strOfInterest) {
	//Appliquer le parcours en profondeur, et afficher la consultation de chacun des noeuds au fur et à mesure
	System.out.println(root.getAbsolutePath());
	ArrayDeque<IntelligentFile>visitor=new ArrayDeque<IntelligentFile>();
	visitor.add(root);

	//Lors d'un premier parcours en largeur, on detecte les primitives dans chacun des noms intermediaires
	//Si ce sont des dossiers, on se contente d'ajouter la primitive detectee au fichier correspondant, ainsi que les primitives du parent
	//Si ce sont des fichiers, on detecte en plus des primitives dans les metadata.
	
	while(visitor.size()>0) {
		IntelligentFile test=visitor.poll();
		if(test!=null) {
			int nbCh=getChildCount(test);
			test.detectIndexesInName(strOfInterest);
			if(nbCh>0)System.out.println("Visite de : "+test);			
			for(int i=0;i<nbCh;i++) {
				IntelligentFile inteChild=(IntelligentFile)getChild(test,i);
				inteChild.addIndexes(test.getIndexes());
				visitor.addLast(inteChild);
			}
		}
	}

  
	//On effectue ensuite un parcours en profondeur, dans lequel les dossiers se voient attribués les etiquettes communes à leurs enfants
  
  
  }
  // This method is only invoked by the JTree for editable trees.  
  // This TreeModel does not allow editing, so we do not implement 
  // this method.  The JTree editable property is false by default.
  public void valueForPathChanged(TreePath path, Object newvalue) {}

  // Since this is not an editable tree model, we never fire any events,
  // so we don't actually have to keep track of interested listeners.
  public void addTreeModelListener(TreeModelListener l) {}
  public void removeTreeModelListener(TreeModelListener l) {}
}

