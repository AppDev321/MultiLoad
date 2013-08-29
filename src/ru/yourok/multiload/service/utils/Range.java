package ru.yourok.multiload.service.utils;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 03.08.12
 * Time: 11:54
 */
public class Range
{
    public long start;
    public long offset;
    public long end;

    public Range(long start, long offset, long end)
    {
        this.start = start;
        this.offset = offset;
        this.end = end;
    }

    public Range()
    {
        this.start = 0;
        this.offset = 0;
        this.end = 0;
    }
}
