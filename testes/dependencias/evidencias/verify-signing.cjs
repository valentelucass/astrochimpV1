// Confere a biblioteca de assinatura usada pelo Expo, com chaves descartáveis em memória.
const assert = require('node:assert/strict');
const { createRequire } = require('node:module');
const path = require('node:path');
const projectRequire = createRequire(path.join(process.cwd(), 'package.json'));
const expoRequire = createRequire(projectRequire.resolve('expo/package.json'));
const cliRequire = createRequire(expoRequire.resolve('@expo/cli'));
const signingPath = cliRequire.resolve('@expo/code-signing-certificates');
const signing = cliRequire('@expo/code-signing-certificates');
const signingRequire = createRequire(signingPath);
const forge = signingRequire('node-forge');

// As duas dependências do Expo devem usar a versão corrigida.
assert.equal(signingRequire('node-forge/package.json').version, '1.4.2');
assert.equal(cliRequire('node-forge/package.json').version, '1.4.2');
const keys = signing.generateKeyPair();
const pem = signing.convertKeyPairToPEM(keys);
const restoredKeys = signing.convertKeyPairPEMToKeyPair(pem);
assert.equal(keys.publicKey.n.toString(16), restoredKeys.publicKey.n.toString(16));
console.log('PASS: Expo CLI e assinatura resolvem node-forge 1.4.2; chaves PEM preservadas.');

// Um certificado temporário valida o fluxo de assinatura sem publicar nenhum artefato.
const now = Date.now();
const cert = signing.generateSelfSignedCodeSigningCertificate({
  keyPair: restoredKeys,
  validityNotBefore: new Date(now - 60_000),
  validityNotAfter: new Date(now + 3_600_000),
  commonName: 'AstroChimp teste local descartavel',
});
const certPEM = signing.convertCertificateToCertificatePEM(cert);
const restoredCert = signing.convertCertificatePEMToCertificate(certPEM);
signing.validateSelfSignedCertificate(restoredCert, restoredKeys);
const signature = signing.signBufferRSASHA256AndVerify(
  restoredKeys.privateKey, restoredCert, Buffer.from('AstroChimp verificacao local'),
);
assert.ok(Buffer.from(signature, 'base64').length > 0);
console.log('PASS: certificado Expo gerado, convertido, validado e assinatura RSA-SHA256 verificada.');

// A versão 1.4.2 restaura a leitura de Buffer alterada na versão 1.4.1.
const decoded = forge.pem.decode(Buffer.from(certPEM));
assert.equal(decoded[0].type, 'CERTIFICATE');
console.log('PASS: pem.decode aceita Buffer de certificado.');
