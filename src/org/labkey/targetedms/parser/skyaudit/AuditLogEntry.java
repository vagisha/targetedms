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

import org.labkey.api.data.Table;
import org.labkey.api.util.GUID;
import org.labkey.targetedms.TargetedMSManager;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;

public class AuditLogEntry
{
    private Integer _entryId;
    private Integer _versionId;
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

    public AuditLogTree getTreeEntry(){
        return new AuditLogTree(_entryId, _documentGUID, _entryHash, _parentEntryHash);
    }

    protected List<AuditLogMessage> _allInfoMessage = new LinkedList<AuditLogMessage>();

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


    public Integer getVersionId()
    {
        return _versionId;
    }

    public void setVersionId(Integer versionId)
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

    public byte[] getTimezoneHashingBytes(){
        DecimalFormat dd = new DecimalFormat("##.#");
        byte[] tzBytes = dd.format(_timezoneOffsetMinutes/60.).getBytes(Charset.forName("UTF8"));
        return tzBytes;
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

    public String getEntryHash()
    {
        return _entryHash;
    }

    public String getParentEntryHash()
    {
        return _parentEntryHash;
    }

    public byte[] getHashBytes() {return _calculatedHashBytes;}
    public String getHashString() {
        return new String(Base64.getEncoder().encode(_calculatedHashBytes), Charset.forName("US-ASCII"));
    }

    /***
     * Returns true if hash of this entry matches with calculated hash
     * @return
     */
    public boolean verifyHash() throws NoSuchAlgorithmException{
            return this.calculateHash().equals(_entryHash);
    }

    /***
    Calculates entry hash based on the entry data
    */
    public String calculateHash() throws NoSuchAlgorithmException
    {
        var utf8 = Charset.forName("UTF8");
        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA1");

        digest.update(_userName.getBytes(utf8));
        if(_extraInfo != null)
            digest.update(_extraInfo.getBytes(utf8));
//TODO: Make sure reason is hashed on the client side.
//        if(_reason != null)
//            digest.update(_reason.getBytes(utf8));

        for(AuditLogMessage msg : _allInfoMessage)
            digest.update(msg.getHashBytes(utf8));

        digest.update(_formatVersion.getBytes(utf8));
        digest.update(getTimestampHashingBytes());
        digest.update(getTimezoneHashingBytes());

        _calculatedHashBytes = digest.digest();

        String result = new String(Base64.getEncoder().encode(_calculatedHashBytes), Charset.forName("US-ASCII"));

        return result;
    }

    /***
     * Returns true if all messages of this entry have expanded English text
     * @return
     */
    public boolean canBeHashed(AuditLogMessageExpander p_expander){
        if(p_expander.needsExpansion(_extraInfo))
            return false;
        for(AuditLogMessage msg : _allInfoMessage){
            if(msg.getEnText() == null)
                return false;
        }
        return true;
    }
    /***
     * Saves this entry into the database
     * @return
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

    public AuditLogEntry expandEntry(AuditLogMessageExpander p_expander){
        if(_extraInfo != null)
            setExtraInfo(p_expander.expandLogString(_extraInfo));
        for(int i = 0; i < _allInfoMessage.size(); i++)
            p_expander.expandMessage(_allInfoMessage.get(i));

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
            result.append("\n\t" + msg.toString());

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
