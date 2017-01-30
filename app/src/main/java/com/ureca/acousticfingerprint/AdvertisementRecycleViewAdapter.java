package com.ureca.acousticfingerprint;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Andrew on 11/17/16.
 */

public class AdvertisementRecycleViewAdapter extends RecyclerView.Adapter<AdvertisementRecycleViewAdapter.AdvertisementViewHolder> {

    public static class AdvertisementViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView adName;
        TextView adDetails;
        //TextView adSummary;
        Button adLink;
        ImageView adImage;
        Button adRemove;

        AdvertisementViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv);
            adName = (TextView) itemView.findViewById(R.id.ad_name);
            adDetails = (TextView) itemView.findViewById(R.id.ad_details);
            //adSummary = (TextView)itemView.findViewById(R.id.ad_summary);
            adLink = (Button) itemView.findViewById(R.id.ad_link);
            adImage = (ImageView) itemView.findViewById(R.id.ad_image);
            adRemove = (Button) itemView.findViewById(R.id.ad_remove);

        }
    }

    List<Advertisement> ads;
    Context context;

    AdvertisementRecycleViewAdapter(List<Advertisement> ads, Context context) {
        this.ads = ads;
        this.context = context;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public AdvertisementViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ad_card, viewGroup, false);
        AdvertisementViewHolder avh = new AdvertisementViewHolder(v);
        return avh;
    }

    //int mExpandedPosition = -1;

    @Override
    public void onBindViewHolder(final AdvertisementViewHolder adViewHolder, int i) {
        adViewHolder.adName.setText(ads.get(i).name);
        adViewHolder.adDetails.setText(ads.get(i).details);
        //adViewHolder.adDetails.setVisibility(View.GONE);
        //adViewHolder.adSummary.setText(ads.get(i).summary);
        final int j = i;
        adViewHolder.adLink.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myWebLink = new Intent(android.content.Intent.ACTION_VIEW);
                myWebLink.setData(Uri.parse(ads.get(j).link));
                v.getContext().startActivity(myWebLink);
            }
        });
        adViewHolder.adImage.setImageResource(ads.get(i).imageID);

        /*
        final boolean isExpanded = i==mExpandedPosition;
        adViewHolder.adDetails.setVisibility(isExpanded?View.VISIBLE:View.GONE);
        adViewHolder.adSummary.setVisibility(isExpanded?View.GONE:View.VISIBLE);
        adViewHolder.itemView.setActivated(isExpanded);
        adViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpandedPosition = isExpanded ? -1:j;
                TransitionManager.beginDelayedTransition((ViewGroup)v.getParent());
                notifyDataSetChanged();
            }
        });
        */

        adViewHolder.adRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

                // set title
                alertDialogBuilder.setTitle("Confirmation");

                // set dialog message
                alertDialogBuilder
                        .setMessage("Are you sure you want to delete this advertisement?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                remove(j);
                                //removeAd.setVisibility(adapter.getAds().isEmpty()?View.GONE:View.VISIBLE);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });
    }

    public void insert(int position, Advertisement ad) {
        ads.add(position, ad);
        notifyDataSetChanged();
    }

    public void remove(int position) {
        ads.remove(position);
        notifyDataSetChanged();
    }

    public void removeAll() {
        int range = ads.size();
        for (int i = 0; i < range; i++) {
            remove(0);
        }
    }

    public void collapseAll() {
    }

    public List<Advertisement> getAds() {
        return ads;
    }

    @Override
    public long getItemId(int position) {
        return ads.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return ads.size();
    }
}
