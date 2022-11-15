package me.pqpo.log4a;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by pqpo on 2017/11/21.
 */

public class FileUtils {

    public static File getLogDir(Context context) {
        File log = new File(Environment.getExternalStorageDirectory(), "logs");
        if (!log.exists()) {
            log.mkdir();
        }
        return log;
    }

}
