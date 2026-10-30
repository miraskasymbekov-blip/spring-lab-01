package kz.iitu.springlab.tx;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    protected AuditRecord() { }

    public AuditRecord(String message) {
        this.message = message;
    }

    public Long getId() { return id; }

    public String getMessage() { return message; }
}
