INSERT INTO users(username,
                  password,
                  email,
                  role,
                  status,
                  timezone)
VALUES (${owner}, '$2a$10$cfR6fkXvcxHt1wPb9BsJD.ELmm.lt9/tL4QUweDckSU.vopgIEcZS', 'owner', 'OWNER', 'ACTIVE', 'UTC');