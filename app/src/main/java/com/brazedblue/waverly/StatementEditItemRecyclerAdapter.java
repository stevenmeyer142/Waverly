package com.brazedblue.waverly;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Vector;

/**
  */
public class StatementEditItemRecyclerAdapter extends RecyclerView.Adapter<StatementEditItemRecyclerAdapter.ViewHolder> {

    private final List<StatementData> mValues;
    private boolean [] m_DeletedStatements;
    private Drawable m_UndeletedColor;
    private final DeleteItemChangeListener m_Listener;

    StatementEditItemRecyclerAdapter(DeleteItemChangeListener listener,
                                            List<StatementData> items) {
        mValues = items;
        m_Listener = listener;
        m_DeletedStatements = new boolean[mValues.size()];
        for (int i = 0; i < m_DeletedStatements.length; i++) {
            m_DeletedStatements[i] = false;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        boolean isEmpty = mValues.size() <= 0;
        int id = isEmpty ? com.brazedblue.waverly.R.layout.fragment_item_empty : com.brazedblue.waverly.R.layout.fragment_item_edit;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(id, parent, false);
        if (m_UndeletedColor == null)
        {
            m_UndeletedColor = view.getBackground();
        }
        return new ViewHolder(view, isEmpty);
    }

    int getDeletedStatementsCount()
    {
        int result = 0;

        if (m_DeletedStatements != null)
        {
            for (boolean deleted : m_DeletedStatements) {
                if (deleted)
                {
                    result++;
                }
            }
        }

        return result;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (!holder.isEmpty()) {
            holder.setValues(mValues.get(position), position);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int beforeDeleted = getDeletedStatementsCount();
                    m_DeletedStatements[holder.mIndex] = !m_DeletedStatements[holder.mIndex];
                    int afterDeleted = getDeletedStatementsCount();
                    if (m_DeletedStatements[holder.mIndex]) {
                        holder.mView.setBackgroundColor(Color.RED);
                    } else {
                        holder.mView.setBackground(m_UndeletedColor);
                    }

                    if (m_Listener != null) {
                        m_Listener.deleteItemChange(beforeDeleted, afterDeleted);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return Math.max(mValues.size(), 1);
    }

    List<StatementData> getDeletedData() {
        Vector<StatementData> result = new Vector<>();
        if (m_DeletedStatements != null) {
            for (int i = 0; i < m_DeletedStatements.length; ++i)
            {
                if (m_DeletedStatements[i])
                {
                    result.add(mValues.get(i));
                }
            }
        }

        return result;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView mNounTextView;
        private final ImageView mImageView;
        private final boolean mIsEmpty;
        private int mIndex;



        ViewHolder(View view, boolean isEmpty) {
            super(view);
            mView = view;
            mIsEmpty = isEmpty;
            mNounTextView = isEmpty? null : (TextView) view.findViewById(com.brazedblue.waverly.R.id.list_noun);
            mImageView = isEmpty? null : (ImageView)view.findViewById(com.brazedblue.waverly.R.id.list_image);
        }

        boolean isEmpty()
        {
            return mIsEmpty;
        }

        void setValues(StatementData data, int index)
        {
            if (!mIsEmpty) {
                mIndex = index;

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
        }


        @Override

        public String toString() {
            return super.toString() + " '" +
                    (mIsEmpty ?  "Empty" : mNounTextView.getText()) + "'";
        }
    }

    public interface DeleteItemChangeListener
    {
        void deleteItemChange(int beforeDeleted, int afterDeleted);
    }
}
