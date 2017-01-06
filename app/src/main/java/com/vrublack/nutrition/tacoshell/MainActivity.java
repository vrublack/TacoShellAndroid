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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.vrublack.nutrition.core.Formatter;
import com.vrublack.nutrition.core.PercentileScale;
import com.vrublack.nutrition.core.SearchResultItem;
import com.vrublack.nutrition.core.SyncFoodDataSource;
import com.vrublack.nutrition.core.TextMatrix;
import com.vrublack.nutrition.core.uga.UGAFoodServices;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
    private SyncFoodDataSource dataSource;

    private List<SearchResultItem> queryResult;
    private boolean queryRunning;
    private boolean showingList = false;

    private RecyclerView mRecyclerView;
    private FoodListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ViewSwitcher switcher;
    private TextView statusView;
    private SearchView searchView;
    private Spinner datasourceSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        switcher = (ViewSwitcher) findViewById(R.id.switcher);

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

        statusView = (TextView) findViewById(R.id.status);

        datasourceSpinner = (Spinner) findViewById(R.id.spinner);

        datasourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0)
                {
                    statusView.setText(getString(R.string.loading_db));
                    new LoadDBTask().execute();
                }
                else
                {
                    statusView.setText(getString(R.string.loading_uga));
                    new LoadUGATask().execute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
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
        searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s)
            {
                if (s.length() == 0)
                    return false;

                if (!showingList)
                {
                    showingList = true;
                    switcher.setDisplayedChild(1);
                }

                if (!queryRunning)
                {
                    new QueryTask().execute(s);
                }

                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (showingList)
        {
            switcher.setDisplayedChild(0);
            showingList = false;
        }

        super.onBackPressed();
    }

    private void hideKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    private class LoadDBTask extends AsyncTask<Void, Long, Long>
    {
        private long progress = 0;

        @Override
        protected Long doInBackground(Void... params)
        {
            long start = System.nanoTime();
            final float interval = 0.05f;
            dataSource = AndroidUSDADatabase.newDatabase(getResources(), new Runnable()
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
            statusView.setText("DB loaded after " + result / 1000000 + " ms");

            /*
            int iterations = 10;
            long totalTime = 0;
            for (int i = 0; i < iterations; i++)
            {
                long start = System.nanoTime();
                List<SearchResultItem> resultItems = dataSource.search("tomato sauce");
                totalTime += System.nanoTime() - start;
            }
            // statusView.setText("Query completed in " + totalTime / (1000000 * iterations) + " ms");

            new QueryTask().execute("apple sauce");*/
        }

        @Override
        protected void onProgressUpdate(Long... values)
        {
            super.onProgressUpdate(values);

            statusView.setText(values[0] + " % loaded");
        }
    }

    private class LoadUGATask extends AsyncTask<Void, Long, Long>
    {
        private long progress = 0;

        @Override
        protected Long doInBackground(Void... params)
        {
            long start = System.nanoTime();
            final float interval = 0.05f;
            dataSource = new UGAFoodServices();
            return System.nanoTime() - start;
        }


        protected void onPostExecute(Long result)
        {
            statusView.setText("UGA datasource loaded after " + result / 1000000 + " ms");
        }

    }

    private class QueryTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            queryRunning = true;
            queryResult = dataSource.search(params[0]);
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
