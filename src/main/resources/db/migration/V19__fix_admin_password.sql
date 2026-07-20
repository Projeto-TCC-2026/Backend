-- V19: Corrige o password_hash do usuário admin
-- senha em texto puro: password
-- hash gerado com BCryptPasswordEncoder() padrão (strength 10)
UPDATE users
SET password_hash = '$2a$10$.Tj3NVxr3Xllt.zNR3.c9e7z60OVvVM2k9Fs/8zUq/2O232ByHFeC'
WHERE email = 'admin@tcc.com';
