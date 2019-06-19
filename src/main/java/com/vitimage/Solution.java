package com.vitimage;
import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import com.sun.jmx.remote.internal.ArrayQueue;

public class Solution {



    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        String[] nD_latD_long = scanner.nextLine().split(" ");

        int n = Integer.parseInt(nD_latD_long[0]);

        int d_lat = Integer.parseInt(nD_latD_long[1]);

        int d_long = Integer.parseInt(nD_latD_long[2]);

        int []tabLat=new int[n];
        int []tabLong=new int[n];
        int []tabHeight=new int[n];
        int []tabPoints=new int[n];

        for (int nItr = 0; nItr < n; nItr++) {
            String[] latitudeLongitude = scanner.nextLine().split(" ");
            tabLat[nItr] = Integer.parseInt(latitudeLongitude[0]);
            tabLong[nItr] = Integer.parseInt(latitudeLongitude[1]);
            tabHeight[nItr] = Integer.parseInt(latitudeLongitude[2]);
            tabPoints[nItr] = Integer.parseInt(latitudeLongitude[3]);
        }
        scanner.close();
        solveWash(tabLat,tabLong,tabHeight,tabPoints,d_lat,d_long);
    }
     
    
    
    public static void solveWash(int[]tabLat,int[]tabLong,int[]tabHeight,int[]tabPoints,int d_lat,int d_long) {
        int scoreMax=-1000000000;
        int n=tabLat.length;        
        //System.out.println(new Date().getTime());
        ArrayList<Integer> []graphe=new ArrayList[n];
        double hQart=0;
        double hDec=0;
        double hCent=0;
        double hMill=0;
        double hMin=100000000;
        double hMax=-10000000;
        //Construire le graphe des relations
        int nbTot=0;
        for(int i=0;i<n;i++) {
            graphe[i]=new ArrayList<Integer>();
            if(hMin>tabHeight[i])hMin=tabHeight[i];
            if(hMax<tabHeight[i])hMax=tabHeight[i];
            hQart=hMin+(hMax-hMin)/4.0;
            hDec=hMin+(hMax-hMin)/10.0;
            hCent=hMin+(hMax-hMin)/100.0;
            hMill=hMin+(hMax-hMin)/1000.0;
            for(int j=0;j<n;j++) {
            	if(tabHeight[j]>tabHeight[i] && (Math.abs(tabLat[j]-tabLat[i]) <=d_lat) && (Math.abs(tabLong[j]-tabLong[i]) <=d_long)) {
            		graphe[i].add(j);
            		nbTot++;
            	}
            }
        }
        //Pour chaque destination de départ possible
        for(int citDep=0;citDep<n;citDep++) {
        	if(tabHeight[citDep]>hCent)continue;
        	System.out.println("Cité "+citDep+"    -   "+new Date().getTime());
            //Construire le graphe orienté : chaque ville y voit ses possibles suivantes
            int[]cityScore=new int[n];          
            for(int i=0;i<n;i++) {
                cityScore[i]=-100000000;
            }
            //System.out.println("OK 1"+"    -   "+new Date().getTime());
            //Innonder la ville de départ et iterer l'innondation tant que mouvement
            cityScore[citDep]=tabPoints[citDep];           
            int moves=1;
            int iter=0;
            while(moves>0) {
            	//System.out.println("   it "+iter+" moves="+moves+"   -   "+new Date().getTime());
            	iter++;
                moves=0;
              
                for(int i=0;i<n;i++) {
                    for(int fol=0;fol<graphe[i].size();fol++) {
                        int indFol=graphe[i].get(fol);
                        if(cityScore[indFol]<cityScore[i]+tabPoints[indFol]) {
                            cityScore[indFol]=cityScore[i]+tabPoints[indFol];
                            moves++;
                        }
                    }
                }                
            }
                       // System.out.println("OK 2"+"    -   "+new Date().getTime());
            
            //Relever la valeur maximale
            int scoreCur=-10000000;
            for(int i=0;i<n;i++)if(cityScore[i]>scoreCur)scoreCur=cityScore[i];
            
            //Actualisation
            if(scoreCur>scoreMax)scoreMax=scoreCur;
        }
        System.out.println(scoreMax);
    }
}
    