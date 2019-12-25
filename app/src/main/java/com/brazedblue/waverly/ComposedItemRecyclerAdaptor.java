package com.brazedblue.waverly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by stevemeyer on 1/27/16.
 */
public class ComposedItemRecyclerAdaptor extends StatementItemRecyclerAdapter
{
    ComposedItemRecyclerAdaptor(List<StatementData> items, OnListFragmentInteractionListener listener, int statementType) {
        super(items, listener, statementType);
    }

    @Override
    public int getItemCount() {
        return mValues.size() + 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == NEW_VIEWTYPE)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(com.brazedblue.waverly.R.layout.fragment_item_new, parent, false);
            ViewHolder result =  new ViewHolder(view);
            result.setClickable(true);
            return result;
        }
        else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(com.brazedblue.waverly.R.layout.fragment_item, parent, false);
            return new StatementViewHolder(view);
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
        {
            return NEW_VIEWTYPE;
        }
        else
        {
            return STATEMENT_VIEWTYPE;
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (position > 0) {
            holder.setValue(mValues.get(position - 1));
            holder.m_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        mListener.onListFragmentInteraction(holder.getValue());
                    }
                }
            });
        }
        else
        {
            holder.m_View.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        mListener.onNewStatementSelected();
                    }
                }
            });

        }
       }

 }
