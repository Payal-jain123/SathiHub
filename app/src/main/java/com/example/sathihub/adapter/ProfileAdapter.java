package com.example.sathihub.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sathihub.R;
import com.example.sathihub.model.ProfileModel;

import java.util.ArrayList;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProfileModel model);
    }

    ArrayList<ProfileModel> list;
    OnItemClickListener listener;

    public ProfileAdapter(ArrayList<ProfileModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileModel model = list.get(position);

        holder.txtName.setText(model.getName());

        holder.txtDetails.setText(
                model.getProfession() + " | " +
                        model.getEducation() + " | " +
                        model.getReligion()
        );

        holder.itemView.setOnClickListener(v -> listener.onItemClick(model));
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtDetails = itemView.findViewById(R.id.txtDetails);
        }
    }
}
