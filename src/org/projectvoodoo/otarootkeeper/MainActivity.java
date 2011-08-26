
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.libsu.R.id;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String TAG = "Voodoo OTA RootKeeper MainActivity";

    DeviceStatus status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = new DeviceStatus(this);

        setContentView(R.layout.main);

        StatusRow row1 = (StatusRow) findViewById(id.row1);
        row1.setAvailable(false, "market://details?id=com.noshufou.android.su");

        StatusRow row2 = (StatusRow) findViewById(id.row2);
        row2.setAvailable(false);

        StatusRow row3 = (StatusRow) findViewById(id.row3);
        row3.setAvailable(true);

        StatusRow row4 = (StatusRow) findViewById(id.row4);
        row4.setAvailable(true);

        if (!status.isSuProtected())
            ProtectedSuOperations.backup(this, status);

        if (!status.detectValidSuBinaryInPath())
            ProtectedSuOperations.restore(this);
    }


}
