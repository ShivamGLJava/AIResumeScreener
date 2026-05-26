# Security Policy

This document outlines security procedures and policies for the `frontend` project.

## Reporting a Vulnerability

The project team takes security vulnerabilities seriously. We appreciate your efforts to responsibly disclose your findings, and will make every effort to acknowledge your contributions.

To report a security vulnerability, please email the project maintainers. You can find an example of how to structure this process in the security policy of our dependencies, such as `cookie`. [4] We will acknowledge your email within 48 hours and will send a more detailed response indicating the next steps in handling your report.

Please include the following information in your report:
- A description of the vulnerability and its impact.
- Steps to reproduce the vulnerability.
- Any proof-of-concept code.

We ask that you do not disclose the vulnerability publicly until we have had a chance to address it.

## Dependency Security

Our project relies on a number of open-source dependencies. We address security in our dependencies through the following practices:

### 1. Regular Audits

We keep our dependencies up-to-date to mitigate known vulnerabilities. High and critical severity vulnerabilities identified by `npm audit` are addressed immediately by updating the affected packages. This practice protects us from issues in transitive dependencies, such as the Regular Expression Denial of Service (ReDoS) vulnerability in older versions of `ansi-regex`.

### 2. Specific Dependency Vulnerability Mitigations

We have implemented the following mitigations for common vulnerabilities in the JavaScript ecosystem.

#### `jsdom`: Sandbox Escape
- **Vulnerability**: The `jsdom` package, used for running tests in a simulated browser environment, has a `runScripts: "dangerously"` option. If used with untrusted code, a malicious script could escape the sandbox and execute code on the machine running the tests. [6]
- **Mitigation**: Our project uses Jest and React Testing Library for tests, which rely on `jsdom`. We do not enable the `runScripts: "dangerously"` option and only execute our own trusted test code within the `jsdom` environment. This prevents the execution of arbitrary scripts from parsed HTML content. [6]

#### Unsafe Buffer Allocation
- **Vulnerability**: The legacy `new Buffer(number)` constructor in Node.js allocates uninitialized memory. If a number is passed to it based on user input, it can lead to the disclosure of sensitive information (private keys, user data) that may be present in that memory segment. [5]
- **Mitigation**: We follow the best practice of using the safer `Buffer.from()` and `Buffer.alloc()` APIs as recommended by packages like `safe-buffer`. [5] Our linting configuration, inherited from `react-scripts`, prevents the use of the deprecated and unsafe `new Buffer(number)` constructor.

## Secure Coding Practices

We use ESLint with TypeScript-specific rules to enforce secure coding practices and prevent common pitfalls. [3]

### TypeScript: `readonly` Property Integrity
- **Issue**: TypeScript's `readonly` modifier is a compile-time check and is not enforced at runtime. If a TypeScript library is consumed by a plain JavaScript project, `readonly` properties can be modified. [9]
- **Mitigation**: To ensure data integrity at runtime, we enforce the use of `getters` over `readonly` fields for literal class properties. This is managed by the `@typescript-eslint/class-literal-property-style` ESLint rule, which was added to our project's configuration. This guarantees that these values cannot be modified, even when the code is consumed by plain JavaScript. [4, 9]