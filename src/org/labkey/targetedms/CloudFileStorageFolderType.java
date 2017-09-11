package org.labkey.targetedms;

/**
 * Created by davebradlee on 9/8/17.
 */
public class CloudFileStorageFolderType extends TargetedMSFolderType
{
    public static final String NAME = "Cloud File Storage";

    public CloudFileStorageFolderType(TargetedMSModule module)
    {
        super(module, NAME, "Manage targeted MS assays and raw MS files.",
              getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment"), getModule("Cloud")));
    }

    @Override
    public String getLabel()
    {
        return "Panorama With Cloud File Storage";
    }
}
