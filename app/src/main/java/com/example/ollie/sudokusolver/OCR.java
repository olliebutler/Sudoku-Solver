package com.example.ollie.sudokusolver;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



public class OCR {

    Bitmap image;
    private TessBaseAPI mTess; //Tess API reference
    private String datapath = ""; //path to folder containing language data file


    public void initializeOCR(Context c){
        //initialize Tesseract API
        String language = "eng";
        datapath = c.getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"), c);

        mTess.init(datapath, language);
    }

    private void copyFiles(Context c) {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = c.getAssets();


            //open byte streams for reading/writing
            InputStream instream = assetManager.open("eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir, Context c) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles(c);
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles(c);
            }
        }
    }

    public String processImage(View v, Context c, Uri currentPhotoURI) {

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), currentPhotoURI);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");

        String OCRresult = null;
        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();

        return OCRresult;

    }
    public String processBitmap(Bitmap image){

        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");

        String OCRresult = null;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();

        return OCRresult;

    }


}
