// E2E Test for AUTH-01, AUTH-03, AUTH-04 with Isolated Temporary User
const API_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080/api/v1/auth';
const MAILPIT_URL = process.env.MAILPIT_URL || 'http://localhost:8025/api/v1';

function assert(condition, message) {
  if (!condition) {
    console.error('❌ ASSERTION FAILED:', message);
    process.exit(1);
  }
}

async function runTest() {
  const timestamp = Date.now();
  const email = `e2e_temp_${timestamp}@cozynest.vn`;
  const phone = '098' + Math.floor(1000000 + Math.random() * 9000000);
  const initialPassword = 'TempUserPass@2026';
  const newPassword = 'NewTempPass@2026';
  const changedPassword = 'ChangedPass@2026';

  console.log(`=== Step 1: Register Temporary Test User (${email}) ===`);
  const regRes = await fetch(`${API_URL}/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      phone,
      password: initialPassword,
      fullName: 'End-to-End Test User',
      role: 'TENANT'
    })
  });
  const regData = await regRes.json();
  assert(regRes.status === 201, `Expected HTTP 201 on register, got ${regRes.status}`);
  assert(regData?.data?.token, 'Expected token in register response');
  console.log('✅ Temporary user registered successfully.');

  try {
    console.log('=== Step 2: Request Password Reset OTP ===');
    const forgotRes = await fetch(`${API_URL}/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    const forgotData = await forgotRes.json();
    assert(forgotRes.status === 200, `Expected HTTP 200 on forgot-password, got ${forgotRes.status}`);
    console.log('✅ Forgot password OTP requested.');

    console.log('=== Step 3: Fetch OTP from Mailpit ===');
    await new Promise(r => setTimeout(r, 1200));
    const mailRes = await fetch(`${MAILPIT_URL}/messages`);
    const mailData = await mailRes.json();
    assert(Array.isArray(mailData.messages) && mailData.messages.length > 0, 'No emails found in Mailpit');

    // Find email addressed to our test user
    const userMessage = mailData.messages.find(m =>
      m.To && m.To.some(t => t.Address.toLowerCase() === email.toLowerCase())
    ) || mailData.messages[0];

    const msgDetailsRes = await fetch(`${MAILPIT_URL}/message/${userMessage.ID}`);
    const msgDetails = await msgDetailsRes.json();
    const otpMatch = (msgDetails.Text || msgDetails.Snippet || '').match(/\b\d{6}\b/);
    assert(otpMatch, '6-digit OTP not found in reset email body');
    const otpCode = otpMatch[0];
    console.log(`✅ Extracted OTP Code: ${otpCode}`);

    console.log('=== Step 4: Verify Wrong OTP Attempt Fails ===');
    const wrongRes = await fetch(`${API_URL}/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        otp: '999999',
        newPassword,
        confirmPassword: newPassword
      })
    });
    assert(wrongRes.status === 400, `Expected HTTP 400 for wrong OTP, got ${wrongRes.status}`);
    console.log('✅ Wrong OTP was correctly rejected.');

    console.log('=== Step 5: Submit Valid OTP with New Password ===');
    const resetRes = await fetch(`${API_URL}/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        otp: otpCode,
        newPassword,
        confirmPassword: newPassword
      })
    });
    assert(resetRes.status === 200, `Expected HTTP 200 for reset-password, got ${resetRes.status}`);
    console.log('✅ Password reset with OTP succeeded.');

    console.log('=== Step 6: Verify Old Password Login Fails ===');
    const oldLoginRes = await fetch(`${API_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password: initialPassword })
    });
    assert(oldLoginRes.status === 401, `Expected HTTP 401 for old password login, got ${oldLoginRes.status}`);
    console.log('✅ Old password login rejected.');

    console.log('=== Step 7: Verify New Password Login Succeeds ===');
    const newLoginRes = await fetch(`${API_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password: newPassword })
    });
    const newLoginData = await newLoginRes.json();
    assert(newLoginRes.status === 200, `Expected HTTP 200 for new password login, got ${newLoginRes.status}`);
    assert(newLoginData?.data?.token, 'Expected JWT access token in login response');
    const authToken = newLoginData.data.token;
    console.log('✅ New password login verified.');

    console.log('=== Step 8: Test Authenticated Change-Password ===');
    const changeRes = await fetch(`${API_URL}/change-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
      body: JSON.stringify({
        currentPassword: newPassword,
        newPassword: changedPassword
      })
    });
    assert(changeRes.status === 200, `Expected HTTP 200 on change-password, got ${changeRes.status}`);
    console.log('✅ Change-password flow succeeded.');
    console.log('🎉 ALL AUTH E2E ASSERTIONS PASSED (Demo data untouched)!');
  } catch (err) {
    console.error('❌ E2E Execution Error:', err);
    process.exit(1);
  }
}

runTest();
