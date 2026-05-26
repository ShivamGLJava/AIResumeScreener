# Record of Security Issues and Mitigations

This document tracks the security vulnerabilities that have been identified in the project and the steps taken to remediate them.

---

### 1. Outdated Dependencies and Transitive Vulnerabilities

*   **Issue:** An audit revealed that several dependencies were not on their latest versions. This exposed the project to potential downstream vulnerabilities from both direct and transitive dependencies, such as the known Regular Expression Denial of Service (ReDoS) vulnerability in older versions of the `ansi-regex` package.

*   **Fix Applied:**
    *   All dependencies listed in `package.json` were updated to their latest stable and secure versions.
    *   The `SECURITY.md` policy was updated to mandate regular dependency audits using `npm audit` to catch and remediate future vulnerabilities promptly.

---

### 2. Insecure `readonly` Properties in TypeScript

*   **Issue:** The use of TypeScript's `readonly` modifier for class properties only provides compile-time safety. These properties could be modified at runtime if the library were consumed by a plain JavaScript project, leading to potential data integrity issues and unpredictable behavior.

*   **Fix Applied:**
    *   We hardened our static analysis by adding `@typescript-eslint/parser` and `@typescript-eslint/eslint-plugin` to our `devDependencies`.
    *   The ESLint configuration in `package.json` was updated to enforce the `@typescript-eslint/class-literal-property-style` rule, requiring the use of `getters` for literal class properties. This ensures properties are truly read-only at runtime.

---

### 3. Potential for `jsdom` Sandbox Escape

*   **Issue:** The `jsdom` library, used for testing, includes a `runScripts: "dangerously"` option. If ever used with untrusted HTML content, it could allow a malicious script to escape the test sandbox and execute code on the host machine.

*   **Fix Applied:**
    *   While not actively exploited, the risk was acknowledged. Our test suite was reviewed to confirm that this dangerous option is not in use.
    *   The `SECURITY.md` policy was updated to explicitly forbid the use of `runScripts: "dangerously"`, ensuring developers are aware of the risk.

---

These actions have made our project more robust and secure against these identified threats.