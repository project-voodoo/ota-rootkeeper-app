
package org.projectvoodoo.otarootkeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

public class Utils {

    private static final String TAG = "Voodoo OTA RootKeeper Utils";
    private static final String scriptFileName = "commands.sh";

    public static final void copyFromAssets(Context context, String source, String destination)
            throws IOException {

        // read file from the apk
        InputStream is = context.getAssets().open(source);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();

        // write files in app private storage
        FileOutputStream output = context.openFileOutput(destination, Context.MODE_PRIVATE);
        output.write(buffer);
        output.close();

        Log.d(TAG, source + " asset copied to " + destination);
    }

    public static final void runScript(Context context, String content) {
        runScript(context, content, "/system/bin/sh");
    }

    public static final void runScript(Context context, String content, String shell) {
        try {
            FileOutputStream fos = context.openFileOutput(scriptFileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
            Runtime.getRuntime().exec("chmod 700 " + context.getFileStreamPath(scriptFileName));

            // set executable permissions
            String command = context.getFileStreamPath(scriptFileName).getAbsolutePath();
            command = shell + " -c " + command;
            Runtime.getRuntime().exec(command);

            // delete the script file
            new File(scriptFileName).delete();
        } catch (Exception e) {
            Log.d(TAG, "unable to run script: " + scriptFileName);
            e.printStackTrace();
        }
    }

    public static final Boolean isSuid(Context context, String filename) {

        try {

            Process p = Runtime.getRuntime().exec(context.getFilesDir() + "/test -u " + filename);
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

    public static final String getCommandOutput(String command) throws IOException {

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

}
