package com.brazedblue.waverly;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class ShowStatementFragment extends Fragment implements StatementView.SwipeListener,
        StatementView.TapListener
{

    private StatementView m_StatementView;
    private StatementsStorage m_Storage;
    private StatementData m_CurrentStatement;
    private View m_MainView;
    private ImageView m_AnimationImageView;
    HandlingNew m_NewHandling = HandlingNew.NOTHING;

    enum HandlingNew
    {
        NOTHING,
        LAUNCHEDIT,
    }


    static private final String TAG = "ShowStatementFragment";
    static private final int EDIT_STATEMENT_REQUEST = 2;
    public static final String STACK_TAG = "ShowStatementFragment";
    public static final String EXTRA_IS_NEW = "NewPushMe";


    private AnimatorSet m_ViewAnimator;
    static final int DISSOLVE_DURATION = 1000;
    private Interpolator linear = new LinearInterpolator();
    private Interpolator accelDecel = new AccelerateDecelerateInterpolator();





    static ShowStatementFragment newInstance(String uuid, boolean isNew)
    {
        ShowStatementFragment fragment = new ShowStatementFragment();
        Bundle args = new Bundle();
        args.putString(StatementsStorage.EXTRA_UUID, uuid);
        args.putBoolean(EXTRA_IS_NEW, isNew);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        m_Storage = StatementsStorage.getStatementsStorage(this.getActivity());


        Bundle arguments = getArguments();
        if (arguments != null) {
            String uuidString = arguments.getString(StatementsStorage.EXTRA_UUID);
            if (uuidString != null) {
                m_CurrentStatement = m_Storage.getStatementWithUUIDString(uuidString);
            }

            if (arguments.getBoolean(EXTRA_IS_NEW))
            {
                m_NewHandling = HandlingNew.LAUNCHEDIT;
                pushEditingActivity(uuidString);
            }
        }

        setHasOptionsMenu(true);
    }


    void pushEditingActivity(String uuidStr)
    {
        Intent intent = new Intent(this.getActivity(), EditStatementActivity.class);
        try {
            intent.putExtra(StatementsStorage.EXTRA_UUID, uuidStr);
            startActivityForResult(intent, EDIT_STATEMENT_REQUEST);
        } catch (Exception e) {
            CustomLog.d(TAG, "onOptionsItemSelected exception " + e.getLocalizedMessage());
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        m_MainView = inflater.inflate(com.brazedblue.waverly.R.layout.activity_show_statement, container, false);
        m_StatementView = (StatementView) m_MainView.findViewById(com.brazedblue.waverly.R.id.statementView);
//        m_StatementView.setupSubViews();
        m_StatementView.setVerbButtonClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                animateClick();
            }
        });

        m_StatementView.setSwipeListener(this);
        m_StatementView.setTapListener(this);

        return m_MainView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(com.brazedblue.waverly.R.menu.statement_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id)
        {
            case com.brazedblue.waverly.R.id.editStatementAction: {
                try {
                    StatementData editingStatement = m_Storage.cloneStatementData(m_CurrentStatement);
                    pushEditingActivity(editingStatement.getUUID().toString());
                } catch (Exception e) {
                    CustomLog.d(TAG, "onOptionsItemSelected exception " + e.getLocalizedMessage());
                }

                return true;
            }
            case com.brazedblue.waverly.R.id.sendStatementAction:
            {
                try {
                    sendEmailDataIntent(m_CurrentStatement);

                } catch (Exception e) {
                    Toast.makeText(this.getActivity(), "Error sending message\n" + e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                            .show();

                    CustomLog.d(TAG, "onOptionsItemSelected exception " + e.getLocalizedMessage());
                }

                return true;
            }

            case com.brazedblue.waverly.R.id.sendStatementRefresh:
            {
                m_StatementView.setToStart();
                return true;
            }

            default :
                super.onOptionsItemSelected(item);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    void sendEmailDataIntent(StatementData data) throws IOException, GeneralSecurityException
    {
        Intent intent = null;

        intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(com.brazedblue.waverly.R.string.email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_content));
        intent.setTypeAndNormalize("application/waverly");
        File file = m_Storage.encodedFileForData(data);
        String uriString = "content://" + StatementsProvider.URI_AUTHORITY + "/"
                + file.getName();
        Uri fileUri = Uri.parse(uriString);
        intent.putExtra(
                Intent.EXTRA_STREAM,
                fileUri);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
        else {
            CustomLog.e(TAG, "intent.resolveActivity failed");
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (EDIT_STATEMENT_REQUEST == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                String uuidString = data.getStringExtra(StatementsStorage.EXTRA_UUID);

                StatementData newStatement = m_Storage.getStatementWithUUIDString(uuidString);
                if (m_NewHandling != HandlingNew.LAUNCHEDIT) {
                    if (m_CurrentStatement == null || !newStatement.isSame(m_CurrentStatement)) {
                        if (m_CurrentStatement != null) {
                            DialogFragment newFragment = new SaveStatementDialog();
                            Bundle args = new Bundle();
                            args.putString(SaveStatementDialog.NEW_UUID_ARGUMENT, newStatement.getUUID().toString());
                            args.putString(SaveStatementDialog.OLD_UUID_ARGUMENT, m_CurrentStatement.getUUID().toString());
                            newFragment.setArguments(args);
                            newFragment.show(getFragmentManager(), "SaveStatement");
                        } else {
                            setToStatement(newStatement);
                        }
                    } else {
                        m_Storage.deleteStatement(newStatement);
                    }
                }
                else
                {
                    m_NewHandling = HandlingNew.NOTHING;
                }
            }

        }
    }



    @Override
    public void onStart() {
        try {
            if (m_CurrentStatement == null) {
                m_CurrentStatement = m_Storage.getNextStatement(null);

            }
            setToStatement(m_CurrentStatement);
        } catch (Throwable t) {
            CustomLog.e(TAG, "Could not loadData", t);
        }
        super.onStart();
    }

    @Override
    public void onDestroy() {
        if (m_CurrentStatement != null)
        {
            m_CurrentStatement.purge();
        }
        super.onDestroy();
    }

    private void setToStatement(StatementData statement) {
        if (m_CurrentStatement != null && statement != m_CurrentStatement)
        {
            m_CurrentStatement.purge();
        }
        m_CurrentStatement = statement;
        if (m_CurrentStatement != null) {
            m_StatementView.setToStatement(m_CurrentStatement);
        }
    }

    private void animateClick() {
        if (m_ViewAnimator == null) {
            View verbButton = m_StatementView.getVerbButton();
            View verbText = m_StatementView.getVerbText();
            ResultView resultView = m_StatementView.getResultView();

            ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(verbButton, "alpha",
                    1, 0).setDuration(DISSOLVE_DURATION);

            ObjectAnimator verbTextAnimator = ObjectAnimator.ofFloat(verbText, "alpha",
                    0, 1).setDuration(DISSOLVE_DURATION);



            resultView.setAnimationFraction(0);
            resultView.setVisibility(View.VISIBLE);

            int wordCount = resultView.getWordCount();
            ObjectAnimator resultViewAnimator = ObjectAnimator.ofFloat(resultView, "animationFraction",
                    0, wordCount).setDuration(wordCount * 750);


            resultViewAnimator.setInterpolator(linear);

            m_ViewAnimator = new AnimatorSet();
            m_ViewAnimator.play(buttonAnimator).with(verbTextAnimator);
            m_ViewAnimator.play(resultViewAnimator).after(verbTextAnimator);
            m_ViewAnimator.addListener(new MyAnimationListener());
            m_ViewAnimator.start();
        }
    }




    void slideAnimate(boolean toLeft)
    {
        RelativeLayout topLayout = (RelativeLayout) m_MainView.findViewById(com.brazedblue.waverly.R.id.topLayout);
        Bitmap imageBitmap = Bitmap.createBitmap(topLayout.getWidth(), topLayout.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(imageBitmap);
        topLayout.draw(canvas);
        ViewParent parent = topLayout.getParent();
        if (m_AnimationImageView == null) {
            m_AnimationImageView = new ImageView(getActivity());
            ((ViewGroup) parent).addView(m_AnimationImageView);
        }
        else
        {
            m_AnimationImageView.setVisibility(View.VISIBLE);
        }
        m_AnimationImageView.setImageBitmap(imageBitmap);
        m_AnimationImageView.layout((int) topLayout.getX(), (int) topLayout.getY(),
                topLayout.getWidth(), topLayout.getHeight());
        parent.bringChildToFront(topLayout);

        float end = topLayout.getX();
        float start = toLeft ? topLayout.getWidth() : -topLayout.getWidth();

        if (toLeft) {
            m_CurrentStatement = m_Storage.getNextStatement(m_CurrentStatement);
        }
        else {
            m_CurrentStatement = m_Storage.getPreviousStatement(m_CurrentStatement);
        }

        setToStatement(m_CurrentStatement);
        ObjectAnimator slideLeft = ObjectAnimator.ofFloat(topLayout, "X", start, end);
        slideLeft.setDuration(500);
        slideLeft.setInterpolator(accelDecel);
        slideLeft.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                m_AnimationImageView.setVisibility(View.INVISIBLE);
            }
        });
        slideLeft.start();
    }


    @Override
    public void swiped(boolean left) {
        slideAnimate(left);
    }

    //  @Override
    public void selectStatement(String statementID) {
        m_CurrentStatement = m_Storage.getStatementWithUUIDString(statementID);
        if (m_CurrentStatement != null) {
            setToStatement(m_CurrentStatement);
        }
        else
        {
            CustomLog.e(TAG, "selectStatement null statement" );
        }
    }

    @Override
    public void handleTap() {
        animateClick();
    }



    class MyAnimationListener extends AnimatorListenerAdapter {
        public void onAnimationEnd(Animator animation) {
            m_ViewAnimator = null;
            m_MainView.invalidate();
        }
    }

    static public class SaveStatementDialog extends DialogFragment
            implements DialogInterface.OnClickListener

    {
        private String m_NewUUID;
        private String m_OldUUID;
        static final String NEW_UUID_ARGUMENT = "NewStatementUUID";
        static final String OLD_UUID_ARGUMENT = "OldStatementUUID";


        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            m_NewUUID = getArguments().getString(NEW_UUID_ARGUMENT);
            m_OldUUID = getArguments().getString(OLD_UUID_ARGUMENT);       }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(getString(com.brazedblue.waverly.R.string.save_statement_message));
            builder.setPositiveButton(com.brazedblue.waverly.R.string.add_statement, this);
            builder.setNeutralButton(com.brazedblue.waverly.R.string.replace_statement, this);
            builder.setNegativeButton(com.brazedblue.waverly.R.string.delete_statement, this);
            return builder.create();

        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            StatementsStorage storage = StatementsStorage.getStatementsStorage(getActivity());
            String selectID = null;
            // replace by deleting old
            if (which == DialogInterface.BUTTON_NEUTRAL)
            {
                ArrayList<String> toDelete = new ArrayList<>();
                toDelete.add(m_OldUUID);
                storage.deleteItems(toDelete);
                selectID = m_NewUUID;
            }
            // delete new
            else if (which == DialogInterface.BUTTON_NEGATIVE)
            {
                ArrayList<String> toDelete = new ArrayList<>();
                toDelete.add(m_NewUUID);
                storage.deleteItems(toDelete);
            }
            else if (which == DialogInterface.BUTTON_POSITIVE)
            {
                selectID = m_NewUUID;
            }
            if (selectID != null)
            {
                FragmentManager fragmentManager = getFragmentManager();
                ShowStatementFragment fragment = (ShowStatementFragment)fragmentManager.findFragmentByTag(STACK_TAG);
                if (fragment != null) {
                    fragment.selectStatement(selectID);
                }
            }
        }
    }
}
