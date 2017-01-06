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

        public ViewHolder(View itemView, TextView description, TextView details)
        {
            super(itemView);

            this.description = description;
            this.details = details;
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
                (TextView) v.findViewById(R.id.details));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.description.setText(items.get(position).getDescription());
        holder.details.setText(items.get(position).getNutritionInformation());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount()
    {
        return items.size();
    }
}
