package com.example.dharan1011.popular_movie_app;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.dharan1011.popular_movie_app.Adapters.MoviesAdapter;
import com.example.dharan1011.popular_movie_app.Data.MovieContract;
import com.example.dharan1011.popular_movie_app.Models.Data;
import com.example.dharan1011.popular_movie_app.Models.Movie;
import com.example.dharan1011.popular_movie_app.Utils.APIService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements MoviesAdapter.ItemClickHandler {

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private MoviesAdapter mMoviesAdapter;
    private List<Movie> mMovieList;
    private String sortType;

    public static final String EXTRA_OBJECT = "movie-object";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SORT_KEY = "sort_key";
    private static final String SHARED_PREFERENCE_KEY = "shared_preference_key";
    private static final String TOP_RATED = "top_rated";
    private static final String POPULAR = "popular";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading);

        mRecyclerView = (RecyclerView) findViewById(R.id.rcv_movie_list);
        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this,(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)?2:4));

        mMoviesAdapter = new MoviesAdapter(MainActivity.this);
        mRecyclerView.setAdapter(mMoviesAdapter);

        sortType = getSharedPreferences(SHARED_PREFERENCE_KEY, 0).getString(SORT_KEY, POPULAR);

        //TODO
//        if(savedInstanceState == null) {
//            int rowsDeleted = getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI, null, null);
//            if (rowsDeleted > 0)
//                Toast.makeText(this, "Database Emptied", Toast.LENGTH_SHORT).show();
//
//            fetchContent(sortType);
//        }
        if(null == savedInstanceState) fetchContent(sortType);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        fetchContent(sortType);
    }

    public void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    public void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void fetchContent(String sortType) {
        showProgressBar();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        APIService service = retrofit.create(APIService.class);
        Call<Data> call = service.getMoviesData(sortType, APIService.API_KEY);
        call.enqueue(new Callback<Data>() {
            @Override
            public void onResponse(@NonNull Call<Data> call, @NonNull Response<Data> response) {
                if (response.isSuccessful()) {
                    mMovieList = response.body().getMovieList();
                    if(mMovieList == null || mMovieList.size() == 0) return;
//                    insertIntoDatabase(mMovieList);
                    mMoviesAdapter.setmMovieList(mMovieList);
                    hideProgressBar();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Data> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Couldn't Fetch Content", Toast.LENGTH_SHORT).show();
                hideProgressBar();
            }
        });
    }

    //TODO
    private void insertIntoDatabase(List<Movie> mMovieList) {
        List<ContentValues> contentValues = new ArrayList<>();
        for(Movie m : mMovieList){
            ContentValues v = new ContentValues();
            v.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID,m.getId());
            v.put(MovieContract.MovieEntry.COLUMN_MOVIE_TITLE,m.getTitle());
            contentValues.add(v);
        }
        //TODO
        int rowsInserted = getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI,contentValues.toArray(new ContentValues[contentValues.size()]));
        if(rowsInserted > 0) Toast.makeText(this, "Database synced", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "insertIntoDatabase: "+rowsInserted);
    }

    @Override
    public void onItemClick(Movie movie) {

        Intent i = new Intent(MainActivity.this,DetailsActivity.class);
        i.putExtra(EXTRA_OBJECT,movie);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if(sortType.equals(TOP_RATED)){
            menu.getItem(0).setTitle(getResources().getString(R.string.action_sort_popular));
        }else{
            menu.getItem(0).setTitle(getResources().getString(R.string.action_sort_top_rated));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                if(sortType.equals(TOP_RATED)){
                    sortType = POPULAR;
                    fetchContent(sortType);
                    mMoviesAdapter.notifyDataSetChanged();
                    item.setTitle(getResources().getString(R.string.action_sort_top_rated));
                }
                else{
                    sortType = TOP_RATED;
                    fetchContent(sortType);
                    mMoviesAdapter.notifyDataSetChanged();
                    item.setTitle(getResources().getString(R.string.action_sort_popular));
                }
                updateSortKey();
                return true;
            case R.id.action_favourite_movies:
                startActivity(new Intent(this,FavouriteMoviesActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateSortKey() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_KEY, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_KEY, sortType);
        editor.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
