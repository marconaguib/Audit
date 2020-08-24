package com.audit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorResolver;

import java.io.File;


public class Login extends AppCompatActivity{

    private static final String TAG = "EmailPassword";
    
    // Declarations
    private FirebaseAuth mAuth;
    public static final int RESULT_NEEDS_MFA_SIGN_IN = 42;
    LinearLayout emailPasswordButtons,emailPasswordFields;
    Button emailSignInButton,reloadButton,signOutButton;
    EditText fieldEmail,fieldPassword;
    LinearLayout signedInButtons;
    TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Les elements de l'affichage
        emailPasswordButtons = findViewById(R.id.emailPasswordButtons);
        emailPasswordFields = findViewById(R.id.emailPasswordFields);
        emailSignInButton = findViewById(R.id.emailSignInButton);
        fieldEmail = findViewById(R.id.fieldEmail);
        fieldPassword = findViewById(R.id.fieldPassword);
        reloadButton = findViewById(R.id.reloadButton);
        signOutButton = findViewById(R.id.signOutButton);
        signedInButtons = findViewById(R.id.signedInButtons);
        status = findViewById(R.id.status);
        mAuth = FirebaseAuth.getInstance();
        
        //Récuperer les préferences pour sauvegarder l'adresse mail et mot de passe
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = app_preferences.edit();
               
        emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = fieldEmail.getText().toString();
                String password = fieldPassword.getText().toString();
                Log.d(TAG, "signIn:" + email);
                if (!validateForm()) {
                    return;
                }
                //Connexion
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Succès. Mise à jour des données importante
                                    Log.d(TAG, "signInWithEmail:success");
                                    Toast.makeText(Login.this, "Connexion réussie.",
                                            Toast.LENGTH_SHORT).show();
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                    finish();
                                    
                                    editor.putString("email",email);
                                    editor.putString("password",password);
                                    editor.apply();
                                    //Passer à la signature
                                    if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/signature.jpg").exists()) goToSignature();

                                } else {
                                    //Message d'échec sinon
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(Login.this, "Echec d'authentification",
                                            Toast.LENGTH_SHORT).show();
                                    updateUI(null);
                                    checkForMultiFactorFailure(task.getException());
                                }

                                if (!task.isSuccessful()) {
                                    status.setText(R.string.auth_failed);
                                }
                            }
                        });
            }
        });
        //Bouton déconnexion
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                updateUI(null);
                //Suppression de la signature
                new File (Environment.getExternalStorageDirectory().getAbsolutePath() +"/Android/data/com.audit/signature.jpg").delete();
                editor.putString("email",null);
                editor.putString("password",null);
                editor.apply();
            }
        });
        //Vérifier qu'on est toujours connecté
        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateUI(mAuth.getCurrentUser());
                            Toast.makeText(Login.this,
                                    "Rechargé avec succès.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "reload", task.getException());
                            Toast.makeText(Login.this,
                                    "Le rechargement a échoué.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    
    //Valider les entrées
    private boolean validateForm() {
        boolean valid = true;

        String email = fieldEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            fieldEmail.setError("Champs requis.");
            valid = false;
        } else {
           fieldEmail.setError(null);
        }

        String password = fieldPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            fieldPassword.setError("Champs requis.");
            valid = false;
        } else {
            fieldPassword.setError(null);
        }

        return valid;
    }

    //Mise à jour du texte en fonction de l'utilisateur
    private void updateUI(FirebaseUser user) {
       // hideProgressBar();
        if (user != null) {
            status.setText("Connecté en tant que "+user.getEmail().substring(0,user.getEmail().indexOf('@')));

            emailPasswordButtons.setVisibility(View.GONE);
            emailPasswordFields.setVisibility(View.GONE);
            signedInButtons.setVisibility(View.VISIBLE);
            
        } else {
            status.setText(R.string.signed_out);

            emailPasswordButtons.setVisibility(View.VISIBLE);
            emailPasswordFields.setVisibility(View.VISIBLE);
            signedInButtons.setVisibility(View.GONE);
        }
    }

    //Detecter les erreurs dues à plusieurs raisons à la fois
    //cf le Logcat
    private void checkForMultiFactorFailure(Exception e) {
        // Multi-factor authentication with SMS is currently only available for
        // Google Cloud Identity Platform projects. For more information:
        // https://cloud.google.com/identity-platform/docs/android/mfa
        if (e instanceof FirebaseAuthMultiFactorException) {
            Log.w(TAG, "multiFactorFailure", e);
            Intent intent = new Intent();
            MultiFactorResolver resolver = ((FirebaseAuthMultiFactorException) e).getResolver();
            intent.putExtra("EXTRA_MFA_RESOLVER", resolver);
            setResult(RESULT_NEEDS_MFA_SIGN_IN, intent);
            finish();
        }
    }

    private void goToSignature() {
        Intent intent = new Intent(this, Signature.class);
        startActivity(intent);
    }
}
    
