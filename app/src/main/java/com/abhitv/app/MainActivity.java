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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.app.AlertDialog;
import android.text.InputType;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String ADMIN_UID = "7OqyZ4l0ySXw1IA9FbEKwAsIuRr1";
    private static final String[] CATEGORIES = {"Movies", "Web Series", "TV Shows", "Cartoons"};

    private final int BG = Color.rgb(5, 10, 24);
    private final int RED = Color.rgb(35, 126, 255);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(157, 177, 207);
    private final int CARD = Color.rgb(16, 29, 52);

    private LinearLayout root;
    private JSONArray localMovies;
    private final List<CloudMovie> cloudMovies = new ArrayList<>();
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration movieListener;
    private boolean showingDetails = false;

    private static class CloudMovie {
        String id, title, description, videoUrl, category;
        CloudMovie(String id, String title, String description, String videoUrl, String category) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.videoUrl = videoUrl;
            this.category = category;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        localMovies = loadMovies();
        showHome();
        listenForCloudMovies();
    }

    @Override
    protected void onDestroy() {
        if (movieListener != null) movieListener.remove();
        super.onDestroy();
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
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            input.close();
            JSONObject data = new JSONObject(output.toString("UTF-8"));
            JSONArray list = data.optJSONArray("movies");
            return list != null ? list : new JSONArray();
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void listenForCloudMovies() {
        movieListener = db.collection("movies").addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(this, "Catalogue sync failed. Check internet/Firebase rules.", Toast.LENGTH_LONG).show();
                return;
            }
            cloudMovies.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    cloudMovies.add(new CloudMovie(
                            doc.getId(),
                            doc.getString("title") == null ? "Untitled" : doc.getString("title"),
                            doc.getString("description") == null ? "" : doc.getString("description"),
                            doc.getString("videoUrl") == null ? "" : doc.getString("videoUrl"),
                            doc.getString("category") == null ? "Movies" : doc.getString("category")
                    ));
                }
            }
            if (!showingDetails) showHome();
        });
    }

    private void showHome() {
        showingDetails = false;
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

        TextView tagline = text("YOUR CINEMATIC UNIVERSE  •  ANYTIME", 10, MUTED);
        tagline.setPadding(0, 3, 0, 10);
        root.addView(tagline);

        TextView adminEntry = text("✦  ADMIN LOGIN  •  MANAGE TITLES", 14, WHITE);
        adminEntry.setGravity(Gravity.CENTER);
        adminEntry.setTypeface(null, Typeface.BOLD);
        adminEntry.setPadding(16, 16, 16, 16);
        adminEntry.setBackground(shape(RED, 14));
        LinearLayout.LayoutParams adminParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        adminParams.bottomMargin = 16;
        root.addView(adminEntry, adminParams);
        adminEntry.setOnClickListener(v -> {
            Toast.makeText(this, "Admin login opening...", Toast.LENGTH_SHORT).show();
            openAdminEntry();
        });

        addFeatured();

        TextView note = text("Discover your next favourite", 13, MUTED);
        note.setPadding(2, 14, 0, 12);
        root.addView(note);

        addSection("Continue Watching", new String[]{"▶  Start Exploring", "＋  Add to Watchlist"});
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
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView profile = text("ADMIN", 12, WHITE);
        profile.setGravity(Gravity.CENTER);
        profile.setTypeface(null, Typeface.BOLD);
        profile.setPadding(18, 14, 18, 14);
        profile.setMinWidth(96);
        profile.setMinHeight(48);
        profile.setBackground(shape(RED, 14));
        profile.setClickable(true);
        profile.setFocusable(true);
        profile.setOnClickListener(v -> openAdminEntry());
        header.addView(profile);
        root.addView(header);
    }

    private void addFeatured() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setGravity(Gravity.BOTTOM);
        banner.setPadding(22, 22, 22, 22);
        GradientDrawable cinematic = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(20, 75, 145), Color.rgb(8, 20, 48), Color.rgb(3, 7, 18)});
        cinematic.setCornerRadius(24);
        banner.setBackground(cinematic);
        TextView tag = text("✦  ABHI TV ORIGINAL", 11, Color.rgb(125, 190, 255));
        tag.setTypeface(null, Typeface.BOLD);
        banner.addView(tag);
        TextView heading = text("ENTERTAINMENT.\nREIMAGINED.", 28, WHITE);
        heading.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        heading.setPadding(0, 10, 0, 0);
        banner.addView(heading);
        TextView subtitle = text("Your next favourite story starts here.", 13, Color.rgb(205, 220, 245));
        subtitle.setPadding(0, 8, 0, 16);
        banner.addView(subtitle);
        TextView explore = text("▶   EXPLORE NOW", 13, WHITE);
        explore.setGravity(Gravity.CENTER);
        explore.setTypeface(null, Typeface.BOLD);
        explore.setPadding(18, 13, 18, 13);
        explore.setBackground(shape(RED, 30));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, -2);
        banner.addView(explore, ep);
        explore.setOnClickListener(v -> {
            ScrollView sc = (ScrollView) root.getParent();
            sc.smoothScrollTo(0, root.getHeight());
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 245);
        params.setMargins(0, 10, 0, 8);
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
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 90, 1);
            params.setMargins(0, 0, 8, 0);
            row.addView(card, params);
            card.setOnClickListener(v -> Toast.makeText(this,
                    item.contains("Watchlist") ? "Watchlist feature is coming soon" :
                            "Choose a title from the catalogue", Toast.LENGTH_SHORT).show());
        }
        root.addView(row);
    }

    private void addMovieSection(String title, String category) {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView heading = text(title, 20, WHITE);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titleRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView seeAll = text("SEE ALL  ›", 10, RED);
        seeAll.setTypeface(null, Typeface.BOLD);
        titleRow.addView(seeAll);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 22, 0, 12);
        root.addView(titleRow, hp);
        HorizontalScrollViewWrap:
        {
            android.widget.HorizontalScrollView horizontal = new android.widget.HorizontalScrollView(this);
            horizontal.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int found = 0;
            for (int i = 0; i < localMovies.length(); i++) {
                JSONObject movie = localMovies.optJSONObject(i);
                if (movie == null || !movie.optString("category", "Movies").equalsIgnoreCase(category)) continue;
                addMovieCard(row, movie.optString("title", "Untitled"),
                        movie.optString("description", "No description available."),
                        movie.optString("videoUrl", ""));
                found++;
            }
            for (CloudMovie movie : cloudMovies) {
                if (!movie.category.equalsIgnoreCase(category)) continue;
                addMovieCard(row, movie.title, movie.description, movie.videoUrl);
                found++;
            }
            if (found == 0) {
                TextView empty = text("   ✦   Fresh titles coming soon", 13, MUTED);
                empty.setPadding(12, 22, 12, 22);
                row.addView(empty);
            }
            horizontal.addView(row);
            root.addView(horizontal);
        }
    }

    private void addMovieCard(LinearLayout row, String name, String description, String videoUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.BOTTOM);
        card.setPadding(12, 12, 12, 12);
        GradientDrawable poster = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(28, 65, 116), Color.rgb(18, 31, 57), Color.rgb(7, 13, 27)});
        poster.setCornerRadius(18);
        poster.setStroke(1, Color.rgb(36, 63, 100));
        card.setBackground(poster);
        TextView badge = text("ABHI  •  PLAY", 9, Color.rgb(125, 190, 255));
        badge.setTypeface(null, Typeface.BOLD);
        card.addView(badge);
        TextView title = text(name, 14, WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setMaxLines(3);
        title.setPadding(0, 8, 0, 0);
        card.addView(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(148, 184);
        params.setMargins(0, 0, 12, 0);
        row.addView(card, params);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showDetails(name, description, videoUrl));
    }

    private void showDetails(String title, String description, String videoUrl) {
        showingDetails = true;
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
            String url = videoUrl == null ? "" : videoUrl.trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Admin Panel mein pehle video URL add karo", Toast.LENGTH_LONG).show();
                return;
            }
            if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                Toast.makeText(this, "Valid http:// ya https:// video URL daalo", Toast.LENGTH_LONG).show();
                return;
            }
            android.content.Intent intent = new android.content.Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_TITLE, title);
            intent.putExtra(PlayerActivity.EXTRA_URL, url);
            startActivity(intent);
        });
    }

    private void addNavigation() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(25, 43, 70));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, 1);
        dp.setMargins(0, 30, 0, 10);
        root.addView(divider, dp);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(4, 6, 4, 6);
        nav.setBackground(shape(Color.rgb(10, 19, 36), 20));
        String[][] tabs = {{"⌂", "HOME"}, {"▣", "MOVIES"}, {"▤", "SERIES"}, {"♡", "MY LIST"}, {"☆", "PROFILE"}};
        for (String[] tab : tabs) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            TextView glyph = text(tab[0], 21, tab[1].equals("HOME") ? RED : MUTED);
            glyph.setGravity(Gravity.CENTER);
            TextView label = text(tab[1], 8, tab[1].equals("HOME") ? WHITE : MUTED);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(null, Typeface.BOLD);
            item.addView(glyph);
            item.addView(label);
            nav.addView(item, new LinearLayout.LayoutParams(0, 52, 1));
            item.setOnClickListener(v -> {
                if (tab[1].equals("HOME")) showHome();
                else if (tab[1].equals("PROFILE")) openAdminEntry();
                else if (tab[1].equals("MY LIST")) new AlertDialog.Builder(this)
                        .setTitle("My List").setMessage("Your watchlist is ready for a future update.")
                        .setPositiveButton("OK", null).show();
                else {
                    String target = tab[1].equals("MOVIES") ? "Movies" : "Web Series";
                    new AlertDialog.Builder(this).setTitle(target)
                            .setMessage("Scroll the home screen to explore " + target + ".")
                            .setPositiveButton("Explore", (d, w) -> showHome()).show();
                }
            });
        }
        root.addView(nav);
    }

    private void openAdminEntry() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && ADMIN_UID.equals(user.getUid())) {
            showAdminMenu();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 0);
        EditText email = new EditText(this);
        email.setHint("Admin email");
        email.setSingleLine(true);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = new EditText(this);
        password.setHint("Password");
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(email);
        form.addView(password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ABHI TV Admin Login")
                .setView(form)
                .setNegativeButton("Cancel", (d, w) -> {})
                .setPositiveButton("Login", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = password.getText().toString();
            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Email aur password dono bharo", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            auth.signInWithEmailAndPassword(e, p).addOnCompleteListener(task -> {
                if (task.isSuccessful() && auth.getCurrentUser() != null
                        && ADMIN_UID.equals(auth.getCurrentUser().getUid())) {
                    dialog.dismiss();
                    showAdminMenu();
                } else {
                    auth.signOut();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Admin permission nahi hai")
                                .setMessage("Firebase login successful hai, lekin is account ki UID app ke Admin UID se match nahi karti.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Exception error = task.getException();
                        String detail = error == null ? "Unknown Firebase Authentication error" :
                                error.getClass().getSimpleName() + "\\n" +
                                (error.getLocalizedMessage() == null ? "No additional details" : error.getLocalizedMessage());
                        new AlertDialog.Builder(this)
                                .setTitle("Firebase Login Failed")
                                .setMessage(detail + "\\n\\nFirebase Console mein ABHI-TV-ROYAL project ka Authentication > Users aur Email/Password provider check karo.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
            });
        }));
        dialog.show();
    }

    private void showAdminMenu() {
        if (!isAdmin()) {
            auth.signOut();
            Toast.makeText(this, "Admin permission nahi hai", Toast.LENGTH_LONG).show();
            return;
        }
        String[] options = {"＋ Add title", "✎ Manage / Edit / Delete titles", "↻ Refresh catalogue", "Logout"};
        new AlertDialog.Builder(this)
                .setTitle("ABHI TV Admin Panel")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showMovieForm(null);
                    else if (which == 1) showManageTitles();
                    else if (which == 2) {
                        showHome();
                        Toast.makeText(this, "Catalogue refreshed", Toast.LENGTH_SHORT).show();
                    } else {
                        auth.signOut();
                        Toast.makeText(this, "Admin logged out", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private boolean isAdmin() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && ADMIN_UID.equals(user.getUid());
    }

    private void showMovieForm(CloudMovie existing) {
        if (!isAdmin()) {
            Toast.makeText(this, "Admin login required", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 0, 24, 0);

        EditText title = new EditText(this);
        title.setHint("Title (required)");
        title.setSingleLine(true);
        EditText description = new EditText(this);
        description.setHint("Description");
        EditText url = new EditText(this);
        url.setHint("Video URL (optional)");
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        form.addView(title);
        form.addView(description);
        form.addView(url);
        form.addView(category);

        if (existing != null) {
            title.setText(existing.title);
            description.setText(existing.description);
            url.setText(existing.videoUrl);
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(existing.category)) category.setSelection(i);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add title" : "Edit title")
                .setView(form)
                .setNegativeButton("Cancel", (d, w) -> {})
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) {
                Toast.makeText(this, "Title required hai", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("title", t);
            data.put("description", description.getText().toString().trim());
            data.put("videoUrl", url.getText().toString().trim());
            data.put("category", CATEGORIES[category.getSelectedItemPosition()]);
            data.put("updatedAt", System.currentTimeMillis());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            if (existing == null) {
                data.put("createdAt", System.currentTimeMillis());
                db.collection("movies").add(data).addOnSuccessListener(ref -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Title added to live catalogue", Toast.LENGTH_SHORT).show();
                    showAdminMenu();
                }).addOnFailureListener(err -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, "Save failed: " + err.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                db.collection("movies").document(existing.id).update(data).addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Title updated", Toast.LENGTH_SHORT).show();
                    showAdminMenu();
                }).addOnFailureListener(err -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, "Update failed: " + err.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }));
        dialog.show();
    }

    private void showManageTitles() {
        if (!isAdmin()) {
            Toast.makeText(this, "Admin login required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cloudMovies.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Manage titles")
                    .setMessage("Firebase catalogue mein abhi koi title nahi hai. Pehle Add title use karo.")
                    .setPositiveButton("Add title", (d, w) -> showMovieForm(null))
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }
        String[] titles = new String[cloudMovies.size()];
        for (int i = 0; i < cloudMovies.size(); i++) {
            CloudMovie m = cloudMovies.get(i);
            titles[i] = m.title + "  •  " + m.category;
        }
        new AlertDialog.Builder(this)
                .setTitle("Select title to manage")
                .setItems(titles, (dialog, which) -> showTitleActions(cloudMovies.get(which)))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showTitleActions(CloudMovie movie) {
        new AlertDialog.Builder(this)
                .setTitle(movie.title)
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) showMovieForm(movie);
                    else confirmDelete(movie);
                }).show();
    }

    private void confirmDelete(CloudMovie movie) {
        new AlertDialog.Builder(this)
                .setTitle("Delete title?")
                .setMessage("Delete \"" + movie.title + "\" from Firebase catalogue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!isAdmin()) {
                        Toast.makeText(this, "Admin login required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("movies").document(movie.id).delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Title deleted", Toast.LENGTH_SHORT).show();
                                showAdminMenu();
                            })
                            .addOnFailureListener(err -> Toast.makeText(this,
                                    "Delete failed: " + err.getLocalizedMessage(), Toast.LENGTH_LONG).show());
                }).show();
    }
}
