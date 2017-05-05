package com.project.coursera.dailyselfie;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Created by Ignacio on 28-04-2017.
 */

class GalleryItemCursor extends CursorWrapper {

    private static int CURSOR_INDEX_ID;
    private static int CURSOR_INDEX_PATH;

    GalleryItemCursor(Cursor cursor) {
        super(cursor);

        if (cursor != null) {
            CURSOR_INDEX_PATH = cursor.getColumnIndex(SelfieActivity.IMAGE_DATA);
            CURSOR_INDEX_ID = cursor.getColumnIndex(SelfieActivity.IMAGE_ID);
        }
    }

    String getImagePath() {
        return getWrappedCursor() == null ?
                null : ImageFileHelper.getFilePath(getWrappedCursor().getString(CURSOR_INDEX_PATH));
    }

    long getImageID() throws NumberFormatException {
        return getWrappedCursor() == null ?
                0 : Long.parseLong(getWrappedCursor().getString(CURSOR_INDEX_ID));
    }

    @Override
    public int getCount() {
        return getWrappedCursor() == null ?
                0 : getWrappedCursor().getCount();
    }
}
