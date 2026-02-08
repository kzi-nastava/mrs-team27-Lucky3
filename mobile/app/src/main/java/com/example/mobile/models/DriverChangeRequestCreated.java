package com.example.mobile.models;

public class DriverChangeRequestCreated {
    public Long driverId;
    public Long changeRequestId;
    public DriverChangeStatus status;

    public DriverChangeRequestCreated(){

    }
    public DriverChangeRequestCreated(long driverId, long changeRequestId, DriverChangeStatus status){
        this.status = status;
        this.driverId = driverId;
        this.changeRequestId = changeRequestId;
    }

    public DriverChangeStatus getStatus(){
        return status;
    }
    public void setStatus(DriverChangeStatus status){
        this.status = status;
    }
    public Long getDriverId(){
        return driverId;
    }
    public void setDriverId(Long id){
        driverId = id;
    }
    public Long getChangeRequestId(){
        return changeRequestId;
    }
    public void setChangeRequestId(Long id){
        changeRequestId = id;
    }

}
