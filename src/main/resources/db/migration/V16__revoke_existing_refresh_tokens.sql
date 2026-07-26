UPDATE refresh_tokens
SET revoked = TRUE
WHERE revoked = FALSE;
