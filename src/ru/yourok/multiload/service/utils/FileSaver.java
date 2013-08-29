package ru.yourok.multiload.service.utils;

import android.os.StatFs;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 27.07.12
 * Time: 11:35
 */
public class FileSaver
{
    boolean isOpen;
    long fileSize;
    String errorString;
    String fileName;
    HashMap<Integer, RandomAccessFile> files;

    public FileSaver(String fileName)
    {
        setFileName(fileName);
        files = new HashMap<Integer, RandomAccessFile>();
    }

    public void setFileName(String fileName)
    {
        //remove ? " / \ < > * | :
        this.fileName = fileName.replaceAll("[\\?\"\\\\<>\\*\\|:]","");
    }

    public boolean Create(long fileSize)
    {
        try
        {
            if (!new File(fileName).exists())
            {//make directory and delete exists file
                String Path = "";
                int sep = fileName.lastIndexOf("/");
                if (sep > 0)
                    Path = fileName.substring(0, sep);
                File f = new File(Path);
                if (!f.exists())
                    f.mkdirs();

                if (!new File(fileName).exists())
                    if (new File(Path).isDirectory() && fileSize > 0)
                    {
                        StatFs stats = new StatFs(Path);
                        long availableBlocks = stats.getAvailableBlocks();
                        long blockSizeInBytes = stats.getBlockSize();
                        long freeSpaceInBytes = availableBlocks * blockSizeInBytes;
                        if (freeSpaceInBytes < fileSize)
                        {
                            errorString = "not enough free space";
                            isOpen = false;
                            return false;
                        }
                    }
            }
            RandomAccessFile writeStream = new RandomAccessFile(fileName, "rw");
            if (fileSize > 0)
                writeStream.setLength(fileSize);
            this.fileSize = fileSize;
            writeStream.close();
            isOpen = true;
        } catch (Exception e)
        {
            isOpen = false;
            errorString = e.getLocalizedMessage();
            e.printStackTrace();
        }
        return isOpen;
    }

    public boolean Open()
    {
        if (!isOpen && !fileName.isEmpty())
        {
            try
            {
                for (int id : files.keySet())
                {
                    files.get(id).close();
                    files.put(id, new RandomAccessFile(fileName, "rw"));
                }
                isOpen = true;
            } catch (Exception e)
            {
                isOpen = false;
                errorString = e.getLocalizedMessage();
                e.printStackTrace();
            }
        }
        return isOpen;
    }

    public int writeBuffer(int id, byte[] buffer, long size)
    {
        if (!isOpen)
            return -1;
        try
        {
            if (files.get(id) == null)
                return -1;

            files.get(id).write(buffer, 0, (int) size);
        } catch (Exception e)
        {
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    public long getFileSize()
    {
        try
        {
            return new File(fileName).length();
        } catch (Exception e)
        {
            e.printStackTrace();
            return 0;
        }
    }

    public String getErrorString()
    {
        return errorString;
    }

    public int copy(String newFileName)
    {
        String Path = "";
        int sep = newFileName.lastIndexOf("/");
        if (sep > 0)
            Path = newFileName.substring(0, sep);
        File f = new File(Path);
        if (!f.exists())
            f.mkdirs();

        new File(fileName).renameTo(new File(newFileName));
        fileName = newFileName;
        if (!Open())
            return -1;
        return 0;
    }

    public void close(int id)
    {
        try
        {
            files.get(id).close();
            files.remove(id);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void seek(int id, long offset)
    {
        try
        {
            if (files.get(id) == null)
            {
                RandomAccessFile writeFile = new RandomAccessFile(fileName, "rw");
                writeFile.seek(offset);
                files.put(id, writeFile);
            } else
                files.get(id).seek(offset);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void close()
    {
        try
        {
            for (int id : files.keySet())
            {
                files.get(id).close();
            }
            files.clear();
            isOpen = false;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String getFileName()
    {
        return fileName;
    }
}