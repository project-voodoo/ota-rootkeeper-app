
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.otarootkeeper.R.id;
import org.projectvoodoo.otarootkeeper.backend.Device;
import org.projectvoodoo.otarootkeeper.backend.Device.FileSystem;
import org.projectvoodoo.otarootkeeper.backend.Utils;
import org.projectvoodoo.otarootkeeper.ui.StatusRow;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

    private static final String TAG = "Voodoo OTA RootKeeper MainActivity";

    private Device mDevice;

    private StatusRow mSuperuserRow;
    private StatusRow mRootedRow;
    private StatusRow mRootGrantedRow;
    private StatusRow mFsSupportedRow;
    private StatusRow mSuProtectedRow;
    private Button mBackupButton;
    private Button mRestoreButton;
    private Button mDeleteBackupButton;
    private Button mUnrootButton;

    private boolean canGainSu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Starting app");

        setContentView(R.layout.main);
        setTitle(getString(R.string.app_name) + " v" + getVersionName());

        mSuperuserRow = (StatusRow) findViewById(id.superuser_app_installed);
        mRootedRow = (StatusRow) findViewById(id.rooted);
        mRootGrantedRow = (StatusRow) findViewById(id.root_granted);
        mFsSupportedRow = (StatusRow) findViewById(id.fs_supported);
        mSuProtectedRow = (StatusRow) findViewById(id.su_protected);
        mBackupButton = (Button) findViewById(id.button_backup_root);
        mBackupButton.setOnClickListener(this);
        mRestoreButton = (Button) findViewById(id.button_restore_root);
        mRestoreButton.setOnClickListener(this);
        mDeleteBackupButton = (Button) findViewById(id.button_delete_backup);
        mDeleteBackupButton.setOnClickListener(this);
        mUnrootButton = (Button) findViewById(id.button_unroot);
        mUnrootButton.setOnClickListener(this);

        mBackupButton.setVisibility(View.GONE);
        mRestoreButton.setVisibility(View.GONE);
        mDeleteBackupButton.setVisibility(View.GONE);
        mUnrootButton.setVisibility(View.GONE);

        new UiSetup().execute();
    }

    private void showStatus() {
        if (mDevice.isSuperuserAppInstalled)
            mSuperuserRow.setAvailable(true);
        else
            mSuperuserRow.setAvailable(false, "market://details?id=com.noshufou.android.su");

        mRootedRow.setAvailable(mDevice.isRooted);

        mRootGrantedRow.setAvailable(canGainSu);

        if (mDevice.mFileSystem == FileSystem.EXTFS)
            mFsSupportedRow.setAvailable(true);
        else
            mFsSupportedRow.setAvailable(false);

        mSuProtectedRow.setAvailable(mDevice.isSuProtected);

        mBackupButton.setText(mDevice.mFileSystem == FileSystem.EXTFS ?
                R.string.protect_root : R.string.backup_root);

        mBackupButton.setVisibility(
                mDevice.isRooted
                        && !mDevice.isSuProtected ?
                        View.VISIBLE : View.GONE);

        mRestoreButton.setVisibility(
                !mDevice.isRooted
                        && mDevice.isSuProtected ?
                        View.VISIBLE : View.GONE);

        mDeleteBackupButton.setVisibility(
                mDevice.isSuProtected
                        && mDevice.isRooted ?
                        View.VISIBLE : View.GONE);

        mUnrootButton.setVisibility(
                mDevice.isSuProtected
                        && mDevice.isRooted ?
                        View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case id.refresh:
                mDevice.analyzeSu();
                showStatus();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.button_backup_root:
                mDevice.mSuOps.backup();
                break;

            case R.id.button_restore_root:
                mDevice.mSuOps.restore();
                break;

            case R.id.button_delete_backup:
                mDevice.mSuOps.deleteBackup();
                break;

            case R.id.button_unroot:
                mDevice.mSuOps.unRoot();
                break;

            default:
                break;
        }

        mDevice.analyzeSu();
        showStatus();
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
        }
        return null;
    }

    class UiSetup extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mDevice = new Device(getApplicationContext());
            canGainSu = Utils.canGainSu(getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            showStatus();
            super.onPostExecute(result);
        }
    }
}
