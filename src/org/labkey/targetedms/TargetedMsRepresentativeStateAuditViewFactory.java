package org.labkey.targetedms;

import org.labkey.api.audit.AuditLogEvent;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.SimpleAuditViewFactory;
import org.labkey.api.audit.query.AuditLogQueryView;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/14/13
 * Time: 4:01 PM
 *
 *
 * Event field documentation:
 *
 * created - Timestamp
 * createdBy - User who triggered the representative data change
 * impersonatedBy - user who was impersonating the user (or null)
 * projectId - the project id
 * containerId - the container id
 * comment
 *
 *
 */
public class TargetedMsRepresentativeStateAuditViewFactory extends SimpleAuditViewFactory
{
    public static final String AUDIT_EVENT_TYPE = "TargetedMSRepresentativeStateEvent";

    private static final TargetedMsRepresentativeStateAuditViewFactory _instance = new TargetedMsRepresentativeStateAuditViewFactory();

    public static TargetedMsRepresentativeStateAuditViewFactory getInstance()
    {
        return _instance;
    }

    private TargetedMsRepresentativeStateAuditViewFactory() {}

    @Override
    public String getEventType()
    {
        return AUDIT_EVENT_TYPE;
    }

    public String getName()
    {
        return "TargetedMS Representative State Event";
    }

    @Override
    public QueryView createDefaultQueryView(ViewContext context)
    {
        AuditLogQueryView view = AuditLogService.get().createQueryView(context, null, getEventType());
        view.setSort(new Sort("-Date"));

        return view;
    }

    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> columns = new ArrayList<FieldKey>();

        columns.add(FieldKey.fromParts("Date"));
        columns.add(FieldKey.fromParts("CreatedBy"));
        columns.add(FieldKey.fromParts("ImpersonatedBy"));
        columns.add(FieldKey.fromParts("ProjectId"));
        columns.add(FieldKey.fromParts("ContainerId"));
        columns.add(FieldKey.fromParts("Comment"));

        return columns;
    }

    public static void addAuditEntry(Container container, User user, String comment)
    {
        AuditLogEvent event = new AuditLogEvent();
        event.setCreated(new Date());
        event.setCreatedBy(user);
        event.setEventType(AUDIT_EVENT_TYPE);
        event.setProjectId(container.getProject().getId());
        event.setContainerId(container.getId());
        event.setComment(comment);
        AuditLogService.get().addEvent(event);
    }
}
