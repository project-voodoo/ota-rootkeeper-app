
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.otarootkeeper.R.id;
import org.projectvoodoo.otarootkeeper.backend.Device;
import org.projectvoodoo.otarootkeeper.backend.Device.FileSystems;
import org.projectvoodoo.otarootkeeper.backend.Utils;
import org.projectvoodoo.otarootkeeper.ui.StatusRow;

import android.app.Activity;
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

    Device device;

    private StatusRow superuserdRow;
    private StatusRow rootedRow;
    private StatusRow rootGrantedRow;
    private StatusRow fsSupportedRow;
    private StatusRow suProtectedRow;
    private Button protectButton;
    private Button backupButton;
    private Button restoreButton;
    private Button deleteBackupButton;
    private Button unrootButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "OnCreate");
        device = new Device(this);

        setContentView(R.layout.main);

        superuserdRow = (StatusRow) findViewById(id.superuser_app_installed);
        rootedRow = (StatusRow) findViewById(id.rooted);
        rootGrantedRow = (StatusRow) findViewById(id.root_granted);
        fsSupportedRow = (StatusRow) findViewById(id.fs_supported);
        suProtectedRow = (StatusRow) findViewById(id.su_protected);
        protectButton = (Button) findViewById(id.button_protect_root);
        protectButton.setOnClickListener(this);
        backupButton = (Button) findViewById(id.button_backup_root);
        backupButton.setOnClickListener(this);
        restoreButton = (Button) findViewById(id.button_restore_root);
        restoreButton.setOnClickListener(this);
        deleteBackupButton = (Button) findViewById(id.button_delete_backup);
        deleteBackupButton.setOnClickListener(this);
        unrootButton = (Button) findViewById(id.button_unroot);
        unrootButton.setOnClickListener(this);

        displayStatus();
    }

    private void displayStatus() {
        if (device.isSuperuserAppInstalled)
            superuserdRow.setAvailable(true);
        else
            superuserdRow.setAvailable(false, "market://details?id=com.noshufou.android.su");

        rootedRow.setAvailable(device.isRooted);

        rootGrantedRow.setAvailable(Utils.canGainSu(this));

        if (device.fs == FileSystems.EXTFS)
            fsSupportedRow.setAvailable(true);
        else
            fsSupportedRow.setAvailable(false);

        suProtectedRow.setAvailable(device.isSuProtected);

        unrootButton.setVisibility(View.GONE);

        if (device.isRooted && !device.isSuProtected) {
            if (device.fs == FileSystems.EXTFS) {
                protectButton.setVisibility(View.VISIBLE);
                backupButton.setVisibility(View.GONE);

            } else {
                protectButton.setVisibility(View.GONE);
                backupButton.setVisibility(View.VISIBLE);
            }
            restoreButton.setVisibility(View.GONE);
            deleteBackupButton.setVisibility(View.GONE);

        } else if (device.isRooted && device.isSuProtected) {
            protectButton.setVisibility(View.GONE);
            backupButton.setVisibility(View.GONE);
            restoreButton.setVisibility(View.GONE);
            deleteBackupButton.setVisibility(View.VISIBLE);
            unrootButton.setVisibility(View.VISIBLE);

        } else if (!device.isRooted && device.isSuProtected) {
            protectButton.setVisibility(View.GONE);
            backupButton.setVisibility(View.GONE);
            restoreButton.setVisibility(View.VISIBLE);
            deleteBackupButton.setVisibility(View.GONE);
        }

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
                device.analyzeSu();
                displayStatus();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        String tag = v.getTag() + "";

        Log.d(TAG, "Button pressed tag: " + tag);

        if (tag.equals("protect") || tag.equals("backup")) {
            device.suOperations.backup();

        } else if (tag.equals("restore")) {
            device.suOperations.restore();

        } else if (tag.equals("delete_backup")) {
            device.suOperations.deleteBackup();

        } else if (tag.equals("unroot")) {
            device.suOperations.unroot();

        }

        device.analyzeSu();
        displayStatus();

    }
}
