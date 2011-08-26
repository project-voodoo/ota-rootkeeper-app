
package org.projectvoodoo.otarootkeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "Voodoo OTA RootKeeper MainActivity";
    private static final String scriptFileName = "commands.sh";
    private static final String protectedSuFullPath = "/system/su-protected";

    private enum FileSystems {
        EXTFS,
        UNSUPPORTED
    }

    private FileSystems fs = FileSystems.UNSUPPORTED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        detectSystemFs();
        if (!isSuProtected())
            backupProtectedSu();

        if (!detectValidSuBinaryInPath())
            restoreProtectedSu();
    }

    private Boolean isSuProtected() {
        ensureAttributeUtilsAvailability();

        switch (fs) {
            case EXTFS:
                try {
                    String lsattr = getFilesDir().getAbsolutePath() + "/lsattr";
                    String attrs = getCommandOutput(lsattr + " " + protectedSuFullPath).trim();
                    Log.d(TAG, "attributes: " + attrs);
                    if (attrs.matches(".*-i-.*\\/su-protected")) {
                        if (isSuid(protectedSuFullPath)) {
                            Log.i(TAG, "su binary is already protected");
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            case UNSUPPORTED:
                return isSuid(protectedSuFullPath);

        }
        return false;
    }

    private Boolean isSuid(String filename) {

        try {

            Process p = Runtime.getRuntime().exec(getFilesDir() + "/test -u " + filename);
            p.waitFor();
            if (p.exitValue() == 0) {
                Log.d(TAG, filename + " is set-user-ID");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, filename + " is not set-user-ID");
        return false;

    }

    private void backupProtectedSu() {
        ensureAttributeUtilsAvailability();

        String script = "";
        String suSource = "/system/xbin/su";

        script += "mount -o remount,rw /system /system\n";

        // de-protect
        if (fs == FileSystems.EXTFS)
            script += getFilesDir().getAbsolutePath() + "/chattr +i " + protectedSuFullPath + "\n";

        if (isSuid("/system/bin/su"))
            suSource = "/system/bin/su";
        script += "cat " + suSource + " > " + protectedSuFullPath + "\n";

        // protect
        if (fs == FileSystems.EXTFS)
            script += getFilesDir().getAbsolutePath() + "/chattr +i " + protectedSuFullPath + "\n";

        script += "mount -o remount,ro /system /system\n";

        runScript(script, "su");

    }

    private void restoreProtectedSu() {
        String script = "";

        script += "mount -o remount,rw /system /system\n";

        script += "cat " + protectedSuFullPath + " > /system/bin/su\n";
        script += "chmod 06755 /system/bin/su\n";
        script += "rm /system/xbin/su\n";

        script += "mount -o remount,ro /system /system\n";

        runScript(script, protectedSuFullPath);
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

    private void runScript(String content) {
        runScript(content, "/system/bin/sh");
    }

    private void runScript(String content, String shell) {
        try {
            FileOutputStream fos = openFileOutput(scriptFileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
            Runtime.getRuntime().exec("chmod 700 " + getFileStreamPath(scriptFileName));

            // set executable permissions
            String command = getFileStreamPath(scriptFileName).getAbsolutePath();
            command = shell + " -c " + command;
            Runtime.getRuntime().exec(command);

            // delete the script file
            new File(scriptFileName).delete();
        } catch (Exception e) {
            Log.d(TAG, "unable to run script: " + scriptFileName);
            e.printStackTrace();
        }
    }

    private void ensureAttributeUtilsAvailability() {

        // verify custom busybox presence by test, lsattr and chattr
        // files/symlinks
        try {
            openFileInput("test");
            openFileInput("lsattr");
            openFileInput("chattr");
        } catch (FileNotFoundException notfoundE) {
            Log.d(TAG, "Extracting tools from assets is required");

            try {
                copyFromAssets("busybox", "test");

                String filesPath = getFilesDir().getAbsolutePath();

                String script = "chmod 700 " + filesPath + "/test\n";
                script += "ln -s test " + filesPath + "/lsattr\n";
                script += "ln -s test " + filesPath + "/chattr\n";
                runScript(script);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public String getCommandOutput(String command) throws IOException {

        StringBuilder output = new StringBuilder();

        InputStream is = Runtime.getRuntime().exec(command).getInputStream();
        InputStreamReader r = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(r);

        String line;
        while ((line = in.readLine()) != null) {
            output.append(line);
            output.append("\n");
        }

        return output.toString();
    }

    public Boolean detectValidSuBinaryInPath() {
        // search for valid su binaries in PATH

        String[] pathToTest = System.getenv("PATH").split(":");

        for (String path : pathToTest) {
            File suBinary = new File(path + "/su");

            if (suBinary.exists()) {
                if (isSuid(suBinary.getAbsolutePath())) {
                    Log.d(TAG, "Found adequate su binary at " + suBinary.getAbsolutePath());
                    return true;
                }
            }
        }
        return false;
    }
}
