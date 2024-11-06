package com.example.yogaappadmin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class YogaClassAdapter extends RecyclerView.Adapter<YogaClassAdapter.YogaClassViewHolder> {
    private final List<YogaClass> yogaClasses;
    private final Context context;
    private final YogaClassListener listener;

    public YogaClassAdapter(List<YogaClass> yogaClasses, Context context, YogaClassListener listener) {
        this.yogaClasses = yogaClasses;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public YogaClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_yoga_class, parent, false);
        return new YogaClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YogaClassViewHolder holder, int position) {
        YogaClass yogaClass = yogaClasses.get(position);

        holder.className.setText(yogaClass.getClassType());
        holder.capacity.setText("Capacity: " + yogaClass.getCapacity());
        holder.classType.setText("Type: " + yogaClass.getClassType());

        holder.viewDetailsButton.setOnClickListener(v -> listener.onViewDetails(yogaClass));
    }

    public void updateList(List<YogaClass> newList) {
        yogaClasses.clear();
        yogaClasses.addAll(newList);
        notifyDataSetChanged(); 
    }

    @Override
    public int getItemCount() {
        return yogaClasses.size();
    }

    public static class YogaClassViewHolder extends RecyclerView.ViewHolder {
        TextView className, capacity, classType;
        Button viewDetailsButton;

        public YogaClassViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.className);
            capacity = itemView.findViewById(R.id.capacity);
            classType = itemView.findViewById(R.id.classType);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}
