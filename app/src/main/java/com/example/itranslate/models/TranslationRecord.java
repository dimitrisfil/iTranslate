package com.example.itranslate.models;

public class TranslationRecord {
    private String userId, country, sourceLanguage, targetLanguage, text;
    private long timestamp;
    private double latitude, longitude;

    private TranslationRecord(TranslationRecord.Builder builder) {
        this.userId = builder.userId;
        this.country = builder.country;
        this.sourceLanguage = builder.sourceLanguage;
        this.targetLanguage = builder.targetLanguage;
        this.text = builder.text;
        this.timestamp = builder.timestamp;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
    }

    public static class Builder {

        private String userId, country, sourceLanguage, targetLanguage, text;
        private long timestamp;
        private double latitude, longitude;

        public Builder(String userId) {
            this.userId = userId;
        }

        public TranslationRecord.Builder withCountry(String country) {
            this.country = country;
            return this;
        }

        public TranslationRecord.Builder withLanguages(String sourceLanguage, String targetLanguage) {
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            return this;
        }

        public TranslationRecord.Builder withText(String text) {
            this.text = text;
            return this;
        }

        public TranslationRecord.Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public TranslationRecord.Builder withLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public TranslationRecord build() {
            return new TranslationRecord(this);
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getCountry() {
        return country;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
