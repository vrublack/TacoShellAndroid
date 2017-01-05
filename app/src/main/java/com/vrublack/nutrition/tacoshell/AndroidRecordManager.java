package com.vrublack.nutrition.tacoshell;


import android.os.Build;
import android.os.Environment;

import com.vrublack.nutrition.core.DailyRecord;
import com.vrublack.nutrition.core.RecordManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AndroidRecordManager extends RecordManager
{
    private static final String EXTERNAL_FOLDER = "TacoShell" + File.separator + "records";


    public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            return true;
        }
        return false;
    }

    private File getFile(String subpath)
    {
        return new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + File.separator + EXTERNAL_FOLDER + File.separator + subpath);
    }

    @Override
    public List<String> loadFileNames()
    {
        File folder = getFile("");
        List<String> fileNames = new ArrayList<>();
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return fileNames;

        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                fileNames.add(file.getName());
            }
        }

        return fileNames;
    }

    @Override
    public DailyRecord loadRecord(String dateString)
    {
        File recordFile = getFile(dateString);
        if (!recordFile.exists())
            return null;

        try
        {
            FileInputStream fis = new FileInputStream(recordFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            return (DailyRecord) ois.readObject();

        } catch (FileNotFoundException e)
        {
            return null;
        } catch (IOException e)
        {
            return null;
        } catch (ClassNotFoundException e)
        {
            return null;
        }
    }

    @Override
    public void saveRecord(DailyRecord record)
    {
        File f = new File(Environment.getExternalStorageDirectory(), "TacoShell");
        if (!f.exists())
        {
            f.mkdirs();
        }

        File folder = getFile("");
        if (!folder.exists())
            folder.mkdirs();
        try
        {
            String fileName = getDateString(RecordManager.getCalendar(record.getDate()));
            FileOutputStream fos = new FileOutputStream(getFile(fileName));
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(record);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
