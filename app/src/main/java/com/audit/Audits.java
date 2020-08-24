package com.audit;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shockwave.pdfium.PdfDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.audit.MainActivity.adressefichiercree;

public class Audits extends AppCompatActivity implements OnPageChangeListener,OnLoadCompleteListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    String nomfichier;

    //Fonctions d'affichage
    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromFile(new File(pdfFileName))
                .defaultPage(pageNumber)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        float pageWidth = pdfView.getOptimalPageWidth();
                        float viewWidth = pdfView.getWidth();
                        pdfView.zoomTo(viewWidth/pageWidth);
                        pdfView.moveTo(0,0);
                        pdfView.loadPages();
                    }
                })
                //.scrollHandle(new DefaultScrollHandle(this))
                .load();
    }
    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    StorageReference mStorageRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audits);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Accès au storage
        mStorageRef = FirebaseStorage.getInstance().getReference();
        
        //Permissions
        verifyStoragePermissions(this);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        
        //Liste des fichiers PDF dans le répertoire Audits
        final File repertoire = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/Audits");
        if(!repertoire.exists()) repertoire.mkdir();
        else {
            final Spinner SpinnerAudits = findViewById(R.id.spinnerAudits);
            File[] Fichiers = new File(repertoire.getAbsolutePath()).listFiles((file, s) -> s.endsWith(".pdf"));

            assert Fichiers != null;
            //Trier du plus récent au plus ancien
            Arrays.sort(Fichiers, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            if (Fichiers.length > 0) {
                //Passage d'un Array à une liste
                List<String> ListeAudits = new ArrayList<>();
                for (File fichier : Fichiers) ListeAudits.add(fichier.getName());
                Collections.reverse(ListeAudits);
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, ListeAudits);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                SpinnerAudits.setAdapter(spinnerArrayAdapter);
    
                //Le bouton sert à envoyer le fichier PDF, et, au succès, alimenter la base de données avec les données dans le CSV
                FloatingActionButton BoutEnv = findViewById(R.id.fab);
                if (ListeAudits.size() != 0) {
                    BoutEnv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            adressefichiercree = repertoire + "/" + SpinnerAudits.getSelectedItem().toString();
                            nomfichier = SpinnerAudits.getSelectedItem().toString();
                            Toast.makeText(getApplicationContext(), "Envoi en cours...", Toast.LENGTH_SHORT).show();
                            Uri filepdf = Uri.fromFile(new File(adressefichiercree));
                            Uri filecsv = Uri.fromFile(new File(adressefichiercree.replace(".pdf", ".csv")));
                            StorageReference pdfRef = mStorageRef.child("pdf/" + nomfichier);
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference().child(nomfichier.replace(".pdf",""));
                            pdfRef.putFile(filepdf)
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            boolean erreur = false;
                                            try {
                                                BufferedReader reader = new BufferedReader(new FileReader(Objects.requireNonNull(filecsv.getPath())));
                                                String line;
                                                while ((line = reader.readLine()) != null) {
                                                    if(!line.isEmpty()) {
                                                        String[] ligne = line.split(";");
                                                        database.child(ligne[0]).setValue(ligne[1]);
                                                    }
                                                }
                                            } catch (FileNotFoundException e) {
                                                erreur=true;
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                erreur=true;
                                                e.printStackTrace();
                                            }
                                            if (!erreur) Toast.makeText(getApplicationContext(), "Audit envoyé avec succès.", Toast.LENGTH_SHORT).show();
                                            else Toast.makeText(getApplicationContext(), "Erreur, merci de réessayer ultérieurement.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            Toast.makeText(getApplicationContext(), "Erreur, merci de réessayer ultérieurement.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
                    pdfView = findViewById(R.id.pdfView);
                }
                //A chaque changement de l'element selectionné, on l'affiche.
                SpinnerAudits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        adressefichiercree = repertoire + "/" + SpinnerAudits.getItemAtPosition(i).toString();
                        displayFromAsset(adressefichiercree);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        }
    }
    
    @Override
    public void onPageChanged(int page, int pageCount) {
      /*  pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));*/
    }


    @Override
    public void loadComplete(int nbPages) {
        //PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }


    //Fonctions qui s'occupent de demander les permissions, et vérifier qu'elle sont acquises.
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity the activity concerned
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }



}
