package com.vrublack.nutrition.tacoshell;

import android.content.res.Resources;

import com.vrublack.nutrition.core.DummySearchHistory;
import com.vrublack.nutrition.core.SearchHistory;
import com.vrublack.nutrition.core.search.DescriptionBase;
import com.vrublack.nutrition.core.usda.USDAFoodDatabase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class AndroidUSDADatabase extends USDAFoodDatabase
{
    private static Resources r;


    public static AndroidUSDADatabase newDatabase(Resources r, Runnable onStatusUpdate, float percentagInterval)
    {
        AndroidUSDADatabase.r = r;
        return new AndroidUSDADatabase(onStatusUpdate, percentagInterval);
    }

    private AndroidUSDADatabase(Runnable onStatusUpdate, float percentagInterval)
    {
        super(onStatusUpdate, percentagInterval);
    }

    @Override
    protected SearchHistory getSearchHistory()
    {
        return new DummySearchHistory();
    }

    @Override
    public BufferedReader getBufferedReader() throws FileNotFoundException
    {
        return new BufferedReader(new InputStreamReader(r.openRawResource(R.raw.usda_db)));
    }

    @Override
    public DescriptionBase getDescriptionBase() throws FileNotFoundException {
        return DescriptionBase.getDescriptionBase(r.openRawResource(R.raw.food_english));
    }
}
