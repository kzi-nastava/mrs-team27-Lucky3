package com.example.mobile.models;

/**
 * Request DTO for reporting a ride inconsistency.
 * Matches backend's InconsistencyRequest DTO.
 */
public class InconsistencyRequest {
    private String remark;

    public InconsistencyRequest() {}

    public InconsistencyRequest(String remark) {
        this.remark = remark;
    }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
