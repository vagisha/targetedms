package org.labkey.targetedms;

import org.labkey.api.util.FileUtil;

/**
 * User: vsharma
 * Date: 11/26/13
 * Time: 4:36 PM
 */
public class SkylineFileUtils
{
    public static final String EXT_ZIP = "zip";
    private static final String EXT_SKY_ZIP_W_DOT = ".sky.zip";

    public static String getBaseName(String fileName)
    {
        if(fileName == null)
            return "";
        if(fileName.toLowerCase().endsWith(EXT_SKY_ZIP_W_DOT))
            return FileUtil.getBaseName(fileName, 2);
        else
            return FileUtil.getBaseName(fileName, 1);
    }
}
