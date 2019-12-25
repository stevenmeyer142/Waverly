package com.brazedblue.waverly;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class ResultView extends AppCompatTextView {

    final private ArrayList<StatementDrawable> m_Drawables = new ArrayList<>();
    Point m_TextLocation = new Point();
    private float m_AnimationFraction = 0;
    private int[] m_WordEnds = new int[0];
    private boolean m_Randomize = true;

    static private final String TAG = "ResultView";

    static private final String WORD_SPLIT_EXPRESSION = "\\s";

    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {

        super.setText(text, type);
        if (null != m_Drawables) {
            m_Drawables.clear();
        }
    }

    void setRandomized(boolean randomized)
    {
        m_Randomize = randomized;
    }

    void randomizeDrawables()
    {
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();
        Random random = new Random();
        for (int i = 0; i < m_Drawables.size(); i++)
        {
            Point randomCenter = new Point();
            StatementDrawable drawable = m_Drawables.get(i);
            int width = drawable.getWidth();
            int height = drawable.getHeight();
            randomCenter.x = (int)(width + (viewWidth - 2 * width) * random.nextFloat());
            randomCenter.y = (int)(height + (viewHeight - 2 * height)  * random.nextFloat() );
            drawable.setCenterStart(randomCenter);
            drawable.setRotationStart(720 * random.nextFloat());
            drawable.setAnimationFraction(0.1f);

        }
    }

    static class SplitString
    {
        final String mString;
        final Vector<String> mSplits = new Vector<>();

        SplitString(String string)
        {
            mString = string;
        }

        void addSplit(String str)
        {
            mSplits.add(str);
        }

        int wordCount()
        {
            return mSplits.size();
        }

        String getString()
        {
            return mString;
        }

        Vector<String> getSubStrings()
        {
            return mSplits;
        }
    }


    private void setUpDrawables()
    {
        Paint paint = new Paint(this.getPaint());
        String text = this.getText().toString();
        Vector<String> textStrings = new Vector<>();
        Vector<Integer> textWidthsVector = new Vector<>();

        String[] returnSplit = text.split("\\n");

        SplitString[] splitStrings = new SplitString[returnSplit.length];
        int wordEndCounts = 0;
        for (int i = 0; i < splitStrings.length; i++) {
            splitStrings[i] = new SplitString(returnSplit[i]);
            String[] initialSplit = returnSplit[i].split(WORD_SPLIT_EXPRESSION);
            for (String word : initialSplit) {
                if (!word.equals("")) {
                    splitStrings[i].addSplit(word);
                }

            }
            wordEndCounts += splitStrings[i].wordCount();

        }
        m_WordEnds = new int[wordEndCounts];
        int wordCount = 0;
        int previousChars = 0;
        for (SplitString split : splitStrings) {
            int start = 0;
            String string = split.getString();
            Vector<String> subStrings = split.getSubStrings();
            for (String sub :subStrings) {
                int wordIndex = string.indexOf(sub, start);
                if (wordIndex < 0) {
                    CustomLog.e(TAG, "Could not find String '" + string + "'");
                    wordIndex = start + 1;
                }
                start = wordIndex + sub.length();
                m_WordEnds[wordCount++] = wordIndex + previousChars + sub.length();

            }
            previousChars += string.length();

        }

        m_WordEnds[m_WordEnds.length - 1] = text.length();

        final int OUTLINE_WIDTH = (int)getResources().getDimension(R.dimen.resultBorder);
        int bitmapWidth = 1;
        previousChars = 0;
        for (SplitString split : splitStrings)  {
            String string = split.getString();
            int subStringChars;
            int stringChars = 0;
            String subString = new String(string);

            do
            {
                subStringChars = paint.breakText(subString, true, this.getWidth(), null);

                int lastWord = 0;
                if (subStringChars < subString.length())
                {
                    for (int i = 0; i < m_WordEnds.length; i++)
                    {
                        if (m_WordEnds[i] > previousChars + stringChars + subStringChars)
                        {
                            break;
                        }
                        lastWord = m_WordEnds[i] - previousChars - stringChars;
                    }

                    String thisLine = subString.substring(0, lastWord);
                    textStrings.addElement(thisLine);
                    subString = subString.substring(lastWord, subString.length());
                }
                else
                {
                    textStrings.addElement(subString);
                    lastWord = subString.length();
                }
                int lineWidth = (int)paint.measureText(textStrings.lastElement());
                bitmapWidth = Math.max(lineWidth + OUTLINE_WIDTH, bitmapWidth);
                textWidthsVector.add(lineWidth);
                stringChars += lastWord;
            } while (stringChars < string.length());
            previousChars += string.length();
        }

        int lineHeight = (int)paint.getFontSpacing() + OUTLINE_WIDTH;
        int bitmapHeight = lineHeight * textStrings.size();
        m_TextLocation.x = (this.getWidth() - bitmapWidth) / 2;
        m_TextLocation.y = (this.getHeight() - bitmapHeight) / 2;

        Bitmap mainBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas bCanvas = new Canvas(mainBitmap);
        FontMetrics fontMetrics = paint.getFontMetrics();
        m_Drawables.clear();
        for (int lineNum = 0; lineNum < textStrings.size(); lineNum++)
        {
            String lineText = textStrings.elementAt(lineNum);
            float yPos = (lineNum + 1 )* lineHeight - fontMetrics.descent - OUTLINE_WIDTH / 2.0f;
            Paint outlinePaint = new Paint(paint);
            outlinePaint.setColor(Color.rgb(0xEF, 0xEF, 0xEF));
            outlinePaint.setStrokeWidth(OUTLINE_WIDTH);
            outlinePaint.setStyle(Paint.Style.STROKE);

            bCanvas.drawText(lineText, 0, yPos, outlinePaint);
            bCanvas.drawText(lineText, 0, yPos, paint);

            float[] textWidths = new float[lineText.length()];
            paint.getTextWidths(lineText, textWidths);


            int xPos = 0;
            int xToPos = (this.getWidth() - textWidthsVector.elementAt(lineNum).intValue()) / 2;
            int yTop = lineNum * lineHeight;

            for (int charNum = 0; charNum < lineText.length(); charNum++)
            {
                if (textWidths[charNum] >0) {
                    Bitmap bitmap = Bitmap.createBitmap(mainBitmap, xPos, yTop, (int) textWidths[charNum], lineHeight, null, false);
                    StatementDrawable drawable = new StatementDrawable(bitmap);
                    Point center = new Point(xToPos + xPos + (int) textWidths[charNum] / 2, m_TextLocation.y + lineNum * lineHeight + lineHeight / 2);
                    drawable.setCenterStart(center);
                    drawable.setCenterEnd(center);
                    m_Drawables.add(drawable);

                    xPos += (int) textWidths[charNum];
                }
            }
        }
    }

    int getWordCount()
    {
        if (getWidth() > 0 && getHeight() > 0) {
            String text = this.getText().toString();

            if (m_Drawables.size() == 0 && text.length() > 0) {
                setUpDrawables();
                if (m_Randomize) {
                    randomizeDrawables();
                }
            }
        }
        return m_WordEnds.length;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        String text = this.getText().toString();
        if (m_Drawables.size() == 0 && text.length() > 0)
        {
            setUpDrawables();
            if (m_Randomize) {
                randomizeDrawables();
            }
        }

        updateDrawablesAnimationFraction(m_AnimationFraction);
        Paint paint = new Paint();

        for (int i = m_Drawables.size() - 1; i >= 0; --i)
        {
            m_Drawables.get(i).draw(canvas, paint);
        }
    }


     private void updateDrawablesAnimationFraction(float animationFraction)
    {
        int animationStart = (int)Math.floor(animationFraction);
        float remainderFraction = animationFraction;
        if (animationStart > 0)
        {
            remainderFraction = animationFraction - animationStart;
        }
        int wordEnd = m_Drawables.size();
        if (animationStart < m_WordEnds.length)
        {
            wordEnd = m_WordEnds[animationStart];
        }
        int wordStart = 0;
        if (animationStart > 0 && animationStart <= m_WordEnds.length)
        {
            wordStart = m_WordEnds[animationStart - 1];
        }

        for (int i = 0; i < m_Drawables.size(); i++)
        {
            StatementDrawable drawable = m_Drawables.get(i);
            if (i >= wordStart)
            {
                if (i >= wordEnd)
                {
                    drawable.setAnimationFraction(0);
                }
                else
                {
                    drawable.setAnimationFraction(remainderFraction);
                }
            }
            else
            {
                drawable.setAnimationFraction(1);
            }

        }

    }


    public void setAnimationFraction(float animationFraction) {
        this.m_AnimationFraction = animationFraction;
        this.invalidate();
      }

 }
