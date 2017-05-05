package com.project.coursera.dailyselfie;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static java.text.DateFormat.getDateTimeInstance;

/**
 * Created by Ignacio on 28-04-2017.
 */

class ImageFileHelper {
    private static final String SELFIE_FILE_PREFIX = "SELFIE";

    // Tries to create a file, and if successful
    // Returns null otherwise
    static File createPhotoFile(Context context) {
        File photoFile = null;
        try {
            photoFile = createImageFile(context);
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(context, R.string.error_create_file, Toast.LENGTH_LONG).show();
        }
        return photoFile;
    }

    // Creates a temporary file in public storage with a unique filename
    // NOTE: This requires permission WRITE_EXTERNAL_STORAGE
    static private File createImageFile(Context context) throws IOException {
        // Create an image file name
        getDateTimeInstance();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.UK).format(new Date());
        String imageFileName = SELFIE_FILE_PREFIX + "_" + timeStamp;
        File storageDir = getImageStorageDirectory(context);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    // Gets a file path for use with ACTION_VIEW intents
    static String getFilePath(File file) {
        return getFilePath(file.getAbsolutePath());
    }

    static String getFilePath(String absFilePath) {
        return "file:" + absFilePath;
    }

    // Returns a URI for it from the fileprovider
    static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context,
                "com.project.coursera.fileprovider",
                file);
    }

    // Get the directory to store our images in, and create it if it doesn't already exist
    static File getImageStorageDirectory(Context context) throws IOException, SecurityException  {
        if (!((SelfieActivity) context).isStoragePermissionGranted()) {
            throw new SecurityException("App cannot run without WRITE_EXTERNAL_STORAGE permission.");
        }
        File externalStorageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageDir = new File(externalStorageDir, context.getResources().getString(R.string.app_name));

        // Throw if the image directory doesn't already exist and cannot be created
        if (!imageDir.mkdir() && !imageDir.isDirectory())
            throw new IOException("Failed to create image storage directory: " + imageDir.getPath());

        return imageDir;
    }

    // Get the desired scaling factor for use with setting the bitmap in an image view
    static int calculateBitmapScaleFactor(Context context, String photoPath, int targetW, int targetH) {
        // Don't attempt to do anything if the view is one-dimensional
        if (targetW == 0 || targetH == 0)
            return 0;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        try {
            InputStream in = context.getContentResolver().openInputStream(
                    Uri.parse(photoPath));
            try {
                BitmapFactory.decodeStream(in, null, bmOptions);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            Toast.makeText(context, R.string.error_open_photo_file, Toast.LENGTH_LONG).show();
            return 0;
        }
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Default to full size image if the ratio is less than 1
        return scaleFactor > 0 ? scaleFactor : 1;
    }

    // Get the desired scaling factor for use with setting the bitmap in an image view
    static Bitmap scaleBitmap(Context context, String photoPath, int scaleFactor) {
        // Decode the image file into a Bitmap sized to fill the View
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap;
        try {
            InputStream in = context.getContentResolver().openInputStream(
                    Uri.parse(photoPath));
            try {
                bitmap = BitmapFactory.decodeStream(in, null, bmOptions);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            Toast.makeText(context, R.string.error_open_photo_file, Toast.LENGTH_LONG).show();
            return null;
        }
        return bitmap;
    }

    // Ensure that a particular intent has read and write access to a given URI
    static void grantURIPermissionsForIntent(Context context, Intent takePictureIntent, Uri photoURI) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName,
                    photoURI,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    // Delete the file at the given path, returning true if successfully deleted or file doesn't exist
    static boolean deleteFile(Uri photoUri) {
        File file = new File(photoUri.getPath());
        return !file.exists() || file.delete();
    }
}
