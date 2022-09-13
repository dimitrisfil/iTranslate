package com.example.itranslate.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.itranslate.R;
import com.example.itranslate.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText firstName, lastName, email, password, retypePassword;
    private ProgressBar progressBar;
    private final String TAG = "register";
    private final String ROLE_USER = "USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.registerProgressBar);

        firstName = findViewById(R.id.firstNameRegister);
        lastName = findViewById(R.id.lastNameRegister);
        email = findViewById(R.id.emailAddressRegister);
        password = findViewById(R.id.passwordRegister);
        retypePassword = findViewById(R.id.passwordRegister2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        progressBar.setVisibility(View.GONE);
    }

    /**
     * This method registers user to Firebase
     *
     * @param view View
     */
    public void register(View view) {
        String firstNameText = firstName.getText().toString();
        String lastNameText = lastName.getText().toString();
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();
        String retypePasswordText = retypePassword.getText().toString();

        User user = new User.Builder()
                .withFullName(firstNameText, lastNameText)
                .withEmail(emailText)
                .withRole(ROLE_USER)
                .build();

        // Validate form inputs
        if (!validateFields(user, passwordText, retypePasswordText)) {
            return;
        }

        // Firebase method to create a user
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        db.collection("users")
                                .document(firebaseUser.getUid())
                                .set(user)
                                .addOnSuccessListener(documentReference -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, R.string.registerSuccess,
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Log.w(TAG, "Error adding document", e);
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, R.string.registerFailed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * This method validates form inputs
     *
     * @param user User
     * @param password String
     * @param retypePassword String
     * @return boolean
     */
    public boolean validateFields(User user,
                                  String password,
                                  String retypePassword) {
        if (user.getFirstName().equals("") ||
                user.getLastName().equals("") ||
                user.getEmail().equals("") ||
                password.equals("") ||
                retypePassword.equals("")) {
            Toast.makeText(RegisterActivity.this, R.string.fillAllFields, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidEmail(user.getEmail())) {
            Toast.makeText(RegisterActivity.this, R.string.invalidEmail, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(RegisterActivity.this, R.string.invalidPassword, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(retypePassword)) {
            Toast.makeText(RegisterActivity.this, R.string.passwordsDontMatch, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}