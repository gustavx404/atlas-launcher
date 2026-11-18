package tv.atlas.launcher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.Inet4Address;

/** Compact network card with a transport glyph, connection state, and local IPv4 address. */
final class NetworkStatusWidget extends LinearLayout {
    private final NetworkGlyph glyph;
    private final TextView transportLabel;
    private final TextView addressLabel;
    private final ConnectivityManager connectivity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 30_000L);
        }
    };

    NetworkStatusWidget(Context context) {
        super(context);
        connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumWidth(AtlasStyle.dp(this, 184));
        setPadding(AtlasStyle.dp(this, 12), AtlasStyle.dp(this, 8),
                AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 8));
        setBackground(AtlasStyle.panel(0xe90b1a29, 0x4adbe7f0, 17, this));

        glyph = new NetworkGlyph(context);
        addView(glyph, new LayoutParams(AtlasStyle.dp(this, 28), AtlasStyle.dp(this, 28)));
        LinearLayout text = new LinearLayout(context);
        text.setOrientation(VERTICAL);
        LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textParams.leftMargin = AtlasStyle.dp(this, 9);
        addView(text, textParams);

        transportLabel = AtlasStyle.text(this, "VERIFICANDO REDE", 10, AtlasStyle.CYAN, true);
        transportLabel.setLetterSpacing(.08f);
        addressLabel = AtlasStyle.text(this, "Aguarde…", 14, AtlasStyle.WHITE, true);
        addressLabel.setSingleLine(true);
        addressLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(transportLabel);
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addressParams.topMargin = AtlasStyle.dp(this, 3);
        text.addView(addressLabel, addressParams);

        setContentDescription("Status da rede local");
        setFocusable(true);
        setOnClickListener(view -> {
            try { context.startActivity(new android.content.Intent(Settings.ACTION_WIFI_SETTINGS)); }
            catch (Exception ignored) { }
        });
        refresh();
    }

    private void refresh() {
        Network active = connectivity == null ? null : connectivity.getActiveNetwork();
        NetworkCapabilities capabilities = active == null ? null : connectivity.getNetworkCapabilities(active);
        if (capabilities == null) {
            update("SEM CONEXÃO", "Verifique sua rede", false, false);
            return;
        }

        boolean wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean ethernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        String transport = wifi ? "WI-FI" : ethernet ? "ETHERNET" : "REDE ATIVA";
        String address = localIpv4(connectivity.getLinkProperties(active));
        String detail = address.isEmpty() ? "Conectado · sem IP" : address;
        update(transport, detail, true, ethernet);
    }

    private void update(String transport, String detail, boolean connected, boolean ethernet) {
        transportLabel.setText(transport);
        transportLabel.setTextColor(connected ? AtlasStyle.CYAN : 0xffff9b9b);
        addressLabel.setText(detail);
        glyph.setTransport(ethernet, connected ? AtlasStyle.CYAN : 0xffff9b9b);
        setContentDescription(transport + ". " + detail + ". Abrir configurações de rede.");
    }

    private String localIpv4(LinkProperties properties) {
        if (properties == null) return "";
        for (LinkAddress link : properties.getLinkAddresses()) {
            if (link.getAddress() instanceof Inet4Address && !link.getAddress().isLoopbackAddress()) {
                return link.getAddress().getHostAddress();
            }
        }
        return "";
    }

    void setRunning(boolean running) {
        handler.removeCallbacks(poll);
        if (running) handler.post(poll);
    }

    private static final class NetworkGlyph extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean ethernet;
        private int color = AtlasStyle.CYAN;

        NetworkGlyph(Context context) {
            super(context);
            paint.setColor(color);
            paint.setStrokeWidth(AtlasStyle.dp(this, 2.2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        void setTransport(boolean ethernet, int color) {
            this.ethernet = ethernet;
            this.color = color;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(color);
            float scale = Math.min(getWidth(), getHeight()) / 36f;
            canvas.save();
            canvas.scale(scale, scale);
            if (ethernet) drawEthernet(canvas);
            else drawWifi(canvas);
            canvas.restore();
        }

        private void drawWifi(Canvas canvas) {
            Path arcs = new Path();
            arcs.moveTo(4, 12);
            arcs.quadTo(18, 0, 32, 12);
            arcs.moveTo(9, 17);
            arcs.quadTo(18, 9, 27, 17);
            arcs.moveTo(14, 22);
            arcs.quadTo(18, 18, 22, 22);
            canvas.drawPath(arcs, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(18, 28, 2.2f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }

        private void drawEthernet(Canvas canvas) {
            RectF port = new RectF(5, 5, 31, 29);
            canvas.drawRoundRect(port, 4, 4, paint);
            canvas.drawLine(8, 16, 28, 16, paint);
            canvas.drawLine(12, 16, 12, 21, paint);
            canvas.drawLine(18, 16, 18, 21, paint);
            canvas.drawLine(24, 16, 24, 21, paint);
            canvas.drawLine(18, 29, 18, 33, paint);
        }
    }
}
