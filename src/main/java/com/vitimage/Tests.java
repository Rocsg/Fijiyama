package com.vitimage;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Tests{


	protected JTextArea inputField, outputField;
	JFrame fra;
	JPanel pan;
	
	public static void main(String[]args) {
		Tests t=new Tests();
	}
	
	public Tests(){
		 fra=new JFrame();
		 pan=new JPanel(new BorderLayout());
	    inputField = new JTextArea(5, 20);
	    outputField = new JTextArea(2, 20);
	    inputField.addFocusListener(new FocusAdapter() {
	        @Override
	        public void focusLost(FocusEvent e) {
	            actionPerformed(new ActionEvent(e.getSource(), e.getID(), "focusLost"));
	        }
	    });inputField.setEditable(false);
	    JScrollPane scroller2 = new JScrollPane(inputField);
	    JScrollPane scroller1 = new JScrollPane(outputField);

	    pan.add(scroller1, BorderLayout.WEST);
	    pan.add(scroller2, BorderLayout.EAST);
	    fra.add(pan);
	    fra.pack();
	    fra.setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
	    String text = inputField.getText();
	    System.out.println("Pouet");
	}
}