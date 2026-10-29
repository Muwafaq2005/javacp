<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="ai.typesafe.voicebrowser.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Voice Browser Portal · Welcome</title>
    <style>
        :root {
            --bg: #0b0f17;
            --panel: #111827;
            --panel2: #0f172a;
            --line: #1f2937;
            --text: #e5e7eb;
            --muted: #9ca3af;
            --accent: #f59e0b;
            --blue: #3b82f6;
            --green: #22c55e;
            --red: #ef4444;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
            padding: 30px 20px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .container {
            width: 100%;
            max-width: 820px;
        }
        header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 24px;
            padding-bottom: 16px;
            border-bottom: 1px solid var(--line);
        }
        .logo-group h1 {
            font-size: 20px;
            font-weight: 700;
        }
        .logo-group h1 span {
            color: var(--accent);
        }
        .badge {
            display: inline-block;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            color: var(--green);
            background: rgba(34, 197, 94, 0.12);
            border: 1px solid rgba(34, 197, 94, 0.3);
            padding: 3px 8px;
            border-radius: 999px;
            margin-left: 8px;
        }
        .actions {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            cursor: pointer;
            transition: all 0.15s;
        }
        .btn-primary {
            background: var(--accent);
            color: #111827;
            border: 1px solid var(--accent);
        }
        .btn-primary:hover {
            opacity: 0.9;
        }
        .btn-secondary {
            background: var(--panel2);
            color: var(--text);
            border: 1px solid var(--line);
        }
        .btn-secondary:hover {
            border-color: var(--muted);
        }
        .btn-danger {
            background: transparent;
            color: var(--red);
            border: 1px solid rgba(239, 68, 68, 0.4);
        }
        .btn-danger:hover {
            background: rgba(239, 68, 68, 0.1);
        }
        .hero-card {
            background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(17, 24, 39, 0.95));
            border: 1px solid rgba(245, 158, 11, 0.3);
            border-radius: 16px;
            padding: 24px 28px;
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 20px;
        }
        .hero-text h2 {
            font-size: 22px;
            margin-bottom: 6px;
            color: #fff;
        }
        .hero-text p {
            color: var(--muted);
            font-size: 14px;
            line-height: 1.5;
        }
        .launch-btn {
            background: var(--accent);
            color: #111;
            padding: 14px 22px;
            border-radius: 10px;
            font-size: 15px;
            font-weight: 700;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            white-space: nowrap;
            box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
            transition: transform 0.1s, box-shadow 0.15s;
        }
        .launch-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(245, 158, 11, 0.45);
        }
        .card {
            background: var(--panel);
            border: 1px solid var(--line);
            border-radius: 14px;
            padding: 20px;
            margin-bottom: 24px;
        }
        .card h3 {
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            color: var(--muted);
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }
        th, td {
            padding: 10px 12px;
            text-align: left;
            border-bottom: 1px solid var(--line);
        }
        th {
            color: var(--muted);
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            font-weight: 600;
        }
        td.mono {
            font-family: monospace;
            color: var(--accent);
        }
        .current-badge {
            background: rgba(34, 197, 94, 0.2);
            color: var(--green);
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 600;
            margin-left: 6px;
        }
        .tech-pill {
            background: var(--panel2);
            border: 1px solid var(--line);
            padding: 4px 8px;
            border-radius: 6px;
            font-size: 11px;
            color: var(--muted);
            display: inline-block;
            margin: 2px 4px 2px 0;
        }
    </style>
</head>
<body>

<%
    User user = (User) request.getAttribute("user");
    @SuppressWarnings("unchecked")
    List<User> allUsers = (List<User>) request.getAttribute("allUsers");
%>

<div class="container">
    <header>
        <div class="logo-group">
            <h1>Voice<span>Browser</span> <span class="badge">Session Active</span></h1>
        </div>
        <div class="actions">
            <span style="font-size: 13px; color: var(--muted);">Signed in as <b style="color: var(--text);"><%= user != null ? user.getUsername() : "Guest" %></b></span>
            <a href="/logout" class="btn btn-danger">Sign Out</a>
        </div>
    </header>

    <div class="hero-card">
        <div class="hero-text">
            <h2>Welcome back, <%= user != null ? user.getUsername() : "User" %>!</h2>
            <p>Your authentication session is backed by SQLite via raw <b>JDBC</b>, and rendered by <b>JSP</b>. Ready to control the headed browser by voice?</p>
            <div style="margin-top: 12px;">
                <span class="tech-pill">JSP 3.0</span>
                <span class="tech-pill">Spring Boot 3.3.4</span>
                <span class="tech-pill">SQLite JDBC</span>
                <span class="tech-pill">Playwright Java</span>
                <span class="tech-pill">Web Speech API</span>
            </div>
        </div>
        <div>
            <a href="/" class="launch-btn">
                <span>&#127897;&#65039; Launch Voice Browser</span>
                <span>&rarr;</span>
            </a>
        </div>
    </div>

    <div class="card">
        <h3>
            <span>&#128190; Users Persisted in SQLite Database (Via JDBC)</span>
            <span style="font-size: 11px; font-weight: normal; color: var(--muted);">Table: users (voicebrowser.db)</span>
        </h3>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Created At</th>
                </tr>
            </thead>
            <tbody>
                <% if (allUsers != null) {
                    for (User u : allUsers) {
                        boolean isCurrent = user != null && u.getId() == user.getId();
                %>
                    <tr style="<%= isCurrent ? "background: rgba(245, 158, 11, 0.05);" : "" %>">
                        <td class="mono">#<%= u.getId() %></td>
                        <td>
                            <b><%= u.getUsername() %></b>
                            <% if (isCurrent) { %>
                                <span class="current-badge">You</span>
                            <% } %>
                        </td>
                        <td style="color: var(--muted);"><%= u.getEmail() %></td>
                        <td style="color: var(--muted); font-size: 12px;"><%= u.getCreatedAt() != null ? u.getCreatedAt() : "Recently" %></td>
                    </tr>
                <%  }
                } %>
            </tbody>
        </table>
    </div>

    <div style="text-align: center; margin-top: 10px;">
        <a href="/" class="btn btn-secondary">&larr; Open Voice Browser Dashboard directly</a>
    </div>
</div>

</body>
</html>
