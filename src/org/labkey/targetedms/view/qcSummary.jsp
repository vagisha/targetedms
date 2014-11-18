
<div id="docSummary"></div>
<div id="peptideSummary"></div>

<script type="text/javascript">
    function init()
    {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT '
                + '(SELECT COUNT(DISTINCT Sequence) FROM targetedms.Peptide) as peptideCount '
                + ',(SELECT COUNT(Id) FROM targetedms.Runs) as docCount '
                + ',(SELECT COUNT(Id) FROM targetedms.SampleFile) as fileCount',
            success: function (data)
            {
                var docCount = data.rows[0].docCount;
                var fileCount = data.rows[0].fileCount;
                var peptideCount = data.rows[0].peptideCount;
                var docSummaryLine = docCount + " Skyline document" + (docCount == 1 ? "" : "s") + " uploaded containing " + fileCount + " sample file";
                if (fileCount != 1)
                    docSummaryLine += "s";
                Ext.get('docSummary').update(docSummaryLine);
                Ext.get('peptideSummary').update(peptideCount + " peptide" + (peptideCount == 1 ? "" : "s") + " tracked");
            },
            failure: function (response)
            {
                Ext.get('docSummary').update("Error: " + response.exception);
            }
        });
    }
    Ext.onReady(init);
</script>