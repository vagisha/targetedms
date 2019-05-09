package org.labkey.targetedms.parser.skyaudit;

public class AuditLogParsingException extends Exception
{
    public AuditLogParsingException(String message){
        super(message);
    }
    public AuditLogParsingException(String message, Exception e){
        super(message, e);
    }
}
