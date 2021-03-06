package com.android.MakeYouStudy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.MakeYouStudy.Helper.InternetCheck;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.List;

public class TextRecognitionActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Button captureImageBtn;
    private ImageView imageView;
    private TextView textView, textView2;
    private Bitmap imageBitmap;
    private String data;
    private boolean checkValue;

    private long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_learning);

        captureImageBtn = findViewById(R.id.capture_image_btn);
        imageView = findViewById(R.id.image_view);
        textView = findViewById(R.id.text_display);
        textView2 = findViewById(R.id.text_display2);

        Intent intent = getIntent();
        data = intent.getStringExtra("English");
        textView.setText("똑같이 작성해주세요 : "+ data + "\n");

        captureImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            detectTextFromImage();
        }
    }

    private void detectTextFromImage()
    {
        new InternetCheck(new InternetCheck.Consumer() {
            @Override
            public void accept(boolean internet) {
                if(internet){
                    FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
                    FirebaseVisionTextRecognizer firebaseVisionTextDetector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
                    firebaseVisionTextDetector.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                            displayTextFromImage(firebaseVisionText);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(TextRecognitionActivity.this, "Error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d("Error: ", e.getMessage());
                        }
                    });
                }else {
                    Toast.makeText(TextRecognitionActivity.this, "인터넷을 체크하고 다시 촬영해주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blockList = firebaseVisionText.getTextBlocks();
        if(blockList.size() == 0){
            textView2.setText("사진에서 단어가 인식되지 않았습니다. 다시 촬영해주세요.");
        }
        else {
            String text = "";
            for(FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks())
            {
                text = block.getText().toLowerCase();
                Log.d("What is text : ", text );
                check(text, data);
            }

        }
    }

    public void check(String text, String data){
        if(text.equals(data)){
            checkValue = true;
            Intent intent = new Intent();
            intent.putExtra("checkValue", checkValue);
            setResult(RESULT_OK, intent);
            finish();
        }
        else {
            checkValue = false;
            textView2.setText("인식된 단어는 " + text);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if ( pressedTime == 0){
            Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            pressedTime = System.currentTimeMillis();
        }else {
            int seconds = (int) (System.currentTimeMillis() - pressedTime);

            if(seconds > 2000){
                pressedTime = 0;
            }else {
                Intent intent = new Intent();
                intent.putExtra("checkValue", false);

                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}

