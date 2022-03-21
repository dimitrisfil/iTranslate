package com.example.itranslate.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.itranslate.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    private EditText email;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.loginProgressBar);

        email = findViewById(R.id.loginEmailAddress);
        password = findViewById(R.id.loginPassword);
    }

    @Override
    protected void onStart() {
        super.onStart();
        progressBar.setVisibility(View.GONE);
        // If the user is logged in, redirect them to translate activity
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Login to Firebase and redirect user to translate activity
     *
     * @param view View
     */
    public void login(View view) {
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(MainActivity.this, R.string.loginEmpty,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Reset password
     *
     * @param view View
     */
    public void resetPassword(View view) {
        String emailText = email.getText().toString();
        if (emailText.isEmpty()) {
            Toast.makeText(MainActivity.this, R.string.emailEmpty,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(emailText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, R.string.emailReset,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Go to register form
     *
     * @param view View
     */
    public void goToRegister(View view) {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    /**
     * Close app on back pressed
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}