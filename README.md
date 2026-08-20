# Automated User Access Request Portal

A Spring Boot based web application for managing user access requests through a controlled workflow involving employees and reviewers. The project is designed as a DevOps-oriented application demonstrating version control, CI/CD, automated testing, containerization, configuration management, and automated deployment.

---

## 1. Project Overview

The Automated User Access Request Portal provides a centralized system through which employees can request access to organizational resources.

Instead of handling access requests manually through emails or informal communication, the portal provides a structured workflow:

Employee → Access Request → Review → Approval/Rejection → Status Tracking

The application also serves as the base application for implementing a complete DevOps lifecycle using GitHub, Jenkins, Selenium, Docker, and Ansible.

---

## 2. Problem Statement

Organizations often handle user access requests manually through emails, spreadsheets, or other disconnected processes. This can result in:

- Delayed approvals
- Lack of request visibility
- Manual tracking
- Inconsistent approval processes
- Difficulty maintaining request history
- Limited automation

The proposed system provides a centralized web-based platform for submitting, reviewing, approving/rejecting, and tracking access requests.

---

## 3. Objectives

The main objectives of the project are:

1. Provide secure employee authentication.
2. Allow employees to submit access requests.
3. Store access requests in a centralized PostgreSQL database.
4. Allow reviewers to view pending requests.
5. Allow reviewers to approve or reject requests.
6. Allow employees to track request status.
7. Maintain request and review information.
8. Implement an automated DevOps lifecycle.
9. Automate build, testing, containerization, and deployment.

---

## 4. User Roles

### Employee

Employees can:

- Login securely
- Submit access requests
- Specify required access type
- Provide justification
- View submitted requests
- Track request status
- Logout

### Reviewer

Reviewers can:

- Login securely
- View pending requests
- View request details
- Approve requests
- Reject requests
- Add reviewer remarks
- Logout

---

## 5. Core MVP Workflow

```text
Employee
   |
   v
Login
   |
   v
Employee Dashboard
   |
   v
Submit Access Request
   |
   v
Request Stored in PostgreSQL
   |
   v
Status = PENDING
   |
   v
Reviewer Dashboard
   |
   v
Review Request
   |
   +------------+
   |            |
   v            v
APPROVED      REJECTED
   |            |
   +------------+
        |
        v
Employee Status Tracking