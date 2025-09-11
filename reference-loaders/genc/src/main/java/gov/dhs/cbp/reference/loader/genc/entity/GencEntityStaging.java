package gov.dhs.cbp.reference.loader.genc.entity;

import gov.dhs.cbp.reference.loader.common.StagingEntity;
import jakarta.persistence.*;

/**
 * Staging entity for GENC data
 */
@Entity
@Table(name = "genc_staging")
public class GencEntityStaging extends StagingEntity {
    
    @Column(name = "genc3_code", length = 3)
    private String genc3Code;
    
    @Column(name = "genc2_code", length = 2)
    private String genc2Code;
    
    @Column(name = "genc_numeric", length = 3)
    private String gencNumeric;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "capital")
    private String capital;
    
    @Column(name = "region")
    private String region;
    
    @Column(name = "sub_region")
    private String subRegion;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    // Additional GENC specific fields
    @Column(name = "entity_name")
    private String entityName;
    
    @Column(name = "char2_code", length = 2)
    private String char2Code;
    
    @Column(name = "char3_code", length = 3)
    private String char3Code;
    
    @Column(name = "numeric_code", length = 3)
    private String numericCode;
    
    @Column(name = "genc_status")
    private String gencStatus;
    
    @Column(name = "political_status")
    private String politicalStatus;
    
    @Column(name = "parent_code")
    private String parentCode;
    
    @Column(name = "sovereignty")
    private String sovereignty;
    
    @Column(name = "local_short_name")
    private String localShortName;
    
    @Column(name = "local_long_name")
    private String localLongName;
    
    @Column(name = "former_codes")
    private String formerCodes;
    
    @Column(name = "update_date")
    private String updateDate;
    
    @Column(name = "update_type")
    private String updateType;
    
    @Column(name = "update_description", columnDefinition = "TEXT")
    private String updateDescription;
    
    @Column(name = "effective_date")
    private String effectiveDate;
    
    @Column(name = "expiration_date")
    private String expirationDate;
    
    @Column(name = "replacement_codes")
    private String replacementCodes;
    
    @Column(name = "source_document")
    private String sourceDocument;
    
    @Column(name = "subregion")
    private String subregion;
    
    @Column(name = "source_file")
    private String sourceFile;
    
    @Column(name = "source_date")
    private String sourceDate;
    
    // Getters and setters
    
    public String getGenc3Code() {
        return genc3Code;
    }
    
    public void setGenc3Code(String genc3Code) {
        this.genc3Code = genc3Code;
    }
    
    public String getGenc2Code() {
        return genc2Code;
    }
    
    public void setGenc2Code(String genc2Code) {
        this.genc2Code = genc2Code;
    }
    
    public String getGencNumeric() {
        return gencNumeric;
    }
    
    public void setGencNumeric(String gencNumeric) {
        this.gencNumeric = gencNumeric;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getCapital() {
        return capital;
    }
    
    public void setCapital(String capital) {
        this.capital = capital;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getSubRegion() {
        return subRegion;
    }
    
    public void setSubRegion(String subRegion) {
        this.subRegion = subRegion;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String getChar2Code() {
        return char2Code;
    }
    
    public void setChar2Code(String char2Code) {
        this.char2Code = char2Code;
    }
    
    public String getChar3Code() {
        return char3Code;
    }
    
    public void setChar3Code(String char3Code) {
        this.char3Code = char3Code;
    }
    
    public String getNumericCode() {
        return numericCode;
    }
    
    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }
    
    public String getGencStatus() {
        return gencStatus;
    }
    
    public void setGencStatus(String gencStatus) {
        this.gencStatus = gencStatus;
    }
    
    public String getPoliticalStatus() {
        return politicalStatus;
    }
    
    public void setPoliticalStatus(String politicalStatus) {
        this.politicalStatus = politicalStatus;
    }
    
    public String getParentCode() {
        return parentCode;
    }
    
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }
    
    public String getSovereignty() {
        return sovereignty;
    }
    
    public void setSovereignty(String sovereignty) {
        this.sovereignty = sovereignty;
    }
    
    public String getLocalShortName() {
        return localShortName;
    }
    
    public void setLocalShortName(String localShortName) {
        this.localShortName = localShortName;
    }
    
    public String getLocalLongName() {
        return localLongName;
    }
    
    public void setLocalLongName(String localLongName) {
        this.localLongName = localLongName;
    }
    
    public String getFormerCodes() {
        return formerCodes;
    }
    
    public void setFormerCodes(String formerCodes) {
        this.formerCodes = formerCodes;
    }
    
    public String getUpdateDate() {
        return updateDate;
    }
    
    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }
    
    public String getUpdateType() {
        return updateType;
    }
    
    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }
    
    public String getUpdateDescription() {
        return updateDescription;
    }
    
    public void setUpdateDescription(String updateDescription) {
        this.updateDescription = updateDescription;
    }
    
    public String getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public String getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public String getReplacementCodes() {
        return replacementCodes;
    }
    
    public void setReplacementCodes(String replacementCodes) {
        this.replacementCodes = replacementCodes;
    }
    
    public String getSourceDocument() {
        return sourceDocument;
    }
    
    public void setSourceDocument(String sourceDocument) {
        this.sourceDocument = sourceDocument;
    }
    
    public String getSubregion() {
        return subregion;
    }
    
    public void setSubregion(String subregion) {
        this.subregion = subregion;
    }
    
    public String getSourceFile() {
        return sourceFile;
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    public String getSourceDate() {
        return sourceDate;
    }
    
    public void setSourceDate(String sourceDate) {
        this.sourceDate = sourceDate;
    }
}