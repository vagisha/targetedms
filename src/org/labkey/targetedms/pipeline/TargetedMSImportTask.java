/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.util.FileType;
import org.labkey.targetedms.SkylineDocImporter;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.model.ExperimentAnnotations;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;
import org.labkey.targetedms.query.ExperimentAnnotationsManager;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Import a Skyline document, create an experiment run wrapper around it, and wire up to the experiment, if present.
 * Created by Josh on 7/25/2016.
 */
public class TargetedMSImportTask extends PipelineJob.Task<TargetedMSImportTask.Factory>
{
    public TargetedMSImportTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        TargetedMSImportPipelineJob job = (TargetedMSImportPipelineJob)getJob();

        try
        {
            XarContext context = new XarContext(job.getDescription(), job.getContainer(), job.getUser());
            SkylineDocImporter importer = new SkylineDocImporter(job.getUser(), job.getContainer(), context.getJobDescription(),
                                                                 job.getExpData(), job.getLogger(), context, job.getRepresentative(),
                                                                 job.getLocalDirectory(), job.getPipeRoot());
            TargetedMSRun run = importer.importRun(job.getRunInfo());

            Integer jobId = PipelineService.get().getJobId(getJob().getUser(), getJob().getContainer(), getJob().getJobGUID());
            ExpRun expRun = TargetedMSManager.ensureWrapped(run, job.getUser(), job.getPipeRoot(), jobId);

            // Check if an experiment is defined in the current folder, or if an experiment defined in a parent folder
            // has been configured to include subfolders.
            ExperimentAnnotations expAnnotations = ExperimentAnnotationsManager.getExperimentIncludesContainer(job.getContainer());
            if (expAnnotations != null)
            {
                ExperimentAnnotationsManager.addSelectedRunsToExperiment(expAnnotations.getExperiment(), new int[]{expRun.getRowId()}, job.getUser());
            }
        }
        catch (ExperimentException | XMLStreamException | IOException | AuditLogException e)
        {
            throw new PipelineJobException(e);
        }

        return new RecordedActionSet();
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, ExperimentExportTask.Factory>
    {
        public Factory()
        {
            super(TargetedMSImportTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new TargetedMSImportTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            return Collections.emptyList();
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.emptyList();
        }

        public String getStatusName()
        {
            return "IMPORT";
        }

        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }

}
