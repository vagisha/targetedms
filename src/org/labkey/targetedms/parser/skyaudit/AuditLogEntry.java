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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.GUID;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class AuditLogEntry
{
    private final BigDecimal _documentFormatVersion;
    private Integer _entryId;
    private Long _versionId;
    private LocalDateTime _createTimestamp;
    private int _timezoneOffsetMinutes;
    private String _userName;
    private String _formatVersion;
    private String _reason;
    private String _extraInfo;
    private String _entryHash;
    private String _parentEntryHash;
    protected GUID _documentGUID;

    byte[] _calculatedHashBytes;

    private static final BigDecimal SEQUENTIAL_LOG_HASH_MIN_VERSION = new BigDecimal("20.21");

    public AuditLogEntry()
    {
        this(null);
    }

    public AuditLogEntry(BigDecimal documentFormatVersion)
    {
        _documentFormatVersion = documentFormatVersion;
    }

    public static AuditLogEntry retrieve(int pEntryId, ViewContext viewContext)
    {
        TargetedMSSchema schema = new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer());
        TableSelector sel =  new TableSelector(schema.getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG_ENTRY), new SimpleFilter(FieldKey.fromParts("EntryId"), pEntryId), null);
        List<AuditLogEntry> results = sel.getArrayList(AuditLogEntry.class);
        // Possible to get more than one match if two documents share an audit history. In this case, we don't care
        // which we use
        return results.isEmpty() ? null : results.get(0);
    }

    public AuditLogTree getTreeEntry(){
        if(_entryId != null)
            return new AuditLogTree(_entryId, _documentGUID, _entryHash, _parentEntryHash, _versionId);
        else
            return null;
    }

    final protected List<AuditLogMessage> _allInfoMessage = new LinkedList<>();

    public List<AuditLogMessage> getAllInfoMessage()
    {
        return _allInfoMessage;
    }

    public Integer getEntryId()
    {
        return _entryId;
    }

    public void setEntryId(Integer entryId)
    {
        _entryId = entryId;
    }

    /** RunId is a synonym for VersionId */
    public Long getRunId()
    {
        return getVersionId();
    }

    public void setRunId(Long runId)
    {
        setVersionId(runId);
    }

    public Long getVersionId()
    {
        return _versionId;
    }

    public void setVersionId(Long versionId)
    {
        _versionId = versionId;
    }

    public GUID getDocumentGUID()
    {
        return _documentGUID;
    }

    public void setDocumentGUID(GUID documentGUID)
    {
        _documentGUID = documentGUID;
    }

    //--------------------- Timestamp access methods -------------------
    //this method is used by the log parser to set the date fields using string from XML file
    public void parseCreateTimestamp(String createTimestampStr) throws DateTimeParseException
    {
        OffsetDateTime odt = OffsetDateTime.parse(createTimestampStr);
        _createTimestamp = odt.toLocalDateTime();
        _timezoneOffsetMinutes = odt.getOffset().getTotalSeconds()/60;
    }

    //accessor for the app to get full timestamp with timezone info
    public OffsetDateTime getOffsetCreateTimestamp(){
        return OffsetDateTime.of(_createTimestamp, ZoneOffset.ofTotalSeconds(_timezoneOffsetMinutes * 60));
    }

    //getters and setters for the persistence layer. These map to the database fields.
    //Note that it stores local time, not UTC
    public void setCreateTimestamp(Date ts){
        _createTimestamp = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.of("UTC"));
    }

    public void setTimezoneOffset(int offsetMinutes){
        _timezoneOffsetMinutes = offsetMinutes;
    }

    public Date getCreateTimestamp()
    {
        return Date.from(_createTimestamp.toInstant(ZoneOffset.UTC));
    }

    //returns timezone offset in minutes
    public int getTimezoneOffset(){
        return _timezoneOffsetMinutes;
    }

    public byte[] getTimestampHashingBytes(){
        OffsetDateTime odt =getOffsetCreateTimestamp();

        //calculate windows file time in UTC
        long fileTime = (odt.atZoneSameInstant(ZoneId.of("UTC")).toEpochSecond() + 11644473600L);
        //convert to a byte array
        byte[] timeBytes = BigInteger.valueOf(fileTime).toByteArray();
        //since toByteArray() is big-endian we need to reverse and pad it to 8 bytes to match the client implementation
        byte[] bytesToHash = {0, 0, 0, 0, 0, 0, 0, 0};
        for(int i = 0; i < timeBytes.length; i++)
            bytesToHash[i] = timeBytes[timeBytes.length - 1 - i];
        return bytesToHash;
    }

    public byte[] getTimezoneHashingBytes()
    {
        DecimalFormat dd = new DecimalFormat("##.#");
        return dd.format(_timezoneOffsetMinutes/60.).getBytes(StandardCharsets.UTF_8);
    }

    //--------------------------------------------------------------------------------


    public String getUserName()
    {
        return _userName;
    }

    public String getFormatVersion()
    {
        return _formatVersion;
    }

    public String getReason()
    {
        return _reason;
    }

    public String getExtraInfo()
    {
        return _extraInfo;
    }

    /** @return the hash from the .skyl XML file */
    public String getEntryHash()
    {
        return _entryHash;
    }

    public String getParentEntryHash()
    {
        return _parentEntryHash;
    }

    public byte[] getHashBytes() {return _calculatedHashBytes;}

    /** @return the Panorama-calculated hash based on the audit entry */
    public String getHashString()
    {
        return new String(Base64.getEncoder().encode(_calculatedHashBytes), StandardCharsets.US_ASCII);
    }

    /***
     * Verifies if entry hash is correct
     * @return Returns true if hash of this entry matches with calculated hash
     */
    public boolean verifyHash()
    {
        String calculatedHash = this.calculateHash();
        return calculatedHash.equals(_entryHash);
    }

    /***
    Calculates entry hash based on the entry data
    */
    public String calculateHash()
    {
        var utf8 = StandardCharsets.UTF_8;
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.update(_userName.getBytes(utf8));
            if (_extraInfo != null)
                digest.update(_extraInfo.getBytes(utf8));
            if (_reason != null)
                digest.update(_reason.getBytes(utf8));

            for (AuditLogMessage msg : _allInfoMessage)
                digest.update(msg.getHashBytes(utf8));

            digest.update(_formatVersion.getBytes(utf8));
            digest.update(getTimestampHashingBytes());
            digest.update(getTimezoneHashingBytes());

            if (_documentFormatVersion == null)
            {
                throw new IllegalStateException("Can't calculate the hash without knowing the document format version");
            }

            if (_parentEntryHash != null && _documentFormatVersion.compareTo(SEQUENTIAL_LOG_HASH_MIN_VERSION) >= 0)
            {
                digest.update(Base64.getDecoder().decode(_parentEntryHash));
            }

            _calculatedHashBytes = digest.digest();

            return new String(Base64.getEncoder().encode(_calculatedHashBytes), StandardCharsets.US_ASCII);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("Couldn't find hash algorithm", e);
        }
    }

    /***
     * Checks if the entry hash can be calculated.
     * @return true if all messages of this entry have expanded English text
     */
    public boolean canBeHashed(AuditLogMessageExpander pExpander){
        if(pExpander.needsExpansion(_extraInfo))
            return false;
        for(AuditLogMessage msg : _allInfoMessage){
            if(msg.getEnText() == null)
                return false;
        }
        return true;
    }
    /***
     * Saves this entry into the database
     * @return this object
     */
    public AuditLogEntry persist(){
        Table.insert(null, TargetedMSManager.getTableInfoSkylineAuditLogEntry(), this);
        for(AuditLogMessage msg : _allInfoMessage)
        {
            msg.setEntryId(_entryId);
            Table.insert(null, TargetedMSManager.getTableInfoSkylineAuditLogMessage(), msg);
        }
        return this;
    }

    /**
     * Updates entry's versionId field in the database. It is the only field that can change,
     * the rest of the entry is immutable.
     * @param pVersionId new versionId value.
     * @return this instance if the update is successful.
     */
    public AuditLogEntry updateVersionId(Long pVersionId){
        this.setVersionId(pVersionId);
        SQLFragment sqlUpdate = new SQLFragment("");

        if(this._entryId == null){  //entryId can be null if this is an existing entry read from a file and not persisted into the database
            //attempting to retrieve entryId using the alternative key of document GUID and entry hash.
            sqlUpdate.append("UPDATE targetedms.AuditLogEntry SET versionId = ? WHERE documentGUID = ? AND entryHash = ?");
            sqlUpdate.addAll(pVersionId, this._documentGUID.toString(), this._entryHash);
        }
        else
        {
            sqlUpdate.append("UPDATE targetedms.AuditLogEntry SET versionId = ? WHERE entryId = ?");
            sqlUpdate.addAll(pVersionId, this._entryId);
        }

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sqlUpdate);
        return this;
    }

    /***
     * Expands tokenized log messages in the entry into readable English text using local resource files.
     * @param pExpander an instance of AuditLogMessageExpander class to perform the expansion.
     * @return this object.
     */
    public AuditLogEntry expandEntry(AuditLogMessageExpander pExpander){
        if(_extraInfo != null)
            setExtraInfo(pExpander.expandLogString(_extraInfo));
        for(AuditLogMessage msg : _allInfoMessage)
            pExpander.expandMessage(msg);

        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder("AuditLogEntry{" +
                "_versionId=" + _versionId +
                ", _createTimestamp=" + _createTimestamp +
                ", _userName='" + _userName + '\'' +
                ", _formatVersion='" + _formatVersion + '\'' +
                ", _reason='" + _reason + '\'' +
                ", _extraInfo='" + _extraInfo + '\'' +
                ", _entryHash='" + _entryHash + '\'' +
                ", _parentEntryHash='" + _parentEntryHash + '\'' +
                ", _documentGUID=" + _documentGUID +
                ", _allInfoMessage=");
        for(AuditLogMessage msg : _allInfoMessage)
        {
            result.append("\n\t");
            result.append(msg.toString());
        }

        return result.toString();
    }

    public void setUserName(String userName)
    {
        _userName = userName;
    }

    public void setFormatVersion(String formatVersion)
    {
        _formatVersion = formatVersion;
    }

    public void setReason(String reason)
    {
        _reason = reason;
    }

    public void setExtraInfo(String extraInfo)
    {
        _extraInfo = extraInfo;
    }

    public void setEntryHash(String entryHash)
    {
        _entryHash = entryHash;
    }

    public void setParentEntryHash(String parentEntryHash)
    {
        _parentEntryHash = parentEntryHash;
    }
}
