package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Ticket createTicket(String title, String description, String createdByUserId, 
                               Category category, Priority priority) {
        if (title == null || title.trim().length() < 5) {
            throw new IllegalArgumentException("Ticket title must be at least 5 characters long");
        }
        if (description == null || description.trim().length() < 10) {
            throw new IllegalArgumentException("Ticket description must be at least 10 characters long");
        }

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found: " + createdByUserId));

        if (!creator.isActive()) {
            throw new IllegalStateException("Inactive users cannot create support tickets");
        }

        String ticketId = "TCK-" + idCounter.incrementAndGet();
        Ticket ticket = new Ticket(ticketId, title.trim(), description.trim(), 
                                   createdByUserId, category, priority);
        return ticketRepository.save(ticket);
    }

    public Optional<Ticket> getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsByUser(String userId) {
        return ticketRepository.findByCreator(userId);
    }

    public void cancelTicket(String ticketId, String requestingUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestingUserId));

        // Only the ticket creator or an ADMIN can cancel the ticket
        boolean isCreator = ticket.getCreatedByUserId().equals(requestingUserId);
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new IllegalStateException("Only ticket creators or Admins can cancel this ticket");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel ticket in status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }
}