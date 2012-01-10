
package org.projectvoodoo.otarootkeeper.ui;

import org.projectvoodoo.otarootkeeper.R.id;
import org.projectvoodoo.otarootkeeper.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;

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

    public void setAvailable(Boolean availability, String marketLink) {

        if (mView != null)
            removeView(mView);

        if (!availability) {
            mView = inflate(context, R.layout.status_unavailable_with_market_link, null);

            Button installButton = (Button) mView.findViewById(id.button_install);
            installButton.setOnClickListener(this);
            installButton.setTag(marketLink);
            setCustomPadding();
            addView(mView);
        }
    }

    private void setCustomPadding() {
        mView.setPadding(15, 0, 0, 0);
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) v.getTag()));
        context.startActivity(intent);

    }

}
