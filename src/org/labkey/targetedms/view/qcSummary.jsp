
<div id="docSummary"></div>
<div id="precursorSummary"></div>

<script type="text/javascript">
    function init()
    {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT '
                + '(SELECT COUNT(DISTINCT ModifiedSequence) FROM targetedms.Precursor) as precursorCount '
                + ',(SELECT COUNT(Id) FROM targetedms.Runs) as docCount '
                + ',(SELECT COUNT(Id) FROM targetedms.SampleFile) as fileCount',
            success: function (data)
            {
                var docCount = data.rows[0].docCount;
                var fileCount = data.rows[0].fileCount;
                var precursorCount = data.rows[0].precursorCount;
                var docSummaryLine = docCount + " Skyline document" + (docCount == 1 ? "" : "s") + " uploaded containing " + fileCount + " sample file";
                if (fileCount != 1)
                    docSummaryLine += "s";
                Ext.get('docSummary').update(docSummaryLine);
                Ext.get('precursorSummary').update(precursorCount + " precursor" + (precursorCount == 1 ? "" : "s") + " tracked");
            },
            failure: function (response)
            {
                Ext.get('docSummary').update("Error: " + response.exception);
            }
        });
    }
    Ext.onReady(init);
</script>