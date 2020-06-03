package com.android.MakeYouStudy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.MakeYouStudy.Helper.InternetCheck;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
//import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
//import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
//import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
//import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
//import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
//import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class ImageLabelActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btnDetect;
    AlertDialog waitingDialog;

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_label);

        cameraView = (CameraView)findViewById(R.id.camera_view);
        btnDetect = (Button)findViewById(R.id.btn_detect);

        waitingDialog = new SpotsDialog.Builder().
                setContext(this)
                .setMessage("Please waiting...")
                .setCancelable(false).build();

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(), false);
                cameraView.stop();

                runDetector(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        btnDetect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                cameraView.start();
                cameraView.captureImage();
            }
        });
    }

    private void runDetector(Bitmap bitmap) {
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        new InternetCheck(new InternetCheck.Consumer() {
            @Override
            public void accept(boolean internet) {
                if(internet)
                {
                    //인터넷이 있을 때 클라우드 사용
                    FirebaseVisionCloudImageLabelerOptions options =
                            new FirebaseVisionCloudImageLabelerOptions.Builder()
                                    .setConfidenceThreshold(0.8f) // 가장 높은 신뢰도로 1개의 결과 얻기
                                    .build();
                    FirebaseVisionImageLabeler detector =
                            FirebaseVision.getInstance().getCloudImageLabeler(options);

                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionCloudLabels) {
                                    processDataResultCloud(firebaseVisionCloudLabels);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("EDMTERROR", e.getMessage());
                                }
                            });
                }
                else
                {
                    FirebaseVisionCloudImageLabelerOptions options =
                            new FirebaseVisionCloudImageLabelerOptions.Builder()
                                    .setConfidenceThreshold(0.8f) // 감지된 Label의 신뢰도 설정. 이 값보다 높은 신뢰도의 label만 반환됨
                                    .build();
                    FirebaseVisionImageLabeler detector =
                            FirebaseVision.getInstance().getCloudImageLabeler(options);

                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionLabels) {
                                    processDataResult(firebaseVisionLabels);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("EDMTERROR", e.getMessage());
                                }
                            });
                }
            }
        });
    }

    private void processDataResultCloud(List<FirebaseVisionImageLabel> firebaseVisionCloudLabels) {
        for(FirebaseVisionImageLabel label : firebaseVisionCloudLabels)
        {
            String labeling = label.getText();

//            Toast.makeText(this, "Cloud result: "+label.getLabel(), Toast.LENGTH_SHORT).show();
//            Log.d("entityId", label.getEntityId());
            Log.d("confidence", ""+ label.getConfidence());

            Intent intent = new Intent();

            intent.putExtra("labeling", labeling);

            setResult(RESULT_OK, intent);
            finish();
        }

        if(waitingDialog.isShowing())
            waitingDialog.dismiss();
    }

    private void processDataResult(List<FirebaseVisionImageLabel> firebaseVisionLabels) {
        for(FirebaseVisionImageLabel label : firebaseVisionLabels)
        {
            String labeling = label.getText();

            //Toast.makeText(this, "Device result: "+label.getLabel(), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent();

            intent.putExtra("labeling", labeling);

            setResult(RESULT_OK, intent);
            finish();
        }

        if(waitingDialog.isShowing())
            waitingDialog.dismiss();
    }
}
