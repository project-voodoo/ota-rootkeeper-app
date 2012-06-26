
package org.projectvoodoo.otarootkeeper.backend;

import org.projectvoodoo.otarootkeeper.R.string;
import org.projectvoodoo.otarootkeeper.backend.Device.FileSystem;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class SuOperations {

    private Context mContext;
    private Device mDevice;

    private static final String TAG = "Voodoo OTA RootKeeper ProtectedSuOperation";
    public static final String SU_BACKUP_PATH = "/system/su-backup";
    public static final String CMD_REMOUNT_RW = "mount -o remount,rw /system /system\n";
    public static final String CMD_REMOUNT_RO = "mount -o remount,ro /system /system\n";

    public SuOperations(Context context, Device device) {
        mContext = context;
        mDevice = device;
    }

    public final void backup() {

        Log.i(TAG, "Backup to protected su");

        String suSource = "/system/xbin/su";

        String script = "";
        script += CMD_REMOUNT_RW;

        // de-protect
        if (mDevice.mFileSystem == FileSystem.EXTFS)
            script += mContext.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + SU_BACKUP_PATH + "\n";

        if (Utils.isSuid(mContext, "/system/bin/su"))
            suSource = "/system/bin/su";

        script += "cat " + suSource + " > " + SU_BACKUP_PATH + "\n";
        script += "chmod 06755 " + SU_BACKUP_PATH + "\n";

        // protect
        if (mDevice.mFileSystem == FileSystem.EXTFS)
            script += mContext.getFilesDir().getAbsolutePath()
                    + "/chattr +i " + SU_BACKUP_PATH + "\n";

        script += CMD_REMOUNT_RO;

        Utils.runScript(mContext, script, "su");

        String toastText;
        if (mDevice.mFileSystem == FileSystem.EXTFS)
            toastText = mContext.getString(string.toast_su_protected);
        else
            toastText = mContext.getString(string.toast_su_backup);

        Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show();

    }

    public final void restore() {
        String script = "";

        script += CMD_REMOUNT_RW;

        // restore su binary to /system/bin/su
        // choose bin over xbin to avoid confusion
        script += "cat " + SU_BACKUP_PATH + " > /system/bin/su\n";
        script += "chmod 06755 /system/bin/su\n";
        script += "rm /system/xbin/su\n";

        script += CMD_REMOUNT_RO;

        Utils.runScript(mContext, script, SU_BACKUP_PATH);

        Toast.makeText(mContext, mContext.getString(string.toast_su_restore),
                Toast.LENGTH_LONG).show();
    }

    public final void deleteBackup() {

        Log.i(TAG, "Delete protected or backup su");

        String script = "";
        script += CMD_REMOUNT_RW;

        // de-protect
        if (mDevice.mFileSystem == FileSystem.EXTFS)
            script += mContext.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + SU_BACKUP_PATH + "\n";

        script += "rm " + SU_BACKUP_PATH + "\n";
        script += CMD_REMOUNT_RO;

        Utils.runScript(mContext, script, "su");

        Toast.makeText(mContext, mContext.getString(string.toast_su_delete_backup),
                Toast.LENGTH_LONG).show();

    }

    public final void unroot() {

        Log.i(TAG, "Unroot device but keep su backup");

        String script = "";

        script += CMD_REMOUNT_RW;

        // delete su binaries
        script += "rm /system/*bin/su\n";

        script += CMD_REMOUNT_RO;

        Utils.runScript(mContext, script, "su");

        Toast.makeText(mContext, mContext.getString(string.toast_unroot), Toast.LENGTH_LONG).show();

    }
}
