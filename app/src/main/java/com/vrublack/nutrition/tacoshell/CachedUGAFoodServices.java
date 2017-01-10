package com.vrublack.nutrition.tacoshell;

import android.content.Context;
import android.content.res.Resources;

import com.vrublack.nutrition.core.FoodItem;
import com.vrublack.nutrition.core.Pair;
import com.vrublack.nutrition.core.SearchResultItem;
import com.vrublack.nutrition.core.SearchableFoodItem;
import com.vrublack.nutrition.core.SyncFoodDataSource;
import com.vrublack.nutrition.core.search.FoodSearch;
import com.vrublack.nutrition.core.search.LevenshteinFoodSearch;
import com.vrublack.nutrition.core.uga.UGAFoodItem;
import com.vrublack.nutrition.core.uga.UGAScraper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Like UGAFoodServices but caches items
 */
public class CachedUGAFoodServices implements SyncFoodDataSource
{
    private List<UGAFoodItem> items;

    private FoodSearch foodSearch;

    private static final String FNAME = "UGA_cached";

    private Context c;

    private boolean usedCache;

    public CachedUGAFoodServices(Context c)
    {
        this.c = c;
        items = loadCachedIfNotExpired();
        if (items == null)
        {
            items = UGAScraper.scrapeAllLocations();
            if (!items.isEmpty()) // don't cache if empty because it might be due to network error
                cacheItems(items);
        } else
        {
            usedCache = true;
        }
        foodSearch = new LevenshteinFoodSearch(getSearchableFoodItems(), null);
    }

    public boolean usedCache()
    {
        return usedCache;
    }

    private void cacheItems(List<UGAFoodItem> items)
    {
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream(c.openFileOutput(FNAME, Context.MODE_PRIVATE));
            // add current date
            Pair<Date, List<UGAFoodItem>> pair = new Pair<>(new Date(), items);
            oos.writeObject(pair);
            oos.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private List<UGAFoodItem> loadCachedIfNotExpired()
    {
        try
        {
            ObjectInputStream ois = new ObjectInputStream(c.openFileInput(FNAME));
            Pair<Date, List<UGAFoodItem>> pair = (Pair<Date, List<UGAFoodItem>>) ois.readObject();
            ois.close();

            if (!hasExpired(pair.first))
                return pair.second;
            else
                return null;

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private boolean hasExpired(Date date)
    {
        // expired if not on the same day

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date);
        cal2.setTime(new Date());
        boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        return !sameDay;
    }


    @Override
    public List<SearchResultItem> search(String searchStr)
    {
        return foodSearch.searchFood(searchStr);
    }

    @Override
    public FoodItem retrieve(String id)
    {
        for (UGAFoodItem item : items)
            if (item.getId().equals(id))
                return item;
        return null;
    }

    @Override
    public FoodItem get(String id)
    {
        return retrieve(id);
    }

    public List<SearchableFoodItem> getSearchableFoodItems()
    {
        List<SearchableFoodItem> searchableFoodItems = new ArrayList<>();
        searchableFoodItems.addAll(items);
        return searchableFoodItems;
    }
}