package com.brazedblue.waverly;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
//import android.util.CustomLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class StatementsProvider extends ContentProvider {
    private final UriMatcher m_UriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static final String URI_AUTHORITY = "com.brazedblue.waverly.StatementsProvider";
    private static final String TAG = "StatementsProvider";
    private static final String DISPLAY_COL = "_display_name";
    private static final String SIZE_COL = "_size";
    private static final String DISPLAY_NAME = "Waverly.wvly";
    private static final String ATTACHMENTS_DIR = "Attachments";

    @Override
    public boolean onCreate() {
        m_UriMatcher.addURI(URI_AUTHORITY, "*", 1);

        return true;
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        String result = "";
        switch (m_UriMatcher.match(uri)) {

            // If it returns 1 - then it matches the Uri defined in onCreate
            case 1:
                result = getContext().getString(com.brazedblue.waverly.R.string.pushme_mimetype);
                break;

            default:
                CustomLog.v(TAG, "Unsupported uri: getType'" + uri + "'.");
                break;
        }

        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor result = null;

        switch (m_UriMatcher.match(uri)) {

            // If it returns 1 - then it matches the Uri defined in onCreate
            case 1:
                ArrayList<String> columns = new ArrayList<>();
                ArrayList<String> values = new ArrayList<>();
                if (Arrays.binarySearch(projection, DISPLAY_COL) >= 0)
                {
                    columns.add(DISPLAY_COL);
                    values.add(DISPLAY_NAME);
                }
                if (Arrays.binarySearch(projection, SIZE_COL) >= 0)
                {
                    try
                    {
                        ParcelFileDescriptor descriptor = openDescriptorForURI(uri, "", null);
                        columns.add(SIZE_COL);
                        values.add(Long.toString(descriptor.getStatSize()));
                        descriptor.close();
                    }
                    catch (IOException e)
                    {
                        CustomLog.e(TAG, "query" + uri, e);
                    }
                }
                if (columns.size() > 0)
                {
                    MatrixCursor cursor = new MatrixCursor(columns.toArray(new String[columns.size()]));
                    cursor.addRow(values);
                    result = cursor;
                }
                break;
            default:
                CustomLog.v(TAG, "Unsupported uri: query'" + uri + "'.");
                break;
        }

        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {

        Object listener = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            listener = new OnCloseListener();
        }
        ParcelFileDescriptor fd = null;
        try
        {
            fd = openDescriptorForURI(uri, mode, listener);
        }
        catch (IOException e)
        {
            CustomLog.e(TAG, "openFile" + uri, e);
        }
        return  fd;
    }

    private ParcelFileDescriptor openDescriptorForURI(Uri uri, String mode, Object listener) throws FileNotFoundException, IOException
    {

        // Check incoming Uri against the matcher
        switch (m_UriMatcher.match(uri)) {

            // If it returns 1 - then it matches the Uri defined in onCreate
            case 1:
                String fileLocation = getProviderDirectory(getContext()) + File.separator
                        + uri.getLastPathSegment();

                // Create & return a ParcelFileDescriptor pointing to the file
                // Note: I don't care what mode they ask for - they're only getting
                // read only
                ParcelFileDescriptor pfd  = null;
                if (listener == null) {
                    pfd = ParcelFileDescriptor.open(new File(
                            fileLocation), ParcelFileDescriptor.MODE_READ_ONLY);
                }
                else
                {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    OnCloseListener closeListener = (OnCloseListener)listener;
                    File file = new File(fileLocation);
                    closeListener.setFile(file);
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY, handler,
                            closeListener);

                }
                return pfd;

            // Otherwise unrecognised Uri
            default:
                CustomLog.v(TAG, "Unsupported uri: '" + uri + "'.");
                throw new FileNotFoundException("Unsupported uri: "
                        + uri.toString());
        }

    }


    static File getProviderDirectory(Context context)
    {
        File dir = new File(context.getCacheDir(), ATTACHMENTS_DIR);
        if (!dir.exists())
        {
            if (!dir.mkdirs())
            {
                CustomLog.e(TAG, "getProviderDirectory could not make dirs" + dir);
            }
        }

        return dir;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    static class OnCloseListener implements ParcelFileDescriptor.OnCloseListener
    {
        File mFile;

        void setFile(File file)
        {
            mFile = file;
        }
        @Override
        public void onClose(IOException e) {
            if (mFile.exists())
            {
                mFile.delete();
            }
        }
    }
}
