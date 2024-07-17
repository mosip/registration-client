package io.mosip.registration.entity;


import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "process_spec", schema = "reg")
public class ProcessSpec {

    @Id
    @Column(name = "type")
    private String type;

    @Column(name = "id")
    private String id;

    @Column(name = "id_version")
    private double idVersion;

    @Column(name = "order_num")
    private int orderNum;

    @Column(name = "is_sub_process")
    private boolean isSubProcess;

    @Column(name= "flow")
    private String flow;

    @Column(name = "is_active")
    private boolean isActive;
}
