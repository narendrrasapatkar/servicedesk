package com.servicedesk.repository;

import com.servicedesk.model.Category;
import com.servicedesk.model.Priority;
import com.servicedesk.model.Ticket;
import com.servicedesk.model.TicketStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TicketRepository {
    private final Map<String, Ticket> ticketStorage = new ConcurrentHashMap<>();

    public Ticket save(Ticket ticket) {
        ticketStorage.put(ticket.getId(), ticket);
        return ticket;
    }

    public Optional<Ticket> findById(String id) {
        return Optional.ofNullable(ticketStorage.get(id));
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(ticketStorage.values());
    }

    public List<Ticket> findByCreator(String userId) {
        return ticketStorage.values().stream()
                .filter(t -> t.getCreatedByUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Ticket> findByTechnician(String technicianId) {
        return ticketStorage.values().stream()
                .filter(t -> technicianId.equals(t.getAssignedTechnicianId()))
                .collect(Collectors.toList());
    }

    public List<Ticket> findByStatus(TicketStatus status) {
        return ticketStorage.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Ticket> findByPriority(Priority priority) {
        return ticketStorage.values().stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public List<Ticket> findByCategory(Category category) {
        return ticketStorage.values().stream()
                .filter(t -> t.getCategory() == category)
                .collect(Collectors.toList());
    }

    public void clear() {
        ticketStorage.clear();
    }
}