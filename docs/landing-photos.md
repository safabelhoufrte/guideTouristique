Landing-page photographs in `frontend/public/images`, served locally and downloaded from Unsplash:

- `morocco-zellige.jpg`: https://images.unsplash.com/photo-1539020140153-e479b8c22e70
- `morocco-kasbah.jpg`: image source: https://images.unsplash.com/photo-1489749798305-4fea3ae63d43

License: https://unsplash.com/license. The photographs illustrate Moroccan architecture and landscapes; they do not represent bookable circuits or verified destinations in the catalogue.

- `agadir-coast.jpg`: SnapSaga, https://unsplash.com/photos/a-beach-with-a-mountain-in-the-background-7MzuWztc8kY (https://images.unsplash.com/photo-1710092538995-4af0c11a3f3c).
- `marrakech-garden.jpg`: Beth Chobanova, https://unsplash.com/photos/courtyard-with-fountain-and-lush-tropical-plants-pz_KJnCoMyg (https://images.unsplash.com/photo-1772531073238-affd063538dc).

Circuit cards use these as labelled inspiration photographs because Circuit API does not currently contain image metadata. Public review cards use published Avis API records. If no published records exist, clearly labelled fictional examples preview the layout; these examples are never saved as customer reviews or shown on API failures.

## Demo circuit photos

Circuit Flyway migration `V4__seed_demo_circuits` stores one photo per demo circuit, from
`circuit-service/src/main/resources/db/demo/circuit/` (resized to 1000 px max, JPEG):

- `kasbah-agadir.jpg`: `agadir-coast.jpg` above.
- `souk-el-had.jpg`: diego fabra (armada42), https://unsplash.com/photos/colorful-spices-displayed-in-conical-piles-at-a-market-XeHV9a4ArdA (https://images.unsplash.com/photo-1769628702933-39ba968cb198).
- `vallee-du-paradis.jpg`: `morocco-kasbah.jpg` above.
- `medina-marrakech.jpg`: `morocco-zellige.jpg` above.
- `jardins-marrakech.jpg`: `marrakech-garden.jpg` above.

License: https://unsplash.com/license.
