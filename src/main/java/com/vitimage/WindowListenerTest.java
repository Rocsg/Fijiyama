package com.vitimage;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class WindowListenerTest{
  public static void main (String[] args){
    //création d'une JFrame de test
    final JFrame taJFrame = new JFrame("Test WindowListener");
    taJFrame.setSize(300, 300);
    taJFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    //Ca s'est pour éviter que la fenêtre se ferme même si on clique sur "Non"
 
    //Définition de l'écouteur à l'aide d'une classe interne anonyme
    taJFrame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   int reponse = JOptionPane.showConfirmDialog(taJFrame,
                                        "Voulez-vous quitter l'application",
                                        "Confirmation",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE);
                   if (reponse==JOptionPane.YES_OPTION){
                           taJFrame.dispose();
                   }
             }
    });
    taJFrame.setVisible(true);
  }
}