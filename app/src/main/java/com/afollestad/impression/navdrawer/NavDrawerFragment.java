package com.afollestad.impression.navdrawer;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.impression.R;
import com.afollestad.impression.accounts.Account;
import com.afollestad.impression.accounts.AccountDbUtil;
import com.afollestad.impression.accounts.PicasaHelper;
import com.afollestad.impression.api.LocalMediaFolderEntry;
import com.afollestad.impression.api.MediaFolderEntry;
import com.afollestad.impression.base.ThemedActivity;
import com.afollestad.impression.media.MainActivity;
import com.afollestad.impression.media.MediaAdapter;
import com.afollestad.impression.providers.ExcludedFolderProvider;
import com.afollestad.impression.utils.PrefUtils;
import com.afollestad.impression.utils.Utils;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.picasaclient.PicasaClient;

import java.util.List;
import java.util.Set;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

public class NavDrawerFragment extends Fragment implements NavDrawerAdapter.Callback {

    private RecyclerView mRecyclerView;

    private NavDrawerAdapter mAdapter;

    public void notifyClosed() {
        mAdapter.setShowingAccounts(false);
    }

    public void reloadAlbums() {
        if (mAdapter.getCurrentAccount() == null) {
            reload(null);
        } else {
            loadMediaFolders(mAdapter.getCurrentAccount());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        PicasaClient.get().attachFragment(getActivity(), this);
    }

    private void showAddAccountDialog() {
        if (getActivity() == null) {
            return;
        }
        new MaterialDialog.Builder(getActivity())
                .title(R.string.add_account)
                .items(R.array.account_options)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
                        if (i == 0) {
                            //Google Photos
                            PicasaClient.get().pickAccount();
                        } else {
                            // TODO - Other web accounts
                            Toast.makeText(getActivity(), "Not implemented", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).build().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        PicasaClient.get().onActivityResult(requestCode, resultCode, data)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        android.accounts.Account picasaAccount = PicasaClient.get().getAccount();
                        Account account = PicasaHelper.newInstance(getActivity(), picasaAccount.name);

                        AccountDbUtil.addAccount(account);
                        PrefUtils.setCurrentAccountId(getActivity(), account.getId());

                        mAdapter.addAccount(account);
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navdrawer, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        animator.setSupportsChangeAnimations(false);
        mRecyclerView.setItemAnimator(animator);

        mAdapter = new NavDrawerAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ThemedActivity act = (ThemedActivity) getActivity();

        if (ContextCompat.checkSelfPermission(act, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 69);
            return;
        }
        reload(savedInstanceState);
    }

    public void setTopInsets(int insetsTop) {
        mAdapter.setInsetsTop(insetsTop);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        PicasaClient.get().detach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mAdapter.saveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    public void reload(final Bundle instanceState) {
        if (getActivity() == null) {
            return;
        }

        if (instanceState != null) {
            mAdapter.restoreInstanceState(instanceState);
        }

        AccountDbUtil.getCurrentAccount(getActivity())
                .doOnSuccess(new Action1<Account>() {
                    @Override
                    public void call(Account account) {
                        mAdapter.setCurrentAccountId(account.getId());
                        loadMediaFolders(account);
                    }
                })
                .flatMap(new Func1<Account, Single<List<Account>>>() {
                    @Override
                    public Single<List<Account>> call(Account account) {
                        return AccountDbUtil.getAllAccounts();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Account>>() {
                    @Override
                    public void call(List<Account> accounts) {
                        mAdapter.setAccounts(accounts);
                    }
                });

    }

    public void loadMediaFolders(final Account account) {
        mAdapter.clear();
        mAdapter.add(new NavDrawerAdapter.Entry(LocalMediaFolderEntry.OVERVIEW_PATH, NavDrawerAdapter.OVERVIEW_ID));

        account.getMediaFolders(getActivity(), MediaAdapter.SORT_NAME_DESC, MediaAdapter.FILTER_ALL)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<Set<? extends MediaFolderEntry>>() {
                    @Override
                    public void onSuccess(Set<? extends MediaFolderEntry> folderEntries) {
                        for (MediaFolderEntry f : folderEntries) {
                            mAdapter.add(new NavDrawerAdapter.Entry(f.getData(), f.getId()));
                        }
                        /*if (account.supportsIncludedFolders()) {
                            loadIncludedFolders();
                        } else {*/
                            mAdapter.notifyDataSetChangedAndSort();
                        /*}*/
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (getActivity() == null) {
                            return;
                        }
                        Utils.showErrorDialog(getActivity(), error);
                    }
                });
    }

    private void loadIncludedFolders() {
        //TODO
        /*mCurrentAccount.getIncludedFolders(preAlbums, new Account.AlbumCallback() {
            @Override
            public void onAlbums(OldAlbumEntry[] albums) {
                for (OldAlbumEntry a : albums)
                    mAdapter.update(new NavDrawerAdapter.Entry(a.data(), false, true));
                mAdapter.add(new NavDrawerAdapter.Entry("", true, false));
                mAdapter.notifyDataSetChangedAndSort();
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                Utils.showErrorDialog(getActivity(), e);
            }
        });*/


        //mAdapter.add(new NavDrawerAdapter.Entry("", true, false));
        mAdapter.notifyDataSetChangedAndSort();
    }

    @Override
    public void onEntrySelected(final int index, final NavDrawerAdapter.Entry entry, boolean longClick) {
        /*if (entry.isAdd()) {
            new FolderChooserDialog.Builder((MainActivity) getActivity())
                    .chooseButton(R.string.choose)
                    .show();
        } else */
        if (longClick) {
            /*if (entry.isIncluded()) {
                new MaterialDialog.Builder(getActivity())
                        .content(Html.fromHtml(getString(R.string.confirm_folder_remove, entry.getPath())))
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog materialDialog) {
                                IncludedFolderProvider.removeMediaEntry(getActivity(), entry.getPath());
                                getMediaFolders(mCurrentAccount); // reload albums

                                if (getActivity() == null) {
                                    return;
                                }
                                MainActivity act = (MainActivity) getActivity();
                                *//*act.notifyFoldersChanged();*//*

                                if (entry.getId() == mAdapter.getSelectedId()) {
                                    if (mCurrentSelectedPosition > mAdapter.getItemCount() - 1) {
                                        mCurrentSelectedPosition = mAdapter.getItemCount() - 1;
                                    }
                                    if (mAdapter.get(mCurrentSelectedPosition).isAdd()) {
                                        mCurrentSelectedPosition--;
                                    }
                                    NavDrawerAdapter.Entry newPath = mAdapter.get(mCurrentSelectedPosition);
                                    act.navDrawerSwitchAlbum(newPath.getPath());
                                    mAdapter.setCheckedItemId(mCurrentSelectedPosition);
                                }
                            }
                        }).show();
            } else {*/
            new MaterialDialog.Builder(getActivity())
                    .content(Html.fromHtml(getString(R.string.confirm_exclude_album, entry.getPath())))
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction action) {

                            ExcludedFolderProvider.add(getActivity(), entry.getPath());
                            mAdapter.removeMediaEntry(index);

                            if (getActivity() == null) {
                                return;
                            }
                            MainActivity act = (MainActivity) getActivity();

                            NavDrawerAdapter.Entry newPath = mAdapter.getSelectedEntry();
                            act.navDrawerSwitchAlbum(newPath.getPath());
                        }
                    }).show();
            /*}*/
        } else {
            ((MainActivity) getActivity()).navDrawerSwitchAlbum(entry.getPath());
        }
    }


    @Override
    public void onAccountSelected(Account account) {


    }

    @Override
    public void onAddAccountPressed() {
        showAddAccountDialog();
    }

    @Override
    public void onSpecialItemPressed(long id) {
        if (id == NavDrawerAdapter.SETTINGS_ID) {
            ((MainActivity) getActivity()).openSettings();
        } else if (id == NavDrawerAdapter.ABOUT_ID) {
            ((MainActivity) getActivity()).showAboutDialog();
        }
    }
}