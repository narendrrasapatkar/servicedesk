package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TicketServiceTest {

    private TicketRepository ticketRepository;
    private UserRepository userRepository;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        userRepository = new UserRepository();
        ticketService = new TicketService(ticketRepository, userRepository);

        userRepository.save(new User("U101", "Bob Employee", "bob@corp.com", Role.EMPLOYEE, "Marketing"));
        userRepository.save(new User("U102", "Sarah Admin", "sarah@corp.com", Role.ADMIN, "IT"));
        userRepository.save(new User("U103", "Eve Outsider", "eve@corp.com", Role.EMPLOYEE, "Sales"));

        User inactive = new User("U104", "Ghost User", "ghost@corp.com", Role.EMPLOYEE, "HR");
        inactive.setActive(false);
        userRepository.save(inactive);
    }

    @Test
    @DisplayName("Should successfully create a ticket with valid inputs")
    void shouldCreateTicketSuccessfully() {
        Ticket ticket = ticketService.createTicket(
                "VPN Connectivity Failure",
                "Cannot connect to internal network from home office",
                "U101",
                Category.NETWORK,
                Priority.HIGH
        );

        assertNotNull(ticket);
        assertTrue(ticket.getId().startsWith("TCK-"));
        assertEquals("VPN Connectivity Failure", ticket.getTitle());
        assertEquals(TicketStatus.NEW, ticket.getStatus());
        assertEquals("U101", ticket.getCreatedByUserId());
    }

    @Test
    @DisplayName("Should reject ticket if title is too short (< 5 chars)")
    void shouldRejectShortTitle() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket("VPN", "Valid description that meets length requirement", "U101", Category.NETWORK, Priority.LOW)
        );

        assertTrue(ex.getMessage().contains("at least 5 characters"));
    }

    @Test
    @DisplayName("Should reject ticket if description is too short (< 10 chars)")
    void shouldRejectShortDescription() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket("VPN Disconnected", "Broken", "U101", Category.NETWORK, Priority.LOW)
        );

        assertTrue(ex.getMessage().contains("at least 10 characters"));
    }

    @Test
    @DisplayName("Should reject ticket creation by inactive user")
    void shouldRejectInactiveUserTicketCreation() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ticketService.createTicket("Monitor Flickering", "Monitor keeps blacking out every hour", "U104", Category.HARDWARE, Priority.MEDIUM)
        );

        assertTrue(ex.getMessage().contains("Inactive users cannot create"));
    }

    @Test
    @DisplayName("Should allow creator to cancel their own ticket")
    void shouldAllowCreatorToCancelTicket() {
        Ticket ticket = ticketService.createTicket("Printer Driver Error", "Need latest HP driver installed on laptop", "U101", Category.HARDWARE, Priority.LOW);

        ticketService.cancelTicket(ticket.getId(), "U101");

        Optional<Ticket> updated = ticketService.getTicketById(ticket.getId());
        assertTrue(updated.isPresent());
        assertEquals(TicketStatus.CANCELLED, updated.get().getStatus());
    }

    @Test
    @DisplayName("Should prevent non-creator, non-admin user from cancelling ticket")
    void shouldPreventUnauthorizedTicketCancellation() {
        Ticket ticket = ticketService.createTicket("Software License Expired", "IntelliJ license key renewal needed", "U101", Category.SOFTWARE, Priority.MEDIUM);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ticketService.cancelTicket(ticket.getId(), "U103")
        );

        assertTrue(ex.getMessage().contains("Only ticket creators or Admins"));
    }
}