package com.audit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

import static com.audit.R.id.bienvenue;


public class MainActivity extends AppCompatActivity {

    public static String adressefichiercree;
    public static Integer NumeroLocal;
    public static String Technicien;
    public static String Marche;
    public static String Intervenant;
    public static String Type;
    public static String date;
    public static String Localisation;
    public static float Sign_Position_Top;
    public static boolean signe_par_tech;
    public static String Entite;
    public static String Auditeur;
    

    private FirebaseAuth mAuth;

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    Button ButNouveau, ButAudits;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

                      
        //Demandes des permissions, si elles ne sont pas déjà acquises. 
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsToRequest = permissionsToRequest(permissions);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }
        
        //Création du répertoire propre à l'application
        final File repertoire = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/");
        if(!repertoire.exists()){
            repertoire.mkdir();
            NumeroLocal=0;
        }
        
        //Initialiser les deux boutons
        ButNouveau = findViewById(R.id.buttonnouveau);
        ButAudits = findViewById(R.id.buttonaudits);
        ButAudits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToAudits();
            }
        });

        
        //Verifier l'identification, si l'utilisateur s'était déjà identifié
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String email = app_preferences.getString("email",null);
        String password = app_preferences.getString("password",null);
        if(email!= null && password!= null){
            mAuth.signInWithEmailAndPassword(email,password);
        }
        else mAuth.signOut();

        //Initialisation de la mise à jour de la demande de géolocalisation
        //Elle aurait pu être faite plus tard (quand elle sera nécessaire)
        //Mais c'est mieux de la faire maintenant, comme elle met du temps.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

        //Mise à jour
        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback, Looper.myLooper());

        //Chargement du des deux fichiers des donnees présents dans storage, et les déposer dans le répertoire com.audit
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference marchesRef = storageRef.child("donnees/donnees_marches.txt");
        StorageReference pointsRef = storageRef.child("donnees/donnees_points.txt");
        File marchesfile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_mar");
        File pointsfile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_sec");
        marchesRef.getFile(marchesfile);
        pointsRef.getFile(pointsfile);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth = FirebaseAuth.getInstance();
        
        //OnResume est appelée quand l'activité fille est détruite
        //C'est le cas par exemple quand on passe à la page de connexion, puis on la ferme
        //Il est donc nécessaire de gérer le texte d'accueil ici plutôt que dans OnCreate
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView bienv = findViewById(bienvenue);
        if (currentUser == null) {
            bienv.setText("Merci de vous identifier.");
            ButNouveau.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Vous ne vous êtes pas encore identifié.e")
                            .setMessage("Merci de le faire d'abord")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    goToLogin();
                                }
                            }).create().show();
                }
            });
        } else if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/signature.jpg").exists()) {
            bienv.setText("Vous n'avez pas encore enregistré votre signature !");
            ButNouveau.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Pas de signature")
                            .setMessage("Vous n'avez pas enregistré votre signature. Celle-ci doit être enregistrée avant la rédaction. Voulez-vous l'enregistrer d'abord ?")
                            .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    goToNouveau();
                                }
                            })
                            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    goToSignature();
                                }
                            }).create().show();
                }
            });
        } else {
            Auditeur = Objects.requireNonNull(currentUser.getEmail()).substring(0,currentUser.getEmail().indexOf('@')).replace(".","");
            bienv.setText("Bienvenue à OKULUS, "+Auditeur+" !");
            ButNouveau.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToNouveau();
                }
            });
        }
        
        
        //Petit texte pour donner la date de la dernière mise à jour
        //Concrètement, c'est la date de la dernière modification qu'a subit le fichier data_mar
        //Mais les deux fichiers sont toujours touchés en même temps
        TextView MiseAJour = findViewById(R.id.mise_a_jour);
        File localFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/data_mar");
        if (localFile.exists()) MiseAJour.setText("Dernière mise à jour des données :\n"+new SimpleDateFormat("dd-MM-yyyy HH:mm").format(localFile.lastModified()));
        else MiseAJour.setText("Pas de mise à jour.");
        
    }

    //Menu trois petits points
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            goToSignature();
        }
        else if (id==R.id.action_login){
            goToLogin();
        }

        return super.onOptionsItemSelected(item);
    }

    private void goToNouveau() {
        Intent intent = new Intent(this, Nouveau.class);
        startActivity(intent);
    }

    private void goToAudits() {
        Intent intent = new Intent(this, Audits.class);
        startActivity(intent);
    }

    private void goToSignature() {
        Intent intent = new Intent(this, Signature.class);
        startActivity(intent);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    //Fonctions qui s'occupent de demander les permissions, et vérifier qu'elle sont acquises.
    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (hasNoPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasNoPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED;
        }

        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (String perm : permissionsToRequest) {
                if (hasNoPermission(perm)) {
                    permissionsRejected.add(perm);
                }
            }

            if (permissionsRejected.size() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                        new AlertDialog.Builder(MainActivity.this).
                                setMessage("Ces permissions sont obligatoires pour accéder à votre localisation. Merci de les autoriser.").
                                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        requestPermissions(permissionsRejected.
                                                toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                    }
                                }).setNegativeButton("Cancel", null).create().show();

                    }
                }
            }
        }
    }
}
