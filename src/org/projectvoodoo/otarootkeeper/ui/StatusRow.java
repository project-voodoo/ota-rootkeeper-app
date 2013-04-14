
package org.projectvoodoo.otarootkeeper.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;

import org.projectvoodoo.otarootkeeper.R;
import org.projectvoodoo.otarootkeeper.R.id;

public class StatusRow extends TableRow implements OnClickListener {

    private Context context;

    private View mView;

    public StatusRow(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
    }

    public void setAvailable(Boolean availability) {

        if (mView != null)
            removeView(mView);

        if (availability)
            mView = inflate(context, R.layout.status_available, null);
        else
            mView = inflate(context, R.layout.status_unavailable, null);

        setCustomPadding();
        addView(mView);
    }

    public void setAvailable(Boolean availability, String googlePlayUrl) {

        if (mView != null)
            removeView(mView);

        if (!availability) {
            mView = inflate(context, R.layout.status_unavailable_with_google_play_link, null);

            Button installButton = (Button) mView.findViewById(id.button_install);
            installButton.setOnClickListener(this);
            installButton.setTag(googlePlayUrl);
            setCustomPadding();
            addView(mView);
        }
    }

    private void setCustomPadding() {
        mView.setPadding(8, 0, 0, 0);
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) v.getTag()));
        context.startActivity(intent);

    }

}
