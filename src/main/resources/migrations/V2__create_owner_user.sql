INSERT INTO users(username,
                  password,
                  email,
                  role,
                  status,
                  timezone)
VALUES (${owner}, '$2a$10$xsdVaZjCfYQdCLpBpI.rUOFQODpZvabf1I2sDXf2MdB7ULzusTzre', 'owner', 'OWNER', 'ACTIVE', 'UTC')