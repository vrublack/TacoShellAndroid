package com.vrublack.nutrition.tacoshell;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vrublack.nutrition.core.SearchResultItem;
import com.vrublack.nutrition.core.SyncFoodDataSource;

import java.util.List;


public class FoodListAdapter extends RecyclerView.Adapter<FoodListAdapter.ViewHolder>
{
    private List<SearchResultItem> items;

    public void changeResults(List<SearchResultItem> results)
    {
        this.items = results;

        notifyDataSetChanged();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView description;
        public TextView details;
        public MacroNutrientChart chart;

        public ViewHolder(View itemView, TextView description, TextView details, MacroNutrientChart chart)
        {
            super(itemView);

            this.description = description;
            this.details = details;
            this.chart = chart;
        }
    }

    public FoodListAdapter(List<SearchResultItem> items)
    {
        this.items = items;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FoodListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType)
    {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.food_item, parent, false);
        // set the view's size, margins, paddings and layout parameters


        return new ViewHolder(v, (TextView) v.findViewById(R.id.description),
                (TextView) v.findViewById(R.id.details), (MacroNutrientChart) v.findViewById(R.id.chart));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        SearchResultItem item = items.get(position);
        holder.description.setText(item.getDescription());
        holder.details.setText(item.getNutritionInformation());
        float[] breakdown = parseMacroBreakdown(item.getNutritionInformation());
        holder.chart.setBreakdown(breakdown[0], breakdown[1], breakdown[2]);
    }

    // this is a bit dirty but several changes had to be made to the core library to allow
    // retrieval of the values themselves
    private float[] parseMacroBreakdown(String nutritionInformation)
    {
        // Per 342g - Calories: 835kcal | Fat: 32.28g | Carbs: 105.43g | Protein: 29.41g
        float[] breakdown = new float[3];
        String fat = "Fat: ";
        String carbs = "Carbs: ";
        String protein = "Protein: ";
        int fatIndex = nutritionInformation.indexOf(fat) + fat.length();
        breakdown[1] = Float.parseFloat(nutritionInformation.substring(fatIndex,
                nutritionInformation.indexOf('g', fatIndex)));

        int carbsIndex = nutritionInformation.indexOf(carbs) + carbs.length();
        breakdown[0] = Float.parseFloat(nutritionInformation.substring(carbsIndex,
                nutritionInformation.indexOf('g', carbsIndex)));

        int proteinIndex = nutritionInformation.indexOf(protein) + protein.length();
        breakdown[2] = Float.parseFloat(nutritionInformation.substring(proteinIndex,
                nutritionInformation.indexOf('g', proteinIndex)));

        return breakdown;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount()
    {
        return items.size();
    }
}
