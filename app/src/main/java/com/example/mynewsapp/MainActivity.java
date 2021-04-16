package com.example.mynewsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("article",urls.get(position));
                startActivity(intent);

            }
        });
        articlesDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articlesDatabase (id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,url VARCHAR)");
        updateListView();

        DownTask task = new DownTask();
        try {
           task.execute(" https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public void updateListView(){
        Cursor c=articlesDB.rawQuery("SELECT * FROM articlesDatabase",null);

        int titleIndex=c.getColumnIndex("title");
        int urlIndex=c.getColumnIndex("url");
        if(c.moveToFirst()){
            titles.clear();
            urls.clear();

            do{
                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));

            }while(c.moveToNext());
         arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpsURLConnection urlConnection = null;
            articlesDB.execSQL("DELETE FROM articlesDatabase");

            try {
                url = new URL(strings[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char c = (char) data;
                    result += c;
                    data = reader.read();

                }
                Log.i("URL content", result);
                JSONArray jsonArray=new JSONArray(result);//new json array base on result
                int number=20;// we always want to show 20 results
                if(jsonArray.length()<20){ // if Id returned by the above process is <20
                    number=jsonArray.length();
                }


               for(int i=0;i<number;i++){
                    String articleInfo="";
                    Log.i("JSONItem :",jsonArray.getString(i));
                    String articleId=jsonArray.getString(i);
                    URL url1=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    HttpsURLConnection urlConnection1=(HttpsURLConnection)url1.openConnection();
                    InputStream in1=urlConnection1.getInputStream();
                    InputStreamReader reader1=new InputStreamReader(in1);
                    int data1=reader1.read();
                    while(data1!=-1){
                        char c=(char)data1;
                        articleInfo+=c;
                        data1=reader1.read();
                    }
                    Log.i("Article Info",articleInfo);
                   JSONObject jsonObject=new JSONObject(articleInfo);

                   if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                       String articleTitle = jsonObject.getString("title");
                       Log.i("title",articleTitle);
                       String articleURL = jsonObject.getString("url");
                       Log.i("URL ",articleURL);

                       String sql="INSERT INTO articlesDatabase (articleId,title,url) VALUES (? , ?, ?)";
                       SQLiteStatement statement=articlesDB.compileStatement(sql);
                       statement.bindString(1,articleId);
                       statement.bindString(2,articleTitle);
                       statement.bindString(3,articleURL);
                       statement.execute();
                   }

              }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}