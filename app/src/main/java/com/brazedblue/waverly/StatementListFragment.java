package com.brazedblue.waverly;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class StatementListFragment extends Fragment {
    private OnListFragmentInteractionListener mListener;
    RecyclerView m_RecyclerView;
    private int m_StatementType;


    private static final String ARG_STATEMENT_TYPE = "statement-type";
    static final int RECEIVED_STATEMENT_TYPE = 0;
    static final int COMPOSED_STATEMENT_TYPE = 1;
    private static final String TAG = "StatementListFragment";
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StatementListFragment() {
    }


    public static StatementListFragment newInstance(int statementType) {
        StatementListFragment fragment = new StatementListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STATEMENT_TYPE, statementType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            m_StatementType = getArguments().getInt(ARG_STATEMENT_TYPE);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu.findItem(com.brazedblue.waverly.R.id.editStatementListAction) == null) {
            inflater.inflate(com.brazedblue.waverly.R.menu.main_statements, menu);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.brazedblue.waverly.R.id.editStatementListAction) {
            Intent intent = new Intent(this.getActivity(), StatementListEditActivity.class);
            try {
                intent.putExtra(StatementListEditActivity.ARG_STATEMENT_TYPE, m_StatementType);
                startActivity(intent);
            }
            catch (Exception e)
            {
                CustomLog.d(TAG, "onOptionsItemSelected exception " + e.getLocalizedMessage());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (m_RecyclerView != null)
        {
            updateRecyclerViewAdapter();
        }
    }

    private void updateRecyclerViewAdapter()
    {
        List<StatementData> items = null;
        StatementsStorage statementsStorage = StatementsStorage.getStatementsStorage(getActivity());

        if (m_StatementType == COMPOSED_STATEMENT_TYPE)
        {
            items = statementsStorage.getComposedStatements();
            m_RecyclerView.setAdapter(new ComposedItemRecyclerAdaptor(items, mListener, m_StatementType));
        }
        else if (m_StatementType == RECEIVED_STATEMENT_TYPE)
        {
            items = statementsStorage.getReceivedStatements();
            m_RecyclerView.setAdapter(new ReceivedItemRecyclerAdaptor(items, mListener, m_StatementType));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(com.brazedblue.waverly.R.layout.fragment_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            m_RecyclerView = (RecyclerView) view;
            m_RecyclerView.setLayoutManager(new LinearLayoutManager(context));

            updateRecyclerViewAdapter();
         }
        return view;
    }


    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
