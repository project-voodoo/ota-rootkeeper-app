
package org.projectvoodoo.otarootkeeper.backend;

import org.projectvoodoo.otarootkeeper.R.string;
import org.projectvoodoo.otarootkeeper.backend.Device.FileSystems;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class SuOperations {

    Context context;
    Device device;

    private static final String TAG = "Voodoo OTA RootKeeper ProtectedSuOperation";
    public static final String suBackupPath = "/system/su-backup";
    public static final String remountRw = "mount -o remount,rw /system /system\n";
    public static final String remountRo = "mount -o remount,ro /system /system\n";

    public SuOperations(Context context, Device device) {
        this.context = context;
        this.device = device;
    }

    public final void backup() {

        Log.i(TAG, "Backup to protected su");

        String suSource = "/system/xbin/su";

        String script = "";
        script += remountRw;

        // de-protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + suBackupPath + "\n";

        if (Utils.isSuid(context, "/system/bin/su"))
            suSource = "/system/bin/su";

        script += "cat " + suSource + " > " + suBackupPath + "\n";
        script += "chmod 06755 " + suBackupPath + "\n";

        // protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr +i " + suBackupPath + "\n";

        script += remountRo;

        Utils.runScript(context, script, "su");

        String toastText;
        if (device.fs == FileSystems.EXTFS)
            toastText = context.getString(string.toast_su_protected);
        else
            toastText = context.getString(string.toast_su_backup);

        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();

    }

    public final void restore() {
        String script = "";

        script += remountRw;

        // restore su binary to /system/bin/su
        // choose bin over xbin to avoid confusion
        script += "cat " + suBackupPath + " > /system/bin/su\n";
        script += "chmod 06755 /system/bin/su\n";
        script += "rm /system/xbin/su\n";

        script += remountRo;

        Utils.runScript(context, script, suBackupPath);

        Toast.makeText(context, context.getString(string.toast_su_restore),
                Toast.LENGTH_LONG).show();
    }

    public final void deleteBackup() {

        Log.i(TAG, "Delete protected or backup su");

        String script = "";
        script += remountRw;

        // de-protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + suBackupPath + "\n";

        script += "rm " + suBackupPath + "\n";
        script += remountRo;

        Utils.runScript(context, script, "su");

        Toast.makeText(context, context.getString(string.toast_su_delete_backup),
                Toast.LENGTH_LONG).show();

    }

    public final void unroot() {

        Log.i(TAG, "Unroot device but keep su backup");

        String script = "";

        script += remountRw;

        // delete su binaries
        script += "rm /system/*bin/su\n";

        script += remountRo;

        Utils.runScript(context, script, "su");

        Toast.makeText(context, context.getString(string.toast_unroot), Toast.LENGTH_LONG).show();

    }
}
