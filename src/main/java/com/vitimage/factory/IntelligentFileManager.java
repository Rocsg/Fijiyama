package com.vitimage.factory;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.io.File;





/* SOURCE FOR THE TREE ALGORITHM
 * This example is from the book "Java Foundation Classes in a Nutshell".
 * Written by David Flanagan. Copyright (c) 1999 by O'Reilly & Associates.  
 * You may distribute this source code for non-commercial purposes only.
 * You may study, modify, and use this example for any purpose, as long as
 * this notice is retained.  Note that this example is provided "as is",
 * WITHOUT WARRANTY of any kind either expressed or implied.
 */
public class IntelligentFileManager implements TreeModel{
	  protected File root;

	  public static void main(String[] args) {
	    // Figure out where in the filesystem to start displaying
	    File root;
	    String rootPath="/home/Bureau/Test";
	    String  experience="B001";
	    root = new File(rootPath);

	    // Create a TreeModel object to represent our tree of files
	    IntelligentFileManager model = new IntelligentFileManager(root);

	    // Create a JTree and tell it to display our model
	    JTree tree = new JTree();
	    tree.setModel(model);

	    // The JTree can get big, so allow it to scroll.
	    JScrollPane scrollpane = new JScrollPane(tree);
	    
	    // Display it all in a window and make the window appear
	    JFrame frame = new JFrame("FileTreeDemo");
	    frame.getContentPane().add(scrollpane, "Center");
	    frame.setSize(1200,800);
	    frame.setVisible(true);
	    model.lookForData(experience);
  	}
	
	  
  public void lookForData(String name) {
	  //Construire toute l'arborescence
	  
	  //Parcourir toute l'arborescence, et localiser : 
	  	//tous les fichiers contenant le nom
	  	//tous les dossiers contenant le nom, et leurs sous-dossiers
	  	
	  	//Pour tous les dossiers contenant le nom, identifier des parametres complémentaires
	  
	  
	  
  }
	  
	
  // We specify the root directory when we create the model.
  public IntelligentFileManager(File root) { this.root = root; }

  // The model knows how to return the root object of the tree
  public Object getRoot() { return root; }

  // Tell JTree whether an object in the tree is a leaf or not
  public boolean isLeaf(Object node) {  return ((File)node).isFile(); }

  // Tell JTree how many children a node has
  public int getChildCount(Object parent) {
    String[] children = ((File)parent).list();
    if (children == null) return 0;
    return children.length;
  }

  // Fetch any numbered child of a node for the JTree.
  // Our model returns File objects for all nodes in the tree.  The
  // JTree displays these by calling the File.toString() method.
  public Object getChild(Object parent, int index) {
    String[] children = ((File)parent).list();
    if ((children == null) || (index >= children.length)) return null;
    return new File((File) parent, children[index]);
  }

  // Figure out a child's position in its parent node.
  public int getIndexOfChild(Object parent, Object child) {
    String[] children = ((File)parent).list();
    if (children == null) return -1;
    String childname = ((File)child).getName();
    for(int i = 0; i < children.length; i++) {
      if (childname.equals(children[i])) return i;
    }
    return -1;
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


	           
	
	
