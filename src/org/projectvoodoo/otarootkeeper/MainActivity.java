
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.libsu.R.id;
import org.projectvoodoo.otarootkeeper.backend.Device;
import org.projectvoodoo.otarootkeeper.backend.Device.FileSystems;
import org.projectvoodoo.otarootkeeper.backend.Utils;
import org.projectvoodoo.otarootkeeper.ui.StatusRow;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
        restoreButton = (Button) findViewById(id.button_restore_root);
        restoreButton.setOnClickListener(this);
        deleteBackupButton = (Button) findViewById(id.button_delete_backup);
        deleteBackupButton.setOnClickListener(this);

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

        if (device.isSuProtected)
            deleteBackupButton.setVisibility(View.VISIBLE);
        else
            deleteBackupButton.setVisibility(View.INVISIBLE);

        protectButton.setVisibility(View.INVISIBLE);
        backupButton.setVisibility(View.INVISIBLE);
        restoreButton.setVisibility(View.INVISIBLE);

        if (device.isRooted && !device.isSuProtected) {
            if (device.fs == FileSystems.EXTFS) {
                protectButton.setVisibility(View.VISIBLE);
                backupButton.setVisibility(View.INVISIBLE);
            } else {
                protectButton.setVisibility(View.INVISIBLE);
                backupButton.setVisibility(View.VISIBLE);
            }

            restoreButton.setVisibility(View.INVISIBLE);
        }

        if (device.isSuProtected && !device.isRooted) {
            restoreButton.setVisibility(View.VISIBLE);
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
        }

        device.analyzeSu();
        displayStatus();

    }
}
