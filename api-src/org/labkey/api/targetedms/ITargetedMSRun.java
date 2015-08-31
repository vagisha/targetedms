package org.labkey.api.targetedms;

import org.labkey.api.data.Container;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 2:13 PM
 */
public interface ITargetedMSRun
{
    public Container getContainer();
    public String getBaseName();
    public String getDescription();
}
