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
import android.widget.ImageView;
import android.graphics.BitmapFactory;
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
        String id, title, description, videoUrl, category, posterUrl;
        CloudMovie(String id, String title, String description, String videoUrl, String category, String posterUrl) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.videoUrl = videoUrl;
            this.category = category;
            this.posterUrl = posterUrl;
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
                            doc.getString("category") == null ? "Movies" : doc.getString("category"),
                            doc.getString("posterUrl") == null ? "" : doc.getString("posterUrl")
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
        root.setPadding(16, 12, 16, 24);
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        addHeader();

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(14, 0, 8, 0);
        searchRow.setBackground(shape(Color.rgb(43, 48, 60), 14));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(WHITE);
        search.setHintTextColor(Color.rgb(190, 194, 204));
        search.setHint("⌕  Search movies, series...");
        search.setTextSize(15);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(0, 10, 0, 10);
        searchRow.addView(search, new LinearLayout.LayoutParams(0, 52, 1));
        TextView searchButton = text("Search", 14, Color.rgb(45, 235, 160));
        searchButton.setTypeface(null, Typeface.BOLD);
        searchButton.setPadding(8, 0, 6, 0);
        searchRow.addView(searchButton);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        searchParams.setMargins(0, 14, 0, 8);
        root.addView(searchRow, searchParams);
        searchButton.setOnClickListener(v -> {
            String q = search.getText().toString().trim();
            if (q.isEmpty()) Toast.makeText(this, "Search mein movie ya series ka naam likho", Toast.LENGTH_SHORT).show();
            else {
                int match = -1;
                for (int i = 0; i < localMovies.length(); i++) {
                    JSONObject m = localMovies.optJSONObject(i);
                    if (m != null && m.optString("title", "").toLowerCase().contains(q.toLowerCase())) { match = i; break; }
                }
                if (match >= 0) {
                    JSONObject m = localMovies.optJSONObject(match);
                    showDetails(m.optString("title", "Untitled"), m.optString("description", ""),
                            m.optString("videoUrl", ""));
                } else {
                    for (CloudMovie m : cloudMovies) if (m.title.toLowerCase().contains(q.toLowerCase())) {
                        showDetails(m.title, m.description, m.videoUrl); return;
                    }
                    Toast.makeText(this, "Is naam ka title catalogue mein nahi mila", Toast.LENGTH_SHORT).show();
                }
            }
        });

        android.widget.HorizontalScrollView categoryScroll = new android.widget.HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        String[] chips = {"Football", "Trending", "Anime", "Cricket", "Movies", "Series", "TV Shows", "Cartoons"};
        for (int i = 0; i < chips.length; i++) {
            final String chip = chips[i];
            TextView item = text(chip, 15, i == 1 ? WHITE : MUTED);
            item.setTypeface(null, i == 1 ? Typeface.BOLD : Typeface.NORMAL);
            item.setPadding(12, 10, 16, 10);
            categories.addView(item);
            item.setOnClickListener(v -> {
                String target = chip.equals("Series") ? "Web Series" : chip;
                showCategoryScreen(target);
            });
        }
        categoryScroll.addView(categories);
        root.addView(categoryScroll);

        addFeatured();
        addPremiumBanner();
        addMovieSection("Trending Movies", "Movies");
        addMovieSection("Watch on ABHI TV", "Web Series");
        addMovieSection("TV Shows & Cartoons", "TV Shows");
        addNavigation();
    }

    private void addPremiumBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(16, 14, 14, 14);
        GradientDrawable gold = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(255, 238, 177), Color.rgb(223, 171, 88), Color.rgb(140, 87, 43)});
        gold.setCornerRadius(18);
        banner.setBackground(gold);
        TextView emblem = text("♛", 30, Color.rgb(86, 49, 20));
        emblem.setGravity(Gravity.CENTER);
        banner.addView(emblem, new LinearLayout.LayoutParams(48, 52));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(10, 0, 4, 0);
        TextView heading = text("Unlock Premium Benefits", 16, Color.rgb(50, 32, 15));
        heading.setTypeface(null, Typeface.BOLD);
        words.addView(heading);
        TextView sub = text("Discover more entertainment", 11, Color.rgb(78, 54, 31));
        sub.setPadding(0, 4, 0, 0);
        words.addView(sub);
        banner.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView unlock = text("Explore", 13, Color.rgb(70, 43, 17));
        unlock.setTypeface(null, Typeface.BOLD);
        unlock.setGravity(Gravity.CENTER);
        unlock.setPadding(12, 10, 12, 10);
        unlock.setBackground(shape(Color.rgb(255, 244, 208), 28));
        banner.addView(unlock);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 14, 0, 4);
        root.addView(banner, p);
        unlock.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("ABHI TV Premium")
                .setMessage("Premium subscription options are not configured yet.")
                .setPositiveButton("OK", null).show());
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
                        movie.optString("videoUrl", ""), movie.optString("posterUrl", ""));
                found++;
            }
            for (CloudMovie movie : cloudMovies) {
                if (!movie.category.equalsIgnoreCase(category)) continue;
                addMovieCard(row, movie.title, movie.description, movie.videoUrl, movie.posterUrl);
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

    private void showCategoryScreen(String category) {
        showingDetails = false;
        root.removeAllViews();
        TextView back = text("←  Back to Home", 16, WHITE);
        back.setPadding(0, 12, 0, 20);
        back.setOnClickListener(v -> showHome());
        root.addView(back);
        TextView heading = text(category + " on ABHI TV", 24, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        root.addView(heading);
        android.widget.ScrollView list = new android.widget.ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        int count = 0;
        for (int i = 0; i < localMovies.length(); i++) {
            JSONObject m = localMovies.optJSONObject(i);
            if (m == null) continue;
            String cat = m.optString("category", "Movies");
            if (category.equalsIgnoreCase("Trending") || category.equalsIgnoreCase("Football")
                    || category.equalsIgnoreCase("Anime") || category.equalsIgnoreCase("Cricket")
                    || category.equalsIgnoreCase("Movies") && cat.equalsIgnoreCase("Movies")
                    || category.equalsIgnoreCase("Series") && cat.equalsIgnoreCase("Web Series")
                    || category.equalsIgnoreCase("TV Shows") && cat.equalsIgnoreCase("TV Shows")
                    || category.equalsIgnoreCase("Cartoons") && cat.equalsIgnoreCase("Cartoons")) {
                addMovieCard(rows, m.optString("title", "Untitled"), m.optString("description", ""),
                        m.optString("videoUrl", ""), m.optString("posterUrl", ""));
                count++;
            }
        }
        for (CloudMovie m : cloudMovies) {
            boolean match = category.equalsIgnoreCase("Trending") || category.equalsIgnoreCase("Football")
                    || category.equalsIgnoreCase("Anime") || category.equalsIgnoreCase("Cricket")
                    || category.equalsIgnoreCase("Movies") && m.category.equalsIgnoreCase("Movies")
                    || category.equalsIgnoreCase("Series") && m.category.equalsIgnoreCase("Web Series")
                    || category.equalsIgnoreCase("TV Shows") && m.category.equalsIgnoreCase("TV Shows")
                    || category.equalsIgnoreCase("Cartoons") && m.category.equalsIgnoreCase("Cartoons");
            if (match) { addMovieCard(rows, m.title, m.description, m.videoUrl, m.posterUrl); count++; }
        }
        if (count == 0) {
            TextView empty = text("Titles yahan dikhane ke liye Admin Panel se content add karo. Poster URL dene par image bhi dikhegi.", 15, MUTED);
            empty.setPadding(4, 24, 4, 24);
            rows.addView(empty);
        }
        list.addView(rows);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void addMovieCard(LinearLayout row, String name, String description, String videoUrl, String posterUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.BOTTOM);
        card.setPadding(12, 12, 12, 12);
        GradientDrawable poster = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(28, 65, 116), Color.rgb(18, 31, 57), Color.rgb(7, 13, 27)});
        poster.setCornerRadius(18);
        poster.setStroke(1, Color.rgb(36, 63, 100));
        card.setBackground(poster);
        if (posterUrl != null && (posterUrl.startsWith("https://") || posterUrl.startsWith("http://"))) {
            ImageView artwork = new ImageView(this);
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(-1, 112);
            artParams.bottomMargin = 8;
            card.addView(artwork, 0, artParams);
            final String imageUrl = posterUrl;
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(imageUrl).openConnection();
                    connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
                    connection.setDoInput(true); connection.connect();
                    android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                    connection.disconnect();
                    if (bitmap != null) runOnUiThread(() -> artwork.setImageBitmap(bitmap));
                } catch (Exception ignored) { }
            });
        }
        TextView badge = text("✦  ABHI TV", 9, Color.rgb(125, 230, 190));
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
        EditText posterUrl = new EditText(this);
        posterUrl.setHint("Poster image URL (optional)");
        posterUrl.setSingleLine(true);
        posterUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        form.addView(title);
        form.addView(description);
        form.addView(url);
        form.addView(posterUrl);
        form.addView(category);

        if (existing != null) {
            title.setText(existing.title);
            description.setText(existing.description);
            url.setText(existing.videoUrl);
            posterUrl.setText(existing.posterUrl);
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
            data.put("posterUrl", posterUrl.getText().toString().trim());
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
    private void addHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text("ABHI", 28, Color.rgb(44, 235, 158));
        logo.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        header.addView(logo);
        TextView tv = text(" TV", 28, WHITE);
        tv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        header.addView(tv);

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView profile = text("●", 18, WHITE);
        profile.setGravity(Gravity.CENTER);
        profile.setPadding(12, 8, 12, 8);
        profile.setBackground(shape(Color.rgb(38, 44, 56), 24));
        profile.setOnClickListener(v -> openAdminEntry());
        header.addView(profile);
        root.addView(header);
    }

    private void addFeatured() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setGravity(Gravity.BOTTOM);
        banner.setPadding(20, 24, 20, 20);
        GradientDrawable cinematic = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(75, 104, 118), Color.rgb(21, 54, 70), Color.rgb(5, 10, 20)});
        cinematic.setCornerRadius(22);
        banner.setBackground(cinematic);
        TextView tag = text("✦  FEATURED ON ABHI TV", 11, Color.rgb(85, 240, 181));
        tag.setTypeface(null, Typeface.BOLD);
        banner.addView(tag);
        TextView heading = text("YOUR NEXT\nFAVOURITE STORY", 30, WHITE);
        heading.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        heading.setPadding(0, 10, 0, 0);
        banner.addView(heading);
        TextView subtitle = text("Movies • Series • Unlimited stories", 12, Color.rgb(222, 230, 238));
        subtitle.setPadding(0, 8, 0, 16);
        banner.addView(subtitle);
        TextView explore = text("▶   EXPLORE NOW", 13, Color.rgb(8, 25, 26));
        explore.setGravity(Gravity.CENTER);
        explore.setTypeface(null, Typeface.BOLD);
        explore.setPadding(18, 13, 18, 13);
        explore.setBackground(shape(Color.rgb(47, 235, 164), 30));
        banner.addView(explore, new LinearLayout.LayoutParams(-2, -2));
        explore.setOnClickListener(v -> {
            if (root != null && root.getParent() instanceof ScrollView) ((ScrollView) root.getParent()).smoothScrollTo(0, Math.min(root.getHeight(), banner.getBottom() + 20));
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 300);
        params.setMargins(0, 4, 0, 4);
        root.addView(banner, params);
    }

}
