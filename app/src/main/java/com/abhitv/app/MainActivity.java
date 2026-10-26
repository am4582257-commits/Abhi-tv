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

public class MainActivity extends Activity {

    private final int BG = Color.rgb(12, 12, 16);
    private final int RED = Color.rgb(229, 9, 20);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(170, 170, 180);

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
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
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView profile = text("●", 23, WHITE);
        profile.setGravity(Gravity.CENTER);
        header.addView(profile);

        root.addView(header);

        TextView tagline = text("YOUR ENTERTAINMENT. YOUR WORLD.", 10, MUTED);
        tagline.setPadding(0, 2, 0, 18);
        root.addView(tagline);

        TextView featured = text(
                "  ABHI TV ORIGINALS\\n\\n  Entertainment starts here",
                23, WHITE);
        featured.setTypeface(null, Typeface.BOLD);
        featured.setGravity(Gravity.CENTER_VERTICAL);
        featured.setPadding(18, 22, 12, 22);
        featured.setBackground(shape(Color.rgb(57, 12, 22), 18));
        root.addView(featured, new LinearLayout.LayoutParams(
                -1, 190));

        TextView note = text(
                "Discover your next favourite",
                13, MUTED);
        note.setPadding(2, 12, 0, 18);
        root.addView(note);

        addSection("Continue Watching", new String[]{
                "▶  Start Exploring", "＋  Add to Watchlist"
        });

        addSection("Popular Movies", new String[]{
                "MOVIE 01", "MOVIE 02", "MOVIE 03"
        });

        addSection("Web Series", new String[]{
                "SERIES 01", "SERIES 02", "SERIES 03"
        });

        addSection("TV Shows", new String[]{
                "TV SHOW 01", "TV SHOW 02"
        });

        addSection("Cartoons", new String[]{
                "CARTOON 01", "CARTOON 02"
        });

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
            card.setBackground(shape(Color.rgb(38, 38, 46), 12));

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, 100, 1);
            lp.setMargins(0, 0, 8, 0);
            row.addView(card, lp);
        }

        root.addView(row);
    }
}
