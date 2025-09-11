package gov.dhs.cbp.reference.loader.genc.model;

/**
 * Model representing GENC data from source
 */
public class GencData {
    private String genc3Code;
    private String genc2Code;
    private String gencNumeric;
    private String name;
    private String fullName;
    private String status;
    private String entityType;
    private String capital;
    private String region;
    private String subRegion;
    private String latitude;
    private String longitude;
    private String politicalStatus;
    private String parentCode;
    private String sovereignty;
    private String localShortName;
    private String localLongName;
    private String formerCodes;
    private String updateDate;
    private String updateType;
    private String updateDescription;
    private String effectiveDate;
    private String expirationDate;
    private String replacementCodes;
    private String sourceDocument;
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
    
    public String getLatitude() {
        return latitude;
    }
    
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    
    public String getLongitude() {
        return longitude;
    }
    
    public void setLongitude(String longitude) {
        this.longitude = longitude;
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
    
    public String getSourceDate() {
        return sourceDate;
    }
    
    public void setSourceDate(String sourceDate) {
        this.sourceDate = sourceDate;
    }
    
    @Override
    public String toString() {
        return "GencData{" +
                "genc3Code='" + genc3Code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}