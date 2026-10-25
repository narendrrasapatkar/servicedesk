package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final Map<String, List<TicketComment>> ticketComments = new ConcurrentHashMap<>();
    private final AtomicInteger commentIdCounter = new AtomicInteger(5000);

    public WorkflowService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public void assignTicket(String ticketId, String technicianId, String assignedByUserId) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User assigner = getUserOrThrow(assignedByUserId);
        User technician = getUserOrThrow(technicianId);

        // Only HELPDESK_AGENT or ADMIN can assign tickets
        if (assigner.getRole() != Role.HELPDESK_AGENT && assigner.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only Helpdesk Agents or Admins can assign tickets");
        }

        // Assigned user must be an active TECHNICIAN
        if (technician.getRole() != Role.TECHNICIAN || !technician.isActive()) {
            throw new IllegalArgumentException("User " + technicianId + " is not an active technician");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.ASSIGNED)) {
            throw new IllegalStateException("Cannot transition ticket from " + ticket.getStatus() + " to ASSIGNED");
        }

        ticket.setAssignedTechnicianId(technicianId);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticketRepository.save(ticket);

        addComment(ticketId, assignedByUserId, "Ticket assigned to technician: " + technician.getName());
    }

    public void startProgress(String ticketId, String technicianId) {
        Ticket ticket = getTicketOrThrow(ticketId);
        verifyAssignedTechnician(ticket, technicianId);

        if (!ticket.getStatus().canTransitionTo(TicketStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Cannot transition ticket from " + ticket.getStatus() + " to IN_PROGRESS");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        addComment(ticketId, technicianId, "Technician started working on this ticket.");
    }

    public void resolveTicket(String ticketId, String technicianId, String resolutionNotes) {
        Ticket ticket = getTicketOrThrow(ticketId);
        verifyAssignedTechnician(ticket, technicianId);

        if (resolutionNotes == null || resolutionNotes.trim().length() < 10) {
            throw new IllegalArgumentException("Resolution notes must be at least 10 characters long");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.RESOLVED)) {
            throw new IllegalStateException("Cannot transition ticket from " + ticket.getStatus() + " to RESOLVED");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionNotes(resolutionNotes.trim());
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        addComment(ticketId, technicianId, "Ticket resolved. Notes: " + resolutionNotes.trim());
    }

    public void closeTicket(String ticketId, String closingUserId) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User closingUser = getUserOrThrow(closingUserId);

        boolean isCreator = ticket.getCreatedByUserId().equals(closingUserId);
        boolean isAdmin = closingUser.getRole() == Role.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new IllegalStateException("Only the creator or an Admin can confirm ticket closure");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.CLOSED)) {
            throw new IllegalStateException("Cannot transition ticket from " + ticket.getStatus() + " to CLOSED");
        }

        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        addComment(ticketId, closingUserId, "Ticket confirmed closed.");
    }

    public void reopenTicket(String ticketId, String reopeningUserId, String reason) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User reopeningUser = getUserOrThrow(reopeningUserId);

        boolean isCreator = ticket.getCreatedByUserId().equals(reopeningUserId);
        boolean isAdmin = reopeningUser.getRole() == Role.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new IllegalStateException("Only the creator or an Admin can reopen a ticket");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Cannot reopen ticket from status " + ticket.getStatus());
        }

        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A valid reason (minimum 5 chars) is required to reopen a ticket");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        addComment(ticketId, reopeningUserId, "Ticket reopened. Reason: " + reason.trim());
    }

    public TicketComment addComment(String ticketId, String authorUserId, String commentText) {
        getTicketOrThrow(ticketId);
        getUserOrThrow(authorUserId);

        if (commentText == null || commentText.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }

        String commentId = "CMT-" + commentIdCounter.incrementAndGet();
        TicketComment comment = new TicketComment(commentId, ticketId, authorUserId, commentText.trim());
        ticketComments.computeIfAbsent(ticketId, k -> new ArrayList<>()).add(comment);
        return comment;
    }

    public List<TicketComment> getCommentsForTicket(String ticketId) {
        getTicketOrThrow(ticketId);
        return new ArrayList<>(ticketComments.getOrDefault(ticketId, Collections.emptyList()));
    }

    private Ticket getTicketOrThrow(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void verifyAssignedTechnician(Ticket ticket, String technicianId) {
        if (ticket.getAssignedTechnicianId() == null || !ticket.getAssignedTechnicianId().equals(technicianId)) {
            throw new IllegalStateException("Only the assigned technician (" + ticket.getAssignedTechnicianId() + ") can update progress/resolve");
        }
    }
}