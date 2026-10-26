const crypto = require('node:crypto');

const TOKEN_TTL_SECONDS = 60 * 60 * 24 * 7;

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}

function verifyPassword(password, stored) {
  if (!stored || !stored.startsWith('scrypt$')) return false;
  const [, salt, expectedHex] = stored.split('$');
  const actual = crypto.scryptSync(password, salt, 64);
  const expected = Buffer.from(expectedHex, 'hex');
  return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
}

function encode(value) {
  return Buffer.from(JSON.stringify(value)).toString('base64url');
}

function signToken(user, secret) {
  const now = Math.floor(Date.now() / 1000);
  const header = encode({ alg: 'HS256', typ: 'JWT' });
  const payload = encode({ sub: user.id, role: user.role, name: user.full_name, iat: now, exp: now + TOKEN_TTL_SECONDS });
  const signature = crypto.createHmac('sha256', secret).update(`${header}.${payload}`).digest('base64url');
  return { token: `${header}.${payload}.${signature}`, expiresIn: TOKEN_TTL_SECONDS };
}

function verifyToken(token, secret) {
  const parts = String(token || '').split('.');
  if (parts.length !== 3) return null;
  const [header, payload, signature] = parts;
  const expected = crypto.createHmac('sha256', secret).update(`${header}.${payload}`).digest('base64url');
  const left = Buffer.from(signature);
  const right = Buffer.from(expected);
  if (left.length !== right.length || !crypto.timingSafeEqual(left, right)) return null;
  try {
    const parsed = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    if (!parsed.exp || parsed.exp < Math.floor(Date.now() / 1000)) return null;
    return parsed;
  } catch {
    return null;
  }
}

function authMiddleware(secret, required = true) {
  return (req, res, next) => {
    const value = req.headers.authorization || '';
    const payload = value.startsWith('Bearer ') ? verifyToken(value.slice(7), secret) : null;
    if (payload) req.auth = payload;
    if (required && !payload) return res.status(401).json({ error: 'Debes iniciar sesión.' });
    next();
  };
}

module.exports = { hashPassword, verifyPassword, signToken, verifyToken, authMiddleware };
