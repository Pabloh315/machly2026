package com.machly.app.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DbExportUtil {
    public static void exportDatabase(Context context) {
        try {
            File sd = context.getExternalFilesDir(null);
            File data = context.getDatabasePath("machly.db");

            if (sd.canWrite()) {
                String currentDBPath = data.getAbsolutePath();
                String backupDBPath = "machly_backup.db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    Log.d("DbExport", "DB Exported to " + backupDB.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e("DbExport", "Error exporting DB", e);
        }
    }
}
