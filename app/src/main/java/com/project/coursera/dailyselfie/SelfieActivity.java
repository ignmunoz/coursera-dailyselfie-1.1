package com.project.coursera.dailyselfie;

import java.io.File;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SelfieActivity extends AppCompatActivity implements OnViewImageListener {

    // Elements in the Activity UI
    GalleryFragment mGalleryFragment;
    ImageViewerFragment mImageViewerFragment;

    static final String IMAGE_VIEWER_FRAGMENT_TAG = "com.project.coursera.dailyselfie.imageviewerfragment";
    static final String GALLERY_FRAGMENT_TAG = "com.project.coursera.dailyselfie.dailyselfie.galleryfragment";
    public static final int IMAGE_LOADER_ID = 0;
    public static final Uri IMAGE_STORE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String IMAGE_DATA = MediaStore.Images.Media.DATA;
    public static final String IMAGE_ID = MediaStore.Images.Media._ID;
    public static final String[] IMAGE_FILE_PROJECTION = { IMAGE_DATA, IMAGE_ID };
    public static final String IMAGE_SORT_ORDER = MediaStore.Images.Media.DATE_TAKEN + " DESC";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CURRENT_PHOTO_PATH_KEY = "mCurrentPhotoPath";
    private static final String CURRENT_PHOTO_URI_KEY = "mCurrentPhotoUri";
    public static final String EXTERNAL_ACTION_TAKE_SELFIE = "com.project.coursera.dailyselfie.ext_action_take_selfie";

    private String mCurrentPhotoPath = null;
    private Uri mCurrentPhotoUri;

    // Notification variables
    private AlarmManager mAlarmManager;
    Intent mNotificationReceiverIntent;
    PendingIntent mNotificationReceiverPendingIntent;
    private static final long INITIAL_ALARM_DELAY = 2 * 60 * 1000L; // = 2 minutes

    // For WRITE_EXTERNAL_STORAGE permission (API >= 23)
    private static final int REQUEST_FOR_STORAGE = 1111;

    //private static final String LOG_TAG = "LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (isStoragePermissionGranted()){
            initialiseFragments(savedInstanceState);
        }

        // Get the AlarmManager Service
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Create an Intent to broadcast to the AlarmNotificationReceiver
        mNotificationReceiverIntent = new Intent(SelfieActivity.this,
                SelfieNotificationReceiver.class);

        // Create an PendingIntent that holds the NotificationReceiverIntent
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(
                SelfieActivity.this, 0, mNotificationReceiverIntent, PendingIntent.FLAG_ONE_SHOT);

        // Get the intent that started this activity
        resolveCallingIntent();

    }

    // Check if we've been started with an action, and carry out the action if so
    private void resolveCallingIntent() {
        Intent intent = getIntent();
        if (intent == null || intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case EXTERNAL_ACTION_TAKE_SELFIE:
                dispatchTakePictureIntent();
                // Reset action now that we've handled it
                intent.setAction("android.intent.action.MAIN");
                break;
            default:
                break;
        }
    }

    // Set up the fragments to use in the main content view
    private void initialiseFragments(Bundle savedInstanceState) {
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Gallery Fragment to be placed in the activity layout
            mGalleryFragment = new GalleryFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            mGalleryFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mGalleryFragment, GALLERY_FRAGMENT_TAG).commitAllowingStateLoss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (mCurrentPhotoPath != null) {
            savedInstanceState.putString(CURRENT_PHOTO_PATH_KEY, mCurrentPhotoPath);
        }
        if (mCurrentPhotoUri != null) {
            savedInstanceState.putParcelable(CURRENT_PHOTO_URI_KEY, mCurrentPhotoUri);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(CURRENT_PHOTO_PATH_KEY)) {
            mCurrentPhotoPath = savedInstanceState.getString(CURRENT_PHOTO_PATH_KEY);
        }
        if (savedInstanceState.containsKey(CURRENT_PHOTO_URI_KEY)) {
            mCurrentPhotoUri = savedInstanceState.getParcelable(CURRENT_PHOTO_URI_KEY);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(item.getItemId() == R.id.action_camera) {
            dispatchTakePictureIntent();
        }
        return true;
    }

    @Override
    protected void onResume() {
        cancelNotificationAlarm();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        cancelNotificationAlarm();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if(isStoragePermissionGranted()) {
            setNotificationAlarm();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {
        ImageViewerFragment imageViewerFragment = (ImageViewerFragment) getSupportFragmentManager().
                findFragmentByTag(IMAGE_VIEWER_FRAGMENT_TAG);
        if (imageViewerFragment != null && imageViewerFragment.isVisible()) {
            mImageViewerFragment.refreshImage();
            updateGalleryImage(mCurrentPhotoUri);
        } else {
            updateGalleryImage(Uri.parse(mCurrentPhotoPath));
        }
      } else {
          ImageFileHelper.deleteFile(mCurrentPhotoUri);
      }
      super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_FOR_STORAGE :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initialiseFragments(null);
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = ImageFileHelper.createPhotoFile(this);

            // Continue only if we successfully created a file
            if (photoFile != null) {
                // Get the image Uri
                mCurrentPhotoUri = ImageFileHelper.getFileUri(this, photoFile);
                // Store photo path for use later when the camera intent returns
                mCurrentPhotoPath = ImageFileHelper.getFilePath(photoFile);

                // Ask a local camera to fill in the image for us
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                ImageFileHelper.grantURIPermissionsForIntent(this, takePictureIntent, mCurrentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Signal the cursor to update the gallery
    public void updateGalleryImage(Uri photoUri) {
        final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(photoUri);
        this.sendBroadcast(scanIntent);
    }

    @Override
    public void viewImage(String photoPath) {
        if (mImageViewerFragment == null) {
            mImageViewerFragment = new ImageViewerFragment();
        }
        Bundle args = new Bundle();
        args.putString(ImageViewerFragment.ARG_IMAGE_PATH, photoPath);
        mImageViewerFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mImageViewerFragment, IMAGE_VIEWER_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    private void setNotificationAlarm(){
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INITIAL_ALARM_DELAY,
                mNotificationReceiverPendingIntent);

        Log.i("Alarm", "Notification will send two minutes from now");
    }

    private void cancelNotificationAlarm() {
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(SelfieActivity.this, 0, mNotificationReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(mNotificationReceiverPendingIntent);

        Log.i("Alarm", "Notification has been cancel!");
    }

    boolean isStoragePermissionGranted(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(SelfieActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(SelfieActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessageOK("You need to allow access to save the selfies.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(SelfieActivity.this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_FOR_STORAGE);
                        }
                    });
                return false;
            } else {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FOR_STORAGE);
                return false;
            }
        } else {
            return true;
        }
    }

    private void showMessageOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(SelfieActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }
}