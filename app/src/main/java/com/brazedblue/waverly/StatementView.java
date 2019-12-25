package com.brazedblue.waverly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by stevemeyer on 3/31/15.
 */

public class StatementView extends ViewGroup
{
    protected ResultView m_ResultView;
    protected TextView m_NounText;
    protected TextView m_VerbText;
    protected ImageView m_PictureView;
    protected Button m_VerbButton;
    private Rect m_VerbButtonRect = new Rect();

    private SwipeListener m_SwipeListener;
    private GestureDetectorCompat m_GestureDetector;
    private TapListener m_TapListener;


    private static final String TAG = "StatementView";

    public StatementView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        m_GestureDetector = new GestureDetectorCompat(context, new MyGestureListener());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setupSubViews();
    }

    protected void setupSubViews()
    {
        m_NounText = (TextView) findViewById(com.brazedblue.waverly.R.id.textNoun);
        m_ResultView = (ResultView) findViewById(com.brazedblue.waverly.R.id.resultView);
        m_VerbText = (TextView) findViewById(com.brazedblue.waverly.R.id.textVerb);
        m_PictureView = (ImageView) findViewById(com.brazedblue.waverly.R.id.pictureView);
        m_VerbButton = (Button) findViewById(com.brazedblue.waverly.R.id.verbButton);
    }

    void setSwipeListener(SwipeListener l)
    {
        m_SwipeListener = l;
    }

        @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        int myWidth = r - l;
        int myHeight = b - t;
        TextView nounView = (TextView) findViewById(com.brazedblue.waverly.R.id.textNoun);
        int viewWidth = Math.max(nounView.getMeasuredWidth(), nounView.getMinWidth());
        int measuredHeight = textViewHeight(nounView);
        Resources resources = getContext().getResources();

        int top = resources.getDimensionPixelOffset(com.brazedblue.waverly.R.dimen.statement_top);
        int left = (myWidth - viewWidth) / 2;
        nounView.layout(left, top, left + viewWidth, top + measuredHeight);
        nounView.setGravity(Gravity.CENTER);
        top += measuredHeight;

        int verbHeight = getVerbHeight();
        Button button = (Button)findViewById(com.brazedblue.waverly.R.id.verbButton);
        button.setMinWidth(myWidth / 2);
        viewWidth = Math.max(button.getMeasuredWidth(), button.getMinWidth());
        top += resources.getDimensionPixelOffset(com.brazedblue.waverly.R.dimen.statement_button_gap);
        left = (myWidth - viewWidth) / 2;
        button.layout(left, top, left + viewWidth, top + textViewHeight(button));

        TextView verbText = (TextView)findViewById(com.brazedblue.waverly.R.id.textVerb);
        viewWidth = Math.max(verbText.getMeasuredWidth(), verbText.getMinWidth());;
        left = (myWidth - viewWidth) / 2;
        verbText.layout(left, top, left + viewWidth, top + textViewHeight(verbText));
        verbText.setGravity(Gravity.CENTER);

        top += verbHeight;
        top += resources.getDimensionPixelOffset(com.brazedblue.waverly.R.dimen.statement_result_gap);


        View pictureView = findViewById(com.brazedblue.waverly.R.id.pictureView);
        viewWidth = pictureView.getMeasuredWidth();
        measuredHeight = pictureView.getMeasuredHeight();

        int remainingHeight = myHeight - top;
        if (measuredHeight > remainingHeight && measuredHeight > 0)
        {
            viewWidth = (viewWidth * measuredHeight) / remainingHeight;
            measuredHeight = remainingHeight;
        }

        int pictureTop = top + (remainingHeight - measuredHeight) / 2;
        left = (myWidth - viewWidth) / 2;
        pictureView.layout(left, pictureTop, left + viewWidth, pictureTop + measuredHeight);

        View resultView = findViewById(com.brazedblue.waverly.R.id.resultView);

        int resultViewTop = top;
        left = 0;
        resultView.layout(left, resultViewTop, myWidth, myHeight);

    }

    protected int getVerbHeight()
    {
        Button button = (Button)findViewById(com.brazedblue.waverly.R.id.verbButton);
        int measuredHeight = textViewHeight(button);
        TextView verbText = (TextView)findViewById(com.brazedblue.waverly.R.id.textVerb);
        int measuredHeight2 = textViewHeight(verbText);
        return Math.max(measuredHeight, measuredHeight2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Find rightmost and bottom-most child
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {


                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            }
        }



        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    void setToStatement(StatementData statement)
    {
        setupSubViews();
        m_NounText.setText(statement.getNoun());
        m_VerbText.setText(statement.getVerbText());
        m_ResultView.setText(statement.getResult());
        String text = statement.getVerbButtonText();
        if (text.isEmpty())
        {
            text = getContext().getString(com.brazedblue.waverly.R.string.verb_button_blank);
        }
        m_VerbButton.setText(text);

        DisplayMetrics metrics = Utils.getDisplayMetrics(getContext());
        Bitmap bitmap = statement.getPicture(metrics);
        if (bitmap != null)
        {
            m_PictureView.setVisibility(View.VISIBLE);
            m_PictureView.setImageBitmap(bitmap);
        }
        else
        {
            m_PictureView.setVisibility(View.INVISIBLE);
        }
        setToStart();
    }

    void setToStart()
    {
        m_VerbButton.setAlpha(1);
        m_VerbText.setAlpha(0);
        m_ResultView.setVisibility(View.INVISIBLE);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if (m_SwipeListener == null)
        {
            return super.onInterceptTouchEvent(ev);
        }
        else {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                m_VerbButton.getHitRect(m_VerbButtonRect);
            }
            return !m_VerbButtonRect.contains((int) ev.getX(), (int) ev.getY());
        }
    }
    public boolean onTouchEvent(MotionEvent ev)
    {
        if (m_SwipeListener != null) {
            return m_GestureDetector.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    void setVerbButtonClickListener(OnClickListener l)
    {
        m_VerbButton.setOnClickListener(l);
    }

    ResultView getResultView()
    {
        return m_ResultView;
    }

    TextView getVerbButton()
    {
        return m_VerbButton;
    }

    TextView getVerbText()
    {
        return m_VerbText;
    }

    TextView getNounText()
    {
        return m_NounText;
    }

    ImageView getPictureView() { return m_PictureView; }


    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            float sensitivity = 50;
            if (Math.abs(e1.getX() - e2.getX()) > sensitivity) {

                m_SwipeListener.swiped(e1.getX() > e2.getX());
                return true;
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        public boolean onSingleTapUp(MotionEvent e)
        {
            //m_TapListener.handleTap();
            return m_TapListener != null && m_VerbButton.getAlpha() == 1;
        }

    }

    protected int textViewHeight(TextView textView)
    {
        int result = 0;
        String text = textView.getText().toString();
        if (text.isEmpty())
        {
            result = (int)Utils.getTextLineHeight(textView);
        }
        else
        {
            result = textView.getMeasuredHeight();
        }
        return result;
    }

    void setTapListener(TapListener listener)
    {
        m_TapListener = listener;
    }

    interface SwipeListener {
        void swiped(boolean left);
    }

    interface TapListener {
        void handleTap();
    }

 }
