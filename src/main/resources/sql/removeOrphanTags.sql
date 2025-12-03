DELETE
FROM tags t
WHERE NOT EXISTS (SELECT 1
                  FROM product_tags pt
                  WHERE pt.tag_id = t.id)
RETURNING id;
