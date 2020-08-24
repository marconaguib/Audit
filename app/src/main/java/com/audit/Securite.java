package com.audit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.audit.MainActivity.Auditeur;
import static com.audit.MainActivity.Entite;
import static com.audit.MainActivity.Intervenant;
import static com.audit.MainActivity.Localisation;
import static com.audit.MainActivity.Marche;
import static com.audit.MainActivity.NumeroLocal;
import static com.audit.MainActivity.Sign_Position_Top;
import static com.audit.MainActivity.Technicien;
import static com.audit.MainActivity.Type;
import static com.audit.MainActivity.adressefichiercree;
import static com.audit.MainActivity.signe_par_tech;


public class Securite extends AppCompatActivity {
    private static final int REQUEST_EXIT = 2;

    //variable pour determiner s'il y a des modifications NON SAUVEGARDEES
    boolean modif=false;
    //variable pour determiner si le contrôles a été imprimé une fois (sinon les fichiers pdf et csv sont vides)
    boolean imprime=false;
    //les previews en bas de chaque catégorie
    ImageView[][] les_previews = new ImageView[10][3];
    //les previews tout en bas
    ImageView[] derniers_previews = new ImageView[30];
    public String adressephoto;
    final File repertoire = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_securite);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        
        if(!repertoire.exists()){
            repertoire.mkdir();
        }

        //Vérifier l'existence du repertoire des photos
        File mediaStorageDir = new File(repertoire+"/Photos");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Toast.makeText(getApplicationContext(),"Erreur. Répertoire Audits non trouvable",Toast.LENGTH_SHORT).show();
            }
        }
        
        //Lire les points de securite
        //Et en même temps les mettre dans l'affichage
        //Il est important de noter que les TextView, les CheckedTextView, les Switchs et les EditText sont tous déjà là (content_securite.xml) invisibles
        //Cette procédure décide s'ils seront visibles et, le cas échéant, indiquer le texte qui y sera placé
        //
        //
        //
        //TRES IMPORTANT
        //Les éléments content_securite.xml atteignent la limite de ce que l'on pourrait placer dans un fichier content.
        //Donc il est INUTILE d'essayer d'ajouter des éléments dans content_securité.xml, ils ne seront pas acceptés
        //Le seul moyen propre d'ajouter des points est de simplement les ajouter dans le fichier présent sur Firebase
        //Et donc de respecter la limite des 6 catégories et de 10 points par catégorie
        //Sinon, il serait peut-être mieux d'ajouter d'autre types de contrôles (cf activité Q1)
        try {
            InputStreamReader is;
            File donnees = new File (Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_sec");
            if (donnees.exists()) is = new FileReader(donnees);
            else is = new InputStreamReader(getAssets().open("points_securite"));
            BufferedReader csvReader = new BufferedReader(is);
            String row;
            int nombre_de_row=0;
            //Ligne = catégorie
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(";");
                //Titre
                TextView titre = findViewById(getResources().getIdentifier("titre"+nombre_de_row,"id",getPackageName()));
                titre.setVisibility(View.VISIBLE);
                titre.setText(data[0]);
                for (int i=1; i<data.length;i++){
                    //points de la catégorie
                    CheckedTextView point = findViewById(getResources().getIdentifier("point"+nombre_de_row+"_"+(i-1), "id", getPackageName()));
                    Switch conformite = findViewById(getResources().getIdentifier("switch"+nombre_de_row+"_"+(i-1), "id", getPackageName()));
                    point.setVisibility(View.VISIBLE);
                    point.setChecked(false);
                    conformite.setVisibility(View.VISIBLE);
                    conformite.setEnabled(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) conformite.setShowText(true);
                    point.setText(data[i]);
                    conformite.setOnClickListener(view -> modif=true);
                    point.setOnClickListener(view -> {
                        modif=true;
                        if(point.isChecked()){
                            point.setTextColor(Color.parseColor("#3B000000"));
                            point.setChecked(false);
                            conformite.setEnabled(false);
                        }
                        else {
                            point.setTextColor(Color.parseColor("#FF000000"));
                            point.setChecked(true);
                            conformite.setEnabled(true);
                        }
                    });
                }
                //Comentaires
                EditText commentaire = findViewById(getResources().getIdentifier("editText"+nombre_de_row,"id",getPackageName()));
                commentaire.setVisibility(View.VISIBLE);
                commentaire.setHint("Commentaire "+data[0]);
                commentaire.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (commentaire.hasFocus()) modif=true;
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start,
                                                  int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start,
                                              int before, int count) {
                    }
                });
                //Les 3 ImageViews
                LinearLayoutCompat layout = findViewById(getResources().getIdentifier("Photos"+nombre_de_row,"id",getPackageName()));
                ViewGroup.LayoutParams params = layout.getLayoutParams();
                params.height = 250;
                layout.setLayoutParams(params);
                for (int i=0; i<3; i++){
                    les_previews[nombre_de_row][i] = findViewById(getResources().getIdentifier("monimage"+nombre_de_row+"_"+i,"id",getPackageName()));
                }

                //Bouton ajouter des photos
                Button bouton = findViewById(getResources().getIdentifier("button_image_"+nombre_de_row,"id",getPackageName()));
                bouton.setVisibility(View.VISIBLE);
                int finalNombre_de_row = nombre_de_row;
                bouton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.getName().startsWith(NumeroLocal + "_"+ finalNombre_de_row +"_");
                            }
                        });
                        if (photos.length<3) takePicture(String.valueOf(finalNombre_de_row));
                        else takePicture("f"); //Les photos passent en bas
                    }
                });
                for (ImageView le_preview : les_previews[nombre_de_row]) le_preview.setEnabled(false);
                //Incrémentation de la boucle
                nombre_de_row++;
            }
            
            //ImageViews en bas
            for (int i=0; i<30; i++){
                derniers_previews[i] = findViewById(getResources().getIdentifier("monimagef_"+i, "id", getPackageName()));
                derniers_previews[i].setEnabled(false);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //Bouton imprimer
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            modif=false;
            Imprimer();
        });

    }
    

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        //Au retour à l'application, on remet tout l'affichage comme il faut
        try {
            InputStreamReader is;
            File donnees = new File (Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_sec");
            if (donnees.exists()) is = new FileReader(donnees);
            else is = new InputStreamReader(getAssets().open("points_securite"));
            BufferedReader csvReader = new BufferedReader(is);
            String row;
            int resID;
            int nombre_de_row = 0;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(";");
                for (int i = 1; i < data.length; i++) {
                    CheckedTextView point = findViewById(getResources().getIdentifier("point"+nombre_de_row+"_"+(i-1), "id", getPackageName()));
                    Switch conformite = findViewById(getResources().getIdentifier("switch"+nombre_de_row+"_"+(i-1), "id", getPackageName()));
                    //Affichage de la couleur des points
                    if (point.isChecked()) {
                        point.setTextColor(Color.parseColor("#FF000000"));
                        conformite.setEnabled(true);
                    } else {
                        point.setTextColor(Color.parseColor("#3B000000"));
                        conformite.setEnabled(false);
                    }
                }
                //Affichage des photos par catégorie
                int finalNombre_de_row = nombre_de_row;
                File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().startsWith(NumeroLocal + "_"+ finalNombre_de_row +"_");
                    }
                });
                Log.d("taille",String.valueOf(photos.length));
                if (photos!=null) for(int i = 0; i<photos.length; i++){
                    les_previews[nombre_de_row][i].setEnabled(true);
                    les_previews[nombre_de_row][i].setImageURI(Uri.fromFile(photos[i]));
                }
                nombre_de_row++;
            }
            //Affichage des photos en bas
            File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith(NumeroLocal + "_f_");
                }
            });
            if (photos!=null) {
                for(int i = 0; i<photos.length; i++){
                    derniers_previews[i].setEnabled(true);
                    derniers_previews[i].setImageURI(Uri.fromFile(photos[i]));
                }
                for (int i=0; i<10;i++){
                    if(photos.length>3*i){
                        //Si par exemple il y a moins de 15 photos, il faut s'arrêter à 5 layouts
                        String layoutID = "Photosf"+i;
                        resID = getResources().getIdentifier(layoutID, "id", getPackageName());
                        LinearLayoutCompat layout = findViewById(resID);
                        ViewGroup.LayoutParams params = layout.getLayoutParams();
                        params.height = 250;
                        layout.setLayoutParams(params);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    //Prendre une photos et la sauvegarder avec un nom pertinent
    public void takePicture(String i) {
        if (i.equals("f")) {
            File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith(NumeroLocal + "_f_");
                }
            });
            if(photos.length>=30){
                new AlertDialog.Builder(Securite.this)
                        .setTitle("Trop de photos")
                        .setMessage("Merci d'adresser le restant des photos par mail au responsable.")
                        .setPositiveButton("Ok", null).create().show();
                return;
            }
        }
        //Modification
        modif=true;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        adressephoto = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.audit/Photos/" + NumeroLocal + "_" + i + "_" + timeStamp + ".jpg";
        Uri file = Uri.fromFile(new File(adressephoto));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Après la prise de photo, il faut la compresser et afficher un message de feedback
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                try {
                    modif = true;
                    Uri takenPhotoUri = Uri.fromFile(new File(adressephoto));
                    Bitmap rawTakenImage = BitmapFactory.decodeFile(takenPhotoUri.getPath());
                    Bitmap resizedBitmap = BitmapScaler.scaleToFitWidth(rawTakenImage, 1000);

                    // Configure byte output stream
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    // Compress the image further
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
                    
                    File resizedFile = new File(adressephoto.replace(".jpg", "_r.jpg"));
                    resizedFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(resizedFile);
                    // Write the bytes of the bitmap to file
                    fos.write(bytes.toByteArray());
                    fos.close();
                    new File(adressephoto).delete();
                    resizedFile.renameTo(new File(adressephoto));
                    if (adressephoto.split("_")[1].equals("f")) Toast.makeText(getApplicationContext(), "Photo ajoutée aux photos supplémentaires.", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(getApplicationContext(), "Votre photo a bien été ajoutée.", Toast.LENGTH_SHORT).show();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //Si on sort de l'apercu avec succès, on doit fermer aussi cette activité
        //Et donc revenir à la page d'accueil
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    //Imprime les données actuelles sur le fichier PDf et sur le fichier CSV
    public void Imprimer(){
        try {
            imprime=true;
            Toast.makeText(getApplicationContext(),"Votre Audit est en train d'être créé.",Toast.LENGTH_SHORT).show();
            //Lecture des points
            InputStreamReader is;
            File donnees = new File (Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_sec");
            if (donnees.exists()) is = new FileReader(donnees);
            else is = new InputStreamReader(getAssets().open("points_securite"));
            BufferedReader csvReader = new BufferedReader(is);
            String row;
            int nombre_de_row = 0;
            int resID;
            ArrayList<String>conf = new ArrayList<>();
            ArrayList<String>nonconf = new ArrayList<>();
            ArrayList<String>comm = new ArrayList<>();
            ArrayList<String>categ = new ArrayList<>();
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(";");
                categ.add(data[0]);
                for (int i=1; i<data.length;i++){
                    Switch conformite = findViewById(getResources().getIdentifier("switch"+nombre_de_row+"_"+(i-1), "id", getPackageName()));
                    try {
                        if(conformite.isEnabled()){ //Ici->Le point est applicable
                        if(conformite.isChecked()) conf.add(data[i]); //Ajouter aux points conformes
                        else nonconf.add(data[i]); //Ajouter aux points conformes
                        }
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }
                String commentaireID = "editText"+nombre_de_row;
                resID = getResources().getIdentifier(commentaireID, "id", getPackageName());
                EditText commentaire = findViewById(resID);
                try {
                    comm.add(commentaire.getText().toString()); // Lire le commentaire
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                }
                nombre_de_row++;
            }

            //Ouverture et creation du writer
            FileWriter CSV = new FileWriter(adressefichiercree.replace(".pdf",".csv"));
            Document document = new Document();
            PdfWriter writer =  PdfWriter.getInstance(document, new FileOutputStream(adressefichiercree));
            writer.setStrictImageSequence(true);
            writer.setFullCompression();
            document.open();
            
            //Entete
            String entete = "";
            @SuppressLint("SimpleDateFormat") String dateheure = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            entete+="Date et Heure : "+dateheure+"\n";
            entete+="Géolocalisation : "+Localisation.replace(".",",")+"\n";
            entete+="Activité : "+Type+"\n";
            entete+= "Entité : "+Entite+"\n";
            entete+="Marché : "+Marche+"\n";
            entete+="Intervenant : "+Intervenant+"\n";
            entete+="Auditeur : "+Auditeur+"\n";
            if (!Technicien.equals("")) entete+="Représentant de l'entreprise : "+Technicien+"\n";
            CSV.write(entete.replace(" : ",";").replace("/","\\").replaceAll("[#$.\\[\\]]", "")+"\n\n");
            //Les remplacement servent à éviter les soucis au niveau de la base de données
            document.add(new Paragraph(entete));
            Chunk line = new Chunk(new String(new char[155]).replace("\0", "\u00a0"));
            line.setUnderline(BaseColor.BLACK,0.1f,0.1f,1,1, PdfContentByte.LINE_CAP_ROUND);
            document.add(line);
            
            //Logo
            InputStream ims1 = getAssets().open(Entite+".png");
            Bitmap bmp1 = BitmapFactory.decodeStream(ims1);
            ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
            bmp1.compress(Bitmap.CompressFormat.PNG, 100, stream1);
            Image image1 = Image.getInstance(stream1.toByteArray());
            image1.setAbsolutePosition(400f, 715f);
            image1.scaleAbsolute(151.2f, 90f);
            document.add(image1);
            
            //Conformités
            Font TitreConf = new Font(Font.FontFamily.TIMES_ROMAN,17,Font.BOLD|Font.UNDERLINE, new BaseColor(2,100,64));
            Font Body = new Font(Font.FontFamily.TIMES_ROMAN,15,Font.NORMAL,BaseColor.BLACK);
            document.add(new Paragraph(new Chunk("Conformités :\n",TitreConf)));
            document.add(new Paragraph("\n"));
            Paragraph Conf = new Paragraph();
            for (int i =0; i<conf.size();i++){
                Conf.add(new Chunk(conf.get(i)+"\n",Body));
                CSV.write(conf.get(i).replace("/","\\").replaceAll("[#$.\\[\\]]", "")+";Conforme\n");
            }
            CSV.write("\n\n\n");
            document.add(Conf);
            document.add(new Paragraph("\n"));

            //Conformités
            Font TitreNonconf = new Font(Font.FontFamily.TIMES_ROMAN,17,Font.BOLD|Font.UNDERLINE, new BaseColor(95,0,0));
            document.add(new Paragraph(new Chunk("Non conformités :\n",TitreNonconf)));
            document.add(new Paragraph("\n"));
            Paragraph Nonconf = new Paragraph();
            for (int i =0; i<nonconf.size();i++){
                Nonconf.add(new Chunk(nonconf.get(i)+"\n",Body));
                CSV.write((nonconf.get(i).replace("/","\\").replaceAll("[#$.\\[\\]]", "")+";Non conforme\n"));
            }
            CSV.write("\n\n\n");
            document.add(Nonconf);
            document.add(new Paragraph("\n"));
            document.newPage();

            //Comentaires et photos
            Font TitreCom = new Font(Font.FontFamily.TIMES_ROMAN,17,Font.BOLD|Font.UNDERLINE, BaseColor.BLACK);
            document.add(new Paragraph(new Chunk("Commentaires et photos :\n",TitreCom)));
            document.add(new Paragraph("\n"));
            Font Categ = new Font(Font.FontFamily.TIMES_ROMAN,15,Font.BOLD, BaseColor.BLACK);
            for (int i =0; i<comm.size();i++){
                //Chaque itération traite d'une catégorie
                Paragraph Comm = new Paragraph();
                Comm.add(new Chunk(categ.get(i)+"\n",Categ));
                Comm.add(new Chunk(comm.get(i)+"\n",Body));
                int finalI = i;
                File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().startsWith(NumeroLocal + "_"+ finalI +"_");
                    }
                });
                for (File photo : photos) {
                    Image cette_image = Image.getInstance(photo.getPath());
                    cette_image.scalePercent(50);
                    Comm.add(cette_image);
                }
                Comm.add(new Paragraph("_____________________________________________________________"));
                document.add(Comm);
                document.newPage();
            }
            document.add(new Paragraph("\n"));
            
            //Photos supplémentaires
            Paragraph Comm = new Paragraph();
            Comm.add(new Chunk("Photos supplémentaires\n",Categ));
            File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith(NumeroLocal + "_f_");
                }
            });
            for (File photo : photos) {
                Image cette_image = Image.getInstance(photo.getPath());
                cette_image.scalePercent(50);
                Comm.add(cette_image);
            }
            document.add(Comm);
            
            //Signatures
            document.newPage();
            document.add(new Paragraph(new Chunk("Signatures :\n",TitreCom)));
            PdfPTable table = new PdfPTable(2);
            table.setTotalWidth(new float[]{160, 160});
            table.setLockedWidth(true);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.setSpacingBefore(20f);
            table.setKeepTogether(true);
            PdfPCell cell;
            cell = new PdfPCell(new Phrase("Auditeur : " + Auditeur, Body));
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Représentant de l'entreprise : " + Technicien, Body));
            table.addCell(cell);
            File signature = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/signature.jpg");
            if(signature.exists()){
                cell = new PdfPCell();
                Image sign = Image.getInstance(signature.getAbsolutePath());
                cell.addElement(sign);
                table.addCell(cell);
            }
            else table.addCell("\n\n\n\n\n\n\n");
            table.addCell("\n\n\n\n\n\n\n");
            document.add(table);
            
            //Recuperer l'endroit ou la signature devra ensuite être collée 
            Sign_Position_Top=writer.getVerticalPosition(false);
            
            //Fermeture
            CSV.close();
            csvReader.close();
            document.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        goToApercu();
    }
    
    //Gestion des DialogBox de sortie en fonction des cas
    @Override
    public void onBackPressed() {
        if(modif) {
            new AlertDialog.Builder(Securite.this)
                    .setTitle("Sortir sans sauvegarder")
                    .setMessage("Vous avez des modifications non sauvegardées. Êtes vous sûr de vouloir quitter ?")
                    .setNegativeButton("Non", null)
                    .setPositiveButton("Oui", (arg0, arg1) -> {
                        Securite.super.onBackPressed();
                        if(!imprime){
                            new File(adressefichiercree).delete();
                            new File(adressefichiercree.replace(".pdf",".csv")).delete();
                            File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                    return file.getName().startsWith(NumeroLocal + "_");
                                }
                            });
                            for (File photo : photos) photo.delete();
                        }
                    }).create().show();
        }
        else if (imprime && !signe_par_tech){
            new AlertDialog.Builder(this)
                    .setTitle("Sortir sans signer")
                    .setMessage("Le représentant n'ayant pas signé, le contrôle n'est pas valide. Êtes vous sûr de vouloir quitter ?")
                    .setNegativeButton("Non", null)
                    .setPositiveButton("Oui", (arg0, arg1) -> Securite.super.onBackPressed()).create().show();
        }
        else {
            if (!imprime) {
                new File(adressefichiercree).delete();
                new File(adressefichiercree.replace(".pdf",".csv")).delete();
                File[] photos = new File(repertoire+"/Photos").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().startsWith(NumeroLocal + "_");
                    }
                });
                for (File photo : photos) photo.delete();
            }
            finish();
        }
    }
    
    private void goToApercu() {
        Intent intent = new Intent(this, Apercu.class);
        startActivityForResult(intent, REQUEST_EXIT);
    }


    //Sauvegarder les valeurs des deux variable quand on sort
    //Par exemple, quand on passe à l'appareil photo
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("modif", modif);
        savedInstanceState.putBoolean("imprime", imprime);
    }
    
    
    //Restaurer les valeurs des deux variable quand on revient
    //Par exemple, quand on passe à l'appareil photo
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        modif = savedInstanceState.getBoolean("modif");
        imprime = savedInstanceState.getBoolean("imprime");
    }
}

//Compression de photos
class BitmapScaler {
    // scale and keep aspect ratio
    public static Bitmap scaleToFitWidth(Bitmap b, int width) {
        float factor = width / (float) b.getWidth();
        return Bitmap.createScaledBitmap(b, width, (int) (b.getHeight() * factor), true);
    }
}

