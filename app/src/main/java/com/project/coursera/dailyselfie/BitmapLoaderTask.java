package com.project.coursera.dailyselfie;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

/**
 * Created by Ignacio on 28-04-2017.
 */

public class BitmapLoaderTask extends AsyncTask<Long, Void, Bitmap> {
    private final Context mContext;
    private final WeakReference<ImageView> mImageViewReference;
    private final WeakReference<RelativeLayout> mProgressIconReference;

    BitmapLoaderTask(Context context, ImageView imageView, RelativeLayout progressIcon) {
        mContext = context;
        // Use a WeakReference to ensure the ImageView can be garbage collected
        mImageViewReference = new WeakReference<>(imageView);
        mProgressIconReference = new WeakReference<>(progressIcon);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Long... params) {
        Bitmap bitmap = null;

        if (params.length > 0)
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(), params[0],
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null);

        return bitmap;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        final RelativeLayout progressIcon = mProgressIconReference.get();
        if (progressIcon != null) {
            progressIcon.setVisibility(View.GONE);
        }
        final ImageView imageView = mImageViewReference.get();
        if (imageView != null) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.color.colorAccent);
            }
        }
    }
}
