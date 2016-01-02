package com.afollestad.impression.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.afollestad.impression.R;
import com.afollestad.impression.api.IncludedFolder;
import com.afollestad.impression.api.LocalExplorerFolderEntry;
import com.afollestad.impression.api.LocalMediaEntry;
import com.afollestad.impression.api.LocalMediaFolderEntry;
import com.afollestad.impression.api.MediaEntry;
import com.afollestad.impression.api.VideoEntry;
import com.afollestad.impression.media.MediaAdapter;
import com.afollestad.impression.providers.ExcludedFolderProvider;
import com.afollestad.impression.providers.IncludedFolderProvider;
import com.afollestad.impression.utils.PrefUtils;
import com.afollestad.impression.utils.Utils;
import com.afollestad.inquiry.Inquiry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class LocalHelper extends AccountHelper {
    private static final String TAG = "LocalAccount";

    public LocalHelper(Account account) {
        super(account);
    }

    public static Account newInstance(Context context) {
        return new Account(context, context.getString(R.string.local), Account.TYPE_LOCAL);
    }

    @Override
    public boolean supportsExplorerMode() {
        return true;
    }

    @Override
    public Single<Set<LocalMediaFolderEntry>> getMediaFolders(final Context context, @MediaAdapter.SortMode final int sortMode, @MediaAdapter.FileFilterMode int filter) {
        final List<Uri> uris = new ArrayList<>();
        if (filter == MediaAdapter.FILTER_PHOTOS || filter == MediaAdapter.FILTER_ALL) {
            uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        if (filter == MediaAdapter.FILTER_VIDEOS || filter == MediaAdapter.FILTER_ALL) {
            uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        }

        return Single.create(new Single.OnSubscribe<Set<LocalMediaFolderEntry>>() {
            @Override
            public void call(SingleSubscriber<? super Set<LocalMediaFolderEntry>> singleSubscriber) {
                Set<LocalMediaFolderEntry> folders = new HashSet<>();
                for (Uri uri : uris) {
                    //WHERE (1) GROUP BY (bucket_id),(bucket_display_name)
                    String bucketGroupBy = "1) GROUP BY (bucket_id),(bucket_display_name";
                    String bucketOrderBy = LocalMediaFolderEntry.getSortQueryForThumb(sortMode);
                    LocalMediaFolderEntry[] albums = Inquiry.get()
                            .selectFrom(uri, LocalMediaFolderEntry.class)
                            .where(bucketGroupBy)
                            .sort(bucketOrderBy)
                            .all();
                    if (albums != null) {
                        Collections.addAll(folders, albums);
                    }
                    Log.e(TAG, Arrays.toString(albums));
                }

                for (Iterator<LocalMediaFolderEntry> iterator = folders.iterator(); iterator.hasNext(); ) {
                    LocalMediaFolderEntry entry = iterator.next();
                    if (ExcludedFolderProvider.contains(context, entry.getData())) {
                        iterator.remove();
                    }
                }

                singleSubscriber.onSuccess(folders);
            }
        }).subscribeOn(Schedulers.io());
    }

    //TODO
    public Single<List<LocalMediaFolderEntry>> getIncludedFolders(final Context context, final @MediaAdapter.FileFilterMode int filter) {
        return Single.create(new Single.OnSubscribe<List<LocalMediaFolderEntry>>() {
            @Override
            public void call(SingleSubscriber<? super List<LocalMediaFolderEntry>> singleSubscriber) {
                IncludedFolder[] albums = Inquiry.get()
                        .selectFrom(IncludedFolderProvider.CONTENT_URI, IncludedFolder.class)
                        .all();

                if (albums == null) {
                    singleSubscriber.onError(new Exception("Included folders retrieval error"));
                    return;
                }

                final List<LocalMediaFolderEntry> allMediaFolders = new ArrayList<>();
                for (IncludedFolder folder : albums) {
                    getMediaFoldersInFolder(context, new File(folder.path), filter).subscribe(new Action1<List<LocalMediaFolderEntry>>() {
                        @Override
                        public void call(List<LocalMediaFolderEntry> mediaFolders) {
                            allMediaFolders.addAll(mediaFolders);
                        }
                    });
                }
                singleSubscriber.onSuccess(allMediaFolders);
            }
        }).subscribeOn(Schedulers.io());
    }

    private Single<List<LocalMediaFolderEntry>> getMediaFoldersInFolder(final Context context, final File root, final @MediaAdapter.FileFilterMode int filter) {
        return Single.create(new Single.OnSubscribe<List<LocalMediaFolderEntry>>() {
            @Override
            public void call(SingleSubscriber<? super List<LocalMediaFolderEntry>> singleSubscriber) {
                final List<LocalMediaFolderEntry> mediaFolders = new ArrayList<>();

                final List<File> mediaFoldersFiles = new ArrayList<>();

                File[] files = root.listFiles();
                for (File child : files) {
                    if (!child.isDirectory()) {
                        String mime = Utils.getMimeType(Utils.getExtension(child.getName()));
                        if (mime != null) {
                            if (mime.startsWith("image/") && filter != MediaAdapter.FILTER_VIDEOS) {
                                mediaFoldersFiles.add(root);
                            } else if (mime.startsWith("video/") && filter != MediaAdapter.FILTER_PHOTOS) {
                                mediaFoldersFiles.add(root);
                            }
                        }
                    } else if (PrefUtils.isSubfoldersIncluded(context)) {
                        getMediaFoldersInFolder(context, child, filter).subscribe(new Action1<List<LocalMediaFolderEntry>>() {
                            @Override
                            public void call(List<LocalMediaFolderEntry> childMediaFolders) {
                                mediaFolders.addAll(childMediaFolders);
                            }
                        });
                    }
                }

                singleSubscriber.onSuccess(mediaFolders);
            }
        });

    }

    private Single<List<LocalMediaFolderEntry>> getOverviewFolders(Context context, @MediaAdapter.SortMode int sort, final @MediaAdapter.FileFilterMode int filter) {
        final List<LocalMediaFolderEntry> finalEntries = new ArrayList<>();
        return getMediaFolders(context, sort, filter)
                .doOnSuccess(new Action1<Set<LocalMediaFolderEntry>>() {
                    @Override
                    public void call(Set<LocalMediaFolderEntry> mediaFolders) {
                        finalEntries.addAll(mediaFolders);
                    }
                })/*.flatMap(new Func1<Set<MediaFolder>, Single<? extends List<MediaFolder>>>() {
                    @Override
                    public Single<? extends List<MediaFolder>> call(Set<MediaFolder> mediaFolders) {
                        return getIncludedFolders(filter);
                    }
                })*/.map(new Func1<Set<LocalMediaFolderEntry>, List<LocalMediaFolderEntry>>() {
                    @Override
                    public List<LocalMediaFolderEntry> call(Set<LocalMediaFolderEntry> mediaFolders) {
                        return finalEntries;
                    }
                });
    }

    /**
     * @param sort Used only for thumbnails of overviews.
     */
    @Override
    public Single<List<MediaEntry>> getEntries(Context context,
                                               final String albumPath, final boolean explorerMode,
                                               final @MediaAdapter.SortMode int sort,
                                               final @MediaAdapter.FileFilterMode int filter) {
        if (explorerMode) {
            return getExplorerModeEntries(context, albumPath, sort, filter);
        } else {
            if ((albumPath == null || albumPath.equals(LocalMediaFolderEntry.OVERVIEW_PATH))) {/*
                    && overviewMode == 1*/
                return getOverviewFolders(context, sort, filter).map(new Func1<List<LocalMediaFolderEntry>, List<MediaEntry>>() {
                    @Override
                    public List<MediaEntry> call(List<LocalMediaFolderEntry> folderEntries) {
                        return new ArrayList<MediaEntry>(folderEntries);
                    }
                });
            }

            return Single.create(new Single.OnSubscribe<List<MediaEntry>>() {
                @Override
                public void call(SingleSubscriber<? super List<MediaEntry>> singleSubscriber) {
                    List<MediaEntry> mediaEntries = new ArrayList<>();

                    final String bucketName = new File(albumPath).getName();

                    List<Class<? extends MediaEntry>> entryClasses = new ArrayList<>();
                    final List<Uri> uris = new ArrayList<>();
                    List<String> selections = new ArrayList<>();
                    List<String[]> selectionArgs = new ArrayList<>();

                    if (filter == MediaAdapter.FILTER_PHOTOS || filter == MediaAdapter.FILTER_ALL) {
                        entryClasses.add(LocalMediaEntry.class);
                        uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        selections.add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?");
                        selectionArgs.add(new String[]{bucketName});
                    }
                    if (filter == MediaAdapter.FILTER_VIDEOS || filter == MediaAdapter.FILTER_ALL) {
                        entryClasses.add(VideoEntry.class);
                        uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        selections.add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " = ?");
                        selectionArgs.add(new String[]{bucketName});
                    }

                    for (int i = 0; i < entryClasses.size(); i++) {
                        Class<? extends MediaEntry> entryClass = entryClasses.get(i);
                        MediaEntry[] entries = Inquiry.get()
                                .selectFrom(uris.get(i), entryClass)
                                .where(selections.get(i), selectionArgs.get(i))
                                .all();
                        if (entries != null) {
                            Collections.addAll(mediaEntries, entries);
                        }
                    }
                    singleSubscriber.onSuccess(mediaEntries);
                }
            }).subscribeOn(Schedulers.io());
        }
    }

    @Override
    public Drawable getHeader(Context context) {
        //TODO: Change to real header
        return ContextCompat.getDrawable(context, R.drawable.temp_header);
    }

    private Single<List<MediaEntry>> getExplorerModeEntries(Context context, final String albumPath, @MediaAdapter.SortMode int sort, @MediaAdapter.FileFilterMode int filter) {
        return getEntries(context, albumPath, false, sort, filter)
                .flatMap(new Func1<List<MediaEntry>, Single<List<MediaEntry>>>() {
                    @Override
                    public Single<List<MediaEntry>> call(final List<MediaEntry> entries) {
                        return Single.create(new Single.OnSubscribe<List<MediaEntry>>() {
                            @Override
                            public void call(SingleSubscriber<? super List<MediaEntry>> singleSubscriber) {

                                File file = new File(albumPath);

                                if (!file.exists()) {
                                    singleSubscriber.onError(new Exception("This directory (" + file.getAbsolutePath() + ") no longer exists."));
                                    return;
                                }

                                for (File fi : file.listFiles()) {
                                    if (!fi.isDirectory()) {
                                        continue;
                                    }
                                    LocalExplorerFolderEntry explorerFolderEntry = new LocalExplorerFolderEntry(fi);
                                    entries.add(explorerFolderEntry);
                                }

                                singleSubscriber.onSuccess(entries);
                            }
                        });
                    }
                }).subscribeOn(Schedulers.io());
    }


}