
package org.projectvoodoo.otarootkeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "Voodoo OTA RootKeeper MainActivity";
    private static final String scriptFileName = "commands.sh";

    private enum FileSystems {
        EXTFS,
        UNSUPPORTED
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detectSystemFs();

        setContentView(R.layout.main);

        if (!isSuProtected())
            backupProtectedSu();
    }

    private Boolean isSuProtected() {
        ensureAttributeUtilsAvailability();

        return false;
    }

    private void backupProtectedSu() {
        ensureAttributeUtilsAvailability();

        try {
            Runtime.getRuntime().exec(
                    getFilesDir().getAbsolutePath() + "lsattr /system/su-protected");
        } catch (IOException e) {
        }

    }

    private void copyFromAssets(String source, String destination) throws IOException {

        // read file from the apk
        InputStream is = getAssets().open(source);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();

        // write files in app private storage
        FileOutputStream output = openFileOutput(destination, Context.MODE_PRIVATE);
        output.write(buffer);
        output.close();

        Log.d(TAG, source + " asset copied to " + destination);
    }

    private void runScript(String content, Boolean withSu) {
        try {
            FileOutputStream fos = openFileOutput(scriptFileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
            Runtime.getRuntime().exec("chmod 700 " + getFileStreamPath(scriptFileName));

            // set executable permissions
            String command = getFileStreamPath(scriptFileName).getAbsolutePath();
            if (withSu)
                command = "su -c " + command;
            else
                command = "/system/bin/sh -c " + command;
            Runtime.getRuntime().exec(command);

            // delete the script file
            new File(scriptFileName).delete();
        } catch (Exception e) {
            Log.d(TAG, "unable to run script: " + scriptFileName);
            e.printStackTrace();
        }

    }

    private void ensureAttributeUtilsAvailability() {
        if (detectSystemFs() == FileSystems.EXTFS) {

            // verify custom busybox presence and its lsattr and chattr symlinks
            try {
                openFileInput("busybox");
                openFileInput("lsattr");
                openFileInput("chattr");
            } catch (FileNotFoundException notfoundE) {
                Log.d(TAG, "Extracting tools from assets is required");

                try {
                    copyFromAssets("busybox", "chattr");

                    String filesPath = getFilesDir().getAbsolutePath();

                    String script = "chmod 700 " + filesPath + "/chattr\n";
                    script += "ln -s chattr " + filesPath + "/lsattr\n";
                    runScript(script, false);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private FileSystems detectSystemFs() {

        // detect an ExtFS filesystem

        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/mounts"), 8192);

            String line;
            String fs;
            while ((line = in.readLine()) != null) {
                if (line.matches(".*system.*")) {
                    Log.i(TAG, "/system mount point: " + line);
                    fs = line.split(" ")[2];
                    if (fs.equals("ext2")
                            || fs.equals("ext3")
                            || fs.equals("ext4"))
                        return FileSystems.EXTFS;
                }
            }
            in.close();

        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse /proc/mounts");
            e.printStackTrace();
        }
        return FileSystems.UNSUPPORTED;

    }
}
