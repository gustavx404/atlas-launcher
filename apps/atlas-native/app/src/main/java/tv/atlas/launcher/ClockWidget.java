package tv.atlas.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Native clock widget; one minute tick, active only while attached to the home screen. */
final class ClockWidget extends LinearLayout {
    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");
    private final TextView time;
    private final TextView date;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", PORTUGUESE);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM", PORTUGUESE);
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            update();
            handler.postDelayed(this, 30_000L);
        }
    };

    ClockWidget(Context context, float heightDp) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.START);
        time = AtlasStyle.text(this, "--:--", Math.min(60, heightDp * .085f), AtlasStyle.WHITE, false);
        time.setLetterSpacing(-.04f);
        date = AtlasStyle.text(this, "", Math.min(18, heightDp * .026f), 0xffe2e9ef, false);
        LayoutParams dateParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = AtlasStyle.dp(this, 8);
        addView(time, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(date, dateParams);
        update();
    }

    private void update() {
        Date now = new Date();
        time.setText(timeFormat.format(now));
        String formatted = dateFormat.format(now);
        date.setText(formatted.substring(0, 1).toUpperCase(PORTUGUESE) + formatted.substring(1));
    }

    void setRunning(boolean running) {
        handler.removeCallbacks(ticker);
        if (running) handler.post(ticker);
    }
}
