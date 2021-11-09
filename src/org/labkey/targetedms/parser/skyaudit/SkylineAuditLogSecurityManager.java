/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.targetedms.TargetedMSModule;

import javax.annotation.Nullable;

/**
 * This class determines security level of the current Skyline document upload operation
 * based on the system settings, user privileges and file access level.
 */
public class SkylineAuditLogSecurityManager
{

    public enum INTEGRITY_LEVEL
    {
        ANY(0), HASH(1), RSA(2);

        private final int value;
        INTEGRITY_LEVEL(int pValue){this.value = pValue;}
        public int getValue(){return this.value;}

    }

    private final INTEGRITY_LEVEL _verificationLevel;
    private static final Logger LOG = LogManager.getLogger(SkylineAuditLogSecurityManager.class);
    private final Logger _jobLogger;

    public SkylineAuditLogSecurityManager(Container container, @Nullable Logger jobLogger)
    {
        _jobLogger = jobLogger;

        int propIndex = Integer.parseInt(TargetedMSModule.SKYLINE_AUDIT_LEVEL_PROPERTY.getEffectiveValue(container));
        _verificationLevel = INTEGRITY_LEVEL.values()[propIndex];
    }

    /**
     * Calculate integrity level for the current upload based on the container access
     * and user privileges.
     * @return returns member if INTEGRITY_LEVEL enum for the current integrity level
     */
    public INTEGRITY_LEVEL getIntegrityLevel(){

        return _verificationLevel;
    }

    /**
     * If the current log integrity level is same of below the minTolerateLevel then
     * the message is logged as warning. Otherwise AuditLogException is thrown.
     */


    public void reportErrorForIntegrityLevel(String pMessage, INTEGRITY_LEVEL minTolerateLevel, @Nullable Throwable e) throws AuditLogException
    {
        if(getIntegrityLevel().getValue() <= minTolerateLevel.getValue())
        {
            LOG.warn(pMessage, e);
            if (_jobLogger != null)
            {
                LOG.warn(pMessage, e);
            }
        }
        else
            throw new AuditLogException(pMessage, e);
    }

    public void reportErrorForIntegrityLevel(String pMessage, INTEGRITY_LEVEL minTolerateLevel) throws AuditLogException
    {
        reportErrorForIntegrityLevel(pMessage, minTolerateLevel, null);
    }
}
