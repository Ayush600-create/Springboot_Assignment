package com.campusguard.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "item_private_details")
public class ItemPrivateDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "report_id", unique = true)
    private LostFoundReport report;

    @Lob
    @Column(nullable = false)
    private byte[] ciphertext;

    @Column(nullable = false, length = 16)
    private byte[] nonce;

    public Long getId() { return id; }
    public LostFoundReport getReport() { return report; }
    public void setReport(LostFoundReport report) { this.report = report; }
    public byte[] getCiphertext() { return ciphertext; }
    public void setCiphertext(byte[] ciphertext) { this.ciphertext = ciphertext; }
    public byte[] getNonce() { return nonce; }
    public void setNonce(byte[] nonce) { this.nonce = nonce; }
}
