import { Circuit } from './circuit';

const photos = {
  coast: {
    src: '/images/agadir-coast.jpg',
    alt: 'La plage d’Agadir et sa colline en arrière-plan',
  },
  heritage: {
    src: '/images/morocco-zellige.jpg',
    alt: 'Fontaine marocaine ornée de zellige et de plâtre sculpté',
  },
  garden: {
    src: '/images/marrakech-garden.jpg',
    alt: 'Un jardin de Marrakech avec une fontaine et une végétation luxuriante',
  },
  landscape: {
    src: '/images/morocco-kasbah.jpg',
    alt: 'Maisons de terre ocre devant une palmeraie et les montagnes',
  },
};

/** Editorial inspiration photos until Circuit API provides its own image metadata. */
export function circuitPhoto(circuit: Pick<Circuit, 'titre' | 'ville'>) {
  const title = circuit.titre
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
  if (title.includes('jardin')) return photos.garden;
  if (title.includes('vallee')) return photos.landscape;
  if (title.includes('medina') || title.includes('kasbah')) return photos.heritage;
  return circuit.ville?.toLowerCase() === 'agadir' ? photos.coast : photos.heritage;
}
