package com.servicedesk;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.service.ReportingService;
import com.servicedesk.service.TicketService;
import com.servicedesk.service.UserService;
import com.servicedesk.service.WorkflowService;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  ServiceDesk: IT Helpdesk & Ticket Management    ");
        System.out.println("  Java Runtime Version: " + System.getProperty("java.version"));
        System.out.println("==================================================");

        // 1. Initialize Repositories
        UserRepository userRepository = new UserRepository();
        TicketRepository ticketRepository = new TicketRepository();

        // 2. Initialize Core Services
        UserService userService = new UserService(userRepository);
        TicketService ticketService = new TicketService(ticketRepository, userRepository);
        WorkflowService workflowService = new WorkflowService(ticketRepository, userRepository);
        ReportingService reportingService = new ReportingService(ticketRepository, userRepository);

        // 3. Seed Realistic System Users
        User emp1 = userService.registerUser("EMP-101", "Alice Johnson", "alice@company.com", Role.EMPLOYEE, "Finance");
        User emp2 = userService.registerUser("EMP-102", "David Miller", "david@company.com", Role.EMPLOYEE, "Marketing");
        User agent = userService.registerUser("AGT-201", "Sarah Helpdesk", "sarah@company.com", Role.HELPDESK_AGENT, "IT Support");
        User tech1 = userService.registerUser("TCH-301", "Marcus Tech", "marcus@company.com", Role.TECHNICIAN, "Hardware Engineering");
        User tech2 = userService.registerUser("TCH-302", "Elena Network", "elena@company.com", Role.TECHNICIAN, "Network Engineering");
        User admin = userService.registerUser("ADM-401", "Chief Admin", "admin@company.com", Role.ADMIN, "IT Infrastructure");

        // 4. Seed Tickets Across Lifecycles
        Ticket t1 = ticketService.createTicket("Corporate VPN Dropping Every 10 Minutes", 
                "OpenVPN client disconnects intermittently when connecting to production databases.", 
                emp1.getId(), Category.NETWORK, Priority.CRITICAL);

        Ticket t2 = ticketService.createTicket("DisplayPort Monitor Flickering", 
                "External 4K monitor goes blank every 30 seconds when connected via docking station.", 
                emp2.getId(), Category.HARDWARE, Priority.HIGH);

        Ticket t3 = ticketService.createTicket("Request Access to Docker Registry", 
                "Need read-write push credentials to internal Harbor container registry.", 
                emp1.getId(), Category.ACCESS_MANAGEMENT, Priority.MEDIUM);

        Ticket t4 = ticketService.createTicket("Outlook Desktop Sync Error 0x80040115", 
                "Emails are queued in outbox and not updating server folders.", 
                emp2.getId(), Category.SOFTWARE, Priority.LOW);

        // 5. Progress Workflows
        workflowService.assignTicket(t1.getId(), tech2.getId(), agent.getId());
        workflowService.startProgress(t1.getId(), tech2.getId());
        workflowService.assignTicket(t3.getId(), tech2.getId(), agent.getId());
        workflowService.assignTicket(t2.getId(), tech1.getId(), agent.getId());
        workflowService.startProgress(t2.getId(), tech1.getId());
        workflowService.resolveTicket(t2.getId(), tech1.getId(), "Docking station Thunderbolt firmware updated successfully.");

        workflowService.assignTicket(t4.getId(), tech1.getId(), admin.getId());
        workflowService.startProgress(t4.getId(), tech1.getId());
        workflowService.resolveTicket(t4.getId(), tech1.getId(), "Cleared local OST cache and renewed authentication token.");
        workflowService.closeTicket(t4.getId(), emp2.getId());

        // 6. Launch Embedded Web Dashboard Server
        try {
        	ServiceDeskServer server = new ServiceDeskServer(8080, ticketRepository, userRepository,
        	        ticketService, workflowService, reportingService);
        	server.start();
        } catch (Exception e) {
            System.err.println("Failed to start ServiceDesk web server: " + e.getMessage());
        }
    }
}