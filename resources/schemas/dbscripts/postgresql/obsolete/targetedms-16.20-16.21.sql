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

CREATE TABLE targetedms.QCMetricConfiguration
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    Container ENTITYID NOT NULL,
    Name VARCHAR(200) NOT NULL ,
    Series1Label VARCHAR(200) NOT NULL ,
    Series1SchemaName VARCHAR(200) NOT NULL ,
    Series1QueryName VARCHAR(200) NOT NULL ,
    Series2Label VARCHAR(200),
    Series2SchemaName VARCHAR(200),
    Series2QueryName VARCHAR(200),

    CONSTRAINT PK_QCMetricConfiguration PRIMARY KEY (Id),
    CONSTRAINT FK_QCMetricConfig_Containers FOREIGN KEY (Container) REFERENCES core.Containers (EntityId),
    CONSTRAINT UQ_QCMetricConfig_Name_Container UNIQUE (Name, Container)
);


WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName) VALUES
    ((select theIdentity from rootIdentity), 'Retention Time','Retention Time','targetedms','QCMetric_retentionTime',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Peak Area','Peak Area','targetedms','QCMetric_peakArea',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Full Width at Half Maximum (FWHM)','Full Width at Half Maximum (FWHM)','targetedms','QCMetric_fwhm',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Full Width at Base (FWB)','Full Width at Base (FWB)','targetedms','QCMetric_fwb',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Light/Heavy Ratio','Light/Heavy Ratio','targetedms','QCMetric_lhRatio',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Transition/Precursor Area Ratio','Transition/Precursor Area Ratio','targetedms','QCMetric_transitionPrecursorRatio',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Transition/Precursor Areas','Transition Area','targetedms','QCMetric_transitionArea','Precursor Area','targetedms','QCMetric_precursorArea'),
    ((select theIdentity from rootIdentity), 'Mass Accuracy','Mass Accuracy','targetedms','QCMetric_massAccuracy',NULL , NULL , NULL );
