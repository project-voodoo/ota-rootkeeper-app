
package org.projectvoodoo.otarootkeeper.backend;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class Utils {

    private static final String TAG = "Voodoo Utils";

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

    public static ArrayList<String> run(String command) {
        return run("/system/bin/sh", command);
    }

    public static ArrayList<String> run(String shell, String command) {
        return run(shell, new String[] {
                command
        });
    }

    public static ArrayList<String> run(String shell, ArrayList<String> commands) {
        String[] commandsArray = new String[commands.size()];
        commands.toArray(commandsArray);
        return run(shell, commandsArray);
    }

    public static ArrayList<String> run(String shell, String[] commands) {
        ArrayList<String> output = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput =
                    new BufferedOutputStream(process.getOutputStream());
            BufferedReader shellOutput =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            for (String command : commands) {
                Log.i(TAG, "command: " + command);
                shellInput.write((command + "\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null)
                output.add(line);

            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return output;
    }

    public static final String getCommandOutput(String command) throws IOException {

        StringBuilder output = new StringBuilder();

        Log.d(TAG, "Getting output for command: " + command);
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

        ArrayList<String> output = run("su", suTestScript + suTestScriptValid);

        if (output.size() == 1 && output.get(0).trim().equals(suTestScriptValid)) {
            Log.d(TAG, "Superuser command auth confirmed");
            return true;

        } else {
            Log.d(TAG, "Superuser command auth refused");
            return false;

        }

    }

}
