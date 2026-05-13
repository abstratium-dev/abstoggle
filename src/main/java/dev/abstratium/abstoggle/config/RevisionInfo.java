package dev.abstratium.abstoggle.config;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import io.quarkus.security.identity.SecurityIdentity;

import java.time.Instant;

/**
 * Custom Envers revision entity that captures the username of the user making changes.
 */
@Entity
@RevisionEntity(RevisionInfo.RevisionInfoListener.class)
@Table(name = "REVINFO")
public class RevisionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long rev;

    @RevisionTimestamp
    private Long revtstmp;

    private String username;

    public Long getRev() {
        return rev;
    }

    public void setRev(Long rev) {
        this.rev = rev;
    }

    public Long getRevtstmp() {
        return revtstmp;
    }

    public void setRevtstmp(Long revtstmp) {
        this.revtstmp = revtstmp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * RevisionListener that captures the current username from the security context.
     */
    public static class RevisionInfoListener implements RevisionListener {

        @Override
        @ActivateRequestContext
        public void newRevision(Object revisionEntity) {
            RevisionInfo revisionInfo = (RevisionInfo) revisionEntity;
            revisionInfo.setRevtstmp(Instant.now().toEpochMilli());
            revisionInfo.setUsername(getCurrentUsername());
        }

        private String getCurrentUsername() {
            try {
                SecurityIdentity identity = CDI.current().select(SecurityIdentity.class).get();
                if (identity != null && identity.getPrincipal() != null) {
                    return identity.getPrincipal().getName();
                }
            } catch (Exception e) {
                // No security context available (e.g., during startup/data loading)
            }
            return "system";
        }
    }
}
