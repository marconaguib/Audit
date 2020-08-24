package com.audit;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.audit.MainActivity.Auditeur;
import static com.audit.MainActivity.Entite;
import static com.audit.MainActivity.Intervenant;
import static com.audit.MainActivity.Localisation;
import static com.audit.MainActivity.Marche;
import static com.audit.MainActivity.NumeroLocal;
import static com.audit.MainActivity.Technicien;
import static com.audit.MainActivity.Type;
import static com.audit.MainActivity.adressefichiercree;
import static com.audit.MainActivity.date;


public class Nouveau extends AppCompatActivity {
    
    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
    Spinner SpinnerMarche, SpinnerIntervenant, SpinnerType ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nouveau);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        //Vérification que le répertoire de la présence du répertoire Audits
        final File repertoire = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/Audits/");
        if(!repertoire.exists()){
            repertoire.mkdir();
        }
        date = dateFormat.format(new Date());

        //Récuperer les préferences (pour avoir le dernier marche à être sélectionné, s'il y en a)
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int N_marche = app_preferences.getInt("N_marche",0);

        //Avoir les listes des marchés, intervenants, entites
        ArrayList<String> Entites = new ArrayList<String>();
        ArrayList<String> Marches = new ArrayList<String>();
        List<List<String>> Intervenants = new ArrayList<List<String>>();
        
        InputStreamReader is = null;
        try {
            File donnees = new File (Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_mar");
            if (donnees.exists()) is = new FileReader(donnees);
            else is = new InputStreamReader(getAssets().open("liste_marches"));
            BufferedReader reader = new BufferedReader(is);
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] ligne = line.split(";");


                    //prendre en compte l'entite
                    Entites.add(ligne[0]);
                    //prendre en compte le marche
                    Marches.add(ligne[1]);
                    //ajouter les intervenants de ce marché
                    Intervenants.add(new ArrayList<String>(Arrays.asList(ligne).subList(2, ligne.length)));
                    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        SpinnerMarche = findViewById(R.id.spinnerMarche);
        ArrayAdapter<String> adapterM = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,Marches);
        adapterM.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerMarche.setAdapter(adapterM);
        //mettre la selection sur le dernier marché selectionné
        SpinnerMarche.setSelection(N_marche);
        
        SpinnerIntervenant = findViewById(R.id.spinnerIntervenant);
        ArrayAdapter<String> adapterI = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapterI.addAll((Intervenants.get(N_marche)));
        adapterI.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerIntervenant.setAdapter(adapterI);
        

        SharedPreferences.Editor editor = app_preferences.edit();

        //sauvegarder le marché dans les préference en cas de changement
        SpinnerMarche.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                adapterI.clear();
                adapterI.addAll(Intervenants.get(i));
                SpinnerIntervenant.setAdapter(adapterI);
                Entite = Entites.get(i);
                editor.putInt("N_marche", i);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                
            }
        });
        
        //Liste des activités, je ne l'ai pas integrée dans le fichiers donnees_mar comme elle ne va pas changer aussi souvent
        ArrayList<String> Types = new ArrayList<String>();
        Types.add("Etude");
        Types.add("Raccordement");
        Types.add("Tirage aérien");
        Types.add("Tirage souterrain");
        Types.add("Génie civil");
        Types.add("Poteau");
        Types.add("Infrastructure (Radio)");
        Types.add("IMES (Radio)");
        //Types.add("Votre nouveau type");
        SpinnerType = findViewById(R.id.spinnerType);
        ArrayAdapter<String> adapterT = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,Types);
        adapterT.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerType.setAdapter(adapterT);

        //Si le champ technicien est ouvert et on clique ailleurs, on veut faire disparaitre le clavier
        EditText ReponseTechnicien = findViewById(R.id.nomTechnicien);
        ReponseTechnicien.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
        

        //Passer à la redaction, pourvu qu'il n'y a pas de problème de géolocalisation
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(Nouveau.this);
                LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(1000);

                LocationCallback locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        Location loc = locationResult.getLastLocation();
                        Localisation = loc.getLatitude()+" "+loc.getLongitude();
                    }
                };

                fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback, Looper.myLooper());
                if (Localisation == null) {
                    Toast.makeText(getApplicationContext(),"Merci d'activer la géolocalisation dans les paramètres de votre téléphone et de réessayer.",Toast.LENGTH_SHORT).show();
                }
                else if(ReponseTechnicien.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(),"Merci d'indiquer le nom du représentant de l'entreprise s'il est présent, sinon mettez \"Absent\".",Toast.LENGTH_SHORT).show();
                }else {
                    //Recuperer les bonnes données
                    Marche = SpinnerMarche.getSelectedItem().toString();
                    Intervenant = SpinnerIntervenant.getSelectedItem().toString();
                    Type = SpinnerType.getSelectedItem().toString();
                    Technicien = ReponseTechnicien.getText().toString().replaceAll("[#$.\\[\\]]", "").replace("/","\\");
                    //Créer les fichiers pdf et csv
                    //Dans les noms, on évite les cartère spéciaux pour ne pas gêner la base de données
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        NumeroLocal = (Objects.requireNonNull(repertoire.listFiles()).length) / 2 + 1;
                        NumeroLocal = (Objects.requireNonNull(repertoire.listFiles()).length) / 2 + 1;
                    }
                    String NomProvisoire = (Marche + "_" + Intervenant + "_" + Type.charAt(0) + "_"+ date).replaceAll("[#$.\\[\\]]", "");
                    adressefichiercree = repertoire.getAbsolutePath() + "/" + NumeroLocal + "_" + Auditeur + "_" + NomProvisoire + ".pdf";
                    File dest = new File(adressefichiercree);
                    File destcsv = new File(adressefichiercree.replace(".pdf", ".csv"));
                    try {
                        dest.createNewFile();
                        destcsv.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    goToRedac();
                }
            }
        });
    }

    private void goToRedac() {
        Intent intent = new Intent(this, Securite.class);
        startActivity(intent);
        finish();
    }


    //faire disparaitre le clavier
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Nouveau.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
