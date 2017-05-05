package com.project.coursera.dailyselfie;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Ignacio on 28-04-2017.
 */

public class ImageViewerFragment extends Fragment implements ActionMode.Callback {

    public static final String ARG_IMAGE_PATH = "com.project.coursera.dailyselfie.imageviewer.photopath";
    AppCompatImageView mImageView;
    private String mImagePath;
    private ActionMode mActionMode;

    public ImageViewerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_IMAGE_PATH))
            mImagePath = getArguments().getString(ARG_IMAGE_PATH);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fullscreen_selfie, container, false);

        mImageView = (AppCompatImageView) view.findViewById(R.id.image_viewer_view);
        displayImage();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionMode == null) {
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
        }
        //((SelfieActivity) getActivity()).hideCamera();
    }

    // Public function to allow the activity to force a redraw of the image
    public void refreshImage() {
        displayImage();
    }

    // Post runnable to display image so that the view is measured before we work out the scale factor
    private void displayImage() {
        if (mImageView == null)
            return;

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                // Get the dimensions of the View
                int targetW = mImageView.getWidth();
                int targetH = mImageView.getHeight();

                Bitmap bitmap = null;
                int scaleFactor = ImageFileHelper.calculateBitmapScaleFactor(getContext(), mImagePath, targetW, targetH);

                if (scaleFactor > 0)
                    bitmap = ImageFileHelper.scaleBitmap(getContext(), mImagePath, scaleFactor);

                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }
            }
        });
    }

    /*// Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.image_viewer_context_menu, menu);
        return true;
    }*/

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.view_image_action_title);
        return false; // Return false if nothing is done
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    // Called when the user exits the action mode
    // In this case, the fragment is an action mode in itself, so on exiting the action mode we exit the fragment
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        FragmentManager fragMan = getActivity().getSupportFragmentManager();
        if (fragMan.findFragmentByTag(SelfieActivity.IMAGE_VIEWER_FRAGMENT_TAG) != null) {
            fragMan.popBackStack();
        }
    }
}
