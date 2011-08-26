
package org.projectvoodoo.otarootkeeper;

import org.projectvoodoo.libsu.R.id;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;

public class StatusRow extends TableRow implements OnClickListener {

    Context context;

    public StatusRow(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
    }

    public void setAvailable(Boolean availability) {

        View view;
        if (availability) {
            view = inflate(context, R.layout.status_available, null);
        } else {
            view = inflate(context, R.layout.status_unavailable, null);

        }
        addView(view);
    }

    public void setAvailable(Boolean availability, String marketLink) {
        if (!availability) {
            View view;
            view = inflate(context, R.layout.status_unavailable_with_market_link, null);

            Button installButton = (Button) view.findViewById(id.button_install);
            installButton.setOnClickListener(this);
            installButton.setTag(marketLink);
            addView(view);
        }
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) v.getTag()));
        context.startActivity(intent);

    }

}
