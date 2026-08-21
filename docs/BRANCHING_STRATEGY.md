# Git Branching Strategy


## 1. Purpose


This document defines the Git branching and naming strategy used for the Automated User Access Request Portal.


The strategy separates stable code, active development, feature development, bug fixes, and documentation changes.


---


## 2. Main Branch


Branch:


```text
main
Purpose

The main branch contains stable and production-ready versions of the application.

Only tested and verified changes should be merged into main.

3. Develop Branch

Branch:

develop
Purpose

The develop branch contains the latest integrated development code.

New features are developed and tested before being considered for the stable main branch.

4. Feature Branches

Naming convention:

feature/<feature-name>

Examples:

feature/access-request
feature/reviewer-dashboard
feature/status-tracking
feature/jenkins
feature/docker

Feature branches are created from develop.

After development and testing, they are merged back into develop.

5. Bug Fix Branches

Naming convention:

bugfix/<bug-name>

Examples:

bugfix/login-validation
bugfix/request-status
bugfix/security-config

Bug-fix branches are used to resolve issues discovered during development or testing.

6. Hotfix Branches

Naming convention:

hotfix/<fix-name>

Examples:

hotfix/security-vulnerability
hotfix/database-connection

Hotfix branches are used for urgent fixes affecting stable/production code.

7. Documentation Branches

Naming convention:

docs/<documentation-name>

Examples:

docs/readme
docs/branching-strategy
docs/devops-guide

Documentation branches are used for project documentation changes.

8. Branch Workflow

The general development workflow is:

main
  |
  | stable code
  |
develop
  |
  +---- feature/<name>
  |
  +---- bugfix/<name>
  |
  +---- docs/<name>
  |
  +---- hotfix/<name>

Normal feature workflow:

develop
   |
   v
feature/<feature-name>
   |
   | Development
   | Testing
   v
develop
   |
   | Release validation
   v
main
9. Branch Naming Rules

The following rules must be followed:

Use lowercase branch names.
Use hyphens to separate words.
Use the appropriate prefix.
Keep names short and descriptive.
Do not use spaces.
Do not use special characters unnecessarily.
Do not commit directly to main for normal feature development.
Valid
feature/access-request
feature/reviewer-dashboard
bugfix/login-validation
docs/readme
hotfix/security-fix
Invalid
MyNewFeature
feature Access Request
FEATURE_ACCESS_REQUEST
newbranch123
10. Summary
Branch	Purpose
main	Stable/production-ready code
develop	Active development
feature/*	New features
bugfix/*	Bug fixes
hotfix/*	Critical fixes
docs/*	Documentation

This branching strategy provides a consistent development workflow and prepares the repository for future CI/CD integration.