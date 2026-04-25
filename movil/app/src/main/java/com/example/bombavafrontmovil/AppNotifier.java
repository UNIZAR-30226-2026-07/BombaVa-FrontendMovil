package com.example.bombavafrontmovil;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AppNotifier {

    public enum Type {
        INFO,
        ERROR,
        SUCCESS
    }

    private static TextView banner;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable hideRunnable;

    public static void show(Activity activity, String message, Type type) {
        if (activity == null || activity.isFinishing()) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        if (banner == null || banner.getParent() == null) {
            banner = new TextView(activity);
            banner.setTextColor(Color.WHITE);
            banner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            banner.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
            banner.setGravity(Gravity.CENTER_VERTICAL);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP;
            params.setMargins(dp(activity, 12), dp(activity, 12), dp(activity, 12), 0);
            banner.setLayoutParams(params);
            banner.setElevation(dp(activity, 8));
            banner.setAlpha(0f);
            banner.setTranslationY(-120f);

            root.addView(banner);
        }

        switch (type) {
            case ERROR:
                banner.setBackgroundColor(Color.parseColor("#C62828"));
                break;
            case SUCCESS:
                banner.setBackgroundColor(Color.parseColor("#2E7D32"));
                break;
            case INFO:
            default:
                banner.setBackgroundColor(Color.parseColor("#455A64"));
                break;
        }

        banner.setText(message);
        banner.animate().cancel();
        banner.setVisibility(TextView.VISIBLE);
        banner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
        }

        hideRunnable = () -> {
            if (banner != null) {
                banner.animate().cancel();
                banner.animate()
                        .alpha(0f)
                        .translationY(-120f)
                        .setDuration(200)
                        .start();
            }
        };

        handler.postDelayed(hideRunnable, 2200);
    }

    private static int dp(Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}