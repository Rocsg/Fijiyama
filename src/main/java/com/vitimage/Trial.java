package com.vitimage;

import java.util.Scanner;

public class Trial {

    public static void main( String[] argv ) throws Exception {
    	
    	
	public func
    	String  line;
    	String str="";
    	Scanner sc = new Scanner(System.in);
        int N=Integer.parseInt(sc.nextLine());
        char [][]cases=new char[N][N];
        char piece='O';
        char mult='*';
        char rien='.';
        int nbPi=0;
        int nbMult=0;
            System.out.println();
    	for(int i=0;i<N;i++){
    		line=sc.nextLine();
            for(int j=0;j<N;j++){
                cases[i][j]=line.charAt(j);
                System.out.print(cases[i][j]);
                if(cases[i][j]==piece)nbPi++;
                if(cases[i][j]==mult)nbMult++;
            }
            System.out.println();
    	}
        int[][]coordsMult=new int[nbMult][2];
        int[][]coordsPi=new int[nbPi][2];
        int incrMult=0;
        int incrPi=0;
    	for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                if(cases[i][j]==piece){coordsPi[incrPi][0]=i;coordsPi[incrPi][1]=j;incrPi++;}
                if(cases[i][j]==mult){coordsMult[incrMult][0]=i;coordsMult[incrMult][1]=j;incrMult++;}
            }
    	}
    
    
        String ret="";
        int x=0;int y=0;
        for(int indM=0;indM<nbPi;indM++){
            int dx=coordsPi[indM][0]-x;            
            int dy=coordsPi[indM][0]-y;            
            System.out.println("Traitement piece "+indM+"/"+nbPi+" aux coordonnees "+dx+" , "+dy);
            int dxabs=dx<0 ? -dx : dx;
            int dyabs=dy<0 ? -dy : dy;
            boolean sensX=(dx<0);
            boolean sensY=(dy<0);
            for(int s=0;s<dxabs;s++)ret+=(sensX ? ">" : "<");
            for(int s=0;s<dyabs;s++)ret+=(sensY ? "v" : "^");
            ret+="x";
            x+=dx;
            y+=dy;
        }
        for(int indM=0;indM<nbMult;indM++){
            int dx=coordsMult[indM][0]-x;            
            int dy=coordsMult[indM][0]-y;            
           System.out.println("Traitement mult "+indM+"/"+nbMult+" aux coordonnees "+dx+" , "+dy);
            int dxabs=dx<0 ? -dx : dx;
            int dyabs=dy<0 ? -dy : dy;
            boolean sensX=(dx<0);
            boolean sensY=(dy<0);
            for(int s=0;s<dxabs;s++)ret+=(sensX ? ">" : "<");
            for(int s=0;s<dyabs;s++)ret+=(sensY ? "v" : "^");
            ret+="x";
            x+=dx;
            y+=dy;
        }
        System.out.println(ret);
    }
}


}
