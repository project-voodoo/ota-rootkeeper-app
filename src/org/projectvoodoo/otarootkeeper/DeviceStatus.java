
package org.projectvoodoo.otarootkeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class DeviceStatus {

    private static final String TAG = "Voodoo OTA RootKeeper DeviceStatus";

    Context context;

    public enum FileSystems {
        EXTFS,
        UNSUPPORTED
    }

    public FileSystems fs = FileSystems.UNSUPPORTED;

    public DeviceStatus(Context context) {
        this.context = context;

        ensureAttributeUtilsAvailability();
        detectSystemFs();
    }

    private void detectSystemFs() {

        // detect an ExtFS filesystem

        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/mounts"), 8192);

            String line;
            String parsedFs;

            while ((line = in.readLine()) != null) {
                if (line.matches(".*system.*")) {
                    Log.i(TAG, "/system mount point: " + line);
                    parsedFs = line.split(" ")[2].trim();

                    if (parsedFs.equals("ext2")
                            || parsedFs.equals("ext3")
                            || parsedFs.equals("ext4")) {
                        Log.i(TAG, "/system filesystem support extended attributes");
                        fs = FileSystems.EXTFS;
                        return;
                    }
                }
            }
            in.close();

        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse /proc/mounts");
            e.printStackTrace();
        }

        Log.i(TAG, "/system filesystem doesn't support extended attributes");
        fs = FileSystems.UNSUPPORTED;

    }

    private void ensureAttributeUtilsAvailability() {

        // verify custom busybox presence by test, lsattr and chattr
        // files/symlinks
        try {
            context.openFileInput("test");
            context.openFileInput("lsattr");
            context.openFileInput("chattr");
        } catch (FileNotFoundException notfoundE) {
            Log.d(TAG, "Extracting tools from assets is required");

            try {
                Utils.copyFromAssets(context, "busybox", "test");

                String filesPath = context.getFilesDir().getAbsolutePath();

                String script = "chmod 700 " + filesPath + "/test\n";
                script += "ln -s test " + filesPath + "/lsattr\n";
                script += "ln -s test " + filesPath + "/chattr\n";
                Utils.runScript(context, script);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean isSuProtected() {

        switch (fs) {
            case EXTFS:
                try {
                    String lsattr = context.getFilesDir().getAbsolutePath() + "/lsattr";
                    String attrs = Utils.getCommandOutput(lsattr + " "
                            + SuOperations.protectedPath).trim();
                    Log.d(TAG, "attributes: " + attrs);

                    if (attrs.matches(".*-i-.*\\/su-protected")) {
                        if (Utils.isSuid(context, SuOperations.protectedPath)) {
                            Log.i(TAG, "su binary is already protected");
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            case UNSUPPORTED:
                return Utils.isSuid(context, SuOperations.protectedPath);

        }
        return false;
    }

    public Boolean detectValidSuBinaryInPath() {
        // search for valid su binaries in PATH

        String[] pathToTest = System.getenv("PATH").split(":");

        for (String path : pathToTest) {
            File suBinary = new File(path + "/su");

            if (suBinary.exists()) {
                if (Utils.isSuid(context, suBinary.getAbsolutePath())) {
                    Log.d(TAG, "Found adequate su binary at " + suBinary.getAbsolutePath());
                    return true;
                }
            }
        }
        return false;
    }
}
