package com.servicedesk.service;

import com.servicedesk.model.Category;
import com.servicedesk.model.Priority;
import com.servicedesk.model.Role;
import com.servicedesk.model.Ticket;
import com.servicedesk.model.TicketStatus;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportingService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public ReportingService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> getDashboardMetrics() {
        List<Ticket> allTickets = ticketRepository.findAll();

        long total = allTickets.size();
        long openCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.NEW).count();
        long assignedCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.ASSIGNED).count();
        long inProgressCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count();
        long resolvedCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED).count();
        long closedCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.CLOSED).count();
        long cancelledCount = allTickets.stream().filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();

        Map<String, Long> statusBreakdown = new HashMap<>();
        statusBreakdown.put("NEW", openCount);
        statusBreakdown.put("ASSIGNED", assignedCount);
        statusBreakdown.put("IN_PROGRESS", inProgressCount);
        statusBreakdown.put("RESOLVED", resolvedCount);
        statusBreakdown.put("CLOSED", closedCount);
        statusBreakdown.put("CANCELLED", cancelledCount);

        Map<String, Long> priorityBreakdown = new HashMap<>();
        for (Priority p : Priority.values()) {
            priorityBreakdown.put(p.name(), allTickets.stream().filter(t -> t.getPriority() == p).count());
        }

        Map<String, Long> categoryBreakdown = new HashMap<>();
        for (Category c : Category.values()) {
            categoryBreakdown.put(c.name(), allTickets.stream().filter(t -> t.getCategory() == c).count());
        }

        long activeTechs = userRepository.findByRole(Role.TECHNICIAN).stream().filter(u -> u.isActive()).count();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTickets", total);
        metrics.put("activeTickets", total - (closedCount + cancelledCount));
        metrics.put("resolvedTickets", resolvedCount);
        metrics.put("closedTickets", closedCount);
        metrics.put("activeTechnicians", activeTechs);
        metrics.put("statusCounts", statusBreakdown);
        metrics.put("priorityCounts", priorityBreakdown);
        metrics.put("categoryCounts", categoryBreakdown);

        return metrics;
    }

    public Map<String, Long> getTechnicianWorkload() {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getAssignedTechnicianId() != null && t.getStatus() != TicketStatus.CLOSED)
                .collect(Collectors.groupingBy(Ticket::getAssignedTechnicianId, Collectors.counting()));
    }
}