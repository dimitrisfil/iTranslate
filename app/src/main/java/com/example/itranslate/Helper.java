package com.example.itranslate;

import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.itranslate.activities.MainActivity;
import com.example.itranslate.activities.RegisterActivity;
import com.example.itranslate.models.TranslationRecord;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Helper {

    public static void generateRandomRecords(Resources resources) {
        FirebaseFirestore db= FirebaseFirestore.getInstance();

        String[] userIds = {
                "1atksWZbn6TKbe0IVAU4jrkkieP2",
                "coV9YIZzrRQA4HTvVvfk5tqXP3C3",
                "OLJrJhgSiKMivYWva1SAaiiyHYH2",
                "XQgu3TTi2PTslwuiOWG4ztX6sXo2",
                "WZkVkkPsvHaoUmXbWCZ7cY16pUy1",
                "POrXFq9zLzZADn1VEOCuNjF05MD3",
                "LTrGZKcJnFMFXijCP0uhocpiWjY2",
                "8W7ZvRtPvPguofZSCSkeO87HZJo2",
                "MHhOABFqbzT3EFWuHQ46vunwCY73",
                "K68gQilXanePmpMXBXfScUp899U2"
        };

        String[] countries = {
                "France",
                "United States",
                "China",
                "Spain",
                "Italy",
                "United Kingdom",
                "Germany",
                "Malaysia",
                "Mexico",
                "Austria",
                "Greece",
                "Canada"
        };

        String[] text = {
            "All she wanted was the answer, but she had no idea how much she would hate it.\n" +
                    "The beauty of the sunset was obscured by the industrial cranes.\n" +
                    "She saw the brake lights, but not in time.",
                "The view from the lighthouse excited even the most seasoned traveler.",
                "He used to get confused between soldiers and shoulders, but as a military man, he now soldiers responsibility.\n" +
                        "Three generations with six decades of life experience.",
                "I made myself a peanut butter sandwich as I didn't want to subsist on veggie crackers.\n",
                "He quietly entered the museum as the super bowl started.\n" +
                        "She had the gift of being able to paint songs.\n" +
                        "She wanted to be rescued, but only if it was Tuesday and raining.",
                "It was the scarcity that fueled his creativity.",
                "He figured a few sticks of dynamite were easier than a fishing pole to catch fish.\n" +
                        "Courage and stupidity were all he had.",
                "She let the balloon float up into the air with her hopes and dreams.\n" +
                        "Mom didn’t understand why no one else wanted a hot tub full of jello.\n" +
                        "Don't piss in my garden and tell me you're trying to help my plants grow.",
                "My biggest joy is roasting almonds while stalking prey.",
                "My biggest joy is roasting almonds while stalking prey.",
                "Green should have smelled more tranquil, but somehow it just tasted rotten.\n" +
                        "It's important to remember to be aware of rampaging grizzly bears.",
                "I may struggle with geography, but I'm sure I'm somewhere around here.\n" +
                        "Greetings from the real universe.\n" +
                        "The tree fell unexpectedly short.",
                "The elephant didn't want to talk about the person in the room.\n" +
                        "Having no hair made him look even hairier.",
                "The gloves protect my feet from excess work.\n" +
                        "All she wanted was the answer, but she had no idea how much she would hate it.",
                "The door slammed on the watermelon.\n" +
                        "She was sad to hear that fireflies are facing extinction due to artificial light, habitat loss, and pesticides.\n",
                "Giving directions that the mountains are to the west only works when you can see them.\n" +
                        "The white water rafting trip was suddenly halted by the unexpected brick wall.\n" +
                        "That must be the tenth time I've been arrested for selling deep-fried cigars.",
                "He hated that he loved what she hated about hate.\n",
                "The minute she landed she understood the reason this was a fly-over state.\n" +
                        "Truth in advertising and dinosaurs with skateboards have much in common.\n",
                "He waited for the stop sign to turn to a go sign.\n" +
                        "He kept telling himself that one day it would all somehow make sense.",
                "The virus had powers none of us knew existed.\n" +
                        "Mary plays the piano.",
                "His confidence would have bee admirable if it wasn't for his stupidity.\n" +
                        "A song can make or ruin a person’s day if they let it get to them.",
                "The blinking lights of the antenna tower came into focus just as I heard a loud snap.\n" +
                        "The truth is that you pay for your lifestyle in hours.",
                "The dead trees waited to be ignited by the smallest spark and seek their revenge.\n" +
                        "The gloves protect my feet from excess work.\n" +
                        "He wasn't bitter that she had moved on but from the radish.",
                "When money was tight, he'd get his lunch money from the local wishing well.",
                "Just because the water is red doesn't mean you can't drink it.\n" +
                        "As he waited for the shower to warm, he noticed that he could hear water change temperature.",
                "Today is the day I'll finally know what brick tastes like.\n" +
                        "The toy brought back fond memories of being lost in the rain forest.\n" +
                        "It took him a month to finish the meal.",
                "It took him a while to realize that everything he decided not to change, he was actually choosing.\n",
                "She hadn't had her cup of coffee, and that made things all the worse.",
                "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
                "We have never been to Asia, nor have we visited Africa.\n" +
                        "I received a heavy fine but it failed to crush my spirit."
        };

        String[] languageCodes = resources.getStringArray(R.array.supportedLanguages);

        long offset = Timestamp.valueOf("2021-01-01 00:00:00").getTime();
        long end = Timestamp.valueOf("2022-03-02 00:00:00").getTime();
        long diff = end - offset + 1;

        int userRnd, countryRnd, languageRnd1, languageRnd2, textRnd;
        long dateRand;
        for (int i = 0; i < 100; i++) {
            userRnd = new Random().nextInt(userIds.length);
            countryRnd = new Random().nextInt(countries.length);
            languageRnd1 = new Random().nextInt(languageCodes.length);
            languageRnd2 = new Random().nextInt(languageCodes.length);
            textRnd = new Random().nextInt(text.length);
            dateRand = new Timestamp(offset + (long)(Math.random() * diff)).getTime();

            TranslationRecord record = new TranslationRecord.Builder(userIds[userRnd])
                    .withCountry(countries[countryRnd])
                    .withLanguages(languageCodes[languageRnd1], languageCodes[languageRnd2])
                    .withText(text[textRnd])
                    .withTimestamp(dateRand)
                    .build();

            db.collection("translations")
                    .add(record)
                    .addOnSuccessListener(documentReference -> {
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }

    public static void languageCodesToLanguages(Resources resources) {
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        db.collection("translations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int languageRnd1, languageRnd2;
                        String[] languageCodes = resources.getStringArray(R.array.supportedLanguages);
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Locale sourceLocale = new Locale(document.getData().get("sourceLanguage").toString());
                            String sourceName = sourceLocale.getDisplayLanguage(sourceLocale);

                            Locale targetLocale = new Locale(document.getData().get("targetLanguage").toString());
                            String targetName = targetLocale.getDisplayLanguage(targetLocale);

                            languageRnd1 = new Random().nextInt(languageCodes.length);
                            languageRnd2 = new Random().nextInt(languageCodes.length);

                            Map<String, String> update = new HashMap<>();
                            update.put("sourceLanguage", languageCodes[languageRnd1]);
                            update.put("targetLanguage", languageCodes[languageRnd2]);
                            db.collection("translations").document(document.getId()).set(update, SetOptions.merge());
                        }
                    }
                });
    }
}
