<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Account · Voice Browser</title>
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
            padding: 36px 30px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.45);
        }
        .header {
            text-align: center;
            margin-bottom: 26px;
        }
        .header .logo-mark {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 44px;
            height: 44px;
            border-radius: 12px;
            background: rgba(59, 130, 246, 0.12);
            border: 1px solid rgba(59, 130, 246, 0.3);
            color: var(--blue);
            font-size: 20px;
            margin-bottom: 14px;
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
            padding: 11px 14px;
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
            margin-bottom: 18px;
        }
        label {
            display: block;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--muted);
            margin-bottom: 7px;
        }
        input[type="text"], input[type="email"], input[type="password"] {
            width: 100%;
            background: var(--panel2);
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 12px 14px;
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
            margin-top: 6px;
        }
        .submit-btn:hover {
            opacity: 0.92;
        }
        .submit-btn:active {
            transform: scale(0.99);
        }
        .footer-links {
            text-align: center;
            margin-top: 22px;
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
        <div class="logo-mark">&#128100;</div>
        <h1>Create Account</h1>
        <p class="subtitle">Set up your credentials to access the Voice Browser</p>
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

        <button type="submit" class="submit-btn">Create Account</button>
    </form>

    <div class="footer-links">
        Already have an account? <a href="/login">Sign in here</a>
    </div>
</div>

</body>
</html>
