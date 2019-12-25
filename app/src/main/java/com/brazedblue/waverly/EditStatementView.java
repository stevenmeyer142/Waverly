package com.brazedblue.waverly;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.brazedblue.waverly.StatementData.STATEMENT_ELEMENT;

/**
 * TODO: document your custom view class.
 */
public class EditStatementView extends StatementView {
    //    private StatementChangeListener m_StatementChangeListener;
    private EditText m_EditText;
    private EditText m_MultilineEditText;
    private STATEMENT_ELEMENT m_StatementElement = STATEMENT_ELEMENT.NONE;
    private StatementData m_EditingData;

    static private final String TAG = "EditStatementView";


    public EditStatementView(Context context, AttributeSet attrs) {
        super(context, attrs);


    }

//    public interface StatementChangeListener {
//        void onStatementTextChange(String text, StatementData.STATEMENT_ELEMENT type);
//    }

    @Override
    void setToStatement(StatementData statement) {
        super.setToStatement(statement);
        m_EditingData = statement;
        m_ResultView.setVisibility(View.VISIBLE);
        m_ResultView.setRandomized(false);
        m_ResultView.setAnimationFraction(0);
        updateWithHints();


    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        View verbButton = getVerbButton();
        int buttonGap = getContext().getResources().getDimensionPixelOffset(com.brazedblue.waverly.R.dimen.statement_button_gap);
        int verbTextTop = verbButton.getBottom() + buttonGap;
        TextView verbText = getVerbText();
        verbText.setAlpha(1);

        verbText.layout(verbText.getLeft(), verbTextTop, verbText.getRight(), verbTextTop + textViewHeight(verbText));

    }

    protected int getVerbHeight() {
        Button button = (Button)findViewById(R.id.verbButton);
        int measuredHeight = textViewHeight(button);
        TextView verbText = (TextView)findViewById(R.id.textVerb);
        int measuredHeight2 = textViewHeight(verbText);
        return measuredHeight + measuredHeight2 +
                getContext().getResources().getDimensionPixelOffset(com.brazedblue.waverly.R.dimen.statement_button_gap);

    }




    void setToEditing(STATEMENT_ELEMENT element) {
        hideCurrentEditingView();
        m_StatementElement = element;
        showCurrentEditingView();
    }

    private TextView getTextViewForElement(STATEMENT_ELEMENT element) {
        TextView result = null;
        switch (m_StatementElement) {
            case NOUN:
                result = getNounText();
                break;
            case VERB_BUTTON:
                result = getVerbButton();
                break;
            case VERB_TEXT:
                result = getVerbText();
                break;
        }

        return result;
    }


    private void hideCurrentEditingView() {
        if (m_StatementElement != STATEMENT_ELEMENT.NONE) {
            CharSequence text = null;
            TextView targetText = getTextViewForElement(m_StatementElement);
            if (null != targetText) {
                text = m_EditText.getText();
                m_EditingData.setElementString(m_StatementElement, text.toString());
                updateWithHints(targetText, m_StatementElement);
                targetText.setVisibility(View.VISIBLE);
                m_EditText.setVisibility(View.INVISIBLE);
            } else if (STATEMENT_ELEMENT.RESULT_TEXT == m_StatementElement) {
                text = m_MultilineEditText.getText();
                m_EditingData.setElementString(m_StatementElement, text.toString());
                ResultView resultView = getResultView();
                updateWithHints(resultView, m_StatementElement);
                resultView.setVisibility(View.VISIBLE);
                ImageView pictureView = getPictureView();
                pictureView.setVisibility(View.VISIBLE);
                m_MultilineEditText.setVisibility(View.INVISIBLE);

            }

//            if (text != null && m_StatementChangeListener != null) {
//                m_StatementChangeListener.onStatementTextChange(text.toString(), m_StatementElement);
//            }
        }

    }

    private void updateWithHints(TextView view, STATEMENT_ELEMENT element)
    {
        if (m_EditingData.wasElementSet(element))
        {
            view.setTextAppearance(getContext(), com.brazedblue.waverly.R.style.NounTheme);

            String text = m_EditingData.getElementString(element);
            if (text.isEmpty() && element == STATEMENT_ELEMENT.VERB_BUTTON) {
                text = m_EditingData.getElementHintString(element);
            }

            view.setText(text);
        }
        else {

            view.setTextAppearance(getContext(), com.brazedblue.waverly.R.style.StatementHintTheme);
            view.setText(m_EditingData.getElementHintString(element));
        }
    }

    private void updateWithHints()
    {
        updateWithHints(m_NounText, STATEMENT_ELEMENT.NOUN);
        updateWithHints(m_VerbText, STATEMENT_ELEMENT.VERB_TEXT);
        updateWithHints(m_ResultView, STATEMENT_ELEMENT.RESULT_TEXT);
        updateWithHints(m_VerbButton, STATEMENT_ELEMENT.VERB_BUTTON);

    }

    private void showCurrentEditingView() {
        if (m_StatementElement == STATEMENT_ELEMENT.NONE) {
            m_StatementElement = STATEMENT_ELEMENT.NOUN;
        }
        boolean singleLineBugFix = Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;


        TextView targetText = getTextViewForElement(m_StatementElement);
        if (null != targetText) {
            String text = m_EditingData.getElementString(m_StatementElement);

            m_EditText.setText(text);
            m_EditText.setHint(m_EditingData.getElementHintString(m_StatementElement));

            if (singleLineBugFix) {
                m_EditText.setSingleLine(false);
            } else {
                m_EditText.setSingleLine(true);
            }
            m_EditText.setLines(1);
            m_EditText.setMaxLines(1);

            m_EditText.measure(getWidth(), targetText.getHeight());
            int targetHeight = Math.max(targetText.getHeight(), m_EditText.getMeasuredHeight());
            int top = Math.max(targetText.getBottom() - targetHeight, 0);
            m_EditText.layout(0, top, getWidth(), top + targetHeight);
            m_EditText.setGravity(Gravity.CENTER);

            m_EditText.setVisibility(View.VISIBLE);
            targetText.setVisibility(View.INVISIBLE);
            m_EditText.clearFocus();
            m_EditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(m_EditText, InputMethodManager.SHOW_IMPLICIT);

        } else if (STATEMENT_ELEMENT.RESULT_TEXT == m_StatementElement) {
            ResultView resultView = getResultView();
            String text = m_EditingData.getElementString(m_StatementElement);
            m_MultilineEditText.setVisibility(View.VISIBLE);
            m_MultilineEditText.setText(text);


            m_MultilineEditText.measure(getWidth(), resultView.getHeight());
            int targetHeight = Math.max(resultView.getHeight(), m_MultilineEditText.getMeasuredHeight());
            int top = resultView.getBottom() - targetHeight;
            m_MultilineEditText.layout(getLeft(), top, getLeft() + getWidth(), top + targetHeight);
            m_MultilineEditText.setMaxWidth(getWidth());

            int lines =  targetHeight / (int)Utils.getTextLineHeight(m_MultilineEditText) - 1;;

            m_MultilineEditText.setLines(lines);
            m_MultilineEditText.setMaxLines(lines);


            ImageView pictureView = getPictureView();
            pictureView.setVisibility(View.INVISIBLE);
            resultView.setVisibility(View.INVISIBLE);
            m_MultilineEditText.setHint(m_EditingData.getElementHintString(m_StatementElement));
            m_MultilineEditText.clearFocus();
            m_MultilineEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(m_MultilineEditText, InputMethodManager.SHOW_IMPLICIT);
        }

    }

    void setToEditingResultView() {
        setToEditing(STATEMENT_ELEMENT.RESULT_TEXT);
    }


    protected void setupSubViews() {
        super.setupSubViews();

        m_EditText = (EditText) findViewById(com.brazedblue.waverly.R.id.statementEdit);
        m_MultilineEditText = (EditText) findViewById(com.brazedblue.waverly.R.id.multilineEditText);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && !m_EditText.isShown() && !m_MultilineEditText.isShown()) {
            showCurrentEditingView();
        }

    }
}
