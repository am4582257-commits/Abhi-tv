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
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    return drawable;
}

private TextView text(String value, int size, int color) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setGravity(Gravity.CENTER_VERTICAL);
    return view;
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

        JSONObject data = new JSONObject(
                output.toString("UTF-8")
        );

        JSONArray list = data.optJSONArray("movies");

        return list != null ? list : new JSONArray();

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
    root.setPadding(20, 18, 20, 28);
    root.setBackgroundColor(BG);

    scroll.addView(root);
    setContentView(scroll);

    addHeader();

    TextView tagline = text(
            "YOUR ENTERTAINMENT. YOUR WORLD.",
            10, MUTED
    );
    tagline.setPadding(0, 3, 0, 20);
    root.addView(tagline);

    addFeatured();

    TextView note = text(
            "Discover your next favourite",
            13, MUTED
    );
    note.setPadding(2, 14, 0, 12);
    root.addView(note);

    addSection("Continue Watching", new String[]{
            "▶  Start Exploring",
            "＋  Add to Watchlist"
    });

    addMovieSection("Popular Movies", "Movies");
    addMovieSection("Web Series", "Web Series");
    addMovieSection("TV Shows", "TV Shows");
    addMovieSection("Cartoons", "Cartoons");

    addNavigation();
}

private void addHeader() {
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setOrientation(LinearLayout.HORIZONTAL);

    TextView logo = text("ABHI", 29, RED);
    logo.setTypeface(null, Typeface.BOLD);
    header.addView(logo);

    TextView tv = text(" TV", 29, WHITE);
    tv.setTypeface(null, Typeface.BOLD);
    header.addView(tv);

    View spacer = new View(this);
    header.addView(
            spacer,
            new LinearLayout.LayoutParams(0, 1, 1)
    );

    TextView profile = text("●", 22, WHITE);
    profile.setGravity(Gravity.CENTER);
    profile.setPadding(10, 8, 0, 8);
    profile.setOnClickListener(v ->
            Toast.makeText(
                    this,
                    "Profile feature is coming soon",
                    Toast.LENGTH_SHORT
            ).show()
    );

    header.addView(profile);
    root.addView(header);
}

private void addFeatured() {
    LinearLayout banner = new LinearLayout(this);
    banner.setOrientation(LinearLayout.VERTICAL);
    banner.setGravity(Gravity.CENTER_VERTICAL);
    banner.setPadding(18, 20, 18, 20);
    banner.setBackground(
            shape(Color.rgb(57, 12, 22), 18)
    );

    TextView heading = text(
            "ABHI TV ORIGINALS",
            24, WHITE
    );
    heading.setTypeface(null, Typeface.BOLD);
    banner.addView(heading);

    TextView subtitle = text(
            "Entertainment starts here",
            17, WHITE
    );
    subtitle.setPadding(0, 8, 0, 0);
    banner.addView(subtitle);

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    190
            );

    params.bottomMargin = 2;
    root.addView(banner, params);
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
        card.setPadding(8, 12, 8, 12);
        card.setBackground(shape(CARD, 12));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, 90, 1);

        params.setMargins(0, 0, 8, 0);
        row.addView(card, params);

        card.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        item.contains("Watchlist")
                                ? "Watchlist feature is coming soon"
                                : "Choose a title from the catalogue",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    root.addView(row);
}

private void addMovieSection(String title, String category) {
    TextView heading = text(title, 19, WHITE);
    heading.setTypeface(null, Typeface.BOLD);
    heading.setPadding(0, 20, 0, 10);
    root.addView(heading);

    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);

    int found = 0;

    for (int i = 0; i < movies.length(); i++) {
        JSONObject movie = movies.optJSONObject(i);

        if (movie == null) {
            continue;
        }

        String movieCategory =
                movie.optString("category", "Movies");

        if (!movieCategory.equalsIgnoreCase(category)) {
            continue;
        }

        String name = movie.optString("title", "Untitled");
        String description = movie.optString(
                "description",
                "No description available."
        );
        String videoUrl = movie.optString("videoUrl", "");

        TextView card = text(name, 14, WHITE);
        card.setGravity(Gravity.CENTER);
        card.setTypeface(null, Typeface.BOLD);
        card.setPadding(10, 10, 10, 10);
        card.setBackground(shape(CARD, 12));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        145,
                        120
                );
        params.setMargins(0, 0, 8, 0);

        row.addView(card, params);

        card.setOnClickListener(v ->
                showDetails(name, description, videoUrl)
        );

        found++;
    }

    if (found == 0) {
        TextView empty = text(
                "No titles added yet",
                13, MUTED
        );
        empty.setPadding(2, 0, 0, 8);
        row.addView(empty);
    }

    root.addView(row);
}

private void showDetails(
        String title,
        String description,
        String videoUrl
) {
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
    play.setTypeface(null, Typeface.BOLD);
    play.setBackground(shape(RED, 12));
    play.setPadding(18, 16, 18, 16);
    root.addView(play);

    play.setOnClickListener(v -> {
        if (videoUrl.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Video link has not been added yet",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Video player is not connected yet",
                    Toast.LENGTH_LONG
            ).show();
        }
    });
}

private void addNavigation() {
    View divider = new View(this);
    divider.setBackgroundColor(CARD);

    LinearLayout.LayoutParams dividerParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
    dividerParams.setMargins(0, 28, 0, 12);
    root.addView(divider, dividerParams);

    LinearLayout nav = new LinearLayout(this);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    nav.setGravity(Gravity.CENTER);

    String[] labels = {
            "HOME", "MOVIES", "SERIES", "MY LIST"
    };

    for (String label : labels) {
        TextView item = text(label, 10, WHITE);
        item.setGravity(Gravity.CENTER);
        item.setPadding(4, 12, 4, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, -2, 1);

        nav.addView(item, params);

        item.setOnClickListener(v -> {
            if (label.equals("HOME")) {
                showHome();
            } else {
                String target = label.equals("MY LIST")
                        ? "My List"
                        : label.equals("MOVIES")
                                ? "Popular Movies"
                                : "Web Series";

                Toast.makeText(
                        this,
                        target + " section",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    root.addView(nav);
}

        }
