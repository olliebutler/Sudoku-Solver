package com.example.ollie.sudokusolver;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class main extends AppCompatActivity {

    Bitmap image; //the chosen image
    private TessBaseAPI mTess; //Tess API reference
    String datapath = ""; //path to folder containing language data file
    OCR ocr = new OCR(); //instance of tessOCR
    CV cv = new CV(); //instance of openCV

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setTitle("Sudoku Solver");


        ocr.initializeOCR(this); //initialize tessOCR



        //initialize OpenCV API

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }


        //initialize buttons


        final Button button2 = (Button) findViewById(R.id.button2);
        final Button button4 = (Button) findViewById(R.id.button4);


        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                pickPicture();
                TableLayout tabelLayout = (TableLayout) findViewById(R.id.tabelLayout);
                tabelLayout.setVisibility(View.INVISIBLE);

            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                processCV(v);

            }
        });

    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    String currentPhotoPath;
    Uri currentPhotoURI;

    static final int REQUEST_TAKE_PHOTO = 1;
    int PICK_IMAGE_REQUEST = 2;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            currentPhotoURI = Uri.fromFile(new File(currentPhotoPath));
            setPic();
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            currentPhotoURI = data.getData();
            setPic();

        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }




    private void setPic(){

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentPhotoURI);

            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void pickPicture(){

        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

    }

    public void processCV(View v){

        ImageView imageView = (ImageView) findViewById(R.id.imgView);
        imageView.setVisibility(View.INVISIBLE);

        TableLayout tabelLayout = (TableLayout) findViewById(R.id.tabelLayout);
        tabelLayout.setVisibility(View.VISIBLE);

        char[][] solved = cv.processImageCV(v,this,currentPhotoURI);

        tabelLayout.removeAllViews();

        for(int i = 0; i <9 ;i++){
            TableRow tr = new TableRow(this);
            for(int j = 0; j <9 ;j++){

                TextView tv = new TextView(this);
                tv.setText(String.valueOf(solved[i][j]));
                tv.setTextSize(20);
                tv.setGravity(Gravity.CENTER);
                tv.setMinHeight(100);
                tv.setMinWidth(100);


                ShapeDrawable border = new ShapeDrawable(new RectShape());
                border.getPaint().setStyle(Paint.Style.STROKE);
                border.getPaint().setColor(Color.BLACK);
                tv.setBackground(border);


                tr.addView(tv);

            }
            tabelLayout.addView(tr);
        }











        /*ImageView imageView = (ImageView) findViewById(R.id.imgView);

        File file = null;

        try {
            file = createImageFile();
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            processed.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.android.fileprovider",
                file);
        currentPhotoURI = photoURI;

        setPic();*/


    }



}
