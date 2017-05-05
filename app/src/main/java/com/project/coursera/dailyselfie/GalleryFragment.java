package com.project.coursera.dailyselfie;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Ignacio on 28-04-2017.
 */

public class GalleryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback{

    static final String GALLERY_SELECTED_ITEMS_TAG = "com.project.coursera.dailyselfie.galleryselecteditems";

    RecyclerView mGalleryView;
    GalleryAdapter mGalleryAdapter;
    CursorLoader mGalleryItemLoader;
    private OnViewImageListener mViewImageCallback;

    public GalleryFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mViewImageCallback = (OnViewImageListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnViewImageListener and OnDeleteImageListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gallery_layout, container, false);

        mGalleryView = (RecyclerView) view.findViewById(R.id.gallery_view);

        mGalleryView.setLayoutManager(new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.grid_layout_items_per_row)));
        ArrayList<Integer> selectedItems = null;
        if (savedInstanceState != null && savedInstanceState.containsKey(GALLERY_SELECTED_ITEMS_TAG)) {
            selectedItems = savedInstanceState.getIntegerArrayList(GALLERY_SELECTED_ITEMS_TAG);
        }
        mGalleryAdapter = new GalleryAdapter(getContext(), mViewImageCallback, this, selectedItems);
        mGalleryView.setAdapter(mGalleryAdapter);
        mGalleryView.setItemAnimator(new DefaultItemAnimator());

        getLoaderManager().initLoader(SelfieActivity.IMAGE_LOADER_ID, null, this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (isInActionMode()) {
            savedInstanceState.putIntegerArrayList(GALLERY_SELECTED_ITEMS_TAG, mGalleryAdapter.getSelectedItems());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String imagesDirectory = "";
        try {
            imagesDirectory = ImageFileHelper.getImageStorageDirectory(getContext()).getAbsolutePath();
        } catch (IOException ex) {
            Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (!imagesDirectory.isEmpty()) {
            String selection = SelfieActivity.IMAGE_DATA + " LIKE '" + imagesDirectory + "%'";
            mGalleryItemLoader = new CursorLoader(getContext(),
                    SelfieActivity.IMAGE_STORE_URI,
                    SelfieActivity.IMAGE_FILE_PROJECTION,
                    selection,
                    null,
                    SelfieActivity.IMAGE_SORT_ORDER);
        }

        return mGalleryItemLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mGalleryAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGalleryAdapter.swapCursor(null);
    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        //((SelfieActivity) getActivity()).hideCamera();
        return false; // Return false if nothing is done
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
        /*switch (item.getItemId()) {
            case R.id.action_delete:
                deleteGalleryItems(mode);
                return true;

            case R.id.action_select_all:
                mGalleryAdapter.selectAll(mGalleryView);
                return true;

            default:
                return false;
        }*/
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mGalleryAdapter.mActionMode = null;
        mGalleryAdapter.clearSelections(mGalleryView);
    }

    // Check if action mode has been started on the gallery
    private boolean isInActionMode() {
        return mGalleryAdapter != null && mGalleryAdapter.mActionMode != null;
    }
}
