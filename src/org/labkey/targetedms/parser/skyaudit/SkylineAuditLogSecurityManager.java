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


import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.security.User;
import org.labkey.targetedms.SkylineAuditLogImporter;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;
import org.mozilla.javascript.commonjs.module.ModuleScope;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_FOLDER_TYPE;

/**
 * This class determines security level of the current Skyline document upload operation
 * based on the system settings, user privileges and file access level.
 */
public class SkylineAuditLogSecurityManager
{

    public enum INTEGRITY_LEVEL
    {
        ANY(0), HASH(1), RSA(2);

        private int value;
        private INTEGRITY_LEVEL(int p_value){this.value = p_value;}
        public int getValue(){return this.value;}

    }

    private INTEGRITY_LEVEL _verificationLevel;
    private Container _container;
    private User _user;
    private Logger _logger;
    private Module _panorama;

    public SkylineAuditLogSecurityManager(Container p_container, User p_user) throws AuditLogException{
        _container = p_container;
        _user = p_user;
        _logger = Logger.getLogger(this.getClass());


        TargetedMSModule targetedMSModule = null;
        for (Module m : _container.getActiveModules())
        {
            if (m instanceof TargetedMSModule)
            {
                targetedMSModule = (TargetedMSModule) m;
            }
        }
        if (targetedMSModule == null)
        {
            throw new AuditLogException("Cannot upload into a non-Panorama container"); // theoretically this should never happen.
        }
        ModuleProperty logLevelProperty = targetedMSModule.getModuleProperties().get(TargetedMSModule.SKYLINE_AUDIT_LEVEL);
        int propIndex = Integer.parseInt(logLevelProperty.getEffectiveValue(_container));
        _verificationLevel = INTEGRITY_LEVEL.values()[propIndex];
    }

    /**
     * Calculate integrity level for the current upload based on the container access
     * and user privileges.
     * @return
     */
    public INTEGRITY_LEVEL getIntegrityLevel(){

        //_container.
        return _verificationLevel;
    }

    /**
     * If the current log integrity level is same of below the minTolerateLevel then
     * the message is logged as warning. Otherwise AuditLogException is thrown.
     */


    public void reportErrorForIntegrityLevel(String p_message, INTEGRITY_LEVEL minTolerateLevel, Throwable e) throws AuditLogException
    {
        if(getIntegrityLevel().getValue() <= minTolerateLevel.getValue())
            _logger.warn(p_message, e);
        else
            throw new AuditLogException(p_message, e);
    }

    public void reportErrorForIntegrityLevel(String p_message, INTEGRITY_LEVEL minTolerateLevel) throws AuditLogException
    {
        reportErrorForIntegrityLevel(p_message, minTolerateLevel, null);
    }
}
