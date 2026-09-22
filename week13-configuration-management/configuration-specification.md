# Week 13 – Configuration Management Specification

## Project
Automated User Access Request Portal

## Objective

The objective of Week 13 is to automate the configuration of the server environment required by the Automated User Access Request Portal using Ansible.

The configuration management setup ensures that required packages, application users, directories, configuration files, and environment prerequisites are created in a repeatable and consistent manner.

---

## 1. Target Environment

| Parameter | Configuration |
|---|---|
| Operating System | Ubuntu Linux on WSL 2 |
| Configuration Management Tool | Ansible |
| Ansible Version | 2.20.1 |
| Python Version | 3.14.4 |
| Target | Local Ubuntu WSL node |
| Connection Type | Local |
| Application Runtime | Java 17 |
| Container Runtime | Docker Desktop with WSL Integration |
| Docker Version | 29.5.2 |

The Ansible controller and target are the same Ubuntu WSL environment for the Week 13 demonstration.

---

## 2. Required Packages

The following packages were identified as prerequisites for the User Access Request Portal environment:

| Package | Purpose | Status |
|---|---|---|
| openjdk-17-jre | Java runtime required by the Spring Boot application | Installed |
| git | Version control and source-code management | Installed |
| curl | HTTP/API connectivity and verification | Installed |
| unzip | Extraction of compressed deployment artifacts | Installed |
| ca-certificates | Trusted SSL/TLS certificate support | Installed |

The packages were configured through the Ansible `apt` module.

---

## 3. Application User

An application-specific system user is created:

```text
Username: accessportal
Type: System user
Shell: /usr/sbin/nologin
