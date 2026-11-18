package com.machly.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.machly.app.R;

public final class BottomNavHelper {
    private BottomNavHelper() {}

    public static void bind(Activity activity, View nav, int selectedId) {
        if (nav == null) return;

        View home = nav.findViewById(R.id.nav_home);
        View favorites = nav.findViewById(R.id.nav_favorites);
        View account = nav.findViewById(R.id.nav_account);

        styleTab(home, selectedId == R.id.nav_home);
        styleTab(favorites, selectedId == R.id.nav_favorites);
        styleTab(account, selectedId == R.id.nav_account);

        home.setOnClickListener(v -> go(activity, R.id.nav_home));
        favorites.setOnClickListener(v -> go(activity, R.id.nav_favorites));
        account.setOnClickListener(v -> go(activity, R.id.nav_account));
    }

    private static void go(Activity activity, int destinationId) {
        if (destinationId == R.id.nav_home) {
            if (activity instanceof MainActivity) return;
            Intent intent = new Intent(activity, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return;
        }

        if (destinationId == R.id.nav_favorites) {
            if (activity instanceof FavoritesActivity) return;
            activity.startActivity(new Intent(activity, FavoritesActivity.class));
            activity.overridePendingTransition(0, 0);
            return;
        }

        if (destinationId == R.id.nav_account) {
            if (activity instanceof AccountActivity) return;
            activity.startActivity(new Intent(activity, AccountActivity.class));
            activity.overridePendingTransition(0, 0);
        }
    }

    private static void styleTab(View tab, boolean selected) {
        if (!(tab instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) tab;
        int color = tab.getResources().getColor(selected ? R.color.forest : R.color.muted);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(color);
            } else if (child instanceof TextView) {
                TextView tv = (TextView) child;
                tv.setTextColor(color);
                tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }
}
