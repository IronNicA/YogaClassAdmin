package com.example.yogaappadmin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClassInstanceAdapter extends RecyclerView.Adapter<ClassInstanceAdapter.ViewHolder> {

    private List<ClassInstance> instanceList;
    private YogaClassListener listener;

    public ClassInstanceAdapter(List<ClassInstance> instanceList, YogaClassListener listener) {
        this.instanceList = instanceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_instance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassInstance instance = instanceList.get(position);
        holder.date.setText(instance.getDate());
        holder.teacher.setText("Teacher: " + instance.getTeacher());
        holder.viewInstanceDetailsButton.setOnClickListener(view -> listener.onViewInstanceDetails(instance));
    }

    @Override
    public int getItemCount() {
        return instanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView date, teacher;
        Button viewInstanceDetailsButton;

        public ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.instanceDate);
            teacher = itemView.findViewById(R.id.teacherName);
            viewInstanceDetailsButton = itemView.findViewById(R.id.viewInstanceDetailsButton);
        }
    }
}
