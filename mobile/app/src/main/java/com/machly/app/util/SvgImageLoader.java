package com.machly.app.util;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.machly.app.AppConstants;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SvgImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SvgImageLoader() {}

    public static void load(Context context, ImageView imageView, String imagePath) {
        if (imageView == null) return;
        final String path = (imagePath == null || imagePath.trim().isEmpty())
                ? "/assets/equipment/default.svg"
                : imagePath.trim();
        imageView.setTag(path);
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(0xFFCFD8CA);

        EXECUTOR.execute(() -> {
            try {
                SVG svg = loadSvg(context, path);
                if (svg == null) return;
                PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
                MAIN.post(() -> {
                    if (path.equals(imageView.getTag())) {
                        imageView.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
                        imageView.setImageDrawable(drawable);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private static SVG loadSvg(Context context, String path) throws Exception {
        String relative = path.startsWith("/") ? path.substring(1) : path;
        try {
            if (relative.startsWith("assets/")) {
                try (InputStream in = context.getAssets().open(relative.substring("assets/".length()))) {
                    return SVG.getFromInputStream(in);
                }
            }
        } catch (Exception ignored) {
        }

        String assetFallback = relative.replace("assets/", "");
        if (assetFallback.startsWith("equipment/")) {
            try (InputStream in = context.getAssets().open(assetFallback)) {
                return SVG.getFromInputStream(in);
            } catch (Exception ignored) {
            }
        }

        String url = path.startsWith("http") ? path : AppConstants.getOriginUrl() + (path.startsWith("/") ? path : "/" + path);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        try (InputStream in = connection.getInputStream()) {
            return SVG.getFromInputStream(in);
        } finally {
            connection.disconnect();
        }
    }
}
