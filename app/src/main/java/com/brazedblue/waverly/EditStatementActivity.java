package com.brazedblue.waverly;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.support.v4.app.FragmentActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.util.UUID;

import javax.xml.transform.TransformerException;

public class EditStatementActivity extends FragmentActivity implements ResultViewChoice.ResultViewChoiceListener
{
    private EditStatementView m_StatementView;
    private StatementsStorage m_Storage;
    private StatementData m_EditingStatement;
    private String m_CurrentPhotoPath;

    private static final String TAG = "EditStatementActivity";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final int ACTION_TAKE_PHOTO = 1;
    private static final int ACTION_SELECT_IMAGE = ACTION_TAKE_PHOTO + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.brazedblue.waverly.R.layout.activity_edit_statement);

        m_StatementView = (EditStatementView) findViewById(com.brazedblue.waverly.R.id.statementView);
 //       m_StatementView.setStatementChangeListener(this);
        m_Storage = StatementsStorage.getStatementsStorage(this);
        Intent intent = getIntent();
        if (null != intent) {
            String uuidString = intent.getStringExtra(StatementsStorage.EXTRA_UUID);
            m_EditingStatement = m_Storage.getStatementWithUUIDString(uuidString);
//            m_StatementView.setupSubViews();
            m_StatementView.setToStatement(m_EditingStatement);

            View view = m_StatementView.getNounText();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m_StatementView.setToEditing(StatementData.STATEMENT_ELEMENT.NOUN);
                }
            });

            view = m_StatementView.getVerbButton();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m_StatementView.setToEditing(StatementData.STATEMENT_ELEMENT.VERB_BUTTON);
                }
            });

            view = m_StatementView.getVerbText();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m_StatementView.setToEditing(StatementData.STATEMENT_ELEMENT.VERB_TEXT);
                }
            });

            ResultView resultView = m_StatementView.getResultView();
            resultView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setToResultViewEditing();
                }
            });

            Intent returnIntent = new Intent();
            returnIntent.putExtra(StatementsStorage.EXTRA_UUID,uuidString);
            setResult(Activity.RESULT_OK, returnIntent);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.brazedblue.waverly.R.menu.edit_statement_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == com.brazedblue.waverly.R.id.doneEditStatement)
        {
            finish();

            return true;
        }
        else if (item.getItemId() == android.R.id.home )
        {
            NavUtils.navigateUpFromSameTask(this);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause()
    {
        m_StatementView.setToEditing(StatementData.STATEMENT_ELEMENT.NONE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            m_Storage.writeStatementData(m_EditingStatement);
        }
        catch (IOException e)
        {
            CustomLog.e(TAG, "writeStatementData Exception - " + e.getLocalizedMessage());
        }
        catch (TransformerException e2)
        {
            CustomLog.e(TAG, "writeStatementData Exception - " + e2.getLocalizedMessage());

        }

        super.onStop();
    }

    protected void setToResultViewEditing() {
        DialogFragment dialog = new ResultViewChoice();
        dialog.show(getFragmentManager(), "ResultViewChoice");

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        m_CurrentPhotoPath = image.getAbsolutePath();
        return image;
    }



    public void editPicture() {
        // Create an image file name
         try {
            if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
                throw new IOException("No Camera App available");
            }
            File imageFile = createImageFile();

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            m_CurrentPhotoPath = imageFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.brazedblue.waverly",
                    imageFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            startActivityForResult(takePictureIntent, ACTION_TAKE_PHOTO);


        } catch (IOException e) {
            m_CurrentPhotoPath = null;
            myShowError(com.brazedblue.waverly.R.string.take_picture_error, e.getLocalizedMessage());
        }
        catch (java.lang.SecurityException e)
        {
            myShowError(R.string.fix_camera_permissions, null);
        }

    }

    public void editPhotoAlbum() {
        try {
            if (isIntentAvailable(this, Intent.ACTION_PICK)) {
                throw new IOException("Album not available");
            }
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            intent.setType("image/*");

            startActivityForResult(intent, ACTION_SELECT_IMAGE);

        } catch (IOException e) {
            CustomLog.e(TAG, "editPhotoAlbum", e);
            myShowError(com.brazedblue.waverly.R.string.album_picture_error, e.getLocalizedMessage());
        }
    }


    public void editText() {
        m_StatementView.setToEditingResultView();
    }


    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() <= 0;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case ACTION_TAKE_PHOTO:
                    handleCameraPhoto();
                    break;

                case ACTION_SELECT_IMAGE:
                    handleAlbumPhoto(intent);
                    break;

            } // switch

        } // if
    }

    private Bitmap getBitmapForPath(String path) {
        Bitmap result = null;
        try {
            Resources res = getResources();
        /* Get the size of the image */
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            DisplayMetrics metrics = Utils.getDisplayMetrics(this);


		/* Figure out which way needs to be reduced less */
            int scaleFactor = (int)Utils.scaleFactorToFit(photoW, photoH, metrics.widthPixels, metrics.heightPixels, false);

		/* Set bitmap options to scale the image decode target */
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
		/* Decode the JPEG file into a Bitmap */
            //return BitmapFactory.decodeFile(path, bmOptions);
            result = BitmapFactory.decodeFile(path);
        }
        catch (OutOfMemoryError e)
        {
            CustomLog.e(TAG, "getBitmapForPath out of memory ", e);
        }

        return result;
    }

    private void handleCameraPhoto() {

        if (m_CurrentPhotoPath != null) {

            try {
                FileInputStream inStream = new FileInputStream(m_CurrentPhotoPath);
                String fileName = UUID.randomUUID().toString() + "." + Utils.getStringSuffix(m_CurrentPhotoPath);
                m_EditingStatement.writePicture(inStream, fileName);
                m_StatementView.setToStatement(m_EditingStatement);

            }
            catch (IOException e)
            {
                CustomLog.e(TAG, "handleCameraPhoto err", e);
            }
        }
    }



    private void handleAlbumPhoto(Intent intent) {
        Uri selectedImageURI = intent.getData();
        try {
            ContentResolver contentResolver = getContentResolver();
             ParcelFileDescriptor parcelFileDescriptor =
                    contentResolver.openFileDescriptor(selectedImageURI, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
             if (null != bitmap) {
                m_EditingStatement.writePicture(bitmap);
                 m_StatementView.setToStatement(m_EditingStatement);
            } else {
                myShowError(com.brazedblue.waverly.R.string.album_picture_error, null);
            }
        } catch (IOException e) {
            myShowError(com.brazedblue.waverly.R.string.album_picture_error, e.getLocalizedMessage());

        }
        catch (OutOfMemoryError e)
        {
            CustomLog.e(TAG, "handleAlbumPhoto ", e);
        }
    }

    private File getAlbumDir() throws IOException {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ),
                    "Waverly"
            );

            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    String errorMessage = "failed to create directory";
                    CustomLog.d(TAG, errorMessage);
                    throw new IOException(errorMessage);
                }

            }

        } else {
            String errorMessage = "External storage is not mounted READ/WRITE.";
            CustomLog.d(TAG, errorMessage);
            throw new IOException(errorMessage);
        }

        return storageDir;
    }

    private void myShowError(int messageID, String extraMessage) {
        DialogFragment newFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        String message = getResources().getString(messageID);
        if (null != extraMessage) {
            message += "\n" + extraMessage;
        }
        args.putString(ErrorDialogFragment.ERROR_MESSAGE_ARGUMENT, message);
        newFragment.setArguments(args);
        newFragment.show(getFragmentManager(), "error");

    }

    static public class ErrorDialogFragment extends DialogFragment {
        private String m_Message;
        static final String ERROR_MESSAGE_ARGUMENT = "message_id";

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            m_Message = getArguments().getString(ERROR_MESSAGE_ARGUMENT);
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(m_Message);
            builder.setPositiveButton(com.brazedblue.waverly.R.string.ok, null);
            return builder.create();
        }
    }

//    public void onStatementTextChange(String text, StatementData.STATEMENT_ELEMENT type) {
//        switch (type) {
//            case VERB_TEXT:
//                m_EditingStatement.setVerbText(text);
//                break;
//
//            case NOUN:
//                m_EditingStatement.setNoun(text);
//                break;
//
//            case VERB_BUTTON:
//                m_EditingStatement.setVerbButtonText(text);
//                break;
//
//            case RESULT_TEXT:
//                m_EditingStatement.setResultText(text);
//                break;
//        }
//    }
}
