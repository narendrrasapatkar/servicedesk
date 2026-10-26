package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowServiceTest {

    private TicketRepository ticketRepository;
    private UserRepository userRepository;
    private TicketService ticketService;
    private WorkflowService workflowService;

    private User employee;
    private User agent;
    private User technician;
    private User otherTechnician;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        userRepository = new UserRepository();
        ticketService = new TicketService(ticketRepository, userRepository);
        workflowService = new WorkflowService(ticketRepository, userRepository);

        employee = userRepository.save(new User("EMP-1", "John Doe", "john@corp.com", Role.EMPLOYEE, "HR"));
        agent = userRepository.save(new User("AGT-1", "Sam Agent", "sam@corp.com", Role.HELPDESK_AGENT, "IT"));
        technician = userRepository.save(new User("TCH-1", "Alex Tech", "alex@corp.com", Role.TECHNICIAN, "Hardware"));
        otherTechnician = userRepository.save(new User("TCH-2", "Bob Tech", "bob@corp.com", Role.TECHNICIAN, "Network"));
        userRepository.save(new User("ADM-1", "Clara Admin", "clara@corp.com", Role.ADMIN, "IT"));

        ticket = ticketService.createTicket(
                "Laptop screen damaged",
                "Flickering colored lines across the screen",
                employee.getId(),
                Category.HARDWARE,
                Priority.HIGH
        );
    }

    @Test
    @DisplayName("Should successfully assign a ticket to an active technician by helpdesk agent")
    void shouldAssignTicketSuccessfully() {
        workflowService.assignTicket(ticket.getId(), technician.getId(), agent.getId());

        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());
        assertEquals(technician.getId(), ticket.getAssignedTechnicianId());

        List<TicketComment> comments = workflowService.getCommentsForTicket(ticket.getId());
        assertEquals(1, comments.size());
        assertTrue(comments.getFirst().getCommentText().contains("Alex Tech"));
    }

    @Test
    @DisplayName("Should reject ticket assignment initiated by standard employee")
    void shouldRejectAssignmentByStandardEmployee() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                workflowService.assignTicket(ticket.getId(), technician.getId(), employee.getId())
        );

        assertTrue(ex.getMessage().contains("Only Helpdesk Agents or Admins"));
    }

    @Test
    @DisplayName("Should reject assigning ticket to a user who is not a technician")
    void shouldRejectAssignmentToNonTechnician() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                workflowService.assignTicket(ticket.getId(), employee.getId(), agent.getId())
        );

        assertTrue(ex.getMessage().contains("not an active technician"));
    }

    @Test
    @DisplayName("Should enforce workflow lifecycle: NEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED")
    void shouldFollowStandardLifecycleSuccessfully() {
        workflowService.assignTicket(ticket.getId(), technician.getId(), agent.getId());
        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());

        workflowService.startProgress(ticket.getId(), technician.getId());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());

        String notes = "Replaced screen ribbon connector and verified display.";
        workflowService.resolveTicket(ticket.getId(), technician.getId(), notes);
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals(notes, ticket.getResolutionNotes());
        assertNotNull(ticket.getResolvedAt());

        workflowService.closeTicket(ticket.getId(), employee.getId());
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
    }

    @Test
    @DisplayName("Should prevent unassigned technician from resolving ticket")
    void shouldPreventUnauthorizedTechnicianFromResolving() {
        workflowService.assignTicket(ticket.getId(), technician.getId(), agent.getId());
        workflowService.startProgress(ticket.getId(), technician.getId());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                workflowService.resolveTicket(ticket.getId(), otherTechnician.getId(), "Resolved without permission")
        );

        assertTrue(ex.getMessage().contains("Only the assigned technician"));
    }

    @Test
    @DisplayName("Should reject ticket resolution with short or missing notes (< 10 chars)")
    void shouldRejectShortResolutionNotes() {
        workflowService.assignTicket(ticket.getId(), technician.getId(), agent.getId());
        workflowService.startProgress(ticket.getId(), technician.getId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                workflowService.resolveTicket(ticket.getId(), technician.getId(), "Done")
        );

        assertTrue(ex.getMessage().contains("at least 10 characters"));
    }

    @Test
    @DisplayName("Should allow creator to reopen a resolved ticket with a valid reason")
    void shouldAllowCreatorToReopenTicket() {
        workflowService.assignTicket(ticket.getId(), technician.getId(), agent.getId());
        workflowService.startProgress(ticket.getId(), technician.getId());
        workflowService.resolveTicket(ticket.getId(), technician.getId(), "Replaced screen ribbon connector.");

        workflowService.reopenTicket(ticket.getId(), employee.getId(), "Issue reoccurred after rebooting");

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        List<TicketComment> comments = workflowService.getCommentsForTicket(ticket.getId());
        assertTrue(comments.stream().anyMatch(c -> c.getCommentText().contains("reopened")));
    }
}