package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
//@BaseWebDriverTest.ClassTimeout(minutes = 25)
public class TargetedMSAuditLogTest extends TargetedMSTest
{
    protected static final String AuditTrail_FILE = "AuditTrail.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSAuditLogTest init = (TargetedMSAuditLogTest) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).setUserPermissions(USER, "Reader");
        importData(AuditTrail_FILE);
    }

    @Test
    public void testAuditLogImported()
    {
        log("Start of test");
        goToProjectHome();
        clickTab("Runs");
        clickAndWait(Locator.linkContainingText(AuditTrail_FILE));

        log("Navigating to the audit log");
        clickAndWait(Locator.tagWithAttribute("a", "data-original-title", "Skyline Audit Log"));
        DataRegionTable auditLog = new DataRegionTable("SkylineAuditLog", getDriver());

        log("Verifying the imported logs");
        assertEquals("Invalid number of audit logs", 9, auditLog.getDataRowCount());
        assertEquals("Start message is incorrect", "Start of audit log for already existing document",
                auditLog.getDataAsText(0, "message_text"));
        assertEquals("End message is incorrect", "Managed results",
                auditLog.getDataAsText(8, "message_text"));

    }
}
