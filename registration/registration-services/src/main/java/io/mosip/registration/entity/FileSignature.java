package io.mosip.registration.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(schema = "reg", name = "file_signature")
@Getter
@Setter
public class FileSignature {

    @Id
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "signature")
    private String signature;

    @Column(name = "content_length")
    private Integer contentLength;

    @Column(name = "encrypted")
    private Boolean encrypted;
}
