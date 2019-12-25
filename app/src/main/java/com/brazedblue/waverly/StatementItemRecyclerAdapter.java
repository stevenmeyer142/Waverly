package com.brazedblue.waverly;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.List;

/**
 */
public class StatementItemRecyclerAdapter extends RecyclerView.Adapter<StatementItemRecyclerAdapter.ViewHolder> {

    protected final List<StatementData> mValues;
    protected final OnListFragmentInteractionListener mListener;
    protected final int m_StatementType;

    private static final String TAG = "StatementItemRecyclerAdapter";
    static protected final int STATEMENT_VIEWTYPE = 0;
    static protected final int EMPTY_VIEWTYPE = STATEMENT_VIEWTYPE + 1;
    static protected final int NEW_VIEWTYPE = EMPTY_VIEWTYPE + 1;


    protected StatementItemRecyclerAdapter(List<StatementData> items,
                                        OnListFragmentInteractionListener listener,
                                        int statementType) {
        mValues = items;
        mListener = listener;
        m_StatementType = statementType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CustomLog.e(TAG, "onCreateViewHolder not overridden");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(com.brazedblue.waverly.R.layout.fragment_item_empty, parent, false);
        return new ViewHolder(view);
    }

      @Override
    public void onBindViewHolder(final ViewHolder holder, int position)
    {
        CustomLog.e(TAG, "onBindViewHolder not overridden");

     }


    @Override
    public int getItemCount() {
        return 0;
    }

    static public class ViewHolder extends RecyclerView.ViewHolder {
        protected final View m_View;
        protected boolean m_Clickable;

        public ViewHolder(View view) {
            super(view);
            m_View = view;

          }

        void setValue(StatementData data)
        {
        }

        StatementData getValue()
        {
            return null;
        }

        boolean clickable()
        {
            return m_Clickable;
        }

        void setClickable(boolean clickable)
        {
            m_Clickable = clickable;
        }
    }

    static public class  StatementViewHolder extends ViewHolder
    {
        private final TextView mNounTextView;
        private final ImageView mImageView;
        public StatementData mItem;

        public StatementViewHolder(View view) {
            super(view);
            mNounTextView = (TextView) view.findViewById(com.brazedblue.waverly.R.id.list_noun);
            mImageView = (ImageView) view.findViewById(com.brazedblue.waverly.R.id.list_image);
            m_Clickable = true;
        }
        void setValue(StatementData data)
        {
            mItem = data;

            mNounTextView.setText(data.getNoun());
            int maxHeight = mImageView.getMaxHeight();
            int maxWidth = mImageView.getMaxWidth();
            Bitmap bitmap = data.getPicture(maxWidth, maxHeight);
            if (bitmap != null) {
                mImageView.setVisibility(View.VISIBLE);

                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }
            } else {
                mImageView.setVisibility(View.INVISIBLE);

            }
        }

        StatementData getValue()
        {
            return mItem;
        }

        public String toString() {
            return super.toString() + " '" + mNounTextView.getText() + "'";
        }
    }

 }
