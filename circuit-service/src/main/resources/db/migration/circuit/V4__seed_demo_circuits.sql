-- Demo circuits for a fresh installation, linked to the guides seeded by
-- guide-service (V2__seed_guides.sql): 1 = Youssef Benali (Agadir),
-- 2 = Salma Alaoui (Marrakech). Guide 3 is inactive, so it gets no circuit.
-- A database that already has circuits is left unchanged.
INSERT INTO circuit.circuits (titre, description, ville, duree_heures, prix, guide_id, disponible)
SELECT demo.titre, demo.description, demo.ville, demo.duree_heures, demo.prix, demo.guide_id, TRUE
FROM (VALUES
  ('Médina et Kasbah d''Agadir',
   'Montée à la Kasbah Agadir Oufella pour le panorama sur la baie, puis visite de la Médina de Coco Polizzi et de ses ateliers d''artisans.',
   'Agadir', 4, 250.00, 1),
  ('Souk El Had et front de mer',
   'Découverte du Souk El Had, ses épices et produits locaux, suivie d''une promenade guidée sur la corniche jusqu''à la marina.',
   'Agadir', 3, 180.00, 1),
  ('Vallée du Paradis',
   'Excursion d''une journée dans l''arrière-pays d''Agadir : palmeraies, gorges et piscines naturelles de la Vallée du Paradis.',
   'Agadir', 8, 450.00, 1),
  ('Médina de Marrakech',
   'Parcours à pied de la place Jemaa el-Fna aux souks, en passant par la médersa Ben Youssef et les fondouks historiques.',
   'Marrakech', 4, 300.00, 2),
  ('Jardins de Marrakech',
   'Visite du Jardin Majorelle et du Jardin Secret, avec l''histoire de leurs plantes, de leur architecture et de leurs créateurs.',
   'Marrakech', 3, 220.00, 2)
) AS demo (titre, description, ville, duree_heures, prix, guide_id)
WHERE NOT EXISTS (SELECT 1 FROM circuit.circuits);
