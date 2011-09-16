
package org.projectvoodoo.otarootkeeper.backend;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

public class Utils {

    private static final String TAG = "Voodoo Utils";
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

    public static final String runScript(Context context, String content) {
        return runScript(context, content, "/system/bin/sh");
    }

    public static final String runScript(Context context, String content, String shell) {

        Log.d(TAG, "Run script content (with shell: " + shell + "):\n" + content);

        StringBuilder output = new StringBuilder();

        try {

            // write script content
            FileOutputStream fos = context.openFileOutput(scriptFileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();

            // set script file permission
            Process p = Runtime.getRuntime().exec(
                            "chmod 700 " + context.getFileStreamPath(scriptFileName));
            p.waitFor();

            // now execute the script
            String command = context.getFileStreamPath(scriptFileName).getAbsolutePath();
            command = shell + " -c " + command;
            p = Runtime.getRuntime().exec(command);
            p.waitFor();

            BufferedReader in =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }

        } catch (Exception e) {
            Log.d(TAG, "unable to run script: " + scriptFileName);
            e.printStackTrace();
        }
        return output.toString();
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

        Process p = Runtime.getRuntime().exec(command);
        InputStream is = p.getInputStream();
        InputStreamReader r = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(r);

        String line;
        while ((line = in.readLine()) != null) {
            output.append(line);
            output.append("\n");
        }

        return output.toString();
    }

    public static final Boolean canGainSu(Context context) {

        String suTestScript = "#!/system/bin/sh\necho ";
        String suTestScriptValid = "SuPermsOkay";

        String output = runScript(context,
                suTestScript + suTestScriptValid,
                "su");

        if (output.trim().equals(suTestScriptValid)) {
            Log.d(TAG, "Superuser command auth confirmed");
            return true;

        } else {
            Log.d(TAG, "Superuser command auth refused");
            return false;

        }

    }

}
