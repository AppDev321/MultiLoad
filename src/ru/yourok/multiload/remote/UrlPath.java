package ru.yourok.multiload.remote;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 10/26/12
 * Time: 5:21 PM
 */
public class UrlPath
{
    private String Url;
    private String Path;

    public UrlPath()
    {
        Url = "";
        Path = "";
    }

    public UrlPath(String url, String path)
    {
        Url = url;
        Path = path;
    }

    public String getUrl()
    {
        return Url;
    }

    public String getPath()
    {
        return Path;
    }
}
