package com.vrublack.nutrition.tacoshell;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;

import com.vrublack.nutrition.core.Formatter;
import com.vrublack.nutrition.core.PercentileScale;
import com.vrublack.nutrition.core.SearchResultItem;
import com.vrublack.nutrition.core.TextMatrix;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{

    private AndroidUSDADatabase db;

    private List<SearchResultItem> queryResult;
    private boolean queryRunning;

    private RecyclerView mRecyclerView;
    private FoodListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new FoodListAdapter(new ArrayList<SearchResultItem>());
        mRecyclerView.setAdapter(mAdapter);

        new LoadDBTask().execute();
    }


    private String printSearchResults(List<SearchResultItem> results)
    {
        Formatter formatter = new com.vrublack.nutrition.tacoshell.TextFormatter();

        // max amount of entries that will be shown to the user (upon request)
        final int entryLimit = Math.min(50, results.size());
        int currentPos = 0;

        float[] percentileReference = new float[Math.min(10, results.size())];
        for (int i = 0; i < percentileReference.length; i++)
            percentileReference[i] = results.get(i).getRelativePopularity();
        PercentileScale percentileScale = new PercentileScale(percentileReference);

        // make matrix with all the search results up to entryLimit
        final int matrixHeight = entryLimit + 1;
        final int matrixWidth = 3;
        TextMatrix matrix = new TextMatrix(matrixWidth, matrixHeight);
        matrix.setRow(0, new String[]{"", "DESCRIPTION", "POPULARITY"});
        for (int i = 0; i < entryLimit; i++)
        {
            float scaledPopularity = 100 * percentileScale.getPercentile(results.get(i).getRelativePopularity());
            matrix.setRow(i + 1, new String[]{"[" + (i + 1) + "]", results.get(i).toString(), formatter.formatPopularity(scaledPopularity)});
        }

        return matrix.formatToString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                System.out.println("Submit");
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s)
            {
                System.out.println("Update");

                if (s.length() != 0) {
                    if (!queryRunning) {
                        new QueryTask().execute(s);
                    }
                }

                return true;
            }
        });

        return true;
    }

    private class LoadDBTask extends AsyncTask<Void, Long, Long>
    {
        private long progress = 0;

        @Override
        protected Long doInBackground(Void... params)
        {
            long start = System.nanoTime();
            final float interval = 0.05f;
            db = AndroidUSDADatabase.newDatabase(getResources(), new Runnable()
            {
                @Override
                public void run()
                {
                    progress++;

                    publishProgress((long) (progress * interval * 100));
                }
            }, interval);
            return System.nanoTime() - start;
        }


        protected void onPostExecute(Long result)
        {
            // statusView.setText("DB loaded after " + result / 1000000 + " ms");

            int iterations = 10;
            long totalTime = 0;
            for (int i = 0; i < iterations; i++)
            {
                long start = System.nanoTime();
                List<SearchResultItem> resultItems = db.search("tomato sauce");
                totalTime += System.nanoTime() - start;
            }
            // statusView.setText("Query completed in " + totalTime / (1000000 * iterations) + " ms");

            new QueryTask().execute("apple sauce");
        }

        @Override
        protected void onProgressUpdate(Long... values)
        {
            super.onProgressUpdate(values);

            // statusView.setText(values[0] + " % loaded");
        }
    }

    private class QueryTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            queryRunning = true;
            queryResult = db.search(params[0]);
            queryRunning = false;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            // statusView.setText("Results: " + queryResult.size());
            String formatted = printSearchResults(queryResult);
            // contentView.setText(formatted);

            mAdapter.changeResults(queryResult);
        }
    }

}
