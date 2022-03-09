package com.example.itranslate.models;

public class User {

    private String fistName, lastName, email, role;

    private User(Builder builder) {
        this.fistName = builder.fistName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.role = builder.role;
    }

    public static class Builder {

        private String fistName, lastName, email, role;

        public Builder withFullName(String fistName, String lastName) {
            this.fistName = fistName;
            this.lastName = lastName;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withRole(String role) {
            this.role = role;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    public String getFistName() {
        return fistName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
