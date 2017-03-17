package eu.opentransportnet.thisway.interfaces;

/**
 * @author Kristaps Krumins
 */
public interface VolleyRequestListener<T> {
    void onResult(T object);

    void onError(T object);
}
