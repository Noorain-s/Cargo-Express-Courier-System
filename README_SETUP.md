# Cargo Express Courier System — Setup

## What's new in this version
Payments now run in **simulated mode** (`payment.simulate=true` in
`application.properties`). Your real Razorpay keys are still in the file and
untouched — but instead of calling Razorpay's API (blocked until your account's
KYC clears), the Pay page shows its own mock checkout popup styled like
Razorpay's: card/UPI tabs, an OTP step, everything. Clicking through it marks
the payment PAID in your database, so invoices, the admin dashboard, and
analytics all work exactly like a real payment went through.

**Once your Razorpay KYC is approved**, switch back to the real gateway by
changing one line in `application.properties`:
```
payment.simulate=false
```
No other code changes needed — the real Razorpay integration code is all
still there, just bypassed while this flag is true.

## What's already done for you
- Maven Wrapper included — you do NOT need to install Maven separately
- Database credentials pre-filled with local defaults (root / me123456)
- Admin account auto-seeds on first run (admin@cargo.com / Admin@123)
- Razorpay test keys pre-filled (not currently used while simulate mode is on)

## What you still need on this computer (one-time)
1. **JDK 17** installed
2. **MySQL Server** running locally, with an empty database created:
   ```sql
   CREATE DATABASE cargo_courier_db;
   ```
   (If your MySQL root password isn't "me123456", edit it in
   `src/main/resources/application.properties`.)

## Running the app
Double-click **START_HERE.bat**.
- First time: downloads Maven and builds automatically (needs internet, a few minutes)
- Every time after: starts straight away
- Once you see "Started CargoCourierApplication", open:
  http://localhost:8081

Log in with admin@cargo.com / Admin@123, or register a new customer.

## Testing a payment (simulated)
1. Log in as a customer, book a shipment, choose "Pay Online"
2. On the Pay page, click **Pay Now** — the mock checkout modal opens
3. Enter anything in the card fields (nothing is validated) and click **Pay**
4. Click **Confirm Payment** on the OTP step (any OTP works)
5. You'll be redirected to the invoice — payment status shows PAID

## If you change application.properties
Delete the `target` folder and double-click START_HERE.bat again — it will
rebuild with your new settings before running.
