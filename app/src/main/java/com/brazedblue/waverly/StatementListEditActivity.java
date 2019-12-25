package com.brazedblue.waverly;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
  */
public class StatementListEditActivity extends FragmentActivity implements StatementEditItemRecyclerAdapter.DeleteItemChangeListener {
    RecyclerView m_RecyclerView;
    StatementEditItemRecyclerAdapter m_RecyclerAdapter;
    private int m_StatementType;
    static final private String DELETE_IDS = "DeleteIDS";


    static final String ARG_STATEMENT_TYPE = "statement-type";
     /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.brazedblue.waverly.R.layout.fragment_item_list);
        View view = findViewById(com.brazedblue.waverly.R.id.list);
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            m_RecyclerView = (RecyclerView) view;
            m_RecyclerView.setLayoutManager(new LinearLayoutManager(context));

            updateRecyclerViewAdapter();
        }

        Intent intent = getIntent();

        if (intent != null) {
            m_StatementType = intent.getIntExtra(ARG_STATEMENT_TYPE, 0);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int deletedCount = 0;
        if (m_RecyclerAdapter != null)
        {
            deletedCount = m_RecyclerAdapter.getDeletedStatementsCount();
        }
        if (deletedCount == 0) {
            getMenuInflater().inflate(com.brazedblue.waverly.R.menu.edit_statement_list_menu, menu);
        }
        else
        {
            getMenuInflater().inflate(com.brazedblue.waverly.R.menu.edit_statement_list_delete_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case com.brazedblue.waverly.R.id.doneEditStatementList:
            {
                finish();

                return true;
            }
            case com.brazedblue.waverly.R.id.deleteEditStatementList: {
                List<StatementData> deletedData = m_RecyclerAdapter.getDeletedData();
                if (deletedData.size() > 0) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (StatementData data : deletedData) {
                        ids.add(data.getUUID().toString());
                    }
                    FragmentManager fragmentManager = getFragmentManager();
                    ConfirmDeleteDialog newFragment = new ConfirmDeleteDialog();
                    Bundle args = new Bundle();
                    args.putStringArrayList(DELETE_IDS, ids);
                    newFragment.setArguments(args);

                    newFragment.show(fragmentManager, "dialog");
                    return true;
                }
            }
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
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
        StatementsStorage statementsStorage = StatementsStorage.getStatementsStorage(this);
        if (m_StatementType == StatementListFragment.COMPOSED_STATEMENT_TYPE)
        {
            items = statementsStorage.getComposedStatements();
        }
        else if (m_StatementType == StatementListFragment.RECEIVED_STATEMENT_TYPE)
        {
            items = statementsStorage.getReceivedStatements();
        }

        m_RecyclerAdapter = new StatementEditItemRecyclerAdapter(this, items);
        m_RecyclerView.setAdapter(m_RecyclerAdapter);

    }


    @Override
    public void deleteItemChange(int beforeDeleted, int afterDeleted)
    {
        if ((beforeDeleted == 0 && afterDeleted > 0) ||
                (beforeDeleted > 0 && afterDeleted == 0))
        {
            invalidateOptionsMenu();
        }
    }


    public static class ConfirmDeleteDialog extends DialogFragment
    {
        ArrayList<String> m_DeleteIDS = null;

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            m_DeleteIDS = getArguments().getStringArrayList(DELETE_IDS);
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(com.brazedblue.waverly.R.string.dialog_delete_selection)
                    .setPositiveButton(com.brazedblue.waverly.R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            StatementsStorage storage = StatementsStorage.getStatementsStorage(ConfirmDeleteDialog.this.getActivity());
                            storage.deleteItems(m_DeleteIDS);
                            Activity activity = getActivity();
                            if (activity != null && activity instanceof StatementListEditActivity) {
                                StatementListEditActivity myActivity = (StatementListEditActivity)activity;
                                myActivity.updateRecyclerViewAdapter();
                                myActivity.invalidateOptionsMenu();
                            }
                            ConfirmDeleteDialog.this.getDialog().dismiss();
                        }
                    })
                    .setNegativeButton(com.brazedblue.waverly.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ConfirmDeleteDialog.this.getDialog().cancel();

                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }

    }

}
