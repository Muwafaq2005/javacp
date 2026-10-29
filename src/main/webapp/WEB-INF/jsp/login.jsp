<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In · Voice Browser Portal</title>
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
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .auth-container {
            width: 100%;
            max-width: 420px;
            background: var(--panel);
            border: 1px solid var(--line);
            border-radius: 16px;
            padding: 32px 28px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
        }
        .header {
            text-align: center;
            margin-bottom: 24px;
        }
        .badge {
            display: inline-block;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.1em;
            color: var(--accent);
            background: rgba(245, 158, 11, 0.1);
            border: 1px solid rgba(245, 158, 11, 0.3);
            padding: 4px 10px;
            border-radius: 999px;
            margin-bottom: 12px;
        }
        h1 {
            font-size: 22px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 6px;
        }
        p.subtitle {
            font-size: 13px;
            color: var(--muted);
        }
        .alert {
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 18px;
            line-height: 1.4;
        }
        .alert-error {
            background: rgba(239, 68, 68, 0.15);
            border: 1px solid rgba(239, 68, 68, 0.4);
            color: #fca5a5;
        }
        .alert-success {
            background: rgba(34, 197, 94, 0.15);
            border: 1px solid rgba(34, 197, 94, 0.4);
            color: #86efac;
        }
        .alert-info {
            background: rgba(59, 130, 246, 0.15);
            border: 1px solid rgba(59, 130, 246, 0.4);
            color: #93c5fd;
        }
        .form-group {
            margin-bottom: 16px;
        }
        label {
            display: block;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--muted);
            margin-bottom: 6px;
        }
        input[type="text"], input[type="password"] {
            width: 100%;
            background: var(--panel2);
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 11px 14px;
            font-size: 14px;
            color: var(--text);
            transition: border-color 0.15s, box-shadow 0.15s;
        }
        input:focus {
            outline: none;
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
        }
        .submit-btn {
            width: 100%;
            background: var(--accent);
            color: #111827;
            font-size: 14px;
            font-weight: 700;
            padding: 12px;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: opacity 0.15s, transform 0.05s;
            margin-top: 8px;
        }
        .submit-btn:hover {
            opacity: 0.92;
        }
        .submit-btn:active {
            transform: scale(0.99);
        }
        .footer-links {
            text-align: center;
            margin-top: 20px;
            font-size: 13px;
            color: var(--muted);
        }
        .footer-links a {
            color: var(--accent);
            text-decoration: none;
            font-weight: 600;
        }
        .footer-links a:hover {
            text-decoration: underline;
        }
        .demo-credentials {
            margin-top: 18px;
            padding-top: 14px;
            border-top: 1px solid var(--line);
            font-size: 12px;
            color: var(--muted);
            display: flex;
            justify-content: space-between;
        }
        .demo-credentials code {
            color: var(--text);
            background: var(--panel2);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
        }
    </style>
</head>
<body>

<div class="auth-container">
    <div class="header">
        <span class="badge">JSP &middot; Spring MVC &middot; SQLite JDBC</span>
        <h1>Voice Browser Portal</h1>
        <p class="subtitle">Sign in to manage and run voice browser operations</p>
    </div>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="alert alert-error">
            <%= request.getAttribute("errorMessage") %>
        </div>
    <% } %>

    <% if (request.getAttribute("successMessage") != null) { %>
        <div class="alert alert-success">
            <%= request.getAttribute("successMessage") %>
        </div>
    <% } %>

    <% if (request.getAttribute("infoMessage") != null) { %>
        <div class="alert alert-info">
            <%= request.getAttribute("infoMessage") %>
        </div>
    <% } %>

    <form action="/login" method="POST">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" value="<%= request.getAttribute("username") != null ? request.getAttribute("username") : "" %>" required autofocus placeholder="Enter your username">
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required placeholder="Enter your password">
        </div>

        <button type="submit" class="submit-btn">Sign In</button>
    </form>

    <div class="demo-credentials">
        <span>Default JDBC Seed:</span>
        <span><code>admin</code> / <code>admin123</code></span>
    </div>

    <div class="footer-links">
        Don't have an account? <a href="/signup">Sign up now</a>
        <br><br>
        <a href="/" style="color: var(--muted); font-size: 12px;">&larr; Return to Voice Control Dashboard</a>
    </div>
</div>

</body>
</html>
