
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.otarootkeeper.DeviceStatus.FileSystems;

import android.content.Context;
import android.util.Log;

public class SuOperations {

    private static final String TAG = "Voodoo OTA RootKeeper ProtectedSuOperation";

    public static final String protectedPath = "/system/su-protected";

    public static final void backup(Context context, DeviceStatus status) {

        Log.i(TAG, "Backup to protected su");

        String script = "";
        String suSource = "/system/xbin/su";

        script += "mount -o remount,rw /system /system\n";

        // de-protect
        if (status.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr -i " + protectedPath + "\n";

        if (Utils.isSuid(context, "/system/bin/su"))
            suSource = "/system/bin/su";
        script += "cat " + suSource + " > " + protectedPath + "\n";
        script += "chmod 06755 " + protectedPath + "\n";

        // protect
        if (status.fs == FileSystems.EXTFS)
            script += context.getFilesDir().getAbsolutePath()
                    + "/chattr +i " + protectedPath + "\n";

        script += "mount -o remount,ro /system /system\n";

        Utils.runScript(context, script, "su");

    }

    public static final void restore(Context context) {
        String script = "";

        script += "mount -o remount,rw /system /system\n";

        script += "cat " + protectedPath + " > /system/bin/su\n";
        script += "chmod 06755 /system/bin/su\n";
        script += "rm /system/xbin/su\n";

        script += "mount -o remount,ro /system /system\n";

        Utils.runScript(context, script, protectedPath);
    }
}
