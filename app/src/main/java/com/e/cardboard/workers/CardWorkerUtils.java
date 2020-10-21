package com.e.cardboard.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.e.cardboard.Constants;
import com.e.cardboard.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.e.cardboard.Constants.CHANNEL_ID;
import static com.e.cardboard.Constants.DELAY_TIME_MILLIS;
import static com.e.cardboard.Constants.NOTIFICATION_ID;
import static com.e.cardboard.Constants.NOTIFICATION_TITLE;
import static com.e.cardboard.Constants.OUTPUT_PATH;

final class CardWorkerUtils {

    static void makeStatusNotification(String message, Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             CharSequence name = Constants.VERBOSE_NOTIFICATION_CHANNEL_NAME;
             String description = Constants.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION;
             int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                 notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0]);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());

    }

    static void sleep() {
        try {
            Thread.sleep(DELAY_TIME_MILLIS, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @WorkerThread
    static Bitmap overlayTextOnBitmap(@NonNull Bitmap bitmap,
                                      @NonNull Context applicationContext,
                                      @NonNull String quote) {
        // Create the output bitmap
        Bitmap output = Bitmap.createBitmap(
                 bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

        Canvas canvas = new Canvas(output);

        float scale = applicationContext.getResources().getDisplayMetrics().density;

        // Create Paint
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(61, 61, 61));
        paint.setTextSize(28 * scale);

        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        int textWidth = (int) (canvas.getWidth() - (16 * scale));


        /*
            Overlay rectangle
         */
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        Point centerOfCanvas = new Point(canvasWidth >> 2, canvasHeight >> 2);

        int left = centerOfCanvas.x - (bitmap.getWidth());
        int top = centerOfCanvas.y - (bitmap.getHeight());
        int right = centerOfCanvas.x + (bitmap.getWidth());
        int bottom = centerOfCanvas.y + (bitmap.getHeight());

        RectF textBg = new RectF(left, top, right, bottom);
        Paint recPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recPaint.setColor(Color.DKGRAY);
        recPaint.setAlpha(255);
        recPaint.setStyle(Paint.Style.FILL);
        recPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        StaticLayout staticLayout = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(
                     quote, 0, quote.length(), paint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(true)
                    .setLineSpacing(1.0f, 1.0f)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                    .setMaxLines(Integer.MAX_VALUE)
                    .build();
        }

        int textHeight = staticLayout.getHeight();

        float x = (bitmap.getWidth() - textWidth) >> 2;
        float y = (bitmap.getHeight() - textHeight) >> 2;
        canvas.save();

        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.drawRect(textBg, recPaint);

        canvas.translate(x, y);
        staticLayout.draw(canvas);

        return output;
    }

    private CardWorkerUtils() {}

     static Uri writeBitmapToFile(@NonNull Context applicationContext,
                                        @NonNull Bitmap bitmap) throws IOException {
        String name = String.format("card-processed-output%s.png", UUID.randomUUID().toString());
         File outputDir = new File(applicationContext.getFilesDir(), OUTPUT_PATH);
         if (!outputDir.exists()) {
              outputDir.mkdirs();
         }
         File outputFile = new File(outputDir, name);
         try {
             try(FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                 bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
         return Uri.fromFile(outputFile);

    }
}
