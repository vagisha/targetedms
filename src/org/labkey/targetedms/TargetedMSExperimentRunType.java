/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

package org.labkey.targetedms;

import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSExperimentRunType extends ExperimentRunType
{
    private String[] _protocolPrefixes;

    public TargetedMSExperimentRunType()
    {
        super("Imported Targeted MS Results", TargetedMSSchema.SCHEMA_NAME, TargetedMSSchema.TABLE_TARGETED_MS_RUNS);
        _protocolPrefixes = new String[] {TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX,
                                          TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX};
    }

    @Override
    public Priority getPriority(ExpProtocol protocol)
    {
        Lsid lsid = new Lsid(protocol.getLSID());
        String objectId = lsid.getObjectId();

        for (String protocolPrefix : _protocolPrefixes)
        {
            if (objectId.startsWith(protocolPrefix))
            {
                return Handler.Priority.HIGH;
            }
        }

        return null;
    }
}