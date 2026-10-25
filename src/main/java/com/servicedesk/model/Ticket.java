package com.servicedesk.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Ticket {
    private final String id;
    private String title;
    private String description;
    private final String createdByUserId;
    private String assignedTechnicianId;
    private Category category;
    private Priority priority;
    private TicketStatus status;
    private String resolutionNotes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public Ticket(String id, String title, String description, String createdByUserId, 
                  Category category, Priority priority) {
        this.id = Objects.requireNonNull(id, "Ticket ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "Creator ID cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.status = TicketStatus.NEW;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { 
        this.title = Objects.requireNonNull(title); 
        this.updatedAt = LocalDateTime.now();
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = Objects.requireNonNull(description);
        this.updatedAt = LocalDateTime.now();
    }
    public String getCreatedByUserId() { return createdByUserId; }
    public String getAssignedTechnicianId() { return assignedTechnicianId; }
    public void setAssignedTechnicianId(String assignedTechnicianId) { 
        this.assignedTechnicianId = assignedTechnicianId;
        this.updatedAt = LocalDateTime.now();
    }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { 
        this.category = Objects.requireNonNull(category);
        this.updatedAt = LocalDateTime.now();
    }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { 
        this.priority = Objects.requireNonNull(priority);
        this.updatedAt = LocalDateTime.now();
    }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { 
        this.status = Objects.requireNonNull(status);
        this.updatedAt = LocalDateTime.now();
    }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { 
        this.resolutionNotes = resolutionNotes;
        this.updatedAt = LocalDateTime.now();
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}