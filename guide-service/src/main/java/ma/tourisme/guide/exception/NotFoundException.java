
package ma.tourisme.guide.exception;

/**
 * Exception levée lorsqu'une ressource demandée
 * est introuvable dans le microservice Guide.
 *
 * <p>Elle est notamment utilisée lorsqu'un guide
 * n'existe pas dans la base de données.
 */
public class NotFoundException extends RuntimeException {

    /** Identifiant de sérialisation de l'exception. */
    private static final long serialVersionUID = 1L;

    /**
     * Crée une exception avec un message explicatif.
     *
     * @param message description de la ressource introuvable
     */
    public NotFoundException(final String message) {
        super(message);
    }
}
