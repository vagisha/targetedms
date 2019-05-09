package org.labkey.targetedms.parser.skyaudit;


public class AuditLogException extends Exception
{
    public AuditLogException(String message){
        super(message);
    }
    public AuditLogException(String message, Throwable e){
        super(message, e);
    }
}
