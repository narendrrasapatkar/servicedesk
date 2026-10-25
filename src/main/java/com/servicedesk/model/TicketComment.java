package com.servicedesk.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class TicketComment {
    private final String id;
    private final String ticketId;
    private final String authorUserId;
    private final String commentText;
    private final LocalDateTime timestamp;

    public TicketComment(String id, String ticketId, String authorUserId, String commentText) {
        this.id = Objects.requireNonNull(id, "Comment ID cannot be null");
        this.ticketId = Objects.requireNonNull(ticketId, "Ticket ID cannot be null");
        this.authorUserId = Objects.requireNonNull(authorUserId, "Author User ID cannot be null");
        this.commentText = Objects.requireNonNull(commentText, "Comment text cannot be null");
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getTicketId() { return ticketId; }
    public String getAuthorUserId() { return authorUserId; }
    public String getCommentText() { return commentText; }
    public LocalDateTime getTimestamp() { return timestamp; }
}