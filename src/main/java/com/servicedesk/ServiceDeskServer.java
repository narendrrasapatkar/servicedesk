package com.servicedesk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.servicedesk.model.Category;
import com.servicedesk.model.Priority;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.service.ReportingService;
import com.servicedesk.service.TicketService;
import com.servicedesk.service.WorkflowService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceDeskServer {
    private final int port;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final WorkflowService workflowService;
    private final ReportingService reportingService;
    private final Gson gson;
    private HttpServer server;

    public ServiceDeskServer(int port, TicketRepository ticketRepository, UserRepository userRepository,
                             TicketService ticketService, WorkflowService workflowService,
                             ReportingService reportingService) {
        this.port = port;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.workflowService = workflowService;
        this.reportingService = reportingService;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/tickets", exchange -> {
            try {
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    String json = gson.toJson(ticketRepository.findAll());
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    sendResponse(exchange, 200, json);
                } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

                    String title = obj.get("title").getAsString();
                    String desc = obj.get("description").getAsString();
                    String userId = obj.get("userId").getAsString();
                    Category category = Category.valueOf(obj.get("category").getAsString());
                    Priority priority = Priority.valueOf(obj.get("priority").getAsString());

                    var created = ticketService.createTicket(title, desc, userId, category, priority);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    sendResponse(exchange, 201, gson.toJson(created));
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/api/workflow", exchange -> {
            try {
                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

                    String action = obj.get("action").getAsString();
                    String ticketId = obj.get("ticketId").getAsString();
                    String techId = obj.get("technicianId").getAsString();

                    if ("START".equalsIgnoreCase(action)) {
                        workflowService.startProgress(ticketId, techId);
                    } else if ("RESOLVE".equalsIgnoreCase(action)) {
                        workflowService.resolveTicket(ticketId, techId, "Resolved by technician via portal.");
                    }
                    sendResponse(exchange, 200, "{\"status\":\"success\"}");
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/api/metrics", exchange -> {
            try {
                String json = gson.toJson(reportingService.getDashboardMetrics());
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/api/users", exchange -> {
            try {
                String json = gson.toJson(userRepository.findAll());
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/") && !path.equals("/index.html")) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            sendResponse(exchange, 200, getDashboardHtml());
        });

        server.setExecutor(null);
        server.start();
        System.out.println(">>> ServiceDesk Portal live at: http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getDashboardHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>ServiceDesk Operations Center</title>
                <style>
                    * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 0; }
                    body { background: #0f172a; color: #f8fafc; padding: 28px; }
                    .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 24px; border-bottom: 1px solid #334155; }
                    .header h1 { font-size: 24px; font-weight: 700; color: #38bdf8; }
                    .badge-online { background: #166534; color: #4ade80; padding: 6px 14px; border-radius: 9999px; font-size: 13px; font-weight: 600; }
                    
                    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 24px 0; }
                    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 20px; }
                    .card .label { font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; font-weight: 600; }
                    .card .value { font-size: 30px; font-weight: 700; margin-top: 8px; color: #f1f5f9; }

                    .form-panel { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 24px; margin-bottom: 24px; }
                    .form-title { font-size: 18px; font-weight: 600; color: #38bdf8; margin-bottom: 16px; }
                    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 16px; }
                    label { display: block; font-size: 13px; color: #94a3b8; margin-bottom: 6px; font-weight: 500; }
                    input, select, textarea { width: 100%; background: #0f172a; border: 1px solid #334155; border-radius: 6px; padding: 10px 12px; color: #f8fafc; font-size: 14px; }
                    input:focus, select:focus, textarea:focus { outline: none; border-color: #38bdf8; }
                    .btn-submit { background: #0284c7; color: white; border: none; border-radius: 6px; padding: 12px 24px; font-weight: 600; cursor: pointer; }
                    .btn-submit:hover { background: #0369a1; }

                    .table-container { background: #1e293b; border: 1px solid #334155; border-radius: 8px; overflow: hidden; }
                    .table-header { padding: 16px 20px; font-weight: 600; font-size: 16px; border-bottom: 1px solid #334155; background: #1e293b; }
                    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }
                    th, td { padding: 14px 20px; border-bottom: 1px solid #334155; vertical-align: middle; }
                    th { background: #0f172a; color: #94a3b8; font-weight: 600; }
                    tr:hover { background: #243248; }
                    .status-tag { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
                    .status-NEW { background: #0369a1; color: #bae6fd; }
                    .status-ASSIGNED { background: #854d0e; color: #fef08a; }
                    .status-IN_PROGRESS { background: #6b21a8; color: #e9d5ff; }
                    .status-RESOLVED { background: #166534; color: #bbf7d0; }
                    .status-CLOSED { background: #334155; color: #cbd5e1; }
                    .priority-CRITICAL { color: #f87171; font-weight: 700; }
                    .priority-HIGH { color: #fb923c; font-weight: 600; }
                    .priority-MEDIUM { color: #facc15; }
                    .priority-LOW { color: #94a3b8; }
                    .btn-action { background: #334155; color: #f8fafc; border: 1px solid #475569; padding: 6px 12px; border-radius: 4px; font-size: 12px; cursor: pointer; }
                    .btn-action:hover { background: #475569; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <h1>ServiceDesk Operations Center</h1>
                        <p style="color: #64748b; font-size: 14px; margin-top: 4px;">Employee IT Support & Complaint Management System</p>
                    </div>
                    <span class="badge-online">&#x25CF; System Active (Java 21)</span>
                </div>

                <div class="metrics-grid">
                    <div class="card"><div class="label">Total Submitted</div><div class="value" id="m-total">0</div></div>
                    <div class="card"><div class="label">Active Workload</div><div class="value" id="m-active" style="color: #fbbf24;">0</div></div>
                    <div class="card"><div class="label">Resolved</div><div class="value" id="m-resolved" style="color: #4ade80;">0</div></div>
                    <div class="card"><div class="label">Closed Complete</div><div class="value" id="m-closed" style="color: #94a3b8;">0</div></div>
                </div>

                <div class="form-panel">
                    <div class="form-title">&#x1F4DD; Raise an IT Support Ticket</div>
                    <form id="raiseForm" onsubmit="submitTicket(event)">
                        <div class="form-grid">
                            <div>
                                <label for="userId">Employee / Reporter</label>
                                <select id="userId" required>
                                    <option value="EMP-101">Alice Johnson (Finance - EMP-101)</option>
                                    <option value="EMP-102">David Miller (Marketing - EMP-102)</option>
                                </select>
                            </div>
                            <div>
                                <label for="category">Issue Category</label>
                                <select id="category" required>
                                    <option value="HARDWARE">HARDWARE (Laptop, Monitor, Printer)</option>
                                    <option value="SOFTWARE">SOFTWARE (OS, Applications, Crashes)</option>
                                    <option value="NETWORK">NETWORK (Wi-Fi, VPN, LAN)</option>
                                    <option value="ACCESS_MANAGEMENT">ACCESS MANAGEMENT (Harbor, AWS, Passwords)</option>
                                    <option value="GENERAL_QUERY">GENERAL QUERY</option>
                                </select>
                            </div>
                            <div>
                                <label for="priority">Priority Level</label>
                                <select id="priority" required>
                                    <option value="LOW">LOW</option>
                                    <option value="MEDIUM" selected>MEDIUM</option>
                                    <option value="HIGH">HIGH</option>
                                    <option value="CRITICAL">CRITICAL</option>
                                </select>
                            </div>
                        </div>

                        <div style="margin-bottom: 16px;">
                            <label for="title">Issue Subject</label>
                            <input id="title" type="text" placeholder="e.g. MacBook Air screen flickering" minlength="5" required />
                        </div>

                        <div style="margin-bottom: 16px;">
                            <label for="description">Detailed Description</label>
                            <textarea id="description" rows="3" placeholder="Describe when the issue occurs..." minlength="10" required></textarea>
                        </div>

                        <button type="submit" class="btn-submit">&#x27A4; Submit IT Support Ticket</button>
                    </form>
                </div>

                <div class="table-container">
                    <div class="table-header">Registered Support Tickets</div>
                    <table>
                        <thead>
                            <tr>
                                <th>Ticket ID</th>
                                <th>Subject</th>
                                <th>Category</th>
                                <th>Priority</th>
                                <th>Status</th>
                                <th>Reporter</th>
                                <th>Technician</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="tickets-body">
                            <tr><td colspan="8" style="text-align: center; color: #64748b;">Loading tickets...</td></tr>
                        </tbody>
                    </table>
                </div>

                <script>
                    async function refreshData() {
                        try {
                            const [mRes, tRes] = await Promise.all([
                                fetch('/api/metrics').then(r => r.json()),
                                fetch('/api/tickets').then(r => r.json())
                            ]);

                            document.getElementById('m-total').innerText = mRes.totalTickets ?? 0;
                            document.getElementById('m-active').innerText = mRes.activeTickets ?? 0;
                            document.getElementById('m-resolved').innerText = mRes.resolvedTickets ?? 0;
                            document.getElementById('m-closed').innerText = mRes.closedTickets ?? 0;

                            const tbody = document.getElementById('tickets-body');
                            if (!tRes || tRes.length === 0) {
                                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #64748b;">No tickets found. Raise one above!</td></tr>';
                                return;
                            }

                            tbody.innerHTML = tRes.map(t => {
                                let actionBtn = '';
                                if (t.status === 'ASSIGNED') {
                                    actionBtn = `<button class="btn-action" onclick="transitionTicket('${t.id}', '${t.assignedTechnicianId}', 'START')">Start Work</button>`;
                                } else if (t.status === 'IN_PROGRESS') {
                                    actionBtn = `<button class="btn-action" style="background:#166534;border-color:#22c55e;" onclick="transitionTicket('${t.id}', '${t.assignedTechnicianId}', 'RESOLVE')">Resolve</button>`;
                                } else {
                                    actionBtn = `<span style="color:#64748b;font-size:12px;">None</span>`;
                                }

                                return `
                                    <tr>
                                        <td style="font-family: monospace; font-weight: 700; color: #38bdf8;">${t.id}</td>
                                        <td style="font-weight: 500;">${t.title}</td>
                                        <td>${t.category}</td>
                                        <td class="priority-${t.priority}">${t.priority}</td>
                                        <td><span class="status-tag status-${t.status}">${t.status}</span></td>
                                        <td>${t.createdByUserId}</td>
                                        <td>${t.assignedTechnicianId || '<span style="color:#64748b">Unassigned</span>'}</td>
                                        <td>${actionBtn}</td>
                                    </tr>
                                `;
                            }).join('');
                        } catch (err) {
                            console.error('Failed to load dashboard:', err);
                        }
                    }

                    async function submitTicket(e) {
                        e.preventDefault();
                        const payload = {
                            userId: document.getElementById('userId').value,
                            category: document.getElementById('category').value,
                            priority: document.getElementById('priority').value,
                            title: document.getElementById('title').value,
                            description: document.getElementById('description').value
                        };

                        const res = await fetch('/api/tickets', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        });

                        if (res.ok) {
                            document.getElementById('title').value = '';
                            document.getElementById('description').value = '';
                            refreshData();
                        } else {
                            const err = await res.json();
                            alert('Submission failed: ' + err.error);
                        }
                    }

                    async function transitionTicket(ticketId, techId, action) {
                        await fetch('/api/workflow', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ ticketId, technicianId: techId, action })
                        });
                        refreshData();
                    }

                    refreshData();
                    setInterval(refreshData, 3000);
                </script>
            </body>
            </html>
            """;
    }
}