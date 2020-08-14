package com.audit;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kyanogen.signatureview.SignatureView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Signature extends AppCompatActivity {

    Bitmap bitmap;
    Button clear,save;
    SignatureView signatureView;
    private static final String IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_signature);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        signatureView = findViewById(R.id.signature_view);
        clear =  findViewById(R.id.clear);
        save = findViewById(R.id.save);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signatureView.clearCanvas();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = signatureView.getSignatureBitmap();
                saveImage(bitmap);
            }
        });

    }

    public void saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(IMAGE_DIRECTORY /*iDyme folder*/);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
            Log.d("hhhhh",wallpaperDirectory.toString());
        }

        try {
            File f = new File(wallpaperDirectory, "signature.jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(Signature.this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Toast.makeText(getApplicationContext(),"Signature sauvgardée.",Toast.LENGTH_SHORT).show();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());
            finish();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

}
