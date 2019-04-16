package com.img2text.jonathanwesterfield.imagetotext;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.Image;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQ_CODE_TAKE_PICTURE = 1;
    FirebaseVisionText imageText;
    ImageView imgView;
    ImageButton captureBtn;
    FirebaseVisionImage visImg;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initializeInterfaces();

        requestCameraPermission(findViewById(android.R.id.content));
    }

    public void initializeInterfaces()
    {
        this.imgView = (ImageView) findViewById(R.id.imageView);
        this.captureBtn = (ImageButton) findViewById(R.id.captureBtn);
        this.listview = (ListView) findViewById(R.id.listView);
    }

    /**
     * Request permission to use the camera if it already isn't written in the manifest file
     * @param view
     */
    private void requestCameraPermission(View view)
    {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
            showCameraPermissionsAlert(view);
        else
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    public void onCaptureBtnClk(View view)
    {
        openCamera();
    }

    public void openCamera()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQ_CODE_TAKE_PICTURE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(resultCode == RESULT_OK)
        {
            Bitmap bmp = (Bitmap) intent.getExtras().get("data");
            this.imgView.setImageBitmap(bmp);

            visImg = FirebaseVisionImage.fromBitmap(bmp);

            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();

            Task<FirebaseVisionText> result =
                    detector.processImage(visImg)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    imageText = firebaseVisionText;
                                    extractVisionText(firebaseVisionText);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            View currentFocus = getWindow().getCurrentFocus();
                                            showFailedProcessAlert(currentFocus.getRootView());
                                        }
                                    });
        }
    }

    /**
     * If the image text extraction works, we use this function to actually parse through the extracted text
     */
    public void extractVisionText(FirebaseVisionText result)
    {
        String resultText = result.getText();
        System.out.println("Image Result: " + resultText + "\n");

        ArrayList<String> textLines = new ArrayList<>();
        textLines.addAll(Arrays.asList(resultText.split("\n")));

        TextAdapter adapter = new TextAdapter(this, textLines);

        this.listview.setAdapter(adapter);
    }

    public void showFailedProcessAlert(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Process Failed").
                setMessage("Failed to Process the image.")
                .setNeutralButton("Yeet", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        Activity parent = getParent();
                        parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    }
                });


        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showCameraPermissionsAlert(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("We need help").
                setMessage("We need permission to use the camera")
                .setPositiveButton("Yeet", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        Activity parent = getParent();
                        parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    }
                })
                .setNegativeButton("Hell Naw",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Activity activity = getParent();
                                if (activity != null)
                                    activity.finish();
                            }
                        });


        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage("This sample needs camera permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }


}
