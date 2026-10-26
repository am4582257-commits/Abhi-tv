
package com.abhitv.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private final int BG = Color.rgb(12, 12, 16);
    private final int RED = Color.rgb(229, 9, 20);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(170, 170, 180);
    private final int CARD = Color.rgb(38, 38, 46);

    private LinearLayout root;
    private JSONArray movies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        movies = loadMovies();
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private JSONArray loadMovies() {
        try {
            InputStream input = getAssets().open("movies.json");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            input.close();

            String json = output.toString("UTF-8");
            JSONObject data = new JSONObject(json);
            return data.optJSONArray("movies") != null
                    ? data.optJSONArray("movies") : new JSONArray();

        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18, 20, 24);
        root.setBackgroundColor(BG);

        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text("ABHI", 27, RED);
        logo.setTypeface(null, Typeface.BOLD);
        header.addView(logo);

        TextView tv = text(" TV", 27, WHITE);
        tv.setTypeface(null, Typeface.BOLD);
        header.addView(tv);

        View spacer = new View(this);
        header.addView(spacer,
                new LinearLayout.LayoutParams(0, 1, 1));

        TextView profile = text("●", 23, WHITE);
        profile.setGravity(Gravity.CENTER);
        profile.setOnClickListener(v ->
                Toast.makeText(this, "Profile feature coming soon",
                        Toast.LENGTH_SHORT).show());
        header.addView(profile);

        root.addView(header);

        TextView tagline = text(
                "YOUR ENTERTAINMENT. YOUR WORLD.", 10, MUTED);
        tagline.setPadding(0, 2, 0, 18);
        root.addView(tagline);

        TextView featured = text(
                "ABHI TV ORIGINALS\n\nEntertainment starts here",
                23, WHITE);
        featured.setTypeface(null, Typeface.BOLD);
        featured.setGravity(Gravity.CENTER_VERTICAL);
        featured.setPadding(18, 22, 12, 22);
        featured.setBackground(shape(Color.rgb(57, 12, 22), 18));
        root.addView(featured,
                new LinearLayout.LayoutParams(-1, 190));

        TextView note = text(
                "Discover your next favourite", 13, MUTED);
        note.setPadding(2, 12, 0, 18);
        root.addView(note);

        addSection("Continue Watching", new String[]{
                "▶  Start Exploring", "＋  Add to Watchlist"
        });

        addMovieSection("Popular Movies", "Movies");
        addMovieSection("Web Series", "Web Series");
        addMovieSection("TV Shows", "TV Shows");
        addMovieSection("Cartoons", "Cartoons");

        TextView footer = text(
                "HOME       MOVIES       SERIES       MY LIST",
                12, WHITE);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 24, 0, 10);
        root.addView(footer);
    }

    private void addSection(String title, String[] items) {
        TextView heading = text(title, 19, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, 12, 0, 10);
        root.addView(heading);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String item : items) {
            TextView card = text(item, 13, WHITE);
            card.setGravity(Gravity.CENTER);
            card.setTypeface(null, Typeface.BOLD);
            card.setPadding(12, 12, 12, 12);
            card.setBackground(shape(CARD, 12));

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, 100, 1);
            lp.setMargins(0, 0, 8, 0);
            row.addView(card, lp);
        }

        root.addView(row);
    }

    private void addMovieSection(String title, String category) {
        TextView heading = text(title, 19, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, 18, 0, 10);
        root.addView(heading);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int found = 0;

        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.optJSONObject(i);
            if (movie == null) continue;

            String movieCategory = movie.optString("category", "Movies");
            if (!movieCategory.equalsIgnoreCase(category)) continue;

            String name = movie.optString("title", "Untitled");
            String description = movie.optString(
                    "description", "No description available.");
            String videoUrl = movie.optString("videoUrl", "");

            TextView card = text(name, 14, WHITE);
            card.setGravity(Gravity.CENTER);
            card.setTypeface(null, Typeface.BOLD);
            card.setPadding(10, 10, 10, 10);
            card.setBackground(shape(CARD, 12));

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, 120, 1);
            lp.setMargins(0, 0, 8, 0);
            row.addView(card, lp);

            card.setOnClickListener(v ->
                    showDetails(name, description, videoUrl));

            found++;
        }

        if (found == 0) {
            TextView empty = text(
                    "Content will appear here", 13, MUTED);
            empty.setPadding(2, 0, 0, 8);
            row.addView(empty);
        }

        root.addView(row);
    }

    private void showDetails(String title, String description,
                             String videoUrl) {
        root.removeAllViews();

        TextView back = text("←  Back to Home", 16, WHITE);
        back.setPadding(0, 12, 0, 24);
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        TextView name = text(title, 26, WHITE);
        name.setTypeface(null, Typeface.BOLD);
        root.addView(name);

        TextView desc = text(description, 15, MUTED);
        desc.setPadding(0, 16, 0, 24);
        root.addView(desc);

        TextView play = text("▶  PLAY", 17, WHITE);
        play.setGravity(Gravity.CENTER);
        play.setBackground(shape(RED, 12));
        play.setPadding(18, 16, 18, 16);
        root.addView(play);

        play.setOnClickListener(v -> {
            if (videoUrl.trim().isEmpty()) {
                Toast.makeText(this,
                        "Video link has not been added yet",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "Video player will be connected next",
                        Toast.LENGTH_LONG).show();
            }
        
        });
    }
}
