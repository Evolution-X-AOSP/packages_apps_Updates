/*
 * Copyright (C) 2017-2023 The LineageOS Project
 * Copyright (C) 2019 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evolution.ota;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.evolution.ota.controller.UpdaterController;
import org.evolution.ota.controller.UpdaterService;
import org.evolution.ota.misc.Constants;
import org.evolution.ota.misc.StringGenerator;
import org.evolution.ota.misc.Utils;
import org.evolution.ota.model.UpdateInfo;
import org.evolution.ota.model.UpdateStatus;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;

public class UpdatesListAdapter extends RecyclerView.Adapter<UpdatesListAdapter.ViewHolder> {

    private static final String TAG = "UpdateListAdapter";

    private static final int BATTERY_PLUGGED_ANY = BatteryManager.BATTERY_PLUGGED_AC
            | BatteryManager.BATTERY_PLUGGED_USB
            | BatteryManager.BATTERY_PLUGGED_WIRELESS;

    private final float mAlphaDisabledValue;

    public static String changelogData;

    private List<String> mDownloadIds;
    private String mSelectedDownload;
    private UpdaterController mUpdaterController;
    private UpdatesListActivity mActivity;
    private UpdateInfo mSelectedUpdate;

    private AlertDialog infoDialog;

    UpdatesListAdapter(UpdatesListActivity activity) {
        mActivity = activity;

        TypedValue tv = new TypedValue();
        mActivity.getTheme().resolveAttribute(android.R.attr.disabledAlpha, tv, true);
        mAlphaDisabledValue = tv.getFloat();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.update_item_view, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (infoDialog != null) {
            infoDialog.dismiss();
        }
    }

    void setUpdaterController(UpdaterController updaterController) {
        mUpdaterController = updaterController;
        notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    private void handleActiveStatus(ViewHolder viewHolder, UpdateInfo update) {
        boolean canDelete = false;

        final String downloadId = update.getDownloadId();
        if (mUpdaterController.isDownloading(downloadId)) {
            canDelete = true;
            String downloaded = Utils.readableFileSize(update.getFile().length());
            String total = Utils.readableFileSize(update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            long eta = update.getEta();
            if (eta > 0) {
                CharSequence etaString = StringGenerator.formatETA(mActivity, eta * 1000);
                viewHolder.mProgressText.setText(mActivity.getString(
                        R.string.list_download_progress_eta_new, downloaded, total, etaString,
                        percentage));
            } else {
                viewHolder.mProgressText.setText(mActivity.getString(
                        R.string.list_download_progress_new, downloaded, total, percentage));
            }
            setButtonAction(viewHolder.mAction, Action.PAUSE, downloadId, true);
            viewHolder.mProgressBar.setIndeterminate(update.getStatus() == UpdateStatus.STARTING);
            viewHolder.mProgressBar.setProgress(update.getProgress());
        } else if (mUpdaterController.isInstallingUpdate(downloadId)) {
            setButtonAction(viewHolder.mAction, Action.CANCEL_INSTALLATION, downloadId, true);
            boolean notAB = !mUpdaterController.isInstallingABUpdate();
            viewHolder.mProgressText.setText(notAB ? R.string.dialog_prepare_zip_message :
                    update.getFinalizing() ?
                            R.string.finalizing_package :
                            R.string.preparing_ota_first_boot);
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getInstallProgress() / 100.f);
            viewHolder.mPercentage.setText(percentage);
            viewHolder.mProgressBar.setIndeterminate(false);
            viewHolder.mProgressBar.setProgress(update.getInstallProgress());
        } else if (mUpdaterController.isVerifyingUpdate(downloadId)) {
            setButtonAction(viewHolder.mAction, Action.INSTALL, downloadId, false);
            viewHolder.mProgressText.setText(R.string.list_verifying_update);
            viewHolder.mProgressBar.setIndeterminate(true);
        } else {
            canDelete = true;
            setButtonAction(viewHolder.mAction, Action.RESUME, downloadId, !isBusy());
            String downloaded = Utils.readableFileSize(update.getFile().length());
            String total = Utils.readableFileSize(update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            viewHolder.mPercentage.setText(percentage);
            viewHolder.mProgressText.setText(mActivity.getString(R.string.list_download_progress_new,
                    downloaded, total, percentage));
            viewHolder.mProgressBar.setIndeterminate(false);
            viewHolder.mProgressBar.setProgress(update.getProgress());
        }
        viewHolder.mWhatsNew.setText(changelogData != null ? changelogData : "");

        String fileSize = Utils.readableFileSize(update.getFileSize());
        viewHolder.mBuildSize.setText(String
                .format(mActivity.getResources().getString(R.string.update_size), fileSize));

        setupOptionMenuListeners(update, canDelete, viewHolder);
        viewHolder.mProgressBar.setVisibility(View.VISIBLE);
        viewHolder.mProgressText.setVisibility(View.VISIBLE);
        viewHolder.mBuildSize.setVisibility(View.VISIBLE);
    }

    private void handleNotActiveStatus(ViewHolder viewHolder, UpdateInfo update) {
        final String downloadId = update.getDownloadId();
        if (mUpdaterController.isWaitingForReboot(downloadId)) {
            setupOptionMenuListeners(update, false, viewHolder);
            setButtonAction(viewHolder.mAction, Action.REBOOT, downloadId, true);
        } else if (update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED) {
            setupOptionMenuListeners(update, true, viewHolder);
            setButtonAction(viewHolder.mAction,
                    Utils.canInstall(update) ? Action.INSTALL : Action.DELETE,
                    downloadId, !isBusy());
        } else if (!Utils.canInstall(update)) {
            setupOptionMenuListeners(update, false, viewHolder);
            setButtonAction(viewHolder.mAction, Action.INFO, downloadId, !isBusy());
        } else {
            setupOptionMenuListeners(update, false, viewHolder);
            setButtonAction(viewHolder.mAction, Action.DOWNLOAD, downloadId, !isBusy());
        }
        viewHolder.mWhatsNew.setText(changelogData != null ? changelogData : "");

        String fileSize = Utils.readableFileSize(update.getFileSize());
        viewHolder.mBuildSize.setText(String
                .format(mActivity.getResources().getString(R.string.update_size), fileSize));

        viewHolder.mProgressBar.setVisibility(View.INVISIBLE);
        viewHolder.mProgressText.setVisibility(View.INVISIBLE);
        viewHolder.mBuildSize.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        if (mDownloadIds == null) {
            viewHolder.mAction.setEnabled(false);
            return;
        }

        final String downloadId = mDownloadIds.get(i);
        UpdateInfo update = mUpdaterController.getUpdate(downloadId);
        if (update == null) {
            // The update was deleted
            viewHolder.mAction.setEnabled(false);
            viewHolder.mAction.setText(R.string.action_download);
            return;
        }

        viewHolder.itemView.setSelected(downloadId.equals(mSelectedDownload));

        boolean activeLayout;
        switch (update.getPersistentStatus()) {
            case UpdateStatus.Persistent.UNKNOWN:
                activeLayout = update.getStatus() == UpdateStatus.STARTING;
                break;
            case UpdateStatus.Persistent.VERIFIED:
                activeLayout = update.getStatus() == UpdateStatus.INSTALLING;
                break;
            case UpdateStatus.Persistent.INCOMPLETE:
                activeLayout = true;
                break;
            default:
                throw new RuntimeException("Unknown update status");
        }

        viewHolder.mWhatsNew.setText(changelogData != null ? changelogData : "");

        String buildDate = StringGenerator.getDateLocalizedUTC(mActivity,
                DateFormat.LONG, update.getTimestamp());
        String buildVersion = update.getName();
        viewHolder.mBuildDate.setText(buildDate);

        if (activeLayout) {
            handleActiveStatus(viewHolder, update);
        } else {
            handleNotActiveStatus(viewHolder, update);
        }
    }

    @Override
    public int getItemCount() {
        return mDownloadIds == null ? 0 : mDownloadIds.size();
    }

    public void setData(List<String> downloadIds) {
        mDownloadIds = downloadIds;
    }

    void notifyItemChanged(String downloadId) {
        if (mDownloadIds == null) {
            return;
        }
        notifyItemChanged(mDownloadIds.indexOf(downloadId));
    }

    void removeItem(String downloadId) {
        if (mDownloadIds == null) {
            return;
        }
        int position = mDownloadIds.indexOf(downloadId);
        mDownloadIds.remove(downloadId);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    private void startDownloadWithWarning(final String downloadId) {
        if (!Utils.isNetworkAvailable(mActivity)) {
            mUpdaterController.notifyNetworkUnavailable();
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean warn = preferences.getBoolean(Constants.PREF_METERED_NETWORK_WARNING, true);
        if (!(Utils.isNetworkMetered(mActivity) && warn)) {
            mUpdaterController.startDownload(downloadId);
            return;
        }

        View checkboxView = LayoutInflater.from(mActivity).inflate(R.layout.checkbox_view, null);
        CheckBox checkbox = checkboxView.findViewById(R.id.checkbox);
        checkbox.setText(R.string.checkbox_metered_network_warning);

        mActivity.findViewById(R.id.mobile_data_warning).setVisibility(View.VISIBLE);

        new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.update_over_metered_network_title)
                .setMessage(R.string.update_over_metered_network_message)
                .setView(checkboxView)
                .setPositiveButton(R.string.action_download,
                        (dialog, which) -> {
                            if (checkbox.isChecked()) {
                                preferences.edit()
                                        .putBoolean(Constants.PREF_METERED_NETWORK_WARNING, false)
                                        .apply();
                                mActivity.supportInvalidateOptionsMenu();
                            }
                            mUpdaterController.startDownload(downloadId);
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setButtonAction(Button button, Action action, final String downloadId,
                                 boolean enabled) {
        final View.OnClickListener clickListener;
        switch (action) {
            case DOWNLOAD:
                button.setText(R.string.action_download);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> startDownloadWithWarning(downloadId) : null;
                break;
            case PAUSE:
                button.setText(R.string.action_pause);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> mUpdaterController.pauseDownload(downloadId)
                        : null;
                break;
            case RESUME: {
                button.setText(R.string.action_resume);
                button.setEnabled(enabled);
                UpdateInfo update = mUpdaterController.getUpdate(downloadId);
                final boolean canInstall = Utils.canInstall(update) ||
                        update.getFile().length() == update.getFileSize();
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        mUpdaterController.resumeDownload(downloadId);
                    } else {
                        mActivity.showSnackbar(R.string.snack_update_not_installable,
                                Snackbar.LENGTH_LONG);
                    }
                } : null;
            }
            break;
            case INSTALL: {
                button.setText(R.string.action_install);
                button.setEnabled(enabled);
                UpdateInfo update = mUpdaterController.getUpdate(downloadId);
                final boolean canInstall = Utils.canInstall(update);
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        getInstallDialog(downloadId).show();
                    } else {
                        mActivity.showSnackbar(R.string.snack_update_not_installable,
                                Snackbar.LENGTH_LONG);
                    }
                } : null;
            }
            break;
            case INFO: {
                button.setText(R.string.details_button);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_updateui_new, 0, 0, 0);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> showInfoDialog() : null;
            }
            break;
            case DELETE: {
                button.setText(R.string.action_delete);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> getDeleteDialog(downloadId).show() : null;
            }
            break;
            case CANCEL_INSTALLATION: {
                button.setText(R.string.action_cancel);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> getCancelInstallationDialog().show() : null;
            }
            break;
            case REBOOT: {
                button.setText(R.string.reboot);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> {
                    PowerManager pm =
                            (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
                    pm.reboot(null);
                } : null;
            }
            break;
            default:
                clickListener = null;
        }
        button.setAlpha(enabled ? 1.f : mAlphaDisabledValue);

        // Disable action mode when a button is clicked
        button.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(v);
            }
        });
    }

    private boolean isBusy() {
        return mUpdaterController.hasActiveDownloads() || mUpdaterController.isVerifyingUpdate()
                || mUpdaterController.isInstallingUpdate();
    }

    private AlertDialog.Builder getDeleteDialog(final String downloadId) {
        return new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_dialog_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            mUpdaterController.pauseDownload(downloadId);
                            mUpdaterController.deleteUpdate(downloadId);
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void setupOptionMenuListeners(UpdateInfo update, final boolean canDelete, ViewHolder viewHolder){
        viewHolder.itemView.setOnLongClickListener(v -> {
            startActionMode(update, canDelete, viewHolder.mBuildDate);
            return true;
        });
    }

    private AlertDialog.Builder getInstallDialog(final String downloadId) {
        if (!isBatteryLevelOk()) {
            Resources resources = mActivity.getResources();
            String message = resources.getString(R.string.dialog_battery_low_message_pct,
                    resources.getInteger(R.integer.battery_ok_percentage_discharging),
                    resources.getInteger(R.integer.battery_ok_percentage_charging));
            return new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                    .setTitle(R.string.dialog_battery_low_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null);
        }
        UpdateInfo update = mUpdaterController.getUpdate(downloadId);
        int resId;
        String extraMessage = "";
        if (Utils.isABDevice()) {
            resId = R.string.apply_update_dialog_message_ab;
        } else {
            resId = R.string.apply_update_dialog_message;
            extraMessage = " (" + Constants.DOWNLOAD_PATH + ")";
        }

        return new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(mActivity.getString(resId, update.getName(),
                        mActivity.getString(android.R.string.ok)) + extraMessage)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            Utils.triggerUpdate(mActivity, downloadId);
                            maybeShowInfoDialog();
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    private AlertDialog.Builder getCancelInstallationDialog() {
        return new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                .setMessage(R.string.cancel_installation_dialog_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            Intent intent = new Intent(mActivity, UpdaterService.class);
                            intent.setAction(UpdaterService.ACTION_INSTALL_STOP);
                            mActivity.startService(intent);
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void maybeShowInfoDialog() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean alreadySeen = preferences.getBoolean(Constants.HAS_SEEN_INFO_DIALOG, false);
        if (alreadySeen) {
            return;
        }
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.info_dialog_title)
                .setMessage(R.string.info_dialog_message)
                .setPositiveButton(R.string.info_dialog_ok, (dialog, which) -> preferences.edit()
                        .putBoolean(Constants.HAS_SEEN_INFO_DIALOG, true)
                        .apply())
                .show();
    }

    private void startActionMode(final UpdateInfo update, final boolean canDelete, View anchor) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        String customURL = preferences.getString(Constants.PREF_CUSTOM_OTA_URL, Constants.OTA_URL);

        mSelectedDownload = update.getDownloadId();
        mSelectedUpdate = update;
        notifyItemChanged(update.getDownloadId());

        ContextThemeWrapper wrapper = new ContextThemeWrapper(mActivity,
                R.style.AppTheme_PopupMenuOverlapAnchor);
        PopupMenu popupMenu = new PopupMenu(wrapper, anchor, Gravity.NO_GRAVITY,
                R.attr.actionOverflowMenuStyle, 0);
        popupMenu.inflate(R.menu.menu_action_mode);

        boolean shouldShowDelete = canDelete;
        boolean isVerified = update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED;
        if (isVerified && !Utils.canInstall(update) && !update.getAvailableOnline()) {
            shouldShowDelete = false;
        }
        MenuBuilder menu = (MenuBuilder) popupMenu.getMenu();
        menu.findItem(R.id.menu_delete_action).setVisible(shouldShowDelete);
        menu.findItem(R.id.menu_copy_url).setVisible(update.getAvailableOnline());
        menu.findItem(R.id.menu_export_update).setVisible(isVerified);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_delete_action:
                    getDeleteDialog(update.getDownloadId()).show();
                    return true;
                case R.id.menu_copy_url:
                    Utils.addToClipboard(mActivity,
                            mActivity.getString(R.string.label_download_url),
                            Utils.getDownloadWebpageUrl(customURL, update.getName()),
                            mActivity.getString(R.string.toast_download_url_copied));
                    return true;
                case R.id.menu_export_update:
                    exportUpdate();
                    return true;
            }
            return false;
        });

        MenuPopupHelper helper = new MenuPopupHelper(wrapper, menu, anchor);
        helper.show();
    }

    private void exportUpdate() {
        File dest = new File(Utils.getExportPath(), mSelectedUpdate.getName());
        Intent intent = new Intent(mActivity, ExportUpdateService.class);
        intent.setAction(ExportUpdateService.ACTION_START_EXPORTING);
        intent.putExtra(ExportUpdateService.EXTRA_SOURCE_FILE, mSelectedUpdate.getFile());
        intent.putExtra(ExportUpdateService.EXTRA_DEST_FILE, dest);
        mActivity.startService(intent);
    }

    private void showInfoDialog() {
        if (infoDialog != null) {
            infoDialog.dismiss();
        }
        infoDialog = new AlertDialog.Builder(mActivity, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.blocked_update_dialog_title)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(R.string.blocked_update_dialog_summary)
                .show();
    }

    void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                requestCode == ExportUpdateService.EXPORT_STATUS_PERMISSION_REQUEST_CODE){
            exportUpdate();
        }
    }

    private boolean isBatteryLevelOk() {
        Intent intent = mActivity.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
            return true;
        }
        int percent = Math.round(100.f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int required = (plugged & BATTERY_PLUGGED_ANY) != 0 ?
                mActivity.getResources().getInteger(R.integer.battery_ok_percentage_charging) :
                mActivity.getResources().getInteger(R.integer.battery_ok_percentage_discharging);
        return percent >= required;
    }

    private enum Action {
        DOWNLOAD,
        PAUSE,
        RESUME,
        INSTALL,
        INFO,
        DELETE,
        CANCEL_INSTALLATION,
        REBOOT,
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private Button mAction;

        private TextView mBuildDate;
        private TextView mBuildSize;
        private TextView mWhatsNew;

        private ProgressBar mProgressBar;
        private TextView mProgressText;
        private TextView mPercentage;

        ViewHolder(final View view) {
            super(view);
            mAction = view.findViewById(R.id.update_action);

            mBuildDate = view.findViewById(R.id.build_date);
            mBuildSize = view.findViewById(R.id.build_size);
            mWhatsNew = view.findViewById(R.id.whats_new);

            mProgressBar = view.findViewById(R.id.progress_bar);
            mProgressText = view.findViewById(R.id.progress_text);
            mPercentage = view.findViewById(R.id.progress_percent);
        }
    }
}
