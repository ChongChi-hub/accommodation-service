# Visual Design Verification & Self-Critique: [APPT-02] Landlord Appointments Dashboard

## 1. Screenshots Inventory
| View / Screen | File Name | Resolution | Theme |
| :--- | :--- | :--- | :--- |
| **List View (Desktop)** | `appointments-list-desktop-1440.png` | 1440 x 900 | Light |
| **List View (Tablet Landscape)** | `appointments-list-tablet-1024.png` | 1024 x 768 | Light |
| **List View (Tablet Portrait)** | `appointments-list-tablet-768.png` | 768 x 1024 | Light |
| **List View (Mobile)** | `appointments-list-mobile-390.png` | 390 x 844 | Light |
| **List View (Dark Mode)** | `appointments-list-dark-1440.png` | 1440 x 900 | Dark |
| **By-Room Grouped View** | `appointments-by-room-desktop-1440.png` | 1440 x 900 | Light |
| **Calendar Schedule View** | `appointments-calendar-desktop-1440.png` | 1440 x 900 | Light |
| **Detail Drawer with Timeline** | `appointments-detail-drawer.png` | 1440 x 900 | Light |
| **Confirm Appointment Modal** | `appointments-confirm-modal.png` | 1440 x 900 | Light |
| **Cancel / Decline Modal** | `appointments-cancel-modal.png` | 1440 x 900 | Light |
| **Complete Outcome Modal** | `appointments-complete-modal.png` | 1440 x 900 | Light |

---

## 2. Three-Point Self-Critique per View

### A. List View (Desktop, Tablet, Mobile, Dark Mode)
1. **Visual Hierarchy & Spacing:** Top summary cards provide immediate operational insight (pending, confirmed, needing outcome, today, completed). Responsive breakpoints seamlessly scale from multi-column grid on desktop to full-width card layout on mobile without clipping.
2. **Information Architecture & Privacy:** Tenant phone numbers are strictly masked (`090••••567`) for pending/cancelled appointments and revealed with clickable `tel:` links only for confirmed/completed appointments. Status badges use standard semantic colors.
3. **Edge Cases & Dark Mode:** Past confirmed appointments immediately surface "Cập nhật kết quả" primary action. Dark mode maintains WCAG AA contrast (4.5:1+) across slate and stone tones.

### B. By-Room View
1. **Visual Hierarchy & Spacing:** Collapsible accordion header highlights room title, address, and quick badge chips showing both pending count and total appointments per property.
2. **Operational Efficiency:** Landlords managing multiple properties can triage appointments room-by-room, avoiding context switching.
3. **Edge Cases:** Empty rooms or rooms with no pending bookings maintain clean badges and collapsed states.

### C. Calendar View
1. **Visual Hierarchy & Spacing:** 7-day schedule grid with current day clearly highlighted in teal.
2. **Time Clarity:** Appointment chips prominently display 24-hour time chips, room snippet, and status.
3. **Interactivity:** One-click on any appointment chip opens the comprehensive detail drawer without page reloads.

### D. Detail Drawer & Action Modals
1. **Detail Drawer:** Full audit trail timeline displays chronological state transitions (`fromStatus -> toStatus`), actor role, and notes. Includes quick copy-to-clipboard for phone numbers on confirmed appointments.
2. **Confirm Modal:** Gracefully handles time conflicts (`TimeConflictException` 409) by displaying an amber alert with the conflicting room and time, accompanied by a explicit `force: true` checkbox.
3. **Cancel & Complete Modals:** Cancellation modal enforces radio reason selection and requires >= 10 characters when reason is `OTHER`. Completion modal offers intuitive choice cards between successful viewing and no-show with landlord notes.
