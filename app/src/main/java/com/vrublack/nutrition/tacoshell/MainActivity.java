package com.vrublack.nutrition.tacoshell;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.vrublack.nutrition.core.Formatter;
import com.vrublack.nutrition.core.PercentileScale;
import com.vrublack.nutrition.core.SearchResultItem;
import com.vrublack.nutrition.core.SyncFoodDataSource;
import com.vrublack.nutrition.core.TextMatrix;
import com.vrublack.nutrition.core.uga.UGAFoodServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
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
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        datasourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    statusView.setText(getString(R.string.loading_db));
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    datasourceSpinner.setEnabled(false);
                    new LoadDBTask().execute();
                } else {
                    statusView.setText(getString(R.string.loading_uga));
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    datasourceSpinner.setEnabled(false);
                    new LoadUGATask().execute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        WebView instructions = (WebView) findViewById(R.id.instructions);
        instructions.setVerticalScrollBarEnabled(false);
        instructions.setBackgroundColor(Color.TRANSPARENT);
        String text = getResources().getString(R.string.instructions);
        System.out.println(text);
        instructions.loadData(text, "text/html", null);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }


    private String printSearchResults(List<SearchResultItem> results) {
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
        for (int i = 0; i < entryLimit; i++) {
            float scaledPopularity = 100 * percentileScale.getPercentile(results.get(i).getRelativePopularity());
            matrix.setRow(i + 1, new String[]{"[" + (i + 1) + "]", results.get(i).toString(), formatter.formatPopularity(scaledPopularity)});
        }

        return matrix.formatToString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.getItem(0).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // shouldn't expand when datasource is currently loading
                if (progressBar.getVisibility() == ProgressBar.VISIBLE) {
                    Toast.makeText(MainActivity.this, getString(R.string.wait_loading), Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (!showingList) {
                    showingList = true;
                    switcher.setDisplayedChild(1);
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (showingList) {
                    switcher.setDisplayedChild(0);
                    showingList = false;
                }

                return true;
            }
        });

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private String sortingQueries = "";

            @Override
            public boolean onQueryTextSubmit(String query) {
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.length() == 0)
                    return false;

                if (!queryRunning) {
                    new QueryTask().execute(s);
                }

                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    private class LoadDBTask extends AsyncTask<Void, Long, Long> {
        private long progress = 0;

        @Override
        protected Long doInBackground(Void... params) {
            long start = System.nanoTime();
            final float interval = 0.05f;
            dataSource = AndroidUSDADatabase.newDatabase(getResources(), new Runnable() {
                @Override
                public void run() {
                    progress++;

                    publishProgress((long) (progress * interval * 100));
                }
            }, interval);
            return System.nanoTime() - start;
        }


        protected void onPostExecute(Long result) {
            statusView.setText("DB loaded after " + result / 1000000000 + " s");

            progressBar.setVisibility(ProgressBar.INVISIBLE);
            datasourceSpinner.setEnabled(true);

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
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);

            statusView.setText(values[0] + " % loaded");
        }
    }

    private class LoadUGATask extends AsyncTask<Void, Long, Long> {
        private long progress = 0;

        @Override
        protected Long doInBackground(Void... params) {
            long start = System.nanoTime();
            final float interval = 0.05f;
            dataSource = new UGAFoodServices(MainActivity.this.getResources().openRawResource(R.raw.uga_cached),
                    MainActivity.this.getResources().openRawResource(R.raw.uga_dict));
            return System.nanoTime() - start;
        }

        protected void onPostExecute(Long result) {
            statusView.setText("UGA items loaded after " + result / 1000000000 + " s (cached)");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            datasourceSpinner.setEnabled(true);
        }

    }

    private class QueryTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            queryRunning = true;
            queryResult = dataSource.search(params[0]);
            queryRunning = false;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // statusView.setText("Results: " + queryResult.size());
            String formatted = printSearchResults(queryResult);
            // contentView.setText(formatted);

            mAdapter.changeResults(queryResult);
        }
    }

}
