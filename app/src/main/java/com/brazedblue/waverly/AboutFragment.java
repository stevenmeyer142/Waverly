package com.brazedblue.waverly;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 */
public class AboutFragment extends Fragment {
    private View m_MainView;
    private WebView m_Webview;
    private Button m_DebugButton;
    private View m_ClickText;
    private Calendar m_LastClick;
    private int     m_ClickCount;

    private static final String ABOUT_FILE = "about.html";
    private static final String TAG = "AboutFragment";

    public AboutFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AboutFragment.
     */
    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_MainView = inflater.inflate(com.brazedblue.waverly.R.layout.fragment_about, container, false);

        m_DebugButton = (Button)m_MainView.findViewById(com.brazedblue.waverly.R.id.debugButton);
        m_DebugButton.setVisibility(View.INVISIBLE);

        m_Webview = (WebView)m_MainView.findViewById(com.brazedblue.waverly.R.id.webView);

        AssetManager assets = getResources().getAssets();

        try {
            InputStream aboutIn = assets.open(ABOUT_FILE);
            byte[] bytes = new byte[aboutIn.available()];
            int bytesRead = aboutIn.read(bytes);
            String htmlStr = new String(bytes, 0, bytesRead, "UTF8");
            m_Webview.loadData(htmlStr, "text/html", null);

        }
        catch (IOException e)
        {
            CustomLog.e(TAG, ABOUT_FILE, e);
        }

        m_ClickText = m_MainView.findViewById(com.brazedblue.waverly.R.id.clickText);
        m_ClickText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_LastClick == null)
                {
                    m_LastClick = Calendar.getInstance();
                    m_ClickCount = 1;
                }
                else
                {
                    m_LastClick.add(GregorianCalendar.SECOND, 1);
                    Calendar currentTime = Calendar.getInstance();
                    if (currentTime.compareTo(m_LastClick) <= 0)
                    {
                        m_LastClick = currentTime;
                        m_ClickCount++;

                        if (m_ClickCount > 2)
                        {
                            m_DebugButton.setVisibility(View.VISIBLE);
                            m_MainView.requestLayout();
                            m_DebugButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(view.getContext(), DebugLogActivity.class);
                                    startActivity(intent);
                                }

                            });
                            m_ClickText.setClickable(false);
                        }
                    }
                    else
                    {
                        m_ClickCount = 0;
                        m_LastClick = null;
                    }
                }
            }
        });


        return m_MainView;
    }


}
