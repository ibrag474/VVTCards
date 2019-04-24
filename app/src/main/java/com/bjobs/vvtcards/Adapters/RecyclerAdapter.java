package com.bjobs.vvtcards.Adapters;

/**
 * Created by Ibragim on 2017-06-29.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.bjobs.vvtcards.CardInfo;
import com.bjobs.vvtcards.R;
import com.bjobs.vvtcards.VVTCards;
import com.bjobs.vvtcards.fragments.CreateDelCard;
import com.bjobs.vvtcards.service.DOMParser;

import java.util.List;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private static final String TAG = "RecyclerAdapter";

    private List<CardInfo> cardList;
    //private String identifer;

    Context context;

    public RecyclerAdapter(List<CardInfo> cardList, Context context) {
        this.cardList = cardList;
        this.context = context;
    }

    public void callDelDialog(String name, String id) {
        FragmentManager fragmentManager = ((FragmentActivity)context).getSupportFragmentManager();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("id", id);
        DialogFragment newFragment = new CreateDelCard();
        newFragment.setArguments(args);
        newFragment.show(fragmentManager, "TAG");
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    @Override
    public void onBindViewHolder(final ViewHolder recyclerViewHolder, int i) {
        CardInfo ci = cardList.get(i);
        final String identifer = ci.cardName;
        final String id = Integer.toString(i);
        recyclerViewHolder.cardName.setText(ci.cardName);
        recyclerViewHolder.cardNumber.setText(ci.cardNumber);
        recyclerViewHolder.cardTicket.setText(ci.cardTicket);
        recyclerViewHolder.cardBalance.setText(ci.cardBalance);


        recyclerViewHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                callDelDialog(identifer, id);
                return true;
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new ViewHolder(itemView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;

        protected TextView cardName;
        protected TextView cardNumber;
        protected TextView cardTicket;
        protected TextView cardBalance;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            cardName =  (TextView) itemView.findViewById(R.id.item_title);
            cardNumber = (TextView) itemView.findViewById(R.id.item_number);
            cardTicket = (TextView) itemView.findViewById(R.id.item_ticket);
            cardBalance = (TextView) itemView.findViewById(R.id.item_balance);
        }
    }
}
