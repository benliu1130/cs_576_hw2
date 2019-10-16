import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class HW2_weiming_liu {

  public static void main(String[] args) throws IOException {
    
    //assuming the size of all sample images is 256 x 256
    int width = 256;
    int height = 256;
    
    //if the number of parameter is incorrect, print the usage and terminate
    if (args.length != 3){
      System.out.println("Usage: java HW2_weiming_liu file_name n m");
      System.exit(0);
    }
    
    //get parameters from command line
    String fileName = args[0];
    int numberOfVectors = Integer.parseInt(args[1]);
    if (numberOfVectors <= 0){ //check if n is positive
      System.out.println("n have to be a positive integer");
      System.exit(1);
    }
    int mode = Integer.parseInt(args[2]); //select algorithm
    
    File file = new File(fileName);
    byte[] bytes = new byte[(int)file.length()];
    
    //copy the data in file into a byte array
    try {
      bytes = fileToBytes(file);
    } catch (IOException e) {
      System.out.println("Unable to load data from the file");
      System.out.println("Please doublecheck the file name");
      System.exit(0);
    }
    
    //put the bytes in the byte array into a 3-dimensional integer array
    int[][][] inputImageArray = new int[3][height][width];
    bytesToArray(bytes, inputImageArray);
    
    //
    //BMP.ReadInt("C:\\temp\\test2.bmp", inputImageArray);
    //BMP.WriteInt("C:\\temp\\test1.bmp", inputImageArray);
    //
    
    //create an ArrayList "vectorList" to store selected representative vectors
    //each vector is an int[3]
    //vector[0] is R value, vector[1] is G value, and vector[2] is B value
    ArrayList<int[]> vectorList = new ArrayList<int[]>();
    
    //this is a look-up table showing which representative vector a certain value will be mapped to
    //e.x. if lookupTable[8][100][30] = 4, it means a pixel with its value r = 8, g = 100, and b = 30
    //will be mapped to 4th representative vector
    int[][][] lookupTable = new int[256][256][256];
    
    if (mode == 1) {//m = 1, use uniform quantization
      int numberOfBits = numberOfBits(numberOfVectors); //number of bits needed to represent the vectors
      System.out.println("It takes " + numberOfBits + " bits to represent " + numberOfVectors + " vectors");
      int bitsR = 0, bitsG = 0, bitsB = 0; //number of bits assigned for each channel
      bitsR = numberOfBits/3;
      bitsG = numberOfBits/3;
      bitsB = numberOfBits/3;
      if (numberOfBits % 3 == 1){ //if not divisible by 3 and the remainder is 1
        double deviationR = deviationOf(inputImageArray[0]);
        double deviationG = deviationOf(inputImageArray[1]);
        double deviationB = deviationOf(inputImageArray[2]);
        if (deviationR > deviationG && deviationR > deviationB){//deviation of R is bigges
          bitsR++;
        }
        else if (deviationG > deviationB){//deviation of G is biggest
          bitsG++;
        }
        else {//deviation of B is biggest
          bitsB++;
        }
      }
      if(numberOfBits % 3 == 2){ //the remainder is 2
        double deviationR = deviationOf(inputImageArray[0]);
        double deviationG = deviationOf(inputImageArray[1]);
        double deviationB = deviationOf(inputImageArray[2]);
        if (deviationR < deviationG && deviationR < deviationB){//deviation of R is smallest
          bitsG++;
          bitsB++;
        }
        else if (deviationG < deviationB){//deviation of G is smallest
          bitsR++;
          bitsB++;
        }
        else {//deviation of B is smallest
          bitsR++;
          bitsG++;
        }
      }
      System.out.printf("R channel is given %d bits\n", bitsR);
      System.out.printf("G channel is given %d bits\n", bitsG);
      System.out.printf("B channel is given %d bits\n", bitsB);
      int sectionR = (int)Math.round(Math.pow(2, bitsR)); //how many sections R is separated
      int sectionG = (int)Math.round(Math.pow(2, bitsG)); //how many sections G is separated
      int sectionB = (int)Math.round(Math.pow(2, bitsB)); //how many sections B is separated
      int section_length_R = 256/sectionR;
      int section_length_G = 256/sectionG;
      int section_length_B = 256/sectionB;
      
      //select vectors
      for (int x = 1; x <= sectionR; x++){
        for (int y = 1; y <= sectionG; y++){
          for (int z = 1; z <= sectionB; z++){
            int[] vector = new int[3]; //select a vector
            vector[0] = section_length_R*(x - 1)+section_length_R/2;
            vector[1] = section_length_G*(y - 1)+section_length_G/2;
            vector[2] = section_length_B*(z - 1)+section_length_B/2;
            vectorList.add(vector);
          }
        }
      }
      //populate the look-up table
      for (int r = 0; r <= 255; r++){
        for (int g = 0; g <= 255; g++){
          for (int b = 0; b <= 255; b++){
            lookupTable[r][g][b] = r/section_length_R * sectionG * sectionB +
                                   g/section_length_G * sectionB +
                                   b/section_length_B;
          }
        }
      }
      //if the number of selected vectors is not the same as n, eliminate some vectors
      if (vectorList.size() != numberOfVectors){
        //With the lookup table and vector list, we then map each pixel to a representative vector
        //a quantized table is used to store the index of a representative vector
        //which is used to represent a certain pixel
        //e.x. if quantizedImageArray[5][18] = 12
        //it means the pixel on 5th row and 18th column is represented by 12th representative vector
        int[][] quantizedImageArray = new int[height][width];
        for (int i = 0; i <= height - 1; i++){
          for (int j = 0; j <= width - 1; j++){
            int red = inputImageArray[0][i][j]; //r value of this pixel
            int green = inputImageArray[1][i][j]; //g value of this pixel
            int blue = inputImageArray[2][i][j]; //b value of this pixel 
            quantizedImageArray[i][j] = lookupTable[red][green][blue]; 
          }
        }
        //this is to show how many pixels are represented by each pixel
        //e.x. if vectorCount[3] = 50, 
        //it means 50 pixels on the image are represented by 3rd representative vector

        int[] vectorCount = new int[vectorList.size()];
        for (int i = 0; i <= height - 1; i++){
          for (int j = 0; j <= width - 1; j++){
            vectorCount[quantizedImageArray[i][j]]++;
          }
        }
        ArrayList<Integer> vectorCountArray = new ArrayList<Integer>();
        for (int k = 0; k <= vectorCount.length - 1; k++){
          vectorCountArray.add(vectorCount[k]);
        }
        while (vectorList.size() != numberOfVectors){
          //find the vector which represents least pixels
          //then remove it from vectorList
          int min = height * width;
          int min_index = 0;
          for (int k = 0; k <= vectorCountArray.size() - 1; k++){
            if(vectorCountArray.get(k) < min){
              min = vectorCountArray.get(k);
              min_index = k;
            }
          }
          vectorList.remove(min_index);
          vectorCountArray.remove(min_index);
        }
        System.out.println("Vectors selected:");
        System.out.println("Index     R     G     B");
        for (int k = 0; k <= vectorList.size() - 1;  k++){
          int[] vector = vectorList.get(k);
          System.out.printf("%5d  (%3d,  %3d,  %3d)\n", k, vector[0], vector[1], vector[2]);
        }
        //populate the look-up table
        System.out.println("Generating look-up table...");
        for (int r = 0; r <= 255; r++){
          for (int g = 0; g <= 255; g++){
            for (int b = 0; b <= 255; b++){
              lookupTable[r][g][b] = indexOfClosestVector(r, g, b, vectorList);
            }
          }
        }
      }
      else{//if the number of selected vector is the same as n
        System.out.println("Vectors selected:");
        System.out.println("Index     R     G     B");
        for (int k = 0; k <= vectorList.size() - 1;  k++){
          int[] vector = vectorList.get(k);
          System.out.printf("%5d  (%3d,  %3d,  %3d)\n", k, vector[0], vector[1], vector[2]);
        }
      }
    }//end mode 1
    
    else if (mode == 2){//my own algorithm
      //create a 3D-histogram from the input image
      int[][][] histogram = histogramOf(antiAliasing(inputImageArray));
      System.out.println("Vectors selected:");
      System.out.println("Index     R     G     B");
      //a "peak" means a point where all the surrounding points are smaller
      //(within the distance of radius)
      int radius = 128;
      while (vectorList.size() < numberOfVectors){ //keep searching
        ArrayList<int[]> peakList = searchPeak(radius, histogram);
        
        //check if the found peaks already in vectorList
        checkDuplicate(peakList, vectorList);
                
        //put the peaks into vectorList
        while(vectorList.size() < numberOfVectors && peakList.size()!=0){
          int[] selected_vector = new int[3];
            selected_vector = peakList.get(0);
            peakList.remove(0);
          System.out.printf("%5d  (%3d,  %3d,  %3d)\n", vectorList.size(),
                            selected_vector[0], selected_vector[1], selected_vector[2]);
          vectorList.add(selected_vector);
        }
        //reduce radius
        if (radius > 64){
          radius = radius/2;
        }
        else {radius = radius-2;
        }
      }
      
      //populate the look-up table
      System.out.println("Generating look-up table...");
      for (int r = 0; r <= 255; r++){
        for (int g = 0; g <= 255; g++){
          for (int b = 0; b <= 255; b++){
            lookupTable[r][g][b] = indexOfClosestVector(r, g, b, vectorList);
          }
        }
      }
    }//end mode 2
    
    else if (mode == 3){//m = 3, use weighted count method
      //create a 3D-histogram from the input image
      int[][][] histogram = histogramOf((inputImageArray));
      System.out.println("Vectors selected:");
      System.out.println("Index     R     G     B");      
      //first find out the maximum in the 256x256x256 color space, and put into vectorList
      int[] maxVector = maxVector(histogram);
      System.out.printf("%5d  (%3d,  %3d,  %3d)\n",
                        vectorList.size(), maxVector[0], maxVector[1], maxVector[2]);
      vectorList.add(maxVector);
      //select a point in RGB space which appears most often in image as first representative vector
      //then keep selecting the point which has maximum "weighted count"
      //weighted count:
      //the number of pixels which have this color * weight of this color
      //weight of this color: (the distance between this color and closest selected color)^4      
      while (vectorList.size() < numberOfVectors){ 
        int max = 0; //this record the max value
        int[] max_vector = new int[3]; //this record the vector associated with the max value
        for (int r = 0; r <= 255; r++){
          for (int g = 0; g <= 255; g++){
            for (int b = 0; b <= 255; b++){
              int distance = distanceToClosestVector(r, g, b, vectorList);
              int weightedCount = histogram[r][g][b] * distance * distance * distance * distance;
              if (weightedCount > max){
                max = weightedCount;
                max_vector[0] = r;
                max_vector[1] = g;
                max_vector[2] = b;
              }
            }
          }
        }
        System.out.printf("%5d  (%3d,  %3d,  %3d)\n",
                          vectorList.size(), max_vector[0], max_vector[1], max_vector[2]);
        vectorList.add(max_vector);
      }
      
      //populate the look-up table
      System.out.println("Generating look-up table...");
      for (int r = 0; r <= 255; r++){
        for (int g = 0; g <= 255; g++){
          for (int b = 0; b <= 255; b++){
            lookupTable[r][g][b] = indexOfClosestVector(r, g, b, vectorList);
          }
        }
      }           
    }//end if m == 3
    else {
      System.out.println("Value of m is invalid");
      System.exit(1);
    } 
    
    //With the lookup table and vector list, we then map each pixel to a representative vector
    //a quantized table is used to store the index of a representative vector
    //which is used to represent a certain pixel
    //e.x. if quantizedImageArray[5][18] = 12
    //it means the pixel on 5th row and 18th column is represented by 12th representative vector
    int[][] quantizedImageArray = new int[height][width];
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        int red = inputImageArray[0][i][j]; //r value of this pixel
        int green = inputImageArray[1][i][j]; //g value of this pixel
        int blue = inputImageArray[2][i][j]; //b value of this pixel 
        quantizedImageArray[i][j] = lookupTable[red][green][blue]; 
      }
    }
    
    //Then, use the quantized data to create an output image array
    int[][][] outputImageArray = new int[3][height][width];
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        int[] vector = vectorList.get(quantizedImageArray[i][j]);
        outputImageArray[0][i][j] = vector[0];
        outputImageArray[1][i][j] = vector[1];
        outputImageArray[2][i][j] = vector[2];
      }
    }
    
    // BMP.WriteInt("C:\\temp\\output.bmp", outputImageArray);
    
    //Compare the difference between original image and output image
    int[][][] differenceR = new int[3][height][width];
    int[][][] differenceG = new int[3][height][width];
    int[][][] differenceB = new int[3][height][width];    
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
          for (int color = 0; color <=2; color++){
            differenceR[color][i][j] = Math.abs(outputImageArray[0][i][j] - inputImageArray[0][i][j]);
            differenceG[color][i][j] = Math.abs(outputImageArray[1][i][j] - inputImageArray[1][i][j]);
            differenceB[color][i][j] = Math.abs(outputImageArray[2][i][j] - inputImageArray[2][i][j]);
          }
      }
    }
    
    //Compute the sum of all the differences per channel, and overall error quantifier
    int sumR = 0;
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        sumR = sumR + differenceR[0][i][j];
      }
    }
    System.out.println("The error quantifier of channel R: " + sumR);
    int sumG = 0;
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        sumG = sumG + differenceG[0][i][j];
      }
    }
    System.out.println("The error quantifier of channel G: " + sumG);
    int sumB = 0;
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        sumB = sumB + differenceB[0][i][j];
      }
    }
    System.out.println("The error quantifier of channel B: " + sumB);
    
    System.out.println("The overall error quantifier: " + (sumR+sumG+sumB));
    
    //create a bufferedImage, based on the original input image, and then
    //convert the 3-dimensional output image array to a 1-dimensional byte array
    //and create another bufferedImage, based on the output image data
    //then create bufferedImages for difference in R, G, and B
    BufferedImage inputImg = bytesToImg(bytes, height, width);
    bytes = arrayToBytes(outputImageArray);
    BufferedImage outputImg = bytesToImg(bytes, height, width);
    bytes = arrayToBytes(differenceR);
    BufferedImage rImg = bytesToImg(bytes, height, width);
    bytes = arrayToBytes(differenceG);
    BufferedImage gImg = bytesToImg(bytes, height, width);
    bytes = arrayToBytes(differenceB);
    BufferedImage bImg = bytesToImg(bytes, height, width);
        
    //use a frame to display the original image
    JFrame originalFrame = new JFrame();
      JLabel originalLabel = new JLabel(new ImageIcon(inputImg));
      originalFrame.getContentPane().add(originalLabel, BorderLayout.CENTER);
      originalFrame.pack();
      originalFrame.setTitle("Original Image");
      originalFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      originalFrame.setVisible(true);
    
    //use a frame to display the compressed image
      JFrame newFrame = new JFrame();
      JLabel newLabel = new JLabel(new ImageIcon(outputImg));
      newFrame.getContentPane().add(newLabel, BorderLayout.CENTER);
      newFrame.pack();
      String title = "Compressed image";
      newFrame.setTitle(title);
      newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      newFrame.setLocation(originalFrame.getLocation().x+width,originalFrame.getLocation().y);
      newFrame.setVisible(true);
      
      //use a frame to display the difference in R
      JFrame rFrame = new JFrame();
      JLabel rLabel = new JLabel(new ImageIcon(rImg));
      rFrame.getContentPane().add(rLabel, BorderLayout.CENTER);
      rFrame.pack();
      rFrame.setTitle("Difference in R channel");
      rFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      rFrame.setLocation(originalFrame.getLocation().x,originalFrame.getLocation().y+height);
      rFrame.setVisible(true);
      
      //use a frame to display the difference in G
      JFrame gFrame = new JFrame();
      JLabel gLabel = new JLabel(new ImageIcon(gImg));
      gFrame.getContentPane().add(gLabel, BorderLayout.CENTER);
      gFrame.pack();
      gFrame.setTitle("Difference in G channel");
      gFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      gFrame.setLocation(originalFrame.getLocation().x+width,originalFrame.getLocation().y+height);
      gFrame.setVisible(true);
      
      //use a frame to display the difference in R
      JFrame bFrame = new JFrame();
      JLabel bLabel = new JLabel(new ImageIcon(bImg));
      bFrame.getContentPane().add(bLabel, BorderLayout.CENTER);
      bFrame.pack();
      bFrame.setTitle("Difference in B channel");
      bFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      bFrame.setLocation(originalFrame.getLocation().x+width*2,originalFrame.getLocation().y+height);
      bFrame.setVisible(true);
      
      System.out.println("Please close the images to terminate the program");
    
  }//end main

  //given the number of vectors, this method tells the number of bits needed to represent all the vectors
  public static int numberOfBits(int numberOfVectors){
    int x = 2;
    int numberOfBits = 1;
    while(x < numberOfVectors){
      x *= 2;
      numberOfBits = numberOfBits + 1;
    }   
    return numberOfBits;
  }//end numberOfBits

  //this method calculate the standard deviation of values in a 2-dimensional array
  public static double deviationOf(int[][] imageArray){
    int height = imageArray.length;
    int width = imageArray[0].length;
    int size = height * width;
    int sum = 0;
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){       
        sum = sum + imageArray[i][j];
      }
    }
    int mean = (int)Math.round((double)sum/size);//calculate mean
    sum = 0;
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){       
        sum = sum + (imageArray[i][j] - mean)*(imageArray[i][j] - mean);
      }
    }
    double deviation = Math.sqrt((double)sum/size);
    return deviation;
  }//end deviationOf
  
  //this method takes an image array as input, and output a 3D-histogram of the image
  public static int[][][] histogramOf(int[][][] imageArray){
    int height = imageArray[0].length;
    int width = imageArray[0][0].length;
    int[][][] histogram = new int[256][256][256]; //output 3D-histogram
    for (int i = 0; i <= height - 1; i++){
      for (int j = 0; j <= width - 1; j++){
        int red = imageArray[0][i][j]; //r value of this pixel
        int green = imageArray[1][i][j]; //g value of this pixel
        int blue = imageArray[2][i][j]; //b value of this pixel
        histogram[red][green][blue]++;
      }
    }
    return histogram;
  }//end histogramOf

  //this method takes a histogram as input, and find out the vector which has most number of pixels
  public static int[] maxVector(int[][][] histogram){
    int[] vector = new int[3];
    int max = 0;
    for (int r = 0; r <= 255; r++){
      for (int g = 0; g <= 255; g++){
        for (int b = 0; b <= 255; b++){
          if (histogram[r][g][b] > max){
            max = histogram[r][g][b];
            vector[0] = r;
            vector[1] = g;
            vector[2] = b;
          }
        }
      }
    }
    return vector;
  }//end maxVector
  
  //given a 3D histogram and the radius
  //it will return a list of all the found peaks
  //a peak is represented by int[4]
  //first integer is R value, then G value, and B value of the vector
  //the last integer means its significance
  public static ArrayList<int[]> searchPeak(int radius, int[][][] histogram){
    //a list of found peaks
    ArrayList<int[]> peakList = new ArrayList<int[]>();
    //the original histogram is too big and takes too much time to analyze
    //so we rebuild a 3D histogram with smaller size
    int divisor = 4;
    int[][][] reducedHistogram = new int[256/divisor][256/divisor][256/divisor];
    for (int r = 0; r <= 255; r++){
      for (int g = 0; g <= 255; g++){
        for (int b = 0; b <= 255; b++){
          reducedHistogram[r/divisor][g/divisor][b/divisor] += histogram[r][g][b];
        }
      }
    }
    int histogram_length = reducedHistogram.length;
    int reduced_radius = radius/divisor;
    for (int r = 0; r<= histogram_length - 1; r++){
      for (int g = 0; g<= histogram_length - 1; g++){
        for (int b = 0; b<= histogram_length - 1; b++){
          //first calculate searching up-bound and low-bound
          int searchLowBoundR = (r-reduced_radius>=0) ? (r-reduced_radius) : 0;
          int searchUpBoundR = (r+reduced_radius<=histogram_length-1) ? 
                               (r+reduced_radius) : histogram_length-1;
          int searchLowBoundG = (g-reduced_radius>=0) ? (g-reduced_radius) : 0;
          int searchUpBoundG = (g+reduced_radius<=histogram_length-1) ? 
                               (g+reduced_radius) : histogram_length-1;
          int searchLowBoundB = (b-reduced_radius>=0) ? (b-reduced_radius) : 0;
          int searchUpBoundB = (b+reduced_radius<=histogram_length-1) ?
                               (b+reduced_radius) : histogram_length-1;
          boolean isPeak = true;
          if (reducedHistogram[r][g][b] ==0){
            isPeak = false;
          }
          for (int x = searchLowBoundR; x <= searchUpBoundR && isPeak; x++){
            for (int y = searchLowBoundG; y <= searchUpBoundG && isPeak; y++){
              for (int z = searchLowBoundB; z <= searchUpBoundB && isPeak; z++){
                if(reducedHistogram[x][y][z] > reducedHistogram[r][g][b]){
                  isPeak = false;
                }
              }
            }             
          }
          if (isPeak){
            int[] vector = {r, g, b};
            peakList.add(vector);
          }
        }
      }
    }
    //check if there exist several peaks with the same altitude close to each other
    for (int k = 0; k <= peakList.size() - 2;k++){
      for (int m = k + 1; m <= peakList.size() - 1; m++){
        int[] vector1 = peakList.get(k);
        int[] vector2 = peakList.get(m);
        if(Math.abs(vector1[0]-vector2[0]) <= reduced_radius &&
           Math.abs(vector1[1]-vector2[1]) <= reduced_radius &&
           Math.abs(vector1[2]-vector2[2]) <= reduced_radius){//they are close to each other
          peakList.remove(m);     //remove one peak
          m--;
        }
      }
    }
    //transfer from the reduced histogram to the original histogram
    for (int k = 0; k <= peakList.size()-1; k++){
      int[] vector = peakList.get(k);
      int max = 0;
      int[] maxVector = new int[3];
      for (int r = vector[0] * divisor; r <= vector[0] * divisor + divisor - 1; r++){
        for (int g = vector[1] * divisor; g <= vector[1] * divisor + divisor - 1; g++){
          for (int b = vector[2] * divisor; b <= vector[2] * divisor + divisor - 1; b++){
            if (histogram[r][g][b] > max){
              max = histogram[r][g][b];
              maxVector[0] = r;
              maxVector[1] = g;
              maxVector[2] = b;
            }
          }
        }
      }
      peakList.remove(k);
      peakList.add(k, maxVector);
    }
    return peakList;
  }//end searchPeak
  
  //this method check if some vectors in peakList already exist in vectorList
  //if yes, delete this vector in peakList
  public static void checkDuplicate(ArrayList<int[]> peakList, ArrayList<int[]> vectorList){
    for (int k = 0; k <= peakList.size()-1; k++){
      boolean duplicate = false;
      for (int m = 0; m <= vectorList.size()-1 && duplicate == false; m++){
        int[] peak = peakList.get(k);
        int[] vector = vectorList.get(m);
        if (peak[0] == vector[0] && peak[1] == vector[1] && peak[2] == vector[2]){
          duplicate = true;
        }
      }
      if (duplicate){
        peakList.remove(k);
        k--;
      }
    }
  }//end checkDuplicate
  
  //this method takes a point in RGB space (r, g, b) as input,
  //and takes a vector list, which includes several vectors, as input
  //then it find out the distance between the point and its closest vector in the list
  public static int distanceToClosestVector
  (int r, int g, int b, ArrayList<int[]> vectorList){
    int min_distance = 2147483647; //a variable to record the minimum distance, initial value is infinity 
    //compute the distance between the point and each stored vector, and find minimum
    for (int k = 0; k <= vectorList.size() - 1; k++){
      int[] vector = vectorList.get(k);
      int distance = distanceOf(r, g, b, vector);
      if (distance < min_distance){
        min_distance = distance;
      }
    }
    return min_distance;
  }//end distanceToClosestVector

  //given a point(r,g,b) and a list of representative vectors (vectorList)
  //this method tells you the index of a vector which is closest to the point
  public static int indexOfClosestVector
  (int r, int g, int b, ArrayList<int[]> vectorList){
    int min_distance = 2147483647; //a variable to record the minimum distance, initial value is infinity
    int selected_index = 0;
    for (int k = 0; k <= vectorList.size() - 1; k++){
      int[] vector = vectorList.get(k);
      int distance = distanceOf(r, g, b, vector);
      if (distance < min_distance){
        min_distance = distance;
        selected_index = k;
      }
    }
    return selected_index;
  }//end indexOfClosestVector
  
  //given a point(r, g, b) in color space and a vector
  //this method gives you the distance between this vector and the point
  public static int distanceOf(int r, int g, int b, int[] vector){
    int distanceR = Math.abs(r - vector[0]);
    int distanceG = Math.abs(g - vector[1]);
    int distanceB = Math.abs(b - vector[2]);
    int distance = distanceR + distanceG + distanceB;
    return distance;
  }//end indexOfClosestVector
  
  //this method do anti-aliasing to input array, and output the result to another array
  public static int[][][] antiAliasing(int[][][] imageArray){
    int height = imageArray[0].length;
    int width = imageArray[0][0].length;
    int[][][] outputImageArray = new int[3][height][width];
    int radius = 1; //radius of the filter
    for (int color = 0; color <= 2; color++){
          for (int i = 0 + radius; i <= height - 1 - radius; i++){
            for (int j = 0 + radius; j <= width - 1 - radius; j++){
              int sum = 0;
              for (int k = -radius; k <= radius; k++){
                for (int l = -radius; l <= radius; l++){
                  sum += imageArray[color][i+k][j+l];
                }
              }
              outputImageArray[color][i][j] = (int)(sum/Math.pow((2*radius+1),2));
            }
          }
        }
        return outputImageArray;
  }//end antiAliasing
  
  //this method take a file as an input 
  //and output the data of the file into a byte array
  public static byte[] fileToBytes(File file) throws IOException{
    InputStream is = new FileInputStream(file);
    byte[] bytes = new byte[(int)file.length()];
    int offset = 0;
      int numRead = 0;
      while (offset < bytes.length && 
          (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
          offset += numRead;
      }
      return bytes;
  }//end fileToBytes

  //this method copy the data in a byte array into a 3-dimensional integer array
  //first dimension means color, (RGB) 0=R, 1=G, 2=B
  //second dimension means height, 0=first row, 1=second row, and so on
  //third dimension means width, 0=first column, 1=second column, and so on
  //the values range from 0 to 255. 0 means most dark, 255 means most bright
  public static void bytesToArray(byte[] bytes, int[][][] imageArray){
    int height = imageArray[0].length;
    int width = imageArray[0][0].length;
    int index = 0;
      for (int color = 0; color <= 2; color++){
        for (int i = 0; i <= height - 1; i++){
          for (int j = 0; j <= width - 1; j++){
            int x = bytes[index];
            imageArray[color][i][j] = (x + ((x < 0)?256:0)); //x = x + 256 if x < 0
            index++;
          }
        }
      }
  }//end bytesToArray
  
  //this method convert a 3-dimensional image array into a byte array
  public static byte[] arrayToBytes(int[][][] imageArray){
    int height = imageArray[0].length;
    int width = imageArray[0][0].length;
    byte[] bytes = new byte[3*height*width];
    int index = 0;
      for (int color = 0; color <= 2; color++){
        for (int i = 0; i <= height - 1; i++){
          for (int j = 0; j <= width - 1; j++){
            bytes[index] = (byte)imageArray[color][i][j];
            index++;
          }
        }
      }
      return bytes;
  }//end arrayToBytes

  //this method create a BufferedImage object from a byte array
  public static BufferedImage bytesToImg(byte[] bytes, int height, int width){
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      int offsetOfG = height * width;
      int offsetOfB = 2 * height * width;
    int index = 0;
    for(int y = 0; y <= height - 1; y++){
      for(int x = 0; x <= width - 1; x++){
        byte r = bytes[index];
        byte g = bytes[offsetOfG + index];
        byte b = bytes[offsetOfB + index]; 
        
        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
        img.setRGB(x,y,pix);
        index++;
      }
    }
    return img;
  }// end bytesToImg
  
}//end HW2_weiming_liu
