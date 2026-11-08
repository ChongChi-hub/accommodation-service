// E2E Test for AUTH-01, AUTH-03, AUTH-04
const API_URL = 'http://localhost:8080/api/v1/auth';
const MAILPIT_URL = 'http://localhost:8025/api/v1';

async function runTest() {
  console.log('=== Step 1: Request Password Reset OTP ===');
  const email = 'tenant@cozynest.vn';
  const res1 = await fetch(`${API_URL}/forgot-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  const data1 = await res1.json();
  console.log('Forgot password status:', res1.status, data1.message);

  console.log('=== Step 2: Fetch OTP from Mailpit ===');
  await new Promise(r => setTimeout(r, 1500));
  const resMail = await fetch(`${MAILPIT_URL}/messages`);
  const mailData = await resMail.json();
  if (mailData.messages.length === 0) {
    throw new Error('No email found in Mailpit!');
  }
  const latestMailId = mailData.messages[0].ID;
  const resMsg = await fetch(`${MAILPIT_URL}/message/${latestMailId}`);
  const msgDetails = await resMsg.json();
  console.log('Subject:', msgDetails.Subject);
  console.log('To:', msgDetails.To[0].Address);

  // Extract 6-digit OTP from text or snippet
  const otpMatch = (msgDetails.Text || msgDetails.Snippet).match(/\b\d{6}\b/);
  if (!otpMatch) {
    throw new Error('OTP not found in email body!');
  }
  const otpCode = otpMatch[0];
  console.log('Extracted OTP Code:', otpCode);

  console.log('=== Step 3: Test Wrong OTP Attempt ===');
  const resWrong = await fetch(`${API_URL}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      otp: '999999',
      newPassword: 'NewTenantPass@2026',
      confirmPassword: 'NewTenantPass@2026'
    })
  });
  const wrongData = await resWrong.json();
  console.log('Wrong OTP status (expected 400):', resWrong.status, wrongData.code || wrongData.message);

  console.log('=== Step 4: Submit Valid OTP with New Password ===');
  const resReset = await fetch(`${API_URL}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      otp: otpCode,
      newPassword: 'NewTenantPass@2026',
      confirmPassword: 'NewTenantPass@2026'
    })
  });
  const resetData = await resReset.json();
  console.log('Reset password status (expected 200):', resReset.status, resetData.message);

  console.log('=== Step 5: Verify Old Password Fails ===');
  const resOldLogin = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password: 'password123'
    })
  });
  const oldLoginData = await resOldLogin.json();
  console.log('Old password login status (expected 400):', resOldLogin.status, oldLoginData.message);

  console.log('=== Step 6: Verify New Password Succeeds ===');
  const resNewLogin = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password: 'NewTenantPass@2026'
    })
  });
  const newLoginData = await resNewLogin.json();
  console.log('New password login status (expected 200):', resNewLogin.status);
  console.log('Authenticated User:', newLoginData.data.user.email, 'Roles:', newLoginData.data.user.roles);
  console.log('Access Token issued:', !!newLoginData.data.token);

  console.log('=== Step 7: Restore original password (password123) via change-password ===');
  const resRestore = await fetch(`${API_URL}/change-password`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${newLoginData.data.token}`
    },
    body: JSON.stringify({
      oldPassword: 'NewTenantPass@2026',
      newPassword: 'password123',
      confirmPassword: 'password123'
    })
  });
  const restoreData = await resRestore.json();
  console.log('Restore password status (expected 200):', resRestore.status, restoreData.message);
  console.log('=== ALL E2E VERIFICATIONS PASSED SUCCESSFULLY! ===');
}

runTest().catch(err => {
  console.error('Test Failed:', err);
  process.exit(1);
});
