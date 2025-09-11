package gov.dhs.cbp.reference.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
    
    @JsonProperty("type")
    private String type = "about:blank";
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("detail")
    private String detail;
    
    @JsonProperty("instance")
    private String instance;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("traceId")
    private String traceId;
    
    @JsonProperty("errors")
    private Map<String, String> errors;
    
    public ProblemDetail() {
    }
    
    public ProblemDetail(String title, Integer status, String detail) {
        this.title = title;
        this.status = status;
        this.detail = detail;
    }
    
    public static ProblemDetail badRequest(String detail) {
        return new ProblemDetail("Bad Request", 400, detail);
    }
    
    public static ProblemDetail notFound(String detail) {
        return new ProblemDetail("Not Found", 404, detail);
    }
    
    public static ProblemDetail conflict(String detail) {
        return new ProblemDetail("Conflict", 409, detail);
    }
    
    public static ProblemDetail internalServerError(String detail) {
        return new ProblemDetail("Internal Server Error", 500, detail);
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
    }
    
    public String getInstance() {
        return instance;
    }
    
    public void setInstance(String instance) {
        this.instance = instance;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
    
    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}