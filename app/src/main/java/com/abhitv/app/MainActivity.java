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
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(18, 20, 18, 20);
        banner.setBackground(shape(Color.rgb(10, 35, 79), 18));

        TextView heading = text("ABHI TV", 30, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        banner.addView(heading);

        TextView subtitle = text("A world of stories awaits.", 16, Color.rgb(194, 218, 255));
        subtitle.setPadding(0, 8, 0, 0);
        banner.addView(subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 190);
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
        TextView heading = text(title, 19, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, 20, 0, 10);
        root.addView(heading);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int found = 0;

        for (int i = 0; i < localMovies.length(); i++) {
            JSONObject movie = localMovies.optJSONObject(i);
            if (movie == null || !movie.optString("category", "Movies").equalsIgnoreCase(category)) continue;
            String name = movie.optString("title", "Untitled");
            String description = movie.optString("description", "No description available.");
            String videoUrl = movie.optString("videoUrl", "");
            addMovieCard(row, name, description, videoUrl);
            found++;
        }

        for (CloudMovie movie : cloudMovies) {
            if (!movie.category.equalsIgnoreCase(category)) continue;
            addMovieCard(row, movie.title, movie.description, movie.videoUrl);
            found++;
        }

        if (found == 0) {
            TextView empty = text("No titles added yet", 13, MUTED);
            empty.setPadding(2, 0, 0, 8);
            row.addView(empty);
        }
        root.addView(row);
    }

    private void addMovieCard(LinearLayout row, String name, String description, String videoUrl) {
        TextView card = text(name, 14, WHITE);
        card.setGravity(Gravity.CENTER);
        card.setTypeface(null, Typeface.BOLD);
        card.setPadding(10, 10, 10, 10);
        card.setBackground(shape(CARD, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(145, 120);
        params.setMargins(0, 0, 8, 0);
        row.addView(card, params);
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
        divider.setBackgroundColor(CARD);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, 28, 0, 12);
        root.addView(divider, dividerParams);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        String[] labels = {"HOME", "MOVIES", "SERIES", "MY LIST"};
        for (String label : labels) {
            TextView item = text(label, 10, WHITE);
            item.setGravity(Gravity.CENTER);
            item.setPadding(4, 12, 4, 12);
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
            item.setOnClickListener(v -> {
                if (label.equals("HOME")) showHome();
                else {
                    String target = label.equals("MY LIST") ? "My List" :
                            label.equals("MOVIES") ? "Popular Movies" : "Web Series";
                    Toast.makeText(this, target + " section", Toast.LENGTH_SHORT).show();
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
