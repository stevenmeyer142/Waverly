package com.brazedblue.waverly;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import java.io.InputStream;

public class MainStatementsActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, OnListFragmentInteractionListener {

    private static final String DEFAULT_PREFS = "Default";
    private static final String FIRST_RUN_KEY = "FirstRun";
    private static String TAG = "MainStatementsActivity";
    private static final String LIST_FRAGMENT_TAG = "MAIN_STATEMENTS_LIST";
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment m_NavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.brazedblue.waverly.R.layout.activity_main_statements);

		m_NavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(com.brazedblue.waverly.R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		m_NavigationDrawerFragment.setUp(com.brazedblue.waverly.R.id.navigation_drawer,
				(DrawerLayout) findViewById(com.brazedblue.waverly.R.id.drawer_layout));

        Intent intent = getIntent();
        if (intent != null) {
            String type = intent.getType();
            if (type == null) {

                if (isFirstRun())
                {
                    m_NavigationDrawerFragment.closeDrawer();
                    onNavigationDrawerItemSelected(StatementListFragment.COMPOSED_STATEMENT_TYPE);
                    StatementsStorage storage = StatementsStorage.getStatementsStorage(this);

                    pushShowStatement(storage.getNextStatement(null), false);
                }
            }
            else if (type.equals(getString(com.brazedblue.waverly.R.string.pushme_mimetype))) {
                try {

                    Uri uri = intent.getData();
                    if (uri == null) {
                        throw new Exception("Uri is null");
                    }
					InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        StatementsStorage storage = StatementsStorage.getStatementsStorage(this);
                        StatementData data = storage.decodeStreamToData(inputStream);
						inputStream.close();
						m_NavigationDrawerFragment.closeDrawer();
                        onNavigationDrawerItemSelected(StatementListFragment.RECEIVED_STATEMENT_TYPE);
                        pushShowStatement(data, false);
                    }
				} catch (Exception e) {
					CustomLog.e(TAG, "onCreate " + intent, e);
					Toast.makeText(this, "Error receiving message", Toast.LENGTH_SHORT)
							.show();
				}
			}
		}

		CustomLog.initializeLog(this);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.popBackStack();
		Fragment fragment = null;
		if (position == 0)
		{
			fragment = StatementListFragment.newInstance(StatementListFragment.COMPOSED_STATEMENT_TYPE);
		}
		else if (position == 1)
		{
			fragment = StatementListFragment.newInstance(StatementListFragment.RECEIVED_STATEMENT_TYPE);
		}
		else {
			fragment = AboutFragment.newInstance();
		}
		fragmentManager
				.beginTransaction()
				.replace(com.brazedblue.waverly.R.id.container,
						fragment, LIST_FRAGMENT_TAG).commit();
	}

    boolean isFirstRun()
    {
        boolean firstRun = true;
        SharedPreferences preferences = getSharedPreferences(DEFAULT_PREFS, MODE_PRIVATE);
        firstRun = preferences.getBoolean(FIRST_RUN_KEY, true);
        if (firstRun)
        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRST_RUN_KEY, false);
            editor.commit();
        }
		return firstRun;
    }

	public void onSectionAttached(int number) {
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!m_NavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}



	public void onListFragmentInteraction(StatementData item)
	{
        pushShowStatement(item, false);
    }

	@Override
	public void onNewStatementSelected() {
		StatementData data = StatementsStorage.getStatementsStorage(this).createNewStatement();
		// push ShowStatementFragment
		m_NavigationDrawerFragment.closeDrawer();
		onNavigationDrawerItemSelected(StatementListFragment.COMPOSED_STATEMENT_TYPE);

		ShowStatementFragment fragment = pushShowStatement(data, true);
	}

    private ShowStatementFragment pushShowStatement(StatementData item, boolean isNew)
    {
        FragmentManager fragmentManager = getFragmentManager();
		ShowStatementFragment fragment = ShowStatementFragment.newInstance(item.getUUID().toString(), isNew);
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(com.brazedblue.waverly.R.id.container, fragment, ShowStatementFragment.STACK_TAG)
                .addToBackStack(null)
                .commit();

		return fragment;
    }

}
