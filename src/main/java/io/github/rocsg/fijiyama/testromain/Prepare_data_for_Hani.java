package io.github.rocsg.fijiyama.testromain;

import java.io.File;

import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;

/* 
Steps expected : 

1-Make inventory by hand in a csv. Copy the corresponding data in a single Unpacked directory.
Make by hand an All_dir_descriptor.csv with all described directories
---------------------------------------------------------------------------------
EXP_CODENAME  | Nb images | Original source | Author | Date | Dirname_in_unpacked
----0--------------1--------------2------------3-------4-------------5-----------

2-Make a tree to verify the dir names

3-Make a function verify_main_dirs that :
    - opens all_dir.csv and for each line
        - List the N1 images in 1_Source.
        - List the N2 roi in 2_CellArea
        - List the N3 roi in 3_CellRoi
        - List the N4 csv in 4_LacunesIndices
        - Verify that the numbers are ok.

3-Make a script verify 2 that :
    - opens all_dir.csv and for each line
        - if the csv already exists, break
        - Create a csv with the EXP_CODENAME.csv
        - List the N1 images in 1_Source.
        - For each image
            - Check availability of rois
            - If available
                - Open the image, draw the aerenchyme, then add the four roi
                - Open a "getyesno" and collect if this image is ok
            - create a csv line : EXP_CODENAME | Image name | TissueRoi | CellRoi | ClickAR | Tissue Roi AI ready | AR AI ready | 
        - write the csv in CSV_descriptors
    
4-Make a script generate database
*/


public class Prepare_data_for_Hani {
    static String mainDir="";
    static String unpackDir=mainDir+"/Unpacked/";
    static String packedDir=mainDir+"/Packed/";
    static String csvDir=mainDir+"/CSV_descriptors/";
    static String csvAllDir=mainDir+"/All_dir_descriptor.csv";

    
    public static void main(String[] args) {
        verify_main_dirs();
    }
    

    public static void verify_main_dirs() {
        /* Organization of the csv 
        ---------------------------------------------------------------------------------
        EXP_CODENAME  | Nb images | Original source | Author | Date | Dirname_in_unpacked
        ----0--------------1--------------2------------3-------4-------------5-----------
        */

        /*Function to do : 
        3-Make a function verify_main_dirs that :
            - opens all_dir.csv and for each line
                - List the N1 images in 1_Source.
                - List the N2 roi in 2_CellArea
                - List the N3 roi in 3_CellRoi
                - List the N4 csv in 4_LacunesIndices
                - Verify that the numbers are ok.
        */
        String[][]tabMainCsv=VitimageUtils.readStringTabFromCsv(csvAllDir);
        int N=tabMainCsv.length;
        for(int i=1;i<N;i++){
            String expCodeName=tabMainCsv[i][0];
            String nbImages=tabMainCsv[i][1];
            String originalSource=tabMainCsv[i][2];
            String author=tabMainCsv[i][3];
            String date=tabMainCsv[i][4];
            String dirName=tabMainCsv[i][5];
            String dirPath=unpackDir+"/"+dirName+"/";
            String sourcePath=dirPath+"1_Source/";
            String cellAreaPath=dirPath+"2_CellArea/";
            String cellRoiPath=dirPath+"3_CellRoi/";
            String lacunesIndicesPath=dirPath+"4_LacunesIndices/";
            String csvPath=dirPath+"5_CSV/";
            int N1=new File(sourcePath).list().length;
            int N2=new File(cellAreaPath).list().length;
            int N3=new File(cellRoiPath).list().length;
            int N4=new File(lacunesIndicesPath).list().length;
            boolean isOk=true;
            if(N2!=(N1*3))isOk=false;
            if(N3!=(N1))isOk=false;
            if(N4!=(N1))isOk=false;
            System.out.println("EXP "+expCodeName+"  Is ok ? "+isOk + " N1="+N1+" N2="+N2+" N3"+N3+" N4"+N4);
        }
    }

    public void verify_images(){
 
        /* 
        - opens all_dir.csv and for each line
        - if the csv already exists, break
        - Create a csv with the EXP_CODENAME.csv
        - List the N1 images in 1_Source.
        - For each image
            - Check availability of rois
            - If available
                - Open the image, draw the aerenchyme, then add the four roi
                - Open a "getyesno" and collect if this image is ok
            - create a csv line : EXP_CODENAME | Image name | TissueRoi | CellRoi | ClickAR | Tissue Roi AI ready | AR AI ready | 
        - write the csv in CSV_descriptors
        */
        String [][]tabMainCsv=VitimageUtils.readStringTabFromCsv(csvAllDir);
        int N=tabMainCsv.length;
        for(int i=1;i<N;i++){
            String targetCsvPath=csvDir+tabMainCsv[i][0]+".csv";
            if(new File(targetCsvPath).exists())continue;
            String expCodeName=tabMainCsv[i][0];
            String nbImages=tabMainCsv[i][1];
            String originalSource=tabMainCsv[i][2];
            String author=tabMainCsv[i][3];
            String date=tabMainCsv[i][4];
            String dirName=tabMainCsv[i][5];
            String dirPath=unpackDir+"/"+dirName+"/";
            String sourceDirPath=dirPath+"1_Source/";
            String cellAreaDirPath=dirPath+"2_CellArea/";
            String cellRoiDirPath=dirPath+"3_CellRoi/";
            String lacunesIndicesDirPath=dirPath+"4_LacunesIndices/";
            String csvPath=dirPath+"5_CSV/";
            String[]imgNames=new File(sourceDirPath).list();
            int nImg=imgNames.length;
            String[][]tabInfoImgs=new String[nImg+1][7];
            for(int j=0;j<imgNames.length;j++){
                String imgName=imgNames[j];
                tabInfoImgs[j+1][0]=expCodeName;
                tabInfoImgs[j+1][1]=imgName;
                String imgPath=sourceDirPath+"/"+imgName;
                String cellAreaPath=cellAreaDirPath+"/"+imgName;//TODO + .zip
                String cellRoiPath=cellRoiDirPath+"/"+imgName;//TODO + .zip
                String lacunesIndicesPath=lacunesIndicesDirPath+"/"+imgName;//TODO + .zip
                if(new File(cellAreaPath).exists())tabInfoImgs[j+1][2]="OK";//TODO check three roi
                if(new File(cellRoiPath).exists())tabInfoImgs[j+1][3]="OK";//TODO
                if(new File(lacunesIndicesPath).exists())tabInfoImgs[j+1][4]="OK";//TODO
                boolean okFiles=tabInfoImgs[j+1][2].equals("OK")&&tabInfoImgs[j+1][3].equals("OK")&&tabInfoImgs[j+1][4].equals("OK");
                if(okFiles){
                    int value=openImageToCheck(imgPath, cellAreaPath, cellRoiPath, lacunesIndicesPath);
                    if(value>1)tabInfoImgs[j+1][5]="OK";
                    if((value%2)==1)tabInfoImgs[j+1][6]="OK";
                }
            }
            VitimageUtils.writeStringTabInCsv(tabInfoImgs, targetCsvPath);
        }
        if(!VitiDialogs.getYesNoUI("Go ahaead ?", "Go ahead ?"))System.exit(0);
    }



    public void generateAllDir(){
        String [][]tabMainCsv=VitimageUtils.readStringTabFromCsv(csvAllDir);
        int N=tabMainCsv.length;
        String[][]tabAllDir=new String[N][2];
        for(int i=1;i<N;i++){
            String expCodeName=tabMainCsv[i][0];
            String nbImages=tabMainCsv[i][1];
            String originalSource=tabMainCsv[i][2];

        }

    }

    public static int openImageToCheck(String imgPath, String cellAreaPath, String cellRoiPath, String lacunesIndicesPath){
        return 1;
    }
}
