<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Account · Voice Browser Portal</title>
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
            max-width: 440px;
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
            color: var(--blue);
            background: rgba(59, 130, 246, 0.1);
            border: 1px solid rgba(59, 130, 246, 0.3);
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
        input[type="text"], input[type="email"], input[type="password"] {
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
            border-color: var(--blue);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
        }
        .submit-btn {
            width: 100%;
            background: var(--blue);
            color: #ffffff;
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
            color: var(--blue);
            text-decoration: none;
            font-weight: 600;
        }
        .footer-links a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>

<div class="auth-container">
    <div class="header">
        <span class="badge">JSP &middot; Spring MVC &middot; SQLite JDBC</span>
        <h1>Create Account</h1>
        <p class="subtitle">Register a new profile in the SQLite database</p>
    </div>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="alert alert-error">
            <%= request.getAttribute("errorMessage") %>
        </div>
    <% } %>

    <form action="/signup" method="POST">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" value="<%= request.getAttribute("username") != null ? request.getAttribute("username") : "" %>" required autofocus placeholder="Choose a unique username">
        </div>

        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email" value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>" required placeholder="you@domain.com">
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required placeholder="Create a strong password">
        </div>

        <div class="form-group">
            <label for="confirmPassword">Confirm Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword" required placeholder="Re-enter your password">
        </div>

        <button type="submit" class="submit-btn">Create Account (via JDBC)</button>
    </form>

    <div class="footer-links">
        Already have an account? <a href="/login">Sign in here</a>
        <br><br>
        <a href="/" style="color: var(--muted); font-size: 12px;">&larr; Return to Voice Control Dashboard</a>
    </div>
</div>

</body>
</html>
