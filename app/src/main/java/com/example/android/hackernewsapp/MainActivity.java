package com.example.android.hackernewsapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Map<Integer, String> articleUrls = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();
    SQLiteDatabase articleDB;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.i("articleURL", urls.get(position));
            }
        });

        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleid INTEGER, articleurl VARCHAR, articletitle VARCHAR, content VARCHAR)");

        DownloadTask downloadTask = new DownloadTask();
        try {
            String result = downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
//            Log.i("Results", result);
            JSONArray jsonArray = new JSONArray(result);
//            for (int i = 0; i < jsonArray.length(); i++) {
            articleDB.execSQL("DELETE FROM articles");
            for (int i = 0; i < 10; i++) {
                String articleId = jsonArray.getString(i);
//                Log.i("Article ID", jsonArray.getString(i));
                DownloadTask getArticle = new DownloadTask();
                String articleInfo = getArticle.execute("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty").get();
                JSONObject jsonObject = new JSONObject(articleInfo);
                String articleTitle = jsonObject.getString("title");
                String articleUrl = jsonObject.getString("url");

                articleIds.add(Integer.valueOf(articleId));
                articleTitles.put(Integer.valueOf(articleId), articleTitle);
                articleUrls.put(Integer.valueOf(articleId), articleUrl);


                String sql = "INSERT INTO articles(articleid,articleurl,articletitle) VALUES (?,?,?)";
                SQLiteStatement statement = articleDB.compileStatement(sql);
                statement.bindString(1, articleId);
                statement.bindString(2, articleUrl);
                statement.bindString(3, articleTitle);
                statement.execute();
//                articleDB.execSQL("INSERT INTO articles(articleid,articleurl,articletitle) VALUES (" + articleId + ",'" + articleUrl + "','" + articleTitle + "')");


//                Log.i("Article title", articleTitle);
//                Log.i("Article URL", articleUrl);
            }
//            Log.i("Article IDs", articleIds.toString());
//            Log.i("Article Titles", articleTitles.toString());
//            Log.i("Article URLs", articleUrls.toString());

            Cursor cursor = articleDB.rawQuery("SELECT * FROM articles ORDER BY articleid DESC", null);

            int articleIdIndex = cursor.getColumnIndex("articleId");
            int articleUrlIndex = cursor.getColumnIndex("articleurl");
            int articleTitleIndex = cursor.getColumnIndex("articletitle");

            cursor.moveToFirst();
            titles.clear();
            urls.clear();
            while (cursor != null) {
                titles.add(cursor.getString(articleTitleIndex));
                urls.add(cursor.getString(articleUrlIndex));

//                Log.i("Article Id", Integer.toString(cursor.getInt(articleIdIndex)));
//                Log.i("Article URL", cursor.getString(articleUrlIndex));
//                Log.i("Article Title", cursor.getString(articleTitleIndex));
                cursor.moveToNext();
            }
            arrayAdapter.notifyDataSetChanged();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String results = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    results += current;
                    data = reader.read();
                }

            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return results;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
