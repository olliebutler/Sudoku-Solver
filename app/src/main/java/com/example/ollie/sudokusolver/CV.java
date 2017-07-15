package com.example.ollie.sudokusolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.HoughLinesP;



public class CV {

    private Mat sourceMat;
    private Mat edgesMat;
    private Mat linesMat;
    private Mat croppedGrid;
    private Bitmap sourceBitmap;
    private Bitmap resultBitmap;

    private Point topLeft;
    private Point topRight;
    private Point bottomLeft;
    private Point bottomRight;

    private TessBaseAPI mTess; //Tess API reference
    OCR ocr = new OCR(); //instance of tessOCR


    public char[][] processImageCV(View v, Context c, Uri uri) { //was prevously return bitmap chnaged to char[][]




        ocr.initializeOCR(c);


        setSourceBitmap(c, uri);

        initMats();

        sourceMat = createMatFromBitmap(sourceBitmap);

        getEdges();

        getLines();

        drawLines();

        getCorners();

        croppedGrid = cropGrid();

        Bitmap[][] bms = getCellBitmaps();

        bms = deleteCellLines(bms);


        char[][] solved = ocrCells(bms);

        return solved;


    }

    private void setSourceBitmap(Context c, Uri uri) {

        try {
            sourceBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), uri);
            resultBitmap = sourceBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initMats() {

        Mat tempMat = new Mat(sourceBitmap.getWidth(), sourceBitmap.getHeight(),
                CvType.CV_8UC1);

        sourceMat = tempMat;
        linesMat = tempMat;
        edgesMat = tempMat;

    }

    private Mat createMatFromBitmap(Bitmap bitmap) {

        Mat newMat = new Mat(sourceBitmap.getWidth(), sourceBitmap.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, newMat);

        return newMat;
    }

    private Bitmap createBitMapFromMat(Mat mat) {

        Bitmap newBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, newBitmap);
        return newBitmap;


    }

    private void getEdges() {


        Imgproc.cvtColor(sourceMat, edgesMat, Imgproc.COLOR_RGB2GRAY);


        Imgproc.threshold(edgesMat, edgesMat , 128, 255, Imgproc.THRESH_BINARY);

        Canny(edgesMat, edgesMat, 100, 100);

        resultBitmap = createBitMapFromMat(edgesMat);


    }

    private void getLines() {

        HoughLinesP(edgesMat,
                linesMat,
                1,
                Math.PI / 180,
                1,          //thresh
                10,        //min line length was 100
                3);         //maxline gap
    }

    private void drawLines() {

        Scalar s = new Scalar(0, 0, 0);
        Mat testMat = new Mat(sourceBitmap.getHeight(), sourceBitmap.getWidth(), CvType.CV_8UC1);


        double[] data;
        Point pt1 = new Point();
        Point pt2 = new Point();

        Size size = linesMat.size();
        int cols = linesMat.cols();
        int rows = linesMat.rows();

        for (int i = 0; i < linesMat.rows(); i++) {

            data = linesMat.get(i, 0);

            pt1.x = data[0];
            pt1.y = data[1];
            pt2.x = data[2];
            pt2.y = data[3];

            Imgproc.line(testMat, pt1, pt2, new Scalar(0, 0, 200), 3);
        }

        resultBitmap = createBitMapFromMat(testMat);

    }

    private void getCorners() {

        //gets lists of all the lines

        List<double[]> horizontalLines = getHorizontalLines();
        List<double[]> verticalLines = getVerticalLines();

        double highestX = 0;
        double highestY = 0;
        double lowestX = 1000;
        double lowestY = 1000;

        int highestXIndex = 0;
        int highestYIndex = 0;
        int lowestXIndex = 0;
        int lowestYIndex = 0;

        double[] data;

        for(int i = 0; i < horizontalLines.size(); i++){

            data = horizontalLines.get(i);

            if((data[0] < 1 && data[1] < 1) || (data[2] <1 && data[3] < 1)){

            }
            else{

                if(data[1]>highestY || data[3]>highestY){
                    highestYIndex = i;
                    if(data[1]>data[3]){
                        highestY = data[1];
                    }
                    else{
                        highestY = data[3];
                    }
                }
                else if(data[1]<lowestY || data[3]<lowestY){
                    lowestYIndex = i;
                    if(data[1]<data[3]){
                        lowestY = data[1];
                    }
                    else{
                        lowestY = data[3];
                    }
                }
            }

        }
        for(int i = 0; i < verticalLines.size(); i++){

            data = verticalLines.get(i);

            if((data[0] < 1 && data[1] < 1) || (data[2] < 1 && data[3] < 1)){

            }
            else{

                if(data[0]>highestX || data[2]>highestX){
                    highestXIndex = i;
                    if(data[0]>data[2]){
                        highestX = data[0];
                    }
                    else{
                        highestX = data[2];
                    }
                }
                else if(data[0]<lowestX || data[2]<lowestX){
                    lowestXIndex = i;
                    if(data[0]<data[2]){
                        lowestX = data[0];
                    }
                    else{
                        lowestX = data[2];
                    }
                }

            }
        }

        double[] topLine = horizontalLines.get(highestYIndex);
        double[] bottomLine = horizontalLines.get(lowestYIndex);
        double[] leftLine = verticalLines.get(lowestXIndex);
        double[] rightLine = verticalLines.get(highestXIndex);

        //corners are where these lines intersect

        double[] fullTopLine = extendLine(topLine);
        double[] fullBottomLine = extendLine(bottomLine);
        double[] fullLeftLine = extendLine(leftLine);
        double[] fullRightLine = extendLine(rightLine);

        topLeft = intersection(fullTopLine[0],fullTopLine[1],fullTopLine[2],fullTopLine[3],fullLeftLine[0],fullLeftLine[1],fullLeftLine[2],fullLeftLine[3]);
        topRight = intersection(fullTopLine[0],fullTopLine[1],fullTopLine[2],fullTopLine[3], fullRightLine[0], fullRightLine[1], fullRightLine[2], fullRightLine[3]);
        bottomLeft = intersection(fullBottomLine[0], fullBottomLine[1], fullBottomLine[2],fullBottomLine[3],fullLeftLine[0],fullLeftLine[1],fullLeftLine[2],fullLeftLine[3]);
        bottomRight = intersection(fullBottomLine[0], fullBottomLine[1], fullBottomLine[2],fullBottomLine[3], fullRightLine[0], fullRightLine[1], fullRightLine[2], fullRightLine[3]);




    }

    private List getHorizontalLines() {

        List<double[]> horizontalLines = new ArrayList<double[]>();

        for (int i = 0; i < linesMat.rows(); i++) {
            double[] line = linesMat.get(i, 0);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];
            if (Math.abs(y2 - y1) < Math.abs(x2 - x1)) {
                horizontalLines.add(line);
            }


        }
        return horizontalLines;
    }
    private List getVerticalLines(){

        List<double[]> verticalLines = new ArrayList<double[]>();

        for (int i = 0; i < linesMat.rows(); i++) {
            double[] line = linesMat.get(i, 0);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];
            if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
                verticalLines.add(line);
            }


        }
        return verticalLines;
    }
    private double slope(double x1, double y1, double x2, double y2){
        return (y2-y1)/(x2-x1);
    }
    private double[] extendLine(double[] data){

        double x1final;
        double x2final;
        double y1final;
        double y2final;

        double[] finalCoordinates = data;

        double x1 = data[0];
        double y1 = data[1];
        double x2 = data[2];
        double y2 = data[3];

        if(x1 == x2){
            finalCoordinates[0]=x1;
            finalCoordinates[1]=0;
            finalCoordinates[2]=x2;
            finalCoordinates[3]=sourceBitmap.getHeight();

            return finalCoordinates;

        }
        else if(y1 == y2){
            finalCoordinates[0]=0;
            finalCoordinates[1]=y1;
            finalCoordinates[2]=sourceBitmap.getWidth();
            finalCoordinates[3]=y2;

            return finalCoordinates;
        }
        else if (Math.abs(y2 - y1) < Math.abs(x2 - x1)){ //line is horizontal

            double slope = slope(x1, y1, x2, y2);

            double b = y1 - (slope * x1);

            finalCoordinates[0] = 0;
            finalCoordinates[1] = b;
            finalCoordinates[2] = sourceBitmap.getWidth();
            finalCoordinates[3] = (sourceBitmap.getWidth() * slope) + b;

            return finalCoordinates;
        }
        else {                                              // line is vertical
            double slope = slope(x1, y1, x2, y2);

            double b = y1 - (slope * x1);

            finalCoordinates[0] = (0-b)/slope;
            finalCoordinates[1] = 0;
            finalCoordinates[2] = (sourceBitmap.getHeight() - b) / slope;
            finalCoordinates[3] =  sourceBitmap.getHeight();

            return finalCoordinates;
        }


    }

    /**
     * Computes the intersection between two lines. The calculated point is approximate,
     * since integers are used. If you need a more precise result, use doubles
     * everywhere.
     * (c) 2007 Alexander Hristov. Use Freely (LGPL license). http://www.ahristov.com
     *
     * @param x1 Point 1 of Line 1
     * @param y1 Point 1 of Line 1
     * @param x2 Point 2 of Line 1
     * @param y2 Point 2 of Line 1
     * @param x3 Point 1 of Line 2
     * @param y3 Point 1 of Line 2
     * @param x4 Point 2 of Line 2
     * @param y4 Point 2 of Line 2
     * @return Point where the segments intersect, or null if they don't
     */
    private Point intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double d = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
        if (d == 0) return null;

        double xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
        double yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;

        return new Point(xi,yi);
    }

    private Mat cropGrid(){



        int bLX = (int) Math.round(bottomLeft.x);
        int bLY = (int) Math.round(bottomLeft.y);

        int width = (int) Math.round(topRight.x - topLeft.x);
        int height = (int) Math.round(topLeft.y - bottomLeft.y);

        Rect rect = new Rect(bLX, bLY, width, height);

        croppedGrid = sourceMat.submat(rect);

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); //to del
        Utils.matToBitmap(croppedGrid, newBitmap); //to del

        return croppedGrid;

    }

    private Point getCellBLCorner(int row, int column){

        double height = croppedGrid.height();
        double width = croppedGrid.width();

        double cellHeight = height/9;
        double cellWidth = width/9;

        double leftX = (column - 1) * cellWidth;
        double bottomY = (row - 1) * cellHeight;

        Point bl = new Point(leftX,bottomY);

        return bl;
    }

    private Bitmap[][] getCellBitmaps(){

        double cellHeight = croppedGrid.height()/9;
        double cellWidth = croppedGrid.width()/9;

        Bitmap[][] res = new Bitmap[9][9];

        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++ ){

                Point bl = getCellBLCorner(i+1,j+1);

                int x = (int) Math.round(bl.x);
                int y = (int) Math.round(bl.y);

                int height = (int) Math.round(cellHeight);
                int width = (int) Math.round(cellWidth);

                Rect rect = new Rect(x, y, width, height);

                Mat cellMat = croppedGrid.submat(rect);

                Bitmap cellBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cellMat, cellBitmap);

                res[i][j] = cellBitmap;

            }

        }

        return res;
    }


    private Bitmap[][] deleteCellLines(Bitmap[][] cells){

        Bitmap[][] res = new Bitmap[9][9];

        for(int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {

                Bitmap cell = cells[i][j];

                // first convert bitmap into OpenCV mat object
                Mat imageMat = new Mat (cell.getHeight(), cell.getWidth(),
                        CvType.CV_8U, new Scalar(4));

                Utils.bitmapToMat(cell, imageMat);

                // now convert to gray
                Mat grayMat = new Mat ( cell.getHeight(), cell.getWidth(),
                        CvType.CV_8U, new Scalar(1));
                Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

                // get the thresholded image
                Mat thresholdMat = new Mat ( cell.getHeight(), cell.getWidth(),
                        CvType.CV_8U, new Scalar(1));
                Imgproc.threshold(grayMat, thresholdMat , 128, 255, Imgproc.THRESH_BINARY);

                // convert back to bitmap for displaying
                Bitmap resultBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(),
                        Bitmap.Config.ARGB_8888);
                thresholdMat.convertTo(thresholdMat, CvType.CV_8UC1);

                Utils.matToBitmap(thresholdMat, resultBitmap);


                resultBitmap = deleteCellLineHorizontal(resultBitmap);
                resultBitmap = deleteCellLineVertical(resultBitmap);


                res[i][j] = resultBitmap;


            }
        }


        return res;
    }

    private Bitmap deleteCellLineHorizontal(Bitmap image){

        for(int i = 0; i < image.getHeight(); i++){

            int blackCount = 0;

            for(int j = 0; j < image.getWidth(); j++){

                //check if pixle is black
                //if is add to count
                if (image.getPixel(j,i) == Color.BLACK) {
                    blackCount++;
                }

            }

            //if black count > 75% width then paint all pixles white
            if(blackCount > (image.getWidth()*0.75)){
                for(int j = 0; j < image.getWidth(); j++){

                   image.setPixel(j,i,Color.WHITE);

                }

            }
        }

        return image;
    }

    private Bitmap deleteCellLineVertical(Bitmap image){


        for(int i = 0; i < image.getWidth(); i++){

            int blackCount = 0;

            for(int j = 0; j < image.getHeight(); j++){

                //check if pixle is black
                //if is add to count
                if (image.getPixel(i,j) == Color.BLACK) {
                    blackCount++;
                }

            }

            //if black count > 75% width then paint all pixles white
            if(blackCount > (image.getHeight()*0.75)){
                for(int j = 0; j < image.getHeight(); j++){

                    image.setPixel(i,j,Color.WHITE);

                }

            }
        }

        return image;
    }


    private char[][] ocrCells(Bitmap[][] images){

        char[][] fin = new char[9][9];

        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++ ){

                Bitmap currentCell = images[i][j];

                String res = ocr.processBitmap(currentCell);

                if(res == null || res.isEmpty()){
                    res = ".";
                };

                fin[i][j] = res.charAt(0);

            }

        }

        char[][] solved = new char[9][9];
        solved = Solver.solveSudoku(fin);

        for(int i = 0; i < 9; i++){
            Log.d("myTag", ""+solved[i][0]+solved[i][1]+solved[i][2]+solved[i][3]+solved[i][4]+solved[i][5]+solved[i][6]+solved[i][7]+solved[i][8]);
        }
        return solved;

    }


}


