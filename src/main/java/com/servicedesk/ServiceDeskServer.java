package com.servicedesk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.service.ReportingService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceDeskServer {
    private final int port;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ReportingService reportingService;
    private final Gson gson;
    private HttpServer server;

    public ServiceDeskServer(int port, TicketRepository ticketRepository, UserRepository userRepository, ReportingService reportingService) {
        this.port = port;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
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
                String json = gson.toJson(ticketRepository.findAll());
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
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
        System.out.println(">>> Web Dashboard live at: http://localhost:" + port);
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
                    body { background: #0f172a; color: #f8fafc; padding: 24px; }
                    .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 24px; border-bottom: 1px solid #334155; }
                    .header h1 { font-size: 24px; font-weight: 700; color: #38bdf8; }
                    .badge-online { background: #166534; color: #4ade80; padding: 4px 12px; border-radius: 9999px; font-size: 13px; font-weight: 600; }
                    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 24px 0; }
                    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 20px; }
                    .card .label { font-size: 13px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; }
                    .card .value { font-size: 28px; font-weight: 700; margin-top: 8px; color: #f1f5f9; }
                    .table-container { background: #1e293b; border: 1px solid #334155; border-radius: 8px; overflow: hidden; margin-top: 24px; }
                    .table-header { padding: 16px 20px; font-weight: 600; font-size: 16px; border-bottom: 1px solid #334155; background: #1e293b; }
                    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }
                    th, td { padding: 14px 20px; border-bottom: 1px solid #334155; }
                    th { background: #0f172a; color: #94a3b8; font-weight: 600; }
                    tr:hover { background: #243248; }
                    .status-tag { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
                    .status-NEW { background: #0369a1; color: #bae6fd; }
                    .status-ASSIGNED { background: #854d0e; color: #fef08a; }
                    .status-IN_PROGRESS { background: #6b21a8; color: #e9d5ff; }
                    .status-RESOLVED { background: #166534; color: #bbf7d0; }
                    .status-CLOSED { background: #334155; color: #cbd5e1; }
                    .priority-CRITICAL { color: #f87171; font-weight: 700; }
                    .priority-HIGH { color: #fb923c; font-weight: 600; }
                    .priority-MEDIUM { color: #facc15; }
                    .priority-LOW { color: #94a3b8; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <h1>ServiceDesk Operations Center</h1>
                        <p style="color: #64748b; font-size: 14px; margin-top: 4px;">Real-Time IT Helpdesk & Ticket Management System</p>
                    </div>
                    <span class="badge-online">&#x25CF; Engine Running (Java 21)</span>
                </div>

                <div class="metrics-grid">
                    <div class="card"><div class="label">Total Submitted</div><div class="value" id="m-total">0</div></div>
                    <div class="card"><div class="label">Active Workload</div><div class="value" id="m-active" style="color: #fbbf24;">0</div></div>
                    <div class="card"><div class="label">Resolved</div><div class="value" id="m-resolved" style="color: #4ade80;">0</div></div>
                    <div class="card"><div class="label">Closed Complete</div><div class="value" id="m-closed" style="color: #94a3b8;">0</div></div>
                </div>

                <div class="table-container">
                    <div class="table-header">System Support Tickets</div>
                    <table>
                        <thead>
                            <tr>
                                <th>Ticket ID</th>
                                <th>Title</th>
                                <th>Category</th>
                                <th>Priority</th>
                                <th>Status</th>
                                <th>Creator</th>
                                <th>Assigned Tech</th>
                            </tr>
                        </thead>
                        <tbody id="tickets-body">
                            <tr><td colspan="7" style="text-align: center; color: #64748b;">Loading tickets...</td></tr>
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
                                tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: #64748b;">No tickets found.</td></tr>';
                                return;
                            }

                            tbody.innerHTML = tRes.map(t => `
                                <tr>
                                    <td style="font-family: monospace; font-weight: 600; color: #38bdf8;">${t.id}</td>
                                    <td>${t.title}</td>
                                    <td>${t.category}</td>
                                    <td class="priority-${t.priority}">${t.priority}</td>
                                    <td><span class="status-tag status-${t.status}">${t.status}</span></td>
                                    <td>${t.createdByUserId}</td>
                                    <td>${t.assignedTechnicianId || '<span style="color:#64748b">Unassigned</span>'}</td>
                                </tr>
                            `).join('');
                        } catch (err) {
                            console.error('Failed to load dashboard data:', err);
                        }
                    }

                    refreshData();
                    setInterval(refreshData, 3000);
                </script>
            </body>
            </html>
            """;
    }
}