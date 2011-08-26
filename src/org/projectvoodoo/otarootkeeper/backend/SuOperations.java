
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
    public static final String protectedPath = "/system/su-protected";

    public SuOperations(Context context, Device device) {
        this.context = context;
        this.device = device;
    }

    public final void backup() {

        Log.i(TAG, "Backup to protected su");

        String script = "";
        String suSource = "/system/xbin/su";

        script += "mount -o remount,rw /system /system\n";

        // de-protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + protectedPath + "\n";

        if (Utils.isSuid(context, "/system/bin/su"))
            suSource = "/system/bin/su";
        script += "cat " + suSource + " > " + protectedPath + "\n";
        script += "chmod 06755 " + protectedPath + "\n";

        // protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr +i " + protectedPath + "\n";

        script += "mount -o remount,ro /system /system\n";

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

        script += "mount -o remount,rw /system /system\n";

        script += "cat " + protectedPath + " > /system/bin/su\n";
        script += "chmod 06755 /system/bin/su\n";
        script += "rm /system/xbin/su\n";

        script += "mount -o remount,ro /system /system\n";

        Utils.runScript(context, script, protectedPath);

        Toast.makeText(context, context.getString(string.toast_su_restore), Toast.LENGTH_LONG).show();
    }

    public final void deleteBackup() {

        Log.i(TAG, "Delete protected or backup su");

        String script = "";

        script += "mount -o remount,rw /system /system\n";

        // de-protect
        if (device.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + protectedPath + "\n";

        script += "rm " + protectedPath + "\n";
        script += "mount -o remount,ro /system /system\n";

        Utils.runScript(context, script, "su");

        Toast.makeText(context, context.getString(string.toast_su_delete_backup), Toast.LENGTH_LONG).show();

    }
}
