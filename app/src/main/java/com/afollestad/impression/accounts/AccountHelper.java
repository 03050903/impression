package com.afollestad.impression.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.afollestad.impression.api.MediaEntry;
import com.afollestad.impression.api.MediaFolderEntry;
import com.afollestad.impression.media.MediaAdapter;

import java.util.List;
import java.util.Set;

import rx.Single;

public abstract class AccountHelper {
    protected Account mAccount;

    public AccountHelper(Account account) {
        mAccount = account;
    }

    public abstract Single<? extends Set<? extends MediaFolderEntry>> getMediaFolders(Context context, @MediaAdapter.SortMode int sortMode, @MediaAdapter.FileFilterMode int filter);

    public abstract Single<List<MediaEntry>> getEntries(Context context, final String albumPath, final boolean explorerMode, final @MediaAdapter.SortMode int sort, final @MediaAdapter.FileFilterMode int filter);

    public abstract Drawable getHeader(Context context);

    public abstract boolean supportsExplorerMode();
}
