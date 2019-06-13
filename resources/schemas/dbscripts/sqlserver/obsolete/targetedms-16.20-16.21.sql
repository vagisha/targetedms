/*
 * Copyright (c) 2017-2019 LabKey Corporation
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


--
CREATE TABLE targetedms.QCMetricConfiguration
(
    Id INT IDENTITY(1, 1) NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Container ENTITYID NOT NULL,
    Name NVARCHAR(200) NOT NULL ,
    Series1Label NVARCHAR(200) NOT NULL ,
    Series1SchemaName NVARCHAR(200) NOT NULL ,
    Series1QueryName NVARCHAR(200) NOT NULL ,
    Series2Label NVARCHAR(200),
    Series2SchemaName NVARCHAR(200),
    Series2QueryName NVARCHAR(200)

    CONSTRAINT PK_QCMetricConfiguration PRIMARY KEY (Id),
    CONSTRAINT FK_QCMetricConfig_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_QCMetricConfig_Name_Container UNIQUE (Name, Container)

);

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Retention Time','Retention Time','targetedms','QCMetric_retentionTime')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Peak Area','Peak Area','targetedms','QCMetric_peakArea')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Full Width at Half Maximum (FWHM)','Full Width at Half Maximum (FWHM)','targetedms','QCMetric_fwhm')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Full Width at Base (FWB)','Full Width at Base (FWB)','targetedms','QCMetric_fwb')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Light/Heavy Ratio','Light/Heavy Ratio','targetedms','QCMetric_lhRatio')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Transition/Precursor Area Ratio','Transition/Precursor Area Ratio','targetedms','QCMetric_transitionPrecursorRatio')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName) VALUES (@rootIdentity, 'Transition/Precursor Areas','Transition Area','targetedms','QCMetric_transitionArea','Precursor Area','targetedms','QCMetric_precursorArea')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Mass Accuracy','Mass Accuracy','targetedms','QCMetric_massAccuracy')



