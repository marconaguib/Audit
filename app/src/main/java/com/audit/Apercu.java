package com.audit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static com.audit.MainActivity.Sign_Position_Top;
import static com.audit.MainActivity.adressefichiercree;
import static com.audit.MainActivity.signe_par_tech;

public class Apercu extends AppCompatActivity implements OnPageChangeListener,OnLoadCompleteListener{
    //Cette page affiche l'aperçu, puis, si le technicien n'a pas encore signé, lui permet de le faire
    //Sinon, elle permet de sauvegarder et de quitter
    private static final String TAG = MainActivity.class.getSimpleName();
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    private static final String IMAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/signature_tech.jpg";
    FloatingActionButton fab;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_apercu);
        signe_par_tech = false;


        pdfView= findViewById(R.id.pdfView);
        displayFromAsset(adressefichiercree);
        fab = findViewById(R.id.fab_signature);
    }

    @Override
    public void onResume(){
        super.onResume();
        //Il est important que cela soit fait dans onResume et pas dans onCreate
        //Pour que la vérification de l'existence de la signature soit faite APRES la signature
        File signature = new File(IMAGE_PATH);
        if(signature.exists()){
            //cette variable est importante pour que les messages qui s'affichent à la sortie de l'aprecu soient bons
            signe_par_tech = true;
            fab.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(Apercu.this)
                            .setTitle("Sauvegarder et sortir")
                            .setMessage("Voulez vous sauvegarder cette version et quitter l'aperçu ?")
                            .setNegativeButton("Non", null)
                            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    setResult(RESULT_OK, null);
                                    finish();
                                }
                            }).create().show();
                }
            });
            fab.setImageResource(R.drawable.ic_save);
            //La signature existe. Il faut :
            // 1- la déposer sur un nouveau PDF AU BON ENDROIT indiqué par Sign_Position_Top
            // 2- remplacer l'ancien PDF par le nouveau
            // 3- SUPPRIMER le fichier JPG contenant la signature
            try {
                PdfReader reader = new PdfReader(adressefichiercree);
                PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(adressefichiercree.replace(".pdf","_stamped.pdf")));
                PdfContentByte content = stamper.getOverContent(reader.getNumberOfPages());

                Image sign = Image.getInstance(String.valueOf(signature));
                content.addImage(sign,140,0,0,85,300,Sign_Position_Top+1);
                stamper.close();
                
                new File(adressefichiercree.replace(".pdf","_stamped.pdf")).renameTo(new File(adressefichiercree));
                displayFromAsset(adressefichiercree);
                signature.delete();
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            //La signature n'existe pas.
            //Le bouton doit permettre de passer à la page de signature
            fab.setImageResource(R.drawable.ic_pen);
            fab.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onClick(View view) {
                    goToSignatureTech();
                }
            });
        }
    }
    
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


    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }


    @Override
    public void loadComplete(int nbPages) {
        //PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    private void goToSignatureTech() {
        Intent intent = new Intent(this, Signature_Tech.class);
        startActivity(intent);
    }

    // A la sorite, cette fonction affiche le bon Dialogbox, en fonction des cas
    @Override
    public void onBackPressed() {
        if(!signe_par_tech){
            new AlertDialog.Builder(this)
                    .setTitle("Retour en arrière sans signer")
                    .setMessage("Le représentant n'ayant pas signé, le contrôle n'est pas valide. Êtes vous sûr de vouloir retourner à la saisie ?")
                    .setNegativeButton("Non", null)
                    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            Apercu.super.onBackPressed();
                        }
                    }).create().show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle("Retour à la saisie")
                    .setMessage("Si vous retournez à la page de saisie, la signature du représentant va s'effacer et sera à remettre. Voulez-vous retourner quand même")
                    .setNegativeButton("Non", null)
                    .setNeutralButton("Sauvegarder et quitter",new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            new AlertDialog.Builder(Apercu.this)
                                    .setTitle("Sauvegarder et sortir")
                                    .setMessage("Voulez vous sauvegarder cette version et quitter l'aperçu ?")
                                    .setNegativeButton("Non", null)
                                    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            setResult(RESULT_OK, null);
                                            finish();
                                        }
                                    }).create().show();
                        }})
                    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                        
                    }).create().show();
        }
    }
}