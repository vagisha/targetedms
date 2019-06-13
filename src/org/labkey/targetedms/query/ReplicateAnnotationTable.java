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
package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.ReplicateAnnotation;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by vsharma on 11/3/2016.
 */
public class ReplicateAnnotationTable extends TargetedMSTable
{
    private static final String SOURCE_COL = "Source";

    public ReplicateAnnotationTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE_ANNOTATION),
              schema, cf, TargetedMSSchema.ContainerJoinType.ReplicateFK);

        var sourceCol = getMutableColumn(FieldKey.fromParts(SOURCE_COL));
        sourceCol.setReadOnly(true);
        sourceCol.setDefaultValue(ReplicateAnnotation.SOURCE_USER);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        if(TargetedMSManager.getFolderType(getContainer()) == TargetedMSModule.FolderType.QC)
        {
            // Allow edits, deletes and inserts only in QC folder types
            return getContainer().hasPermission(user, perm);
        }
        return super.hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            public Map<String,Object> insertRow(User user, Container container, Map<String, Object> row)
                    throws DuplicateKeyException, ValidationException, QueryUpdateServiceException, SQLException
            {
                String source = (String) row.get(SOURCE_COL);
                if(source == null)
                {
                    // Add a value for the "Source" column
                    row.put(SOURCE_COL, ReplicateAnnotation.SOURCE_USER);
                }
                else if(!ReplicateAnnotation.isValidSource(source))
                {
                    throw new ValidationException("Invalid annotation source: " + source);
                }
                else if(ReplicateAnnotation.isSourceSkyline(source))
                {
                    throw new ValidationException("Cannot insert annotations where the source is Skyline.");
                }
                return super.insertRow(user, container, row);
            }

            @Override
            public Map<String,Object> updateRow(User user, Container container, Map<String, Object> row, @NotNull Map<String, Object> oldRow)
                    throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
            {
                Object id = oldRow.get("Id");
                if (id != null)
                {
                    try
                    {
                        int annotationId = Integer.parseInt(id.toString());
                        // Check that the requested annotation belongs to a run in the container.
                        ReplicateAnnotation annotation = ReplicateManager.getReplicateAnnotation(annotationId, container);
                        if(annotation == null)
                        {
                            throw new ValidationException("Annotation not found in container.");
                        }

                        if (ReplicateAnnotation.isSourceSkyline(annotation.getSource()))
                        {
                            // Throw an exception if annotation "Source" is "Skyline"
                            throw new ValidationException("Cannot edit annotation imported from Skyline document - " + annotation.getDisplayName());
                        }

                        String newSrc = (String)row.get(SOURCE_COL);
                        if(newSrc != null && !ReplicateAnnotation.SOURCE_USER.equals(newSrc))
                        {
                            throw new ValidationException("Annotation source can only be \"User\".");
                        }
                    }
                    catch (NumberFormatException ignored){} // deleteRow in DefaultQueryUpdateService will check for valid primary key value
                }

                return super.updateRow(user, container, row, oldRow);
            }

            @Override
            public Map<String,Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap)
                    throws QueryUpdateServiceException, SQLException, InvalidKeyException
            {
                Object id = oldRowMap.get("Id");
                if (id != null)
                {
                    try
                    {
                        int annotationId = Integer.parseInt(id.toString());
                        // Check that the requested annotation belongs to a run in the container.
                        ReplicateAnnotation annotation = ReplicateManager.getReplicateAnnotation(annotationId, container);
                        if(annotation == null)
                        {
                            throw new QueryUpdateServiceException("Annotation not found in container.");
                        }
                        if(ReplicateAnnotation.isSourceSkyline(annotation.getSource()))
                        {
                            // Throw an exception if "Source" is "Skyline"
                            throw new QueryUpdateServiceException("Cannot delete annotation imported from Skyline document - " + annotation.getDisplayName());
                        }
                    }
                    catch (NumberFormatException ignored){} // deleteRow in DefaultQueryUpdateService will check for valid primary key value
                }
                return super.deleteRow(user, container, oldRowMap);
            }
        };
    }
}
