package com.example.facealaala;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.Helpers.PreferencesHelper;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;
import ch.zhaw.facerecognitionlibrary.Recognition.Recognition;
import ch.zhaw.facerecognitionlibrary.Recognition.RecognitionFactory;

public class activity_Train_data extends AppCompatActivity {

    ProgressBar progressBarPersons;
    ProgressBar progressBarImage;
    Thread thread;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity__train_data);
        progressBarPersons = (ProgressBar) findViewById(R.id.progressBarPerson);
        progressBarImage = (ProgressBar) findViewById(R.id.progressBarImage);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final Handler handler = new Handler(Looper.getMainLooper());
        //when it is no face detected it will cause memory leaks.
        thread = new Thread(new Runnable() {
            public void run() {
                if(!Thread.currentThread().isInterrupted()){
                    PreProcessorFactory ppF = new PreProcessorFactory(getApplicationContext());
                    PreferencesHelper preferencesHelper = new PreferencesHelper(getApplicationContext());
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String algorithm = sharedPref.getString("key_classification_method", getResources().getString(R.string.tensorflow));

                    FileHelper fileHelper = new FileHelper();
                    fileHelper.createDataFolderIfNotExsiting();
                    File[] persons = fileHelper.getTrainingList();
                    progressBarPersons.setMax(persons.length);
                    if (persons.length > 0) {
                        Recognition rec = RecognitionFactory.getRecognitionAlgorithm(getApplicationContext(), Recognition.TRAINING, algorithm);
                        int rows= 0;
                        int cols = 0;
                        int upperCounter = 1;
                        for (File person : persons) {
                            if (person.isDirectory()){
                                File[] files = person.listFiles();
                                int counter = 1;
                                progressBarImage.setProgress(0);
                                for (File file : files) {
                                    if (FileHelper.isFileAnImage(file)){
                                        Mat imgRgb = Imgcodecs.imread(file.getAbsolutePath());
                                        Imgproc.cvtColor(imgRgb, imgRgb, Imgproc.COLOR_BGRA2RGBA);
                                        Mat processedImage = new Mat();
                                        imgRgb.copyTo(processedImage);
                                        Mat mat;
                                        List images = ppF.getProcessedImage(processedImage, PreProcessorFactory.PreprocessingMode.RECOGNITION);
                                        if (images == null || images.size() > 1) {
                                            // More than 1 face detected --> cannot use this file for training
                                            continue;
                                        } else {
                                            processedImage = (Mat) images.get(0);
                                        }
                                        if (processedImage.empty()) {
                                            continue;
                                        }
                                        // The last token is the name --> Folder name = Person name
                                        String[] tokens = file.getParent().split("/");
                                        final String name = tokens[tokens.length - 1];

                                        MatName m = new MatName("processedImage", processedImage);
                                        fileHelper.saveMatToImage(m, FileHelper.DATA_PATH);
//
                                        if (!processedImage.isContinuous()){
                                            processedImage = processedImage.clone();
                                            //in this change the not continuous matrix to continuous matrix
                                        }

//                                        //start resizing matrix dimension to be the same cols and rows
                                        if (rows == 0){
                                            rows = processedImage.rows();
                                            cols = processedImage.cols();
                                        }else{
                                            if (processedImage.rows() < rows){
                                                org.opencv.core.Size sz = new Size(rows, cols);
                                                Imgproc.resize(processedImage, processedImage, sz);
                                            }else {
                                                org.opencv.core.Size sz = new Size(rows, cols);
                                                Imgproc.resize(processedImage, processedImage, sz);
                                            }
                                        }
                                        //end of resizing matrix dimension.
                                        if (!processedImage.empty()){
                                            rec.addImage(processedImage, name, false);
                                        }

//                                      fileHelper.saveCroppedImage(imgRgb, ppF, file, name, counter);

                                        // Update screen to show the progress
                                        final int counterPost = counter;
                                        final int filesLength = files.length;
                                        progressBarImage.setProgress(counterPost);

                                        counter++;
                                    }
                                }
                            }
                            progressBarPersons.setProgress(upperCounter);
                            upperCounter++;
                        }
                        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        if (rec.train()) {
                            intent.putExtra("training", "Training successful");
                            Log.d("Training", "Success");
                        } else {
                            intent.putExtra("training", "Training failed");
                            Log.d("Training", "Failed");
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(intent);
                            }
                        });
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.start();
    }
//    @Override
//    public void onResume()
//    {
//        super.onResume();
//
//        final Handler handler = new Handler(Looper.getMainLooper());
//        thread = new Thread(new Runnable() {
//            public void run() {
//                if(!Thread.currentThread().isInterrupted()){
//                    PreProcessorFactory ppF = new PreProcessorFactory(getApplicationContext());
//                    PreferencesHelper preferencesHelper = new PreferencesHelper(getApplicationContext());
//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                    String algorithm = sharedPref.getString("key_classification_method", getResources().getString(R.string.imageReshaping));
//
//                    FileHelper fileHelper = new FileHelper();
//                    fileHelper.createDataFolderIfNotExsiting();
//                    final File[] persons = fileHelper.getTrainingList();
//                    progressBarPersons.setMax(persons.length);
//
//                    if (persons.length > 0) {
//                        Recognition rec = RecognitionFactory.getRecognitionAlgorithm(getApplicationContext(), Recognition.TRAINING, algorithm);
//                        int rows= 0;
//                        int cols = 0;
//                        int progressPersons = 1;
//                        for (File person : persons) {
//                            if (person.isDirectory()){
//                                File[] files = person.listFiles();
//                                int counter = 1;
//                                for (File file : files) {
//                                    if (FileHelper.isFileAnImage(file)){
//                                        Mat imgRgb = Imgcodecs.imread(file.getAbsolutePath());
//                                        Imgproc.cvtColor(imgRgb, imgRgb, Imgproc.COLOR_BGRA2RGBA);
//                                        Mat processedImage = new Mat();
//                                        imgRgb.copyTo(processedImage);
//                                        List<Mat> images = ppF.getProcessedImage(processedImage, PreProcessorFactory.PreprocessingMode.RECOGNITION);
//                                        if (images == null || images.size() > 1) {
//                                            // More than 1 face detected --> cannot use this file for training
//                                            continue;
//                                        } else {
//                                            processedImage = images.get(0);
//                                        }
//                                        if (processedImage.empty()) {
//                                            continue;
//                                        }
//                                        // The last token is the name --> Folder name = Person name
//                                        String[] tokens = file.getParent().split("/");
//                                        final String name = tokens[tokens.length - 1];
//
//                                        MatName m = new MatName("processedImage", processedImage);
//                                        fileHelper.saveMatToImage(m, FileHelper.DATA_PATH);
//
//                                        if (!processedImage.isContinuous()){
//                                            processedImage = processedImage.clone();
//                                        }
//
//                                        if (rows == 0){
//                                            rows = processedImage.rows();
//                                            cols = processedImage.cols();
//                                        }else{
//                                            if (processedImage.rows() < rows){
//                                                org.opencv.core.Size sz = new Size(rows, cols);
//                                                Imgproc.resize(processedImage, processedImage, sz);
//                                            }else {
//                                                org.opencv.core.Size sz = new Size(rows, cols);
//                                                Imgproc.resize(processedImage, processedImage, sz);
//                                            }
//                                        }
//
//                                        if (!processedImage.empty()){
//                                            rec.addImage(processedImage, name, false);
//                                        }
//
////                                      fileHelper.saveCroppedImage(imgRgb, ppF, file, name, counter);
//
//                                        // Update screen to show the progress
//                                        final int counterPost = counter;
//                                        final int filesLength = files.length;
//                                        progressBarImage.setProgress(counterPost);
//
//                                        counter++;
//                                    }
//                                }
//                            }
//                            progressBarPersons.setProgress(progressPersons);
//                            progressPersons++;
//                        }
//                        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        if (rec.train()) {
//                            intent.putExtra("training", "Training successful");
//                        } else {
//                            intent.putExtra("training", "Training failed");
//                        }
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                startActivity(intent);
//                            }
//                        });
//                    } else {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        });
//        thread.start();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        thread.interrupt();
    }

    @Override
    protected void onStop() {
        super.onStop();
        thread.interrupt();
    }
}
