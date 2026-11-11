# Email Deliverability & Transactional DNS Checklist (DEVOPS-02)

Required DNS & operational controls before enabling external production transactional SMTP (password reset, OTP fallback, contract notices, invoice receipts):

| Control | Record / Configuration | Owner | Status |
|---|---|---|---|
| SPF | `v=spf1 include:<smtp-provider> ~all` on sending domain (`mail.toam.vn`) | DevOps | `BLOCKED_HUMAN_REVIEW` (DNS access) |
| DKIM | 2048-bit RSA selector (`cn2026._domainkey.toam.vn`) rotated every 12 months | DevOps | `BLOCKED_HUMAN_REVIEW` |
| DMARC | `v=DMARC1; p=quarantine; rua=mailto:dmarc@toam.vn; adkim=s; aspf=s` | DevOps | `BLOCKED_HUMAN_REVIEW` |
| Sandbox Trap | Non-prod (`dev`, `demo`) routes all SMTP traffic to `Mailpit` (`localhost:1025`) | Engineering | Implemented |
| Bounce/Complaint Hook | Webhook marks bouncing addresses suppressed to protect sender reputation | Engineering/Ops | Planned for SMTP vendor selection |

