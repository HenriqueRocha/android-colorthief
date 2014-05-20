/*
 * Copyright (C) 2014 Fonpit AG
 *
 * License
 * -------
 * Creative Commons Attribution 2.5 License:
 * http://creativecommons.org/licenses/by/2.5/
 *
 * Thanks
 * ------
 * Simon Oualid - For creating java-colorthief
 * available at https://github.com/soualid/java-colorthief
 *
 * Lokesh Dhakar - for the original Color Thief javascript version
 * available at http://lokeshdhakar.com/projects/color-thief/
 *
 */

package de.androidpit.androidcolorthief.sample;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.androidpit.androidcolorthief.MMCQ;

/**
 * @author <a href="mailto:henrique.rocha@androidpit.de">Henrique Rocha</a>
 */
public class SampleMainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.photo1);
        List<int[]> result = new ArrayList<>();
        try {
            result = MMCQ.compute(icon, 5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        icon.recycle();

        int[] dominantColor = result.get(0);
        findViewById(R.id.dominant_color).setBackgroundColor(
                Color.rgb(dominantColor[0], dominantColor[1], dominantColor[2]));

        LinearLayout palette = (LinearLayout) findViewById(R.id.palette);
        for (int i = 1; i < result.size(); i++) {
            View swatch = new View(this);
            Resources resources = getResources();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            int swatchWidth = (int) (48 * displayMetrics.density + 0.5f);
            int swatchHeight = (int) (48 * displayMetrics.density + 0.5f);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(swatchWidth, swatchHeight);
            int margin = (int) (4 * displayMetrics.density + 0.5f);
            layoutParams.setMargins(margin, 0, margin, 0);
            swatch.setLayoutParams(layoutParams);

            int[] color = result.get(i);
            int rgb = Color.rgb(color[0], color[1], color[2]);
            swatch.setBackgroundColor(rgb);
            palette.addView(swatch);
        }
    }
}
