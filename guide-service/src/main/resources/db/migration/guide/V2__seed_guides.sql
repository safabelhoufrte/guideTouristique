INSERT INTO guide (nom, prenom, ville, email, telephone, tarif_par_jour, actif) VALUES
 ('Benali',  'Youssef', 'Agadir',    'youssef@example.com', '0600000000', 400, TRUE),
 ('Alaoui',  'Salma',   'Marrakech', 'salma@example.com',   '0600000001', 350, TRUE),
 ('Idrissi', 'Omar',    'Agadir',    'omar@example.com',    '0600000002', 450, FALSE);

INSERT INTO guide_langues (guide_id, langue) VALUES
 (1,'français'),(1,'anglais'),(1,'arabe'),
 (2,'français'),(2,'arabe'),
 (3,'anglais'),(3,'espagnol');