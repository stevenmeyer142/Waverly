package com.brazedblue.waverly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by stevemeyer on 1/27/16.
 */
public class ReceivedItemRecyclerAdaptor extends StatementItemRecyclerAdapter
{
    ReceivedItemRecyclerAdaptor(List<StatementData> items, OnListFragmentInteractionListener listener, int statementType) {
        super(items, listener, statementType);
    }

    @Override
    public int getItemCount() {
        return Math.max(1, mValues.size());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mValues.size() <= 0)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(com.brazedblue.waverly.R.layout.fragment_item_empty, parent, false);
            return new ViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(com.brazedblue.waverly.R.layout.fragment_item, parent, false);
            return new StatementViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
       if (mValues.size() > 0)
       {
           holder.setValue(mValues.get(position));
           holder.m_View.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   if (null != mListener) {
                       mListener.onListFragmentInteraction(holder.getValue());
                   }
               }
           });
       }
    }

    @Override
    public int getItemViewType(int position) {
        if (mValues.size() <= 0)
        {
            return EMPTY_VIEWTYPE;
        }
        else
        {
            return STATEMENT_VIEWTYPE;
        }
    }
}
