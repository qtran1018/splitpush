package com.splitpush.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invite_tokens")
public class InviteToken {

    @Id
    @Column(length = 36)
    private String token;

    @ManyToOne
    @JoinColumn(name = "trip_group_id", nullable = false)
    private TripGroup tripGroup;

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }

    public InviteToken() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public TripGroup getTripGroup() { return tripGroup; }
    public void setTripGroup(TripGroup tripGroup) { this.tripGroup = tripGroup; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
