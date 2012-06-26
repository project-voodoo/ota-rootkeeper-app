
package org.projectvoodoo.otarootkeeper.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Device {

    private static final String TAG = "Voodoo OTA RootKeeper Device";

    private Context mContext;
    public SuOperations mSuOps;

    public Boolean isRooted = false;
    public Boolean isSuperuserAppInstalled = false;
    public Boolean isSuProtected = false;

    public enum FileSystem {
        EXTFS,
        UNSUPPORTED
    }

    public FileSystem mFileSystem = FileSystem.UNSUPPORTED;

    public Device(Context context) {
        mContext = context;

        ensureAttributeUtilsAvailability();
        detectSystemFs();

        analyzeSu();

        mSuOps = new SuOperations(context, this);
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
                        mFileSystem = FileSystem.EXTFS;
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
        mFileSystem = FileSystem.UNSUPPORTED;

    }

    private void ensureAttributeUtilsAvailability() {

        String[] symlinks = {
                "test",
                "lsattr",
                "chattr"
        };

        // verify custom busybox presence by test, lsattr and chattr
        // files/symlinks
        try {
            mContext.openFileInput("busybox");
            for (String s : symlinks)
                mContext.openFileInput(s);

        } catch (FileNotFoundException notfoundE) {
            Log.d(TAG, "Extracting tools from assets is required");

            try {
                Utils.copyFromAssets(mContext, "busybox", "busybox");

                String filesPath = mContext.getFilesDir().getAbsolutePath();
                String script = "chmod 700 " + filesPath + "/busybox\n";
                for (String s : symlinks) {
                    script += "rm " + filesPath + "/" + s + "\n";
                    script += "ln -s busybox " + filesPath + "/" + s + "\n";
                }

                Utils.runScript(mContext, script);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void analyzeSu() {
        isRooted = detectValidSuBinaryInPath();
        isSuperuserAppInstalled = isSuperUserApkinstalled();
        isSuProtected = isSuProtected();
    }

    private Boolean isSuProtected() {

        switch (mFileSystem) {
            case EXTFS:
                try {
                    String lsattr = mContext.getFilesDir().getAbsolutePath() + "/lsattr";
                    String attrs = Utils.getCommandOutput(lsattr + " "
                            + SuOperations.SU_BACKUP_PATH).trim();
                    Log.d(TAG, "attributes: " + attrs);

                    String filename = SuOperations.SU_BACKUP_PATH.split("/")[2];

                    if (attrs.matches(".*-i-.*\\/" + filename)) {
                        if (Utils.isSuid(mContext, SuOperations.SU_BACKUP_PATH)) {
                            Log.i(TAG, "su binary is already protected");
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            case UNSUPPORTED:
                return Utils.isSuid(mContext, SuOperations.SU_BACKUP_PATH);

        }
        return false;
    }

    private Boolean detectValidSuBinaryInPath() {
        // search for valid su binaries in PATH

        String[] pathToTest = System.getenv("PATH").split(":");

        for (String path : pathToTest) {
            File suBinary = new File(path + "/su");

            if (suBinary.exists()) {
                if (Utils.isSuid(mContext, suBinary.getAbsolutePath())) {
                    Log.d(TAG, "Found adequate su binary at " + suBinary.getAbsolutePath());
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean isSuperUserApkinstalled() {
        try {
            mContext.getPackageManager().getPackageInfo("com.noshufou.android.su", 0);
            Log.d(TAG, "Superuser.apk installed");
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

}
